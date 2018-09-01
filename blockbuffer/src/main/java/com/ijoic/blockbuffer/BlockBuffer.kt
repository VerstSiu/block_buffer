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
  private var offset = 0

  /**
   * Read byte contents.
   *
   * @param func read callback: fun(byteContent, size).
   */
  fun read(func: (ByteArray) -> Unit) {
    val blockIndex = lastBlockIndex
    val offset = this.offset

    var readIndex = 0

    while(true) {
      when {
        readIndex < blockIndex -> func.invoke(getBlock(readIndex))
        readIndex == blockIndex -> {
          val block = getBlock(readIndex)

          if (offset + 1 == blockSize) {
            func.invoke(block)
          } else if (offset >= 0) {
            func.invoke(Arrays.copyOfRange(block, 0, offset + 1))
          }
          return
        }
        else -> return
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
    var offset = this.offset

    val srcSize = content.size
    var srcIndex = 0
    var writeSize: Int
    var writeIndex: Int

    var writeBlock: ByteArray

    while(true) {
      if (offset + 1 >= blockSize) {
        offset = 0
        ++blockIndex
      }

      writeSize = Math.min(srcSize - srcIndex, blockSize - offset + 1)
      writeBlock = getBlock(blockIndex)
      writeIndex = 0

      while(writeIndex < writeSize) {
        writeBlock[offset + writeIndex] = content[srcIndex + writeIndex]
        ++writeIndex
      }
      offset += writeSize
      srcIndex += writeSize

      if (srcIndex >= srcSize) {
        break
      }
    }
    this.lastBlockIndex = blockIndex
    this.offset = offset
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
        val offset = this.offset

        return when {
          blockIndex < 0 -> 0
          blockIndex == 0 -> offset + 1
          else -> (blockIndex - 1) * blockSize + offset + 1
        }
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
    offset = 0
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