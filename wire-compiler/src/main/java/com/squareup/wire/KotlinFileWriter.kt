/*
 * Copyright 2018 Square Inc.
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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type
import java.io.IOException
import java.nio.file.FileSystem
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue

internal class KotlinFileWriter(
  private val destination: String,
  private val kotlinGenerator: KotlinGenerator,
  private val queue: ConcurrentLinkedQueue<Any>,
  private val fs: FileSystem,
  private val log: WireLogger,
  private val dryRun: Boolean
) : Callable<Unit> {

  @Throws(IOException::class)
  override fun call() {
    while (true) {
      val next = queue.poll() ?: return
      val kotlinFile = when (next) {
        is Type -> generateFileForType(next)
        is Service -> generateFileForService(next)
        else -> throw IllegalArgumentException("Unsupported item $next")
      }

      val path = fs.getPath(destination)
      log.artifact(path, kotlinFile)
      if (dryRun) return

      try {
        kotlinFile.writeTo(path)
      } catch (e: IOException) {
        val className = when (next) {
          is Type -> kotlinGenerator.generatedTypeName(next).canonicalName
          is Service -> next.type().toString()
          else -> next.toString()
        }
        throw IOException(
            "Error emitting ${kotlinFile.packageName}.$className to $destination",
            e)
      }
    }
  }

  private fun generateFileForType(type: Type): FileSpec {
    val typeSpec = kotlinGenerator.generateType(type)
    val className = kotlinGenerator.generatedTypeName(type)
    return FileSpec.builder(className.packageName, typeSpec.name!!)
        .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
        .apply {
          val location = type.location()
          if (location != null) {
            addComment("\nSource file: %L", location.withPathOnly())
          }
        }
        .addType(typeSpec)
        .build()
  }

  private fun generateFileForService(service: Service): FileSpec {
    val typeSpec = kotlinGenerator.generateService(service)
    val packageName = service.type().enclosingTypeOrPackage()
    return FileSpec.builder(packageName, typeSpec.name!!)
        .addComment(WireCompiler.CODE_GENERATED_BY_WIRE)
        .indent("  ")
        .apply {
          val location = service.location()
          if (location != null) {
            addComment("\nSource file: %L", location.withPathOnly())
          }
        }
        .addType(typeSpec)
        .build()
  }
}
