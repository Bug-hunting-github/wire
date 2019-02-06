/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import com.squareup.wire.java.JavaGenerator
import com.squareup.wire.java.ProfileLoader
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.schema.IdentifierSet
import com.squareup.wire.schema.SchemaLoader
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import okio.buffer
import okio.source
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Command line interface to the Wire Java generator.
 *
 * Usage
 * -----
 *
 * ```
 * java WireCompiler --proto_path=<path>
 *   [--java_out=<path>]
 *   [--kotlin_out=<path>]
 *   [--files=<protos.include>]
 *   [--includes=<message_name>[,<message_name>...]]
 *   [--excludes=<message_name>[,<message_name>...]]
 *   [--quiet]
 *   [--dry_run]
 *   [--android]
 *   [--android-annotations]
 *   [--compact]
 *   [file [file...]]
 * ```
 *
 * `--java_out` should provide the folder where the files generated by the Java code generator
 * should be placed. Similarly, `--kotlin_out` should provide the folder where the files generated
 * by the Kotlin code generator will be written. Only one of the two should be specified.
 *
 * If the `--includes` flag is present, its argument must be a comma-separated list of
 * fully-qualified message or enum names. The output will be limited to those messages and enums
 * that are (transitive) dependencies of the listed names. The `--excludes` flag excludes types, and
 * takes precedence over `--includes`.
 *
 * If the `--registry_class` flag is present, its argument must be a Java class name. A class with
 * the given name will be generated, containing a constant list of all extension classes generated
 * during the compile. This list is suitable for passing to Wire's constructor at runtime for
 * constructing its internal extension registry.
 *
 * If `--quiet` is specified, diagnostic messages to stdout are suppressed.
 *
 * The `--dry_run` flag causes the compile to just emit the names of the source files that would be
 * generated to stdout.
 *
 * The `--android` flag will cause all messages to implement the `Parcelable`
 * interface. This implies `--android-annotations` as well.
 *
 * The `--android-annotations` flag will add the `Nullable` annotation to optional fields.
 *
 * The `--compact` flag will emit code that uses reflection for reading, writing, and
 * toString methods which are normally implemented with code generation.
 */
class WireCompiler internal constructor(
  val fs: FileSystem,
  val log: WireLogger,
  val protoPaths: List<String>,
  val javaOut: String?,
  val kotlinOut: String?,
  val sourceFileNames: List<String>,
  val identifierSet: IdentifierSet,
  val dryRun: Boolean,
  val namedFilesOnly: Boolean,
  val emitAndroid: Boolean,
  val emitAndroidAnnotations: Boolean,
  val emitCompact: Boolean,
  val javaInterop: Boolean
) {

  @Throws(IOException::class)
  fun compile() {
    val schemaLoader = SchemaLoader()
    for (protoPath in protoPaths) {
      schemaLoader.addSource(fs.getPath(protoPath))
    }
    for (sourceFileName in sourceFileNames) {
      schemaLoader.addProto(sourceFileName)
    }
    var schema = schemaLoader.load()

    if (!identifierSet.isEmpty) {
      log.info("Analyzing dependencies of root types.")
      schema = schema.prune(identifierSet)
      for (rule in identifierSet.unusedIncludes()) {
        log.info("Unused include: $rule")
      }
      for (rule in identifierSet.unusedExcludes()) {
        log.info("Unused exclude: $rule")
      }
    }

    /** Queue which can contain both [Type]s and [Service]s. */
    val queue = ConcurrentLinkedQueue<Any>()
    for (protoFile in schema.protoFiles()) {
      // Check if we're skipping files not explicitly named.
      if (!sourceFileNames.isEmpty() && protoFile.location().path() !in sourceFileNames) {
        if (namedFilesOnly || protoFile.location().path() == DESCRIPTOR_PROTO) continue
      }
      queue.addAll(protoFile.types())
      queue.addAll(protoFile.services())
    }

    val executor = Executors.newCachedThreadPool()
    val futures = ArrayList<Future<Unit>>(MAX_WRITE_CONCURRENCY)

    when {
      javaOut != null -> {
        val profileName = if (emitAndroid) "android" else "java"
        val profile = ProfileLoader(profileName)
            .schema(schema)
            .load()

        val javaGenerator = JavaGenerator.get(schema)
            .withProfile(profile)
            .withAndroid(emitAndroid)
            .withAndroidAnnotations(emitAndroidAnnotations)
            .withCompact(emitCompact)

        // No services for Java.
        val types = ConcurrentLinkedQueue<Type>(queue.filterIsInstance<Type>())
        for (i in 0 until MAX_WRITE_CONCURRENCY) {
          val task = JavaFileWriter(javaOut, javaGenerator, types, dryRun, fs, log)
          futures.add(executor.submit(task))
        }
      }

      kotlinOut != null -> {
        val kotlinGenerator = KotlinGenerator(schema, emitAndroid, javaInterop)

        for (i in 0 until MAX_WRITE_CONCURRENCY) {
          val task = KotlinFileWriter(kotlinOut, kotlinGenerator, queue, fs, log, dryRun)
          futures.add(executor.submit(task))
        }
      }

      else -> throw AssertionError()
    }

    executor.shutdown()

    try {
      for (future in futures) {
        future.get()
      }
    } catch (e: ExecutionException) {
      throw IOException(e.message, e)
    } catch (e: InterruptedException) {
      throw RuntimeException(e.message, e)
    }
  }

  companion object {
    const val CODE_GENERATED_BY_WIRE =
        "Code generated by Wire protocol buffer compiler, do not edit."

    private const val PROTO_PATH_FLAG = "--proto_path="
    private const val JAVA_OUT_FLAG = "--java_out="
    private const val KOTLIN_OUT_FLAG = "--kotlin_out="
    private const val FILES_FLAG = "--files="
    private const val INCLUDES_FLAG = "--includes="
    private const val EXCLUDES_FLAG = "--excludes="
    private const val QUIET_FLAG = "--quiet"
    private const val DRY_RUN_FLAG = "--dry_run"
    private const val NAMED_FILES_ONLY = "--named_files_only"
    private const val ANDROID = "--android"
    private const val ANDROID_ANNOTATIONS = "--android-annotations"
    private const val COMPACT = "--compact"
    private const val JAVA_INTEROP = "--java_interop"
    private const val MAX_WRITE_CONCURRENCY = 8
    private const val DESCRIPTOR_PROTO = "google/protobuf/descriptor.proto"

    @Throws(IOException::class)
    @JvmStatic fun main(args: Array<String>) {
      try {
        val wireCompiler = forArgs(args = *args)
        wireCompiler.compile()
      } catch (e: WireException) {
        System.err.print("Fatal: ")
        e.printStackTrace(System.err)
        System.exit(1)
      }
    }

    @Throws(WireException::class)
    @JvmOverloads
    @JvmStatic
    fun forArgs(
      fileSystem: FileSystem = FileSystems.getDefault(),
      logger: WireLogger = ConsoleWireLogger(),
      vararg args: String
    ): WireCompiler {
      val sourceFileNames = mutableListOf<String>()
      val identifierSetBuilder = IdentifierSet.Builder()
      val protoPaths = mutableListOf<String>()
      var javaOut: String? = null
      var kotlinOut: String? = null
      var quiet = false
      var dryRun = false
      var namedFilesOnly = false
      var emitAndroid = false
      var emitAndroidAnnotations = false
      var emitCompact = false
      var javaInterop = false

      for (arg in args) {
        when {
          arg.startsWith(PROTO_PATH_FLAG) -> {
            protoPaths.add(arg.substring(PROTO_PATH_FLAG.length))
          }

          arg.startsWith(JAVA_OUT_FLAG) -> {
            check(javaOut == null) { "java_out already set" }
            javaOut = arg.substring(JAVA_OUT_FLAG.length)
          }

          arg.startsWith(KOTLIN_OUT_FLAG) -> {
            check(kotlinOut == null) { "kotlin_out already set" }
            kotlinOut = arg.substring(KOTLIN_OUT_FLAG.length)
          }

          arg.startsWith(FILES_FLAG) -> {
            val files = File(arg.substring(FILES_FLAG.length))
            try {
              files.source().buffer().use { source ->
                while (true) {
                  val line = source.readUtf8Line() ?: break
                  sourceFileNames.add(line)
                }
              }
            } catch (ex: FileNotFoundException) {
              throw WireException("Error processing argument $arg", ex)
            }
          }

          arg.startsWith(INCLUDES_FLAG) -> {
            val includes = arg.substring(INCLUDES_FLAG.length)
            identifierSetBuilder.include(includes.split(Regex(",")))
          }

          arg.startsWith(EXCLUDES_FLAG) -> {
            val excludes = arg.substring(EXCLUDES_FLAG.length)
            identifierSetBuilder.exclude(excludes.split(Regex(",")))
          }

          arg == QUIET_FLAG -> quiet = true
          arg == DRY_RUN_FLAG -> dryRun = true
          arg == NAMED_FILES_ONLY -> namedFilesOnly = true
          arg == ANDROID -> emitAndroid = true
          arg == ANDROID_ANNOTATIONS -> emitAndroidAnnotations = true
          arg == COMPACT -> emitCompact = true
          arg == JAVA_INTEROP -> javaInterop = true
          arg.startsWith("--") -> throw IllegalArgumentException("Unknown argument '$arg'.")
          else -> sourceFileNames.add(arg)
        }
      }

      if ((javaOut != null) == (kotlinOut != null)) {
        throw WireException("Only one of $JAVA_OUT_FLAG or $KOTLIN_OUT_FLAG flag must be specified")
      }

      logger.setQuiet(quiet)

      return WireCompiler(fileSystem, logger, protoPaths, javaOut, kotlinOut, sourceFileNames,
          identifierSetBuilder.build(), dryRun, namedFilesOnly, emitAndroid, emitAndroidAnnotations,
          emitCompact, javaInterop)
    }
  }
}
