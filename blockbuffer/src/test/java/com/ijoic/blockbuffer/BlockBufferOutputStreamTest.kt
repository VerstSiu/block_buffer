package com.ijoic.blockbuffer

import org.apache.commons.io.IOUtils
import org.junit.Test

/**
 * Block buffer output stream test.
 *
 * @author verstsiu on 2018/9/3
 * @version 1.0
 */
class BlockBufferOutputStreamTest {
  @Test
  fun testWriteBit() {
    val b1 = 0x00.toByte()
    val b2 = 0x11.toByte()
    val b3 = 0xF0.toByte()
    val b4 = 0xFF.toByte()

    val buffer = BlockBuffer(4)
    val output = BlockBufferOutputStream(buffer)

    output.write(b1.toInt())
    output.write(b2.toInt())
    output.write(b3.toInt())
    output.write(b4.toInt())

    buffer.read {
      assert(it.size == 4)
      assert(it[0] == b1)
      assert(it[1] == b2)
      assert(it[2] == b3)
      assert(it[3] == b4)
    }
  }
}