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

  override fun read(): Int {
    println(">>>> block buffer >>>> read bit")
    return buffer.readBit(currentIndex++)
  }

  override fun read(b: ByteArray?, off: Int, len: Int): Int {
    println(">>>> block buffer >>>> read bytes: size - ${b?.size}, off - $off, len - $len")
    if (b == null) {
      return READ_LENGTH_EOF
    }
    val readCount = buffer.read(b, off, len, currentIndex)
    currentIndex += readCount

    return when(readCount) {
      0 -> READ_LENGTH_EOF
      else -> readCount
    }
  }

  override fun skip(n: Long): Long {
    println(">>>> block buffer >>>> skip: n - $n")
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
  }

  private companion object {
    private const val READ_LENGTH_EOF = -1
  }
}