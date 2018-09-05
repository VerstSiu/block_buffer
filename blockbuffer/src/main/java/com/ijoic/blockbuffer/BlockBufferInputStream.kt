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

import java.io.InputStream

/**
 * Block buffer input stream.
 *
 * @author verstsiu on 2018/9/1
 * @version 1.0
 */
class BlockBufferInputStream(
    private val buffer: BlockBuffer): InputStream() {

  private var currentIndex = 0
  private var eof = false

  override fun read(): Int {
    return buffer.readBit(currentIndex++)
  }

  override fun read(b: ByteArray?, off: Int, len: Int): Int {
    if (eof) {
      return -1
    }
    b ?: throw NullPointerException()

    if (off < 0 || len < 0 || len > b.size - off) {
      throw IndexOutOfBoundsException()
    }
    val readCount = buffer.read(b, off, len, currentIndex)

    when(readCount) {
      -1 -> eof = true
      else -> currentIndex += readCount
    }

    return readCount
  }

  override fun skip(n: Long): Long {
    val skipCount = Math.min(available().toLong(), n)
    currentIndex += skipCount.toInt()
    return skipCount
  }

  override fun available(): Int {
    return Math.max(buffer.size - currentIndex, 0)
  }

  override fun reset() {
    super.reset()
    currentIndex = 0
    eof = false
  }

}