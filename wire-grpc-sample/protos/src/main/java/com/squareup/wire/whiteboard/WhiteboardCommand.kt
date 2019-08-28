// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: com/squareup/wire/whiteboard/whiteboard.proto
package com.squareup.wire.whiteboard

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.countNonNull
import com.squareup.wire.internal.missingRequiredFields
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class WhiteboardCommand(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.whiteboard.WhiteboardCommand${'$'}AddPoint#ADAPTER"
  )
  val add_point: AddPoint? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.whiteboard.WhiteboardCommand${'$'}ClearBoard#ADAPTER"
  )
  val clear_board: ClearBoard? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<WhiteboardCommand, Nothing>(ADAPTER, unknownFields) {
  init {
    require(countNonNull(add_point, clear_board) <= 1) {
      "At most one of add_point, clear_board may be non-null"
    }
  }

  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is WhiteboardCommand) return false
    return unknownFields == other.unknownFields
        && add_point == other.add_point
        && clear_board == other.clear_board
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = add_point.hashCode()
      result = result * 37 + clear_board.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (add_point != null) result += """add_point=$add_point"""
    if (clear_board != null) result += """clear_board=$clear_board"""
    return result.joinToString(prefix = "WhiteboardCommand{", separator = ", ", postfix = "}")
  }

  fun copy(
    add_point: AddPoint? = this.add_point,
    clear_board: ClearBoard? = this.clear_board,
    unknownFields: ByteString = this.unknownFields
  ): WhiteboardCommand = WhiteboardCommand(add_point, clear_board, unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<WhiteboardCommand> = object : ProtoAdapter<WhiteboardCommand>(
      FieldEncoding.LENGTH_DELIMITED, 
      WhiteboardCommand::class
    ) {
      override fun encodedSize(value: WhiteboardCommand): Int = 
        AddPoint.ADAPTER.encodedSizeWithTag(1, value.add_point) +
        ClearBoard.ADAPTER.encodedSizeWithTag(2, value.clear_board) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: WhiteboardCommand) {
        AddPoint.ADAPTER.encodeWithTag(writer, 1, value.add_point)
        ClearBoard.ADAPTER.encodeWithTag(writer, 2, value.clear_board)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): WhiteboardCommand {
        var add_point: AddPoint? = null
        var clear_board: ClearBoard? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> add_point = AddPoint.ADAPTER.decode(reader)
            2 -> clear_board = ClearBoard.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return WhiteboardCommand(
          add_point = add_point,
          clear_board = clear_board,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: WhiteboardCommand): WhiteboardCommand = value.copy(
        add_point = value.add_point?.let(AddPoint.ADAPTER::redact),
        clear_board = value.clear_board?.let(ClearBoard.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }

  class AddPoint(
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.whiteboard.Point#ADAPTER",
      label = WireField.Label.REQUIRED
    )
    val point: Point,
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<AddPoint, Nothing>(ADAPTER, unknownFields) {
    @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Nothing {
      throw AssertionError()
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is AddPoint) return false
      return unknownFields == other.unknownFields
          && point == other.point
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = point.hashCode()
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      result += """point=$point"""
      return result.joinToString(prefix = "AddPoint{", separator = ", ", postfix = "}")
    }

    fun copy(point: Point = this.point, unknownFields: ByteString = this.unknownFields): AddPoint =
        AddPoint(point, unknownFields)

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<AddPoint> = object : ProtoAdapter<AddPoint>(
        FieldEncoding.LENGTH_DELIMITED, 
        AddPoint::class
      ) {
        override fun encodedSize(value: AddPoint): Int = 
          Point.ADAPTER.encodedSizeWithTag(1, value.point) +
          value.unknownFields.size

        override fun encode(writer: ProtoWriter, value: AddPoint) {
          Point.ADAPTER.encodeWithTag(writer, 1, value.point)
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: ProtoReader): AddPoint {
          var point: Point? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> point = Point.ADAPTER.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return AddPoint(
            point = point ?: throw missingRequiredFields(point, "point"),
            unknownFields = unknownFields
          )
        }

        override fun redact(value: AddPoint): AddPoint = value.copy(
          point = Point.ADAPTER.redact(value.point),
          unknownFields = ByteString.EMPTY
        )
      }
    }
  }

  class ClearBoard(
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<ClearBoard, Nothing>(ADAPTER, unknownFields) {
    @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Nothing {
      throw AssertionError()
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is ClearBoard) return false
      return unknownFields == other.unknownFields
    }

    override fun hashCode(): Int = unknownFields.hashCode()

    override fun toString(): String = "ClearBoard{}"

    fun copy(unknownFields: ByteString = this.unknownFields): ClearBoard = ClearBoard(unknownFields)

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<ClearBoard> = object : ProtoAdapter<ClearBoard>(
        FieldEncoding.LENGTH_DELIMITED, 
        ClearBoard::class
      ) {
        override fun encodedSize(value: ClearBoard): Int = 
          value.unknownFields.size

        override fun encode(writer: ProtoWriter, value: ClearBoard) {
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: ProtoReader): ClearBoard {
          val unknownFields = reader.forEachTag(reader::readUnknownField)
          return ClearBoard(
            unknownFields = unknownFields
          )
        }

        override fun redact(value: ClearBoard): ClearBoard = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }
    }
  }
}
