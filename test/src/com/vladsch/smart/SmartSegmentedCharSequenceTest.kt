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

class SmartSegmentedCharSequenceTest() {
    val string123 = """0123456789
0123456789
0123456789
0123456789
"""
    val chars123 = string123.toCharArray()

    val stringAbc = """abcdefghijklmnopqrstuvwxyz
ABCDEFGHIJKLMNOPQRSTUVWXYZ
abcdefghijklmnopqrstuvwxyz
ABCDEFGHIJKLMNOPQRSTUVWXYZ
"""
    val charsAbc = stringAbc.toCharArray()

    @Test
    fun test_basic() {
        val charSeq = SmartCharArraySequence(chars123, 0, string123.length);
        val charSeq1 = charSeq.subSequence(0, 11)
        val charSeq2 = charSeq.subSequence(11, 22)
        val charSeq3 = charSeq.subSequence(22, 33)

        val charSeqOf = SmartCharSequenceBase.smart(charSeq1, charSeq2, charSeq3)

        assertEquals(string123.substring(0, 33), charSeqOf.toString())

        // should have combined into a single sequence
        assertEquals(true, charSeqOf is SmartCharArraySequence)
    }

    @Test
    fun test_delete() {
        val charSeq = SmartCharArraySequence(chars123, 0, string123.length)
        var testCount = 0;
        for (j in 0..string123.length) {
            for (i in 0..string123.length - j) {
                val charSeq1 = charSeq.delete(i, i + j)
                assertEquals("delete($i, ${i + j})", string123.substring(0, i) + string123.substring(i + j), charSeq1.toString())
                testCount++
            }
        }

        println("$testCount delete tests passed")
    }

    @Test
    fun test_insert() {
        val charSeq = SmartCharArraySequence(chars123, 0, string123.length)
        var testCount = 0;
        for (j in 0..string123.length) {
            for (i in 0..string123.length - j) {
                val charSeq1 = charSeq.insert(charSeq.subSequence(i, i + j), j)
                assertEquals("insert(charSeq($i, ${i + j}), $j)", string123.substring(0, j) + string123.substring(i, i + j) + string123.substring(j), charSeq1.toString())
                testCount++
            }
        }
        println("$testCount insert tests passed")
    }

    @Test
    fun test_replace() {
        val charSeq = SmartCharArraySequence(chars123, 0, string123.length)
        var testCount = 0;
        for (j in 0..string123.length) {
            for (i in 0..string123.length - j) {
                for (k in 0..string123.length - j) {
                    val charSeq1 = charSeq.replace(charSeq.subSequence(i, i + j), k, k + j)
                    assertEquals("replace(charSeq($i, ${i + j}), $k, ${k + j})", string123.substring(0, k) + string123.substring(i, i + j) + string123.substring(k + j), charSeq1.toString())
//                    if (TrackingCharSequenceBase.MAX_OPTIMIZATION && string123.substring(i, i + j) == string123.substring(k, k + j)) {
//                        // the result should be a TrackingCharArraySequence
//                        if (charSeq1 !is TrackingCharArraySequence) {
//                            val charSeq2 = charSeq.replace(charSeq.subSequence(i, i + j), k, k + j)
//                        }
//                        assertEquals(true, charSeq1 is TrackingCharArraySequence)
//                    }
                    testCount++
                }
            }
        }
        println("$testCount replace tests passed")
    }

    @Test
    fun test_originalOffsets() {
        val charSeq123 = SmartCharArraySequence(chars123, 0, chars123.size)
        val charSeqAbc = SmartCharArraySequence(charsAbc, 0, charsAbc.size)

        val listSeq = SmartSegmentedCharSequence(
                charSeq123.subSequence(0, 11)
                , charSeqAbc.subSequence(0, 27)
                , charSeq123.subSequence(11, 22)
                , charSeqAbc.subSequence(27, 54)
                , charSeq123.subSequence(22, 33)
                , charSeqAbc.subSequence(54, 81)
                , charSeq123.subSequence(33, 44)
                , charSeqAbc.subSequence(81, 108)
        )

        println(listSeq)

        var reconsAbc = ""
        var recons123 = ""
        for (i in 0..listSeq.lastIndex) {
//            println(i.toString() + ": " + listSeq[i])
//            print(listSeq[i])

            val location = listSeq.trackedSourceLocation(i)
            val offset123 = listSeq.trackedSourceLocation(i).offset
            val offsetAbc = listSeq.trackedSourceLocation(i).offset

            if (location.source === chars123) {
                assertEquals(true, location.offset == offset123)
                assertEquals(true, listSeq[i].isDigit() || listSeq[i] == '\n')
                recons123 += listSeq[i]
            } else {
                assertEquals(charsAbc, location.source)
                assertEquals(true, location.offset == offsetAbc)
                assertEquals(true, listSeq[i].isLetter() || listSeq[i] == '\n')
                reconsAbc += listSeq[i]
            }
        }

        assertEquals(recons123, string123)
        assertEquals(reconsAbc, stringAbc)
    }

    @Test
    fun test_originalOffsetsCached() {
        val charSeq123 = SmartCharArraySequence(chars123, 0, chars123.size)
        val charSeqAbc = SmartCharArraySequence(charsAbc, 0, charsAbc.size)

        val listSeq = SmartSegmentedCharSequence(
                charSeq123.subSequence(0, 11)
                , charSeqAbc.subSequence(0, 27)
                , charSeq123.subSequence(11, 22)
                , charSeqAbc.subSequence(27, 54)
                , charSeq123.subSequence(22, 33)
                , charSeqAbc.subSequence(54, 81)
                , charSeq123.subSequence(33, 44)
                , charSeqAbc.subSequence(81, 108)
        )

        val charSeq123Cached = charSeq123.cachedProxy
        assertEquals(charSeq123Cached, charSeq123)
        val charSeqAbcCached = charSeqAbc.cachedProxy
        assertEquals(charSeqAbcCached, charSeqAbc)
        val listSeqCached = listSeq.cachedProxy
        assertEquals(listSeqCached, listSeq)

        println(listSeq)

        var reconsAbc = ""
        var recons123 = ""
        for (i in 0..listSeq.lastIndex) {
//            println(i.toString() + ": " + listSeq[i])
//            print(listSeq[i])

            val location = listSeq.trackedSourceLocation(i)
            val offset123 = listSeq.trackedSourceLocation(i).offset
            val offsetAbc = listSeq.trackedSourceLocation(i).offset

            if (location.source === chars123) {
                assertEquals(true, location.offset == offset123)
                assertEquals(true, listSeq[i].isDigit() || listSeq[i] == '\n')
                recons123 += listSeq[i]
            } else {
                assertEquals(charsAbc, location.source)
                assertEquals(true, location.offset == offsetAbc)
                assertEquals(true, listSeq[i].isLetter() || listSeq[i] == '\n')
                reconsAbc += listSeq[i]
            }
        }

        assertEquals(recons123, string123)
        assertEquals(reconsAbc, stringAbc)
    }
}
