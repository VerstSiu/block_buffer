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
class BlockBuffer(@IntRange(from = 1) private val blockSize: Int) {

  private val blockItems by lazy { mutableListOf<ByteArray>() }

  private var lastBlockIndex = 0
  private var end = 0

  /**
   * Read byte contents.
   *
   * @param func read callback: fun(byteContent, size).
   */
  fun read(func: (ByteArray) -> Unit) {
    val blockIndex = lastBlockIndex
    val end = this.end

    var readIndex = 0

    while(true) {
      when {
        readIndex < blockIndex -> func.invoke(getBlock(readIndex))
        readIndex == blockIndex -> {
          val block = getBlock(readIndex)

          if (end >= blockSize) {
            func.invoke(block)
          } else if (end > 0) {
            func.invoke(Arrays.copyOfRange(block, 0, end))
          }
          return
        }
        else -> return
      }
      ++readIndex
    }
  }

  /**
   * Read byte contents.
   *
   * @param offset offset.
   * @param size size.
   * @param func read callback: fun(byteContent, size).
   */
  fun read(@IntRange(from = 0) offset: Int, size: Int?, func: (ByteArray) -> Unit) {
    var readSize = when {
      size == null -> this.size - offset
      size <= 0 -> 0
      else -> size
    }

    if (readSize <= 0) {
      return
    }
    val startBlockIndex = offset / blockSize
    val startOffset = offset % blockSize

    val lastBlockIndex = lastBlockIndex
    val lastEnd = this.end

    var readIndex = startBlockIndex

    while(true) {
      when {
        readIndex == startBlockIndex -> {
          val block = getBlock(readIndex)

          if (startBlockIndex == lastBlockIndex) {
            if (startOffset < lastEnd) {
              func.invoke(Arrays.copyOfRange(block, startOffset, Math.min(readSize, lastEnd - startOffset)))
            }
            return
          }

          if (startOffset + readSize <= blockSize) {
            func.invoke(Arrays.copyOfRange(block, startOffset, readSize))
            return
          }

          if (startOffset == 0) {
            func.invoke(block)
          } else {
            func.invoke(Arrays.copyOfRange(block, startOffset, blockSize - startOffset))
          }
          readSize -= blockSize - startOffset
        }
        readIndex < lastBlockIndex -> {
          val block = getBlock(readIndex)

          if (readSize >= blockSize) {
            func.invoke(block)
            readSize -= blockSize
          } else {
            func.invoke(Arrays.copyOfRange(block, 0, readSize));
            return
          }
        }
        readIndex == lastBlockIndex -> {
          val block = getBlock(readIndex)

          if (readSize == blockSize && lastEnd == blockSize) {
            func.invoke(block)
          } else if (readSize > 0 && lastEnd > 0) {
            func.invoke(Arrays.copyOfRange(block, 0, Math.min(readSize, lastEnd)))
          }
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
   * Write byte content.
   *
   * @param content byte content.
   */
  fun write(content: ByteArray) {
    if (content.isEmpty()) {
      return
    }
    var blockIndex = this.lastBlockIndex
    var end = this.end

    val srcSize = content.size
    var srcIndex = 0
    var writeSize: Int
    var writeIndex: Int

    var writeBlock: ByteArray

    while(true) {
      if (end >= blockSize) {
        end = 0
        ++blockIndex
      }

      writeSize = Math.min(srcSize - srcIndex, blockSize - end)
      writeBlock = getBlock(blockIndex)
      writeIndex = 0

      while(writeIndex < writeSize) {
        writeBlock[end + writeIndex] = content[srcIndex + writeIndex]
        ++writeIndex
      }
      end += writeSize
      srcIndex += writeSize

      if (srcIndex >= srcSize) {
        break
      }
    }
    this.lastBlockIndex = blockIndex
    this.end = end
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
          else -> (blockIndex - 1) * blockSize + end
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