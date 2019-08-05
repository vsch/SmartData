/*
 * Copyright (c) 2015-2019 Vladimir Schneider <vladimir.schneider@gmail.com>
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

class SmartRepeatedCharSequenceTest {
    @Test
    fun test_Basic() {
        val seq = SmartRepeatedCharSequence(' ')
        assertEquals(" ", seq.asString())

        seq.length = 2
        assertEquals("  ", seq.asString())

        seq.length = 3
        assertEquals("   ", seq.asString())

        val chars = "1234567890"
        seq.variableChars = chars
        assertEquals(chars.substring(0, 3), seq.asString())

        for (i in 0..100) {
            seq.length = i
            val result = StringBuilder()
            while (result.length < i) {
                if (i - result.length > chars.length) result += chars
                else result += chars.substring(0, i - result.length)
            }
            assertEquals(result.toString(), seq.asString())
        }
    }

    fun test_Embedded() {
        val seq = SmartRepeatedCharSequence(' ')
        val seg = SmartSegmentedCharSequence(seq)
        assertEquals(" ", seg.asString())

        seq.length = 2
        assertEquals("  ", seg.asString())

        seq.length = 3
        assertEquals("   ", seg.asString())

        val chars = "1234567890"
        seq.variableChars = chars
        assertEquals(chars.substring(0, 3), seg.asString())

        for (i in 0..100) {
            seq.length = i
            val result = StringBuilder()
            while (result.length < i) {
                if (i - result.length > chars.length) result += chars
                else result += chars.substring(0, i - result.length)
            }
            assertEquals(result.toString(), seg.asString())
        }
    }

    @Test
    fun test_EmbeddedSpaces() {
        val chars = "1234567890"
        var spcs = " "
        val seq = SmartRepeatedCharSequence(' ')
        val seg = SmartSegmentedCharSequence(chars, seq, chars, seq, chars)
        println(seg)
        assertEquals(chars + spcs + chars + spcs + chars, seg.asString())

        seq.length = 2
        spcs = "  "
        println(seg)
        assertEquals(chars + spcs + chars + spcs + chars, seg.asString())

        seq.length = 3
        spcs = "   "
        println(seg)
        assertEquals(chars + spcs + chars + spcs + chars, seg.asString())

        seq.length = 4
        spcs = "    "
        println(seg)
        assertEquals(chars + spcs + chars + spcs + chars, seg.asString())

        seq.length = 5
        spcs = "     "
        println(seg)
        assertEquals(chars + spcs + chars + spcs + chars, seg.asString())

        seq.length = 6
        spcs = "      "
        println(seg)
        assertEquals(chars + spcs + chars + spcs + chars, seg.asString())
    }
}
