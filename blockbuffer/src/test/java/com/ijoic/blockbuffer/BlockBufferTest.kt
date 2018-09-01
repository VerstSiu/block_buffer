package com.ijoic.blockbuffer

import org.junit.Test

/**
 * Block buffer test.
 *
 * @author verstsiu on 2018/9/1
 * @version 1.0
 */
class BlockBufferTest {

  @Test
  fun testWrite() {
    val buffer = BlockBuffer(10)
    val input = byteArrayOf(B1, B2, B3)

    buffer.write(input)
    val output = buffer.toByteArray()

    assert(output.size == input.size)

    input.forEachIndexed { pos, it ->
      assert(output[pos] == it)
    }
  }

  companion object {
    private const val B1 = 0x01.toByte()
    private const val B2 = 0x02.toByte()
    private const val B3 = 0x03.toByte()
    private const val B4 = 0x04.toByte()
    private const val B5 = 0x05.toByte()
  }
}