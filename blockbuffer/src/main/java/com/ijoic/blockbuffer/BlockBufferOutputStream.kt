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

import java.io.OutputStream

/**
 * Block buffer output stream.
 *
 * @author verstsiu on 2018/9/1
 * @version 1.0
 */
class BlockBufferOutputStream(private val buffer: BlockBuffer): OutputStream() {

  private var currentIndex = 0

  override fun write(b: Int) {
    buffer.writeBit(currentIndex++, b)
  }

  override fun write(b: ByteArray?, off: Int, len: Int) {
    if (b == null) {
      return
    }
    currentIndex += buffer.write(b, off, len)
  }

}