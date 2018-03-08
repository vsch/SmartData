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

class SmartCharSequenceTest() {
    @Test
    @Throws(Exception::class)
    fun test_basic1() {
        val string = "abcDef123"
        val charSeq = SmartCharArraySequence(string.toCharArray());
        val charSeqLower = charSeq.lowercase()
        val charSeqUpper = charSeq.uppercase()

        assertEquals(string.toLowerCase(), charSeqLower.toString())
        assertEquals(string.toUpperCase(), charSeqUpper.toString())
    }

    @Test
    fun test_leading() {
        val string = "0123456789"
        val charSeq = SmartCharArraySequence(string.toCharArray());

        assertEquals(0, charSeq.countLeading("1"))
        assertEquals(10, charSeq.countLeading("0123456789"))
        assertEquals(2, charSeq.countLeading("01"))
        assertEquals(3, charSeq.countLeading("01234", 2))
        assertEquals(2, charSeq.countLeading("01234", 2, 4))
    }

    @Test
    fun test_trailing() {
        val string = "0123456789"
        val charSeq = SmartCharArraySequence(string.toCharArray());

        assertEquals(0, charSeq.countTrailing("1"))
        assertEquals(10, charSeq.countTrailing("0123456789"))
        assertEquals(2, charSeq.countTrailing("89"))
        assertEquals(2, charSeq.countTrailing("01234", 2))
        assertEquals(2, charSeq.countTrailing("01234", 2, 4))
    }

    @Test
    fun test_replace() {
        val string = "aabbbcdefa"
        val charSeq = SmartCharArraySequence(string.toCharArray());

        assertEquals("\\a\\abbbcdef\\a", charSeq.replace("a", "\\a").toString())
        assertEquals("bbbcdef", charSeq.replace("a", "").toString())
        assertEquals("aa.b..b..b.cdefa", charSeq.replace("b", ".b.").toString())
    }
}
