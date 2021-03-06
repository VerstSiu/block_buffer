/*
 *
 *  Copyright(c) 2018 VerstSiu
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.ijoic.blockbuffer

import android.support.annotation.IntRange
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * Block buffer.
 *
 * <p>This class is non-thread-save.</p>
 *
 * @author verstsiu on 2018/9/1
 * @version 1.0
 */
class BlockBuffer(@IntRange(from = 1) val blockSize: Int) {

  private val blockItems by lazy { mutableListOf<ByteArray>() }

  /**
   * Last block index.
   */
  var lastBlockIndex = 0
    private set

  private var end = 0

  /**
   * Read byte contents.
   *
   * @param func read callback: fun(byteContent).
   */
  fun read(func: (ByteArray) -> Unit) {
    readIndexed { it, _ -> func(it) }
  }

  /**
   * Read byte contents.
   *
   * @param func read callback: fun(byteContent, index).
   */
  fun readIndexed(func: (ByteArray, Int) -> Unit) {
    val blockIndex = lastBlockIndex
    val end = this.end

    var readIndex = 0

    while(true) {
      when {
        readIndex < blockIndex -> func.invoke(getBlock(readIndex), readIndex)
        readIndex == blockIndex -> {
          val block = getBlock(readIndex)

          if (end >= blockSize) {
            func.invoke(block, readIndex)
          } else if (end > 0) {
            func.invoke(Arrays.copyOfRange(block, 0, end), readIndex)
          }
          return
        }
        else -> return
      }
      ++readIndex
    }
  }

  /**
   * Copy byte buffer.
   *
   * @param dst destination.
   * @param offset offset.
   * @param size size.
   */
  fun copy(dst: BlockBuffer, @IntRange(from = 0) offset: Int = 0, size: Int? = null) {
    dst.reset()

    var readSize = when {
      size == null -> this.size - offset
      size <= 0 -> 0
      else -> Math.min(size, this.size - offset)
    }

    if (readSize <= 0) {
      return
    }
    val startBlockIndex = offset / blockSize
    val startOffset = offset % blockSize

    val lastBlockIndex = lastBlockIndex
    val lastEnd = this.end

    var readIndex = startBlockIndex
    var readCount: Int

    while(true) {
      when {
        readIndex == startBlockIndex -> {
          val block = getBlock(readIndex)

          if (startBlockIndex == lastBlockIndex) {
            if (startOffset < lastEnd) {
              dst.write(block, startOffset, Math.min(readSize, lastEnd - startOffset))
            }
            return
          }

          if (startOffset + readSize <= blockSize) {
            dst.write(block, startOffset, readSize)
            return
          }

          readCount = blockSize - startOffset
          dst.write(block, startOffset, readCount)
          readSize -= readCount
        }
        readIndex < lastBlockIndex -> {
          val block = getBlock(readIndex)

          if (readSize <= blockSize) {
            dst.write(block, 0, readSize)
            return
          }
          dst.write(block, 0, blockSize)
          readSize -= blockSize
        }
        readIndex == lastBlockIndex -> {
          val block = getBlock(readIndex)

          dst.write(block, 0, Math.min(readSize, lastEnd))
          return
        }
        else -> return
      }

      if (readSize <= 0) {
        return
      }
      ++readIndex
    }
  }

  /**
   * Returns bit content for specific index.
   *
   * @param index read index.
   */
  internal fun readBit(index: Int): Int {
    if (index >= 0) {
      val blockIndex = index / blockSize
      val offset = index % blockSize

      val block = blockItems.getOrNull(blockIndex)

      if (block != null) {
        when {
          blockIndex < lastBlockIndex -> return block.readBit(offset)
          blockIndex == lastBlockIndex && offset < end -> return block.readBit(offset)
        }
      }
    }
    return -1
  }

  private fun ByteArray.readBit(pos: Int): Int {
    return this[pos].toInt() and 0xFF
  }

  /**
   * Read byte content.
   *
   * @param b buffer.
   * @param offset offset.
   * @param length length.
   * @param startPos start position.
   * @return real read length.
   */
  internal fun read(b: ByteArray?, offset: Int, length: Int, startPos: Int): Int { // - 0 512 10
    b ?: throw NullPointerException()

    if (offset < 0 || length < 0 || length > b.size - offset) {
      throw IndexOutOfBoundsException()
    }
    val srcSize = b.size // 512

    if (startPos >= size) { // x
      return -1
    }
    val startBlockIndex = startPos / blockSize // 0
    val startOffset = startPos % blockSize // 10

    val lastBlockIndex = this.lastBlockIndex // 1
    val lastEnd = this.end // 227

    var srcIndex = offset // 0
    val srcEnd = Math.min(srcSize, offset + length) // 512

    var readIndex = startBlockIndex // 0
    var readCount: Int
    var readCountMax: Int

    while(true) {
      if (readIndex == startBlockIndex) { // *
        // read first block
        val block = getBlock(readIndex)
        readCountMax = srcEnd - srcIndex

        if (startBlockIndex == lastBlockIndex) {
          if (startOffset < lastEnd) {
            readCount = Math.min(readCountMax, lastEnd - startOffset)
            srcIndex += b.fill(block, startOffset, srcIndex, readCount)
          }
          break
        }

        if (startOffset + readCountMax <= blockSize) {
          srcIndex += b.fill(block, startOffset, srcIndex, readCountMax)
          break
        }

        srcIndex += b.fill(block, startOffset, srcIndex, blockSize - startOffset)

      } else if (readIndex < startBlockIndex) {
        // read middle block(not last block)
        val block = getBlock(readIndex)
        readCountMax = srcEnd - srcIndex

        if (readCountMax <= blockSize) {
          srcIndex += b.fill(block, 0, srcIndex, readCountMax)
          break
        }

        srcIndex += b.fill(block, 0, srcIndex, blockSize)

      } else if (readIndex == lastBlockIndex) {
        // read last block
        val block = getBlock(readIndex)
        readCountMax = srcEnd - srcIndex

        readCount = Math.min(readCountMax, lastEnd)
        srcIndex += b.fill(block, 0, srcIndex, readCount)

      } else {
        break
      }

      ++readIndex
    }
    return srcIndex - offset
  }

  private fun ByteArray.fill(src: ByteArray, srcOffset: Int, offset: Int, size: Int): Int {
    var copyIndex = 0

    while(copyIndex < size) {
      this[offset + copyIndex] = src[srcOffset + copyIndex]
      ++copyIndex
    }
    return size
  }

  /**
   * Write bit to current byte content.
   *
   * @param index index.
   * @param b bit.
   */
  internal fun writeBit(index: Int, b: Int) {
    if (index >= 0) {
      val blockIndex = index / blockSize
      val offset = index % blockSize

      val block = getBlock(blockIndex)
      block[offset] = b.toByte()

      lastBlockIndex = blockIndex
      end = offset + 1
    }
  }

  /**
   * Write byte content.
   *
   * @param content byte content.
   */
  fun write(content: ByteArray): Int {
    return write(content, 0, content.size)
  }

  /**
   * Write byte content.
   *
   * @param content byte content.
   * @param offset offset.
   * @param length length.
   */
  fun write(content: ByteArray, @IntRange(from = 0) offset: Int, @IntRange(from = 0) length: Int): Int {
    val srcSize = content.size

    if (srcSize == 0 || length == 0 || offset >= srcSize) {
      return 0
    }
    var blockIndex = this.lastBlockIndex
    var end = this.end

    var srcIndex = offset
    val srcEnd = Math.min(srcSize, offset + length)

    var writeSize: Int
    var writeBlock: ByteArray

    while(true) {
      writeSize = Math.min(srcEnd - srcIndex, blockSize - end)
      writeBlock = getBlock(blockIndex)

      writeBlock.fill(content, srcIndex, end, writeSize)
      end += writeSize
      srcIndex += writeSize

      if (end >= blockSize) {
        end = 0
        ++blockIndex
      }
      if (srcIndex >= srcEnd) {
        break
      }
    }
    this.lastBlockIndex = blockIndex
    this.end = end

    return srcEnd - offset
  }

  private fun getBlock(@IntRange(from = 0) index: Int): ByteArray {
    var blockCount = blockItems.size

    return when {
      blockCount > index -> blockItems[index]
      blockCount == index -> ByteArray(blockSize).apply { blockItems.add(this) }
      else -> {
        while (blockCount <= index) {
          blockItems.add(ByteArray(blockSize))
          ++blockCount
        }
        blockItems[index]
      }
    }
  }

  /**
   * Current byte content size.
   */
  val size: Int
      get() {
        val blockIndex = lastBlockIndex
        val end = this.end

        return when {
          blockIndex < 0 -> 0
          blockIndex == 0 -> end
          else -> blockIndex * blockSize + end
        }
      }

  /**
   * Returns byte array of current byte content.
   */
  fun toByteArray(): ByteArray {
    val baos = ByteArrayOutputStream()

    try {
      read { baos.write(it) }

      val result = baos.toByteArray()
      baos.reset()
      return result

    } catch (e: Exception) {
      e.printStackTrace()
    } finally {

      try {
        baos.close()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
    return byteArrayOf()
  }

  override fun toString(): String {
    val sb = StringBuilder()

    read { sb.append(it) }

    val result = sb.toString()
    sb.delete(0, sb.length)
    return result
  }

  /**
   * Reset byte position.
   */
  fun reset() {
    lastBlockIndex = 0
    end = 0
  }

  /**
   * Release block contents that is not currently used.
   */
  fun trimContent() {
    var blockCount = blockItems.size
    val endBlockIndex = lastBlockIndex + 1

    while(blockCount > endBlockIndex) {
      --blockCount
      blockItems.removeAt(blockCount)
    }
  }

  /**
   * Release byte contents.
   */
  fun release() {
    reset()
    blockItems.clear()
  }
}