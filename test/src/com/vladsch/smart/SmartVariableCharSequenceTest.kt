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

class SmartVariableCharSequenceTest {
    val string = """0123456789
0123456789
0123456789
0123456789
"""
    val chars: SmartCharSequence

    init {
        chars = SmartCharArraySequence(string.toCharArray())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic1() {
        val charSeq = SmartVariableCharSequence(chars);
        assertEquals(string, charSeq.toString())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic2() {
        val charSeq = SmartVariableCharSequence(chars)
        assertEquals(string, charSeq.toString())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic3() {
        val charSeq = SmartVariableCharSequence(chars)
        val charSeq2 = charSeq.subSequence(0, string.length).variable()
        assertEquals(charSeq, charSeq2)
    }

    @Test
    @Throws(Exception::class)
    fun test_basic4() {
        val charSeq = SmartVariableCharSequence(chars)
        val charSeq2 = charSeq.subSequence(11, string.length).variable()
        assertEquals(string.substring(11), charSeq2.toString())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic5() {
        val charSeq = SmartVariableCharSequence(chars)
        val charSeq1 = charSeq.subSequence(0, 11).variable()
        val charSeq2 = charSeq.subSequence(11, 22).variable()
        assertEquals(string.substring(0, 11), charSeq1.toString())
        assertEquals(string.substring(11, 22), charSeq2.toString())
    }

    @Test
    @Throws(Exception::class)
    fun test_basic6() {
        val charSeq = SmartVariableCharSequence(chars)
        val charSeq1 = charSeq.subSequence(0, 11).variable()
        val charSeq2 = charSeq.subSequence(11, 22).variable()
        val charSeq3 = charSeq.subSequence(22, 33).variable()

        assertEquals(string.substring(0, 11), charSeq1.toString())
        assertEquals(string.substring(11, 22), charSeq2.toString())
        assertEquals(string.substring(22, 33), charSeq3.toString())

        // padding sequences cannot be spliced because they are variable
        assertEquals(false, charSeq1.splicedWith(charSeq2) != null)
        assertEquals(false, charSeq2.splicedWith(charSeq3) != null)
    }

    @Test
    fun test_LeftAligned() {
        val charSeq = SmartVariableCharSequence(chars)
        val charSeq1 = charSeq.subSequence(0, 10).variable()
        val charSeq2 = charSeq.subSequence(11, 21).variable()
        val charSeq3 = charSeq.subSequence(22, 32).variable()
        var segmented = charSeq1.append(charSeq2, charSeq3)

        println("leftAligned: " + segmented)

        charSeq1.leftAlign(9)

        println("leftAligned: 1:9 " + segmented)

        assertEquals(10, charSeq1.length)
        assertEquals(0, charSeq1.countTrailing(' '))
        assertEquals(20+10, segmented.length)

        charSeq1.leftAlign(10)

        println("leftAligned: 1:10 " + segmented)

        assertEquals(10, charSeq1.length)
        assertEquals(0, charSeq1.countTrailing(' '))
        assertEquals(20+10, segmented.length)

        charSeq1.leftAlign(11)

        println("leftAligned: 1:11 " + segmented)

        assertEquals(11, charSeq1.length)
        assertEquals(1, charSeq1.countTrailing(' '))
        assertEquals(20+11, segmented.length)

        charSeq1.leftAlign(15)

        println("leftAligned: 1:15 " + segmented)

        assertEquals(15, charSeq1.length)
        assertEquals(5, charSeq1.countTrailing(' '))
        assertEquals(20+15, segmented.length)
    }

    @Test
    fun test_rightAligned() {
        val charSeq = SmartVariableCharSequence(chars)
        val charSeq1 = charSeq.subSequence(0, 10).variable()
        val charSeq2 = charSeq.subSequence(11, 21).variable()
        val charSeq3 = charSeq.subSequence(22, 32).variable()
        var segmented = charSeq1.append(charSeq2, charSeq3)

        println("rightAligned: " + segmented)

        charSeq1.rightAlign(9)

        println("rightAligned: 1:9 " + segmented)

        assertEquals(10, charSeq1.length)
        assertEquals(0, charSeq1.countLeading(' '))
        assertEquals(20+10, segmented.length)

        charSeq1.rightAlign(10)

        println("rightAligned: 1:10 " + segmented)

        assertEquals(10, charSeq1.length)
        assertEquals(0, charSeq1.countLeading(' '))
        assertEquals(20+10, segmented.length)

        charSeq1.rightAlign(11)

        println("rightAligned: 1:11 " + segmented)

        assertEquals(11, charSeq1.length)
        assertEquals(1, charSeq1.countLeading(' '))
        assertEquals(20+11, segmented.length)

        charSeq1.rightAlign(15)

        println("rightAligned: 1:15 " + segmented)

        assertEquals(15, charSeq1.length)
        assertEquals(5, charSeq1.countLeading(' '))
        assertEquals(20+15, segmented.length)
    }

    @Test
    fun test_centerAligned() {
        val charSeq = SmartVariableCharSequence(chars)
        val charSeq1 = charSeq.subSequence(0, 10).variable()
        val charSeq2 = charSeq.subSequence(11, 21).variable()
        val charSeq3 = charSeq.subSequence(22, 32).variable()
        var segmented = charSeq1.append(charSeq2, charSeq3)

        println("centerAligned: " + segmented)

        charSeq1.centerAlign(9)

        println("centerAligned: 1:9 " + segmented)

        assertEquals(10, charSeq1.length)
        assertEquals(0, charSeq1.countLeading(' '))
        assertEquals(20+10, segmented.length)

        charSeq1.centerAlign(10)

        println("centerAligned: 1:10 " + segmented)

        assertEquals(10, charSeq1.length)
        assertEquals(0, charSeq1.countLeading(' '))
        assertEquals(20+10, segmented.length)

        charSeq1.centerAlign(11)

        println("centerAligned: 1:11 " + segmented)

        assertEquals(11, charSeq1.length)
        assertEquals(0, charSeq1.countLeading(' '))
        assertEquals(1, charSeq1.countTrailing(' '))
        assertEquals(20+11, segmented.length)

        charSeq1.centerAlign(12)

        println("centerAligned: 1:12 " + segmented)

        assertEquals(12, charSeq1.length)
        assertEquals(1, charSeq1.countLeading(' '))
        assertEquals(1, charSeq1.countTrailing(' '))
        assertEquals(20+12, segmented.length)

        charSeq1.centerAlign(13)

        println("centerAligned: 1:13 " + segmented)

        assertEquals(13, charSeq1.length)
        assertEquals(1, charSeq1.countLeading(' '))
        assertEquals(2, charSeq1.countTrailing(' '))
        assertEquals(20+13, segmented.length)

        charSeq1.centerAlign(15)

        println("centerAligned: 1:15 " + segmented)

        assertEquals(15, charSeq1.length)
        assertEquals(2, charSeq1.countLeading(' '))
        assertEquals(3, charSeq1.countTrailing(' '))
        assertEquals(20+15, segmented.length)
    }
}
