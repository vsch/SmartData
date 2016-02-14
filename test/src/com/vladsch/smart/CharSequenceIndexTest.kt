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

class CharSequenceIndexTest {

    val emptySeq = EMPTY_SEQUENCE
    val blankSeq = RepeatedCharSequence(' ', 10)
    val digitSeq = SmartCharArraySequence("0123456789".toCharArray())
    val digitBlankSeq = SmartCharArraySequence("\n   \t  0123456789\n  \t \n".toCharArray())
    val letterSeq = SmartCharArraySequence("abcdefghijklmnopqrstuvwxyz".toCharArray())
    val oneLineSeq = SmartCharArraySequence("0123456789\n".toCharArray())
    val twoPartialLinesSeq = SmartCharArraySequence("0123456789\nabcdefghijklmnopqrstuvwxyz".toCharArray())
    val twoFullLinesSeq = SmartCharArraySequence("0123456789\nabcdefghijklmnopqrstuvwxyz\n".toCharArray())
    val threePartialLinesSeq = SmartCharArraySequence("0123456789\nabcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray())
    val threeFullLinesSeq = SmartCharArraySequence("0123456789\nabcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n".toCharArray())

    val allCharTestSequences = listOf(
            emptySeq,
            blankSeq,
            digitSeq,
            digitBlankSeq,
            letterSeq
    )

    val allLineTestSequences = listOf(
            emptySeq,
            digitSeq,
            letterSeq,
            oneLineSeq,
            twoPartialLinesSeq,
            twoFullLinesSeq,
            threePartialLinesSeq,
            threeFullLinesSeq
    )


    @Test
    fun getErrors() {
        val seq1 = SafeCharSequenceIndexImpl(emptySeq)
        assertEquals(0, seq1.safeErrors)
        seq1.char
        assertEquals(1, seq1.safeErrors)
        seq1.char
        assertEquals(2, seq1.safeErrors)

        val seq2 = SafeCharSequenceIndexImpl(emptySeq, 0)
        assertEquals(0, seq2.safeErrors)
        seq2.char
        assertEquals(1, seq2.safeErrors)
        seq2.char
        assertEquals(2, seq2.safeErrors)

        val seq3 = SafeCharSequenceIndexImpl(emptySeq, 1)
        assertEquals(1, seq3.safeErrors)
        seq3.char
        assertEquals(2, seq3.safeErrors)
        seq3.char
        assertEquals(3, seq3.safeErrors)
    }

    @Test
    fun clearErrors() {
        val seq = SafeCharSequenceIndexImpl(emptySeq)
        assertEquals(0, seq.safeErrors)
        seq.char
        assertEquals(1, seq.safeErrors)
        seq.char
        assertEquals(2, seq.safeErrors)
        seq.clearSafeErrors()
        assertEquals(0, seq.safeErrors)
        seq.char
        assertEquals(1, seq.safeErrors)
        seq.char
        assertEquals(2, seq.safeErrors)
    }

    @Test
    fun getHadErrors() {
        val seq = SafeCharSequenceIndexImpl(emptySeq)
        assertEquals(false, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
    }

    @Test
    fun clearHadErrors() {
        val seq = SafeCharSequenceIndexImpl(emptySeq)
        assertEquals(false, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        seq.clearHadSafeErrors()
        assertEquals(2, seq.safeErrors)
        assertEquals(false, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
    }

    @Test
    fun getHadErrorsAndClear() {
        val seq = SafeCharSequenceIndexImpl(emptySeq)
        assertEquals(false, seq.hadSafeErrors)
        assertEquals(false, seq.hadSafeErrorsAndClear)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        assertEquals(true, seq.hadSafeErrorsAndClear)
        assertEquals(1, seq.safeErrors)
        assertEquals(false, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        assertEquals(true, seq.hadSafeErrorsAndClear)
        assertEquals(2, seq.safeErrors)
        assertEquals(false, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        assertEquals(true, seq.hadSafeErrorsAndClear)
        assertEquals(3, seq.safeErrors)
        assertEquals(false, seq.hadSafeErrors)
    }

    @Test
    fun setIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequenceRangeImpl(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            for (seqIndex in seq.startIndex - 3..seq.endIndex + 3) {
                                val seqInd = SafeCharSequenceIndexImpl(seq)
                                val expIndex = seqIndex.max(0).min(seq.length)
                                seqInd.index = seqIndex
                                assertEquals("Testing $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndex", expIndex, seqInd.index)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun getChar() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequenceRangeImpl(rawSeq, start, end)
                    seq.beforeStartNonChar = '!'
                    seq.afterEndNonChar = '*'
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            assertEquals("Testing beforeStart in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(rawSeq.subSequence(start, end).subSequence(0, seq.startIndex)).toString(), StringBuilder().append(seq.beforeStart).toString())
                            assertEquals("Testing afterEnd in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(rawSeq.subSequence(start, end).subSequence(seq.endIndex, seq.rawLength)).toString(), StringBuilder().append(seq.afterEnd).toString())

                            for (seqIndex in  - 3..seq.length + 3) {
                                val seqInd = SafeCharSequenceIndexImpl(seq)

                                val expIndex = seqIndex.max(0).min(seq.length)
                                val expChar = if (seqIndex < 0) '!' else if (seqIndex >= seq.length) '*' else rawSeq[start + seq.startIndex + expIndex]

                                assertEquals("Testing [] $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expChar", expChar, seq[seqIndex])

                                seqInd.index = seqIndex
                                assertEquals("Testing index $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndex", expIndex, seqInd.index)

                                val expIndexChar = if (seq.startIndex == seq.endIndex || seqIndex >= seq.length) '*' else rawSeq[start + seq.startIndex + expIndex]
                                assertEquals("Testing char $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndexChar", expIndexChar, seqInd.char)

                                assertEquals("Testing beforeIndex ${seqInd.index} in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(rawSeq.subSequence(start, end).subSequence(seq.startIndex, seq.startIndex+expIndex)).toString(), StringBuilder().append(seqInd.beforeIndex).toString())

                                val expToString = StringBuilder().append(rawSeq.subSequence(start, end).subSequence(seq.startIndex+expIndex, seq.endIndex)).toString()
                                val actualToString = StringBuilder().append(seqInd.afterIndex).toString()
                                if (!expToString.equals(actualToString)) {
                                    val testToString = StringBuilder().append(seqInd.afterIndex).toString()
                                }
                                assertEquals("Testing afterIndex ${seqInd.index} in [${seq.startIndex}, ${seq.endIndex})", expToString, actualToString)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun getLine() {

    }

    @Test
    fun getStartOfLine() {

    }

    @Test
    fun getEndOfLine() {

    }

    @Test
    fun getLastNonBlank() {

    }

    @Test
    fun getFirstNonBlank() {

    }

    @Test
    fun isBlankLine() {

    }

    @Test
    fun isEmptyLine() {

    }

    @Test
    fun getIndent() {

    }

    @Test
    fun getColumn() {

    }

    @Test
    fun columnOf() {

    }

}
