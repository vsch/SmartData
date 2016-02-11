/*
 * Copyright (c) 2015-2016 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.vladsch.smart

import org.junit.Assert.assertEquals
import org.junit.Test

class TextBlockTest {
    class MarkdownFormatter {
        companion object {
            const val INDENT = "    "
            const val BLOCK_QUOTE_PREFIX = "> "
            const val BLOCK_QUOTE_PREFIX_NO_SPACE = ">"
            const val BLOCK_QUOTE_PREFIX_INDENT = BLOCK_QUOTE_PREFIX + INDENT
            const val TAB_SIZE = 4
        }
    }

    val string = """>0123456789
> 0123456789
>  0123456789
>   0123456789
>    0123456789
>     0123456789
>      0123456789
>       0123456789
"""
    val chars = string.toCharArray()
    val charSeq = SmartCharArraySequence(chars)

    val tabbedString = """>0123456789
> 0123456789
>  0123456789
>\t0123456789
>\t 0123456789
>\t  0123456789
>\t   0123456789
>\t    0123456789
"""
    val tabbedChars = string.toCharArray()
    val tabbedCharSeq = SmartCharArraySequence(chars)

    val adjstring = """0123456789
0123456789
0123456789
0123456789
0123456789
0123456789
 0123456789
  0123456789
"""
    val adjchars = string.toCharArray()
    val adjcharSeq = SmartCharArraySequence(chars)

    val adj1string = """0123456789
0123456789
 0123456789
  0123456789
   0123456789
    0123456789
     0123456789
      0123456789
"""
    val adj1chars = string.toCharArray()
    val adj1charSeq = SmartCharArraySequence(chars)

    @Test
    fun test_Basic() {
        val textBlock = TextBlock(charSeq, 0)

        assertEquals(string, textBlock.lines.toString())
    }

    @Test
    fun test_Adjusted1() {
        val parentBlock = TextBlock(charSeq, 0)
        val textBlock = parentBlock.childBlock(MarkdownFormatter.BLOCK_QUOTE_PREFIX)

        assertEquals(adj1string, textBlock.lines.toString())
    }

    @Test
    fun test_Adjusted2() {
        val parentBlock = TextBlock(charSeq, 0)
        val textBlock = parentBlock.childBlock(MarkdownFormatter.BLOCK_QUOTE_PREFIX_INDENT)

        assertEquals(adjstring, textBlock.lines.toString())
    }

    @Test
    fun test_AdjustedSub1() {
        val parentBlock = TextBlock(charSeq, 0)
        val textBlock = parentBlock.childBlock(MarkdownFormatter.BLOCK_QUOTE_PREFIX)
        val childBlock = textBlock.childBlock(MarkdownFormatter.INDENT)
        assertEquals(adj1string, textBlock.lines.toString())
        assertEquals(adjstring, childBlock.lines.toString())

        for (i in 0..childBlock.lines.length - 1) {
            val childSrcOffset = childBlock.getOffset(i)
            val textOffset = textBlock.getIndex(childSrcOffset)
            val textSrcOffset = textBlock.getOffset(textOffset)

            //            println("src: $childSrcOffset, parent: $textOffset, child: $i")
            assertEquals(childSrcOffset, textSrcOffset)
            assertEquals(charSeq[childSrcOffset], childBlock.lines[i])
        }
    }

    @Test
    fun test_Tabbed() {
        val parentBlock = TextBlock(charSeq, 0)
        val tabbedBlock = TextBlock(tabbedCharSeq, 0)
        assertEquals(string, tabbedBlock.lines.toString())
    }

    @Test
    fun test_AdjustedTabbedSub1() {
        val parentBlock = TextBlock(tabbedCharSeq, 4)
        val textBlock = parentBlock.childBlock(MarkdownFormatter.BLOCK_QUOTE_PREFIX)
        val childBlock = textBlock.childBlock(MarkdownFormatter.INDENT)
        assertEquals(adj1string, textBlock.lines.toString())
        assertEquals(adjstring, childBlock.lines.toString())

        for (i in 0..childBlock.lines.length - 1) {
            val childSrcOffset = childBlock.getOffset(i)
            val textOffset = textBlock.getIndex(childSrcOffset)
            val textSrcOffset = textBlock.getOffset(textOffset)

//            println("src: $childSrcOffset, parent: $textOffset, child: $i")
            assertEquals(childSrcOffset, textSrcOffset)
            assertEquals(tabbedCharSeq[childSrcOffset], childBlock.lines[i])
        }
    }
}

