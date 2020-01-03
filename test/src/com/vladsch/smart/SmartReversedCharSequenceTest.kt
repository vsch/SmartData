/*
 * Copyright (c) 2015-2020 Vladimir Schneider <vladimir.schneider@gmail.com>
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

class SmartReversedCharSequenceTest {
    val string = """0123456789
0123456789
0123456789
0123456789
"""
    val chars = string.toCharArray()

    @Test
    @Throws(Exception::class)
    fun test_basic1() {
        val charSeq = SmartReversedCharSequence(chars);
        assertEquals(string.reversed(), charSeq.toString())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic2() {
        val charSeq = SmartReversedCharSequence(chars, 0, string.length);
        assertEquals(string.reversed(), charSeq.toString())
        assertEquals(0, charSeq.trackedSourceLocation(charSeq.length - 1).offset)
        assertEquals(1, charSeq.trackedSourceLocation(charSeq.length - 2).offset)
        assertEquals(charSeq.length - 2, charSeq.trackedSourceLocation(1).offset)
        assertEquals(charSeq.length - 1, charSeq.trackedSourceLocation(0).offset)
    }

    @Test
    @Throws(Exception::class)
    fun test_basic3() {
        val charSeq = SmartReversedCharSequence(chars, 0, string.length);
        val charSeq2 = charSeq.subSequence(0, string.length)
        assertEquals(charSeq, charSeq2)
    }

    @Test
    @Throws(Exception::class)
    fun test_basic4() {
        val charSeq = SmartReversedCharSequence(chars, 0, string.length);
        val charSeq2 = charSeq.subSequence(11, string.length)
        assertEquals(string.substring(11).reversed(), charSeq2.toString())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic5() {
        val charSeq = SmartReversedCharSequence(chars, 0, string.length);
        val charSeq1 = charSeq.subSequence(0, 11)
        val charSeq2 = charSeq.subSequence(11, 22)
        assertEquals(string.substring(0, 11).reversed(), charSeq1.toString())
        assertEquals(string.substring(11, 22).reversed(), charSeq2.toString())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic6() {
        val charSeq = SmartReversedCharSequence(chars, 0, string.length);
        val charSeq1 = charSeq.subSequence(0, 11)
        val charSeq2 = charSeq.subSequence(11, 22)
        val charSeq3 = charSeq.subSequence(22, 33)

        assertEquals(string.substring(0, 11).reversed(), charSeq1.toString())
        assertEquals(string.substring(11, 22).reversed(), charSeq2.toString())
        assertEquals(string.substring(22, 33).reversed(), charSeq3.toString())

        // these are contiguous
        //        assertEquals(true, charSeq3.splicedWith(charSeq2) != null)
        //        assertEquals(true, charSeq2.splicedWith(charSeq1) != null)
        //        assertEquals(charSeq.subSequence(0, 33), charSeq3.splicedWith(charSeq2.splicedWith(charSeq1)))
    }

}
