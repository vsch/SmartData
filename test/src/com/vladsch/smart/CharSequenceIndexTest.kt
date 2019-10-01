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
        val seq1 = SafeCharSequenceIndex(emptySeq)
        assertEquals(0, seq1.safeErrors)
        seq1.char
        assertEquals(1, seq1.safeErrors)
        seq1.char
        assertEquals(2, seq1.safeErrors)

        val seq2 = SafeCharSequenceIndex(emptySeq, 0)
        assertEquals(0, seq2.safeErrors)
        seq2.char
        assertEquals(1, seq2.safeErrors)
        seq2.char
        assertEquals(2, seq2.safeErrors)

        val seq3 = SafeCharSequenceIndex(emptySeq, 1)
        assertEquals(1, seq3.safeErrors)
        seq3.char
        assertEquals(2, seq3.safeErrors)
        seq3.char
        assertEquals(3, seq3.safeErrors)
    }

    @Test
    fun clearErrors() {
        val seq = SafeCharSequenceIndex(emptySeq)
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
        val seq = SafeCharSequenceIndex(emptySeq)
        assertEquals(false, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
        seq.char
        assertEquals(true, seq.hadSafeErrors)
    }

    @Test
    fun clearHadErrors() {
        val seq = SafeCharSequenceIndex(emptySeq)
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
        val seq = SafeCharSequenceIndex(emptySeq)
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
                    val seq = SafeCharSequenceRange(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            for (seqIndex in seq.startIndex - 3..seq.endIndex + 3) {
                                val seqInd = SafeCharSequenceIndex(seq)
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
                    val seq = SafeCharSequenceRange(rawSeq, start, end)
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

                            for (seqIndex in  -3..seq.length + 3) {
                                val seqInd = SafeCharSequenceIndex(seq)

                                val expIndex = seqIndex.max(0).min(seq.length)
                                val expChar = if (seqIndex < 0) '!' else if (seqIndex >= seq.length) '*' else rawSeq[start + seq.startIndex + expIndex]

                                assertEquals("Testing [] $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expChar", expChar, seq[seqIndex])

                                seqInd.index = seqIndex
                                assertEquals("Testing index $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndex", expIndex, seqInd.index)

                                val expIndexChar = if (seq.startIndex == seq.endIndex || seqIndex >= seq.length) '*' else rawSeq[start + seq.startIndex + expIndex]
                                assertEquals("Testing char $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndexChar", expIndexChar, seqInd.char)

                                assertEquals("Testing beforeIndex ${seqInd.index} in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(rawSeq.subSequence(start, end).subSequence(seq.startIndex, seq.startIndex + expIndex)).toString(), StringBuilder().append(seqInd.beforeIndexChars).toString())

                                val expToString = (rawSeq.subSequence(start, end).subSequence(seq.startIndex + expIndex, seq.endIndex)).asString()
                                val actualToString = (seqInd.afterIndexChars).asString()
//                                if (!expToString.equals(actualToString)) {
//                                    val testToString = (seqInd.afterIndexChars).asString()
//                                }
                                assertEquals("Testing afterIndex ${seqInd.index} in [${seq.startIndex}, ${seq.endIndex})", expToString, actualToString)
                            }
                        }
                    }
                }
            }
        }
    }

    val spc1 = " "
    val spc2 = "  "
    val spc3 = "   "

    val string = """Line0
Line1
 Line2
  Line3
   Line4
Line5$spc1
Line6$spc2
Line7$spc3
 Line8$spc1
  Line9$spc2
   Line10$spc3

$spc1
$spc2
$spc3
Line15



Partial Line"""

    val chars = string.toCharArray()
    val charLines = SmartCharArraySequence(chars).splitPartsSegmented('\n', true)

    @Test
    fun test_segmentedLines() {
        for (i in 0..charLines.segments.lastIndex) {
            if (i > 0) assertEquals("Testing segment[$i]: '${charLines.segments[i].asString()}'", '\n', charLines[charLines.startIndex(i) - 1])
            if (i < charLines.segments.lastIndex) assertEquals("Testing segment[$i]: '${charLines.segments[i].asString()}'", '\n', charLines[charLines.endIndex(i) - 1])
        }
    }

    @Test
    fun getStartOfLine() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.startIndex(i), csInfo2.startOfLine)

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.startIndex(i), csInfo.startOfLine)
            }
        }
    }

    @Test
    fun getStartOfLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i), 0)
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", 0, csInfo2.startOfLine)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", 0, csInfo.startOfLine)
            }
        }
    }

    @Test
    fun getStartOfNextSkipLines() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)

                for (skip in 0..charLines.segments.lastIndex - i + 3) {
                    val skipped = if (i + skip > charLines.segments.lastIndex) charLines.length else charLines.endIndex(i + skip)
                    val startSkipped = csInfo2.startOfNextSkipLines(skip)

                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", skipped, startSkipped)

                    csInfo.index = charLines.startIndex(i) + j
                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", skipped, csInfo.startOfNextSkipLines(skip))
                }
            }
        }
    }

    @Test
    fun getStartOfNextSkipLinesSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i), 0)
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)

                for (skip in 0..charLines.segments.lastIndex - i + 3) {
                    val skipped = charLines.endIndex(i) - charLines.startIndex(i)

                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", skipped, csInfo2.startOfNextSkipLines(skip))

                    csInfo.index = j
                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", skipped, csInfo.startOfNextSkipLines(skip))
                }
            }
        }
    }

    @Test
    fun getEndOfPreviousSkipLines() {
//        val csInfoTest = SafeCharSequenceIndex(charLines.getSequence(1), 6)
//        val prevEnd = csInfoTest.endOfPreviousSkipLines(1)

        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)

                for (skip in 0..i + 3) {
                    val skipped = if (i - skip - 1 < 0) 0 else (charLines.endIndex(i - skip - 1) - 1).minLimit(0)
                    val endOfPreviousSkipLines = csInfo2.endOfPreviousSkipLines(skip)
//                    if (skipped != endOfPreviousSkipLines) {
//                        val test = csInfo2.endOfPreviousSkipLines(skip)
//                        val tmp = 0
//                    }
                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", skipped, endOfPreviousSkipLines)

                    csInfo.index = charLines.startIndex(i) + j
                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", skipped, csInfo.endOfPreviousSkipLines(skip))
                }
            }
        }
    }

    @Test
    fun getEndOfPreviousSkipLinesSingle() {
//        val csInfoTest = SafeCharSequenceIndex(charLines.getSequence(0), 0)
//        val prevEnd = csInfoTest.endOfPreviousSkipLines(1)

        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i), 0)
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)

                for (skip in 0..3) {
                    val skipped = 0

                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank} prevEnd:${csInfo2.endOfPreviousLine} nextStart:${csInfo2.startOfNextLine}", skipped, csInfo2.endOfPreviousSkipLines(skip))

                    csInfo.index = j
                    assertEquals("i:$i, j:$j skip:$skip index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank} prevEnd:${csInfo2.endOfPreviousLine} nextStart:${csInfo2.startOfNextLine}", skipped, csInfo.endOfPreviousSkipLines(skip))
                }
            }
        }
    }

    @Test
    fun getEndOfLine() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals(charLines.startIndex(i) + j, csInfo2.index)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.endIndex(i), csInfo2.endOfLine)

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.endIndex(i), csInfo.endOfLine)
            }
        }
    }

    @Test
    fun getEndOfLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.endIndex(i) - charLines.startIndex(i), csInfo2.endOfLine)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.endIndex(i) - charLines.startIndex(i), csInfo.endOfLine)
            }
        }
    }

    @Test
    fun getFirstNonBlank() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.startIndex(i) + charLines.segments[i].countLeading(' '), csInfo2.firstNonBlank)

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.startIndex(i) + charLines.segments[i].countLeading(' '), csInfo.firstNonBlank)
            }
        }
    }

    @Test
    fun getFirstNonBlankSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo2.firstNonBlank)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo.firstNonBlank)
            }
        }
    }

    @Test
    fun getIndent() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo2.indent)

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo.indent)
            }
        }
    }

    @Test
    fun getIndentSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo2.indent)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo.indent)
            }
        }
    }

    @Test
    fun getLastNonBlank() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.lastIndex(i) - charLines.segments[i].countTrailing(' '), csInfo2.lastNonBlank)
                csInfo.index = charLines.startIndex(i) + j

                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.lastIndex(i) - charLines.segments[i].countTrailing(' '), csInfo.lastNonBlank)
            }
        }
    }

    @Test
    fun getLastNonBlankSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.lastIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' '), csInfo2.lastNonBlank)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.lastIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' '), csInfo.lastNonBlank)
            }
        }
    }

    @Test
    fun isBlankLine() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", i in 11..14 || i in 16..18, csInfo2.isBlankLine)

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..14 || i in 16..18, csInfo.isBlankLine)
            }
        }
    }

    @Test
    fun isBlankLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", i in 11..14 || i in 16..18, csInfo2.isBlankLine)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..14 || i in 16..18, csInfo.isBlankLine)
            }
        }
    }

    @Test
    fun isEmptyLine() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", i in 11..11 || i in 16..18, csInfo2.isEmptyLine)

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..11 || i in 16..18, csInfo.isEmptyLine)
            }
        }
    }

    @Test
    fun isEmptyLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", i in 11..11 || i in 16..18, csInfo2.isEmptyLine)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..11 || i in 16..18, csInfo.isEmptyLine)
            }
        }
    }

    @Test
    fun getColumn() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", j, csInfo2.column)

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.column)
            }
        }
    }

    @Test
    fun getColumnSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", j, csInfo2.column)

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.column)
            }
        }
    }

    @Test
    fun columnOf() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", j, csInfo2.columnOf(charLines.startIndex(i) + j))

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.columnOf(charLines.startIndex(i) + j))
            }
        }
    }

    @Test
    fun columnOfSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", j, csInfo2.columnOf(j))

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.columnOf(j))
            }
        }
    }

    @Test
    fun getLineChars() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.segments[i].asString(), csInfo2.lineChars.asString())

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].asString(), csInfo.lineChars.asString())
            }
        }
    }

    @Test
    fun getLineCharsOfSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", charLines.segments[i].asString(), csInfo2.lineChars.asString())

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].asString(), csInfo.lineChars.asString())
            }
        }
    }

    @Test
    fun getStartOfLineToIndexChars() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                val asString = charLines.segments[i].subSequence(0, j).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.startOfLineToIndexChars.asString())

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.startOfLineToIndexChars.asString())
            }
        }
    }

    @Test
    fun getStartOfLineToIndexCharsSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                val asString = charLines.segments[i].subSequence(0, j).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.startOfLineToIndexChars.asString())

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.startOfLineToIndexChars.asString())
            }
        }
    }

    @Test
    fun getFirstNonBlankToIndexChars() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                val asString = charLines.segments[i].subSequence(charLines.segments[i].countLeading(' ', '\t').min(j), j).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.firstNonBlankToIndexChars.asString())

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.firstNonBlankToIndexChars.asString())
            }
        }
    }

    @Test
    fun getFirstNonBlankToIndexCharsSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            val firstNonBlank = charLines.segments[i].countLeading(' ', '\t')

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                val asString = charLines.segments[i].subSequence(firstNonBlank.min(j), j).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.firstNonBlankToIndexChars.asString())

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.firstNonBlankToIndexChars.asString())
            }
        }
    }

    @Test
    fun getIndexToLastNonBlankChars() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' ')
//            val firstNonBlank = charLines.segments[i].countLeading(' ', '\t')

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                val asString = charLines.segments[i].subSequence(j, lastNonBlank).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.afterIndexToLastNonBlankChars.asString())

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.afterIndexToLastNonBlankChars.asString())
            }
        }
    }

    @Test
    fun getIndexToLastNonBlankCharsSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' ')
//            val firstNonBlank = charLines.segments[i].countLeading(' ', '\t')

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                val asString = charLines.segments[i].subSequence(j, lastNonBlank).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.afterIndexToLastNonBlankChars.asString())

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.afterIndexToLastNonBlankChars.asString())
            }
        }
    }

    @Test
    fun getFirstNonBlankToLastNonBlankChars() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' ')
            val firstNonBlank = charLines.segments[i].countLeading(' ', '\t')

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                val asString = charLines.segments[i].subSequence(firstNonBlank, lastNonBlank).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.firstToLastNonBlankLineChars.asString())

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.firstToLastNonBlankLineChars.asString())
            }
        }
    }

    @Test
    fun getFirstNonBlankToLastNonBlankCharsSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' ')
            val firstNonBlank = charLines.segments[i].countLeading(' ', '\t')

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                val asString = charLines.segments[i].subSequence(firstNonBlank, lastNonBlank).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.firstToLastNonBlankLineChars.asString())

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.firstToLastNonBlankLineChars.asString())
            }
        }
    }

    @Test
    fun getStartOfLineToLastNonBlankChars() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' ')
            val firstNonBlank = 0

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)
                val asString = charLines.segments[i].subSequence(firstNonBlank, lastNonBlank).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.startOfLineToLastNonBlankChars.asString())

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.startOfLineToLastNonBlankChars.asString())
            }
        }
    }

    @Test
    fun getStartOfLineToLastNonBlankCharsSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' ')
            val firstNonBlank = 0

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)
                val asString = charLines.segments[i].subSequence(firstNonBlank, lastNonBlank).asString()

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.startOfLineToLastNonBlankChars.asString())

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.startOfLineToLastNonBlankChars.asString())
            }
        }
    }

    @Test
    fun getFirstNonBlankToEndOfLineChars() {
        val csInfo = SafeCharSequenceIndex(charLines)
        for (i in 0..charLines.segments.lastIndex) {
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i)
            val firstNonBlank = charLines.segments[i].countLeading(' ', '\t')
            val asString = charLines.segments[i].subSequence(firstNonBlank, lastNonBlank).asString()

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines, charLines.startIndex(i) + j)

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.firstToLastNonBlankLineChars.asString())

                csInfo.index = charLines.startIndex(i) + j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.firstToLastNonBlankLineChars.asString())
            }
        }
    }

    @Test
    fun getFirstNonBlankToEndOfLineCharsSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            val csInfo = SafeCharSequenceIndex(charLines.getSequence(i))
            val lastNonBlank = charLines.endIndex(i) - charLines.startIndex(i)
            val firstNonBlank = charLines.segments[i].countLeading(' ', '\t')
            val asString = charLines.segments[i].subSequence(firstNonBlank, lastNonBlank).asString()

            for (j in 0..(charLines.lastIndex(i) - charLines.startIndex(i))) {
                val csInfo2 = SafeCharSequenceIndex(charLines.getSequence(i), j)

                assertEquals("i:$i, j:$j index:${csInfo2.index} start:${csInfo2.startOfLine} end:${csInfo2.endOfLine} first:${csInfo2.firstNonBlank} last:${csInfo2.lastNonBlank}", asString, csInfo2.firstToLastNonBlankLineChars.asString())

                csInfo.index = j
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", asString, csInfo.firstToLastNonBlankLineChars.asString())
            }
        }
    }
}
