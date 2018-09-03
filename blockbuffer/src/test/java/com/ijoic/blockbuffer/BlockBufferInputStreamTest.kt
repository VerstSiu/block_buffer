package com.ijoic.blockbuffer

import org.apache.commons.io.IOUtils
import org.junit.Test

/**
 * Block buffer input stream test.
 *
 * @author verstsiu on 2018/9/3
 * @version 1.0
 */
class BlockBufferInputStreamTest {
  @Test
  fun testToString() {
    val charset = Charsets.UTF_8
    val buffer = BlockBuffer(256)
    buffer.write("Hello World!".toByteArray(charset))

    val input = BlockBufferInputStream(buffer)
    val text = IOUtils.toString(input, charset)
    println(text) // Hello World!
  }
}