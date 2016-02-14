/*
 * Copyright (c) 2015-2016 Vladimir Schneider <vladimir.schneider@gmail.com>, all rights reserved.
 *
 * This code is private property of the copyright holder and cannot be used without
 * having obtained a license or prior written permission of the of the copyright holder.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package com.vladsch.smart

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeCharSequenceTest {

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
        val seq1 = SafeCharSequence(emptySeq)
        assertEquals(0, seq1.errors)
        seq1.index++
        assertEquals(1, seq1.errors)
        seq1.index++
        assertEquals(2, seq1.errors)

        val seq2 = SafeCharSequence(emptySeq, 0, 1)
        assertEquals(1, seq2.errors)
        seq2.index++
        assertEquals(2, seq2.errors)
        seq2.index++
        assertEquals(3, seq2.errors)

        val seq3 = SafeCharSequence(emptySeq, 1, 0)
        assertEquals(1, seq3.errors)
        seq3.index++
        assertEquals(2, seq3.errors)
        seq3.index++
        assertEquals(3, seq3.errors)

        val seq4 = SafeCharSequence(emptySeq, 1, 1)
        assertEquals(2, seq4.errors)
        seq4.index++
        assertEquals(3, seq4.errors)
        seq4.index++
        assertEquals(4, seq4.errors)
    }

    @Test
    fun clearErrors() {
        val seq = SafeCharSequence(emptySeq)
        assertEquals(0, seq.errors)
        seq.index++
        assertEquals(1, seq.errors)
        seq.index++
        assertEquals(2, seq.errors)
        seq.clearErrors()
        assertEquals(0, seq.errors)
        seq.index++
        assertEquals(1, seq.errors)
        seq.index++
        assertEquals(2, seq.errors)
    }

    @Test
    fun getHadErrors() {
        val seq = SafeCharSequence(emptySeq)
        assertEquals(false, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
    }

    @Test
    fun clearHadErrors() {
        val seq = SafeCharSequence(emptySeq)
        assertEquals(false, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
        seq.clearHadErrors()
        assertEquals(2, seq.errors)
        assertEquals(false, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
    }

    @Test
    fun getHadErrorsAndClear() {
        val seq = SafeCharSequence(emptySeq)
        assertEquals(false, seq.hadErrors)
        assertEquals(false, seq.hadErrorsAndClear)
        seq.index++
        assertEquals(true, seq.hadErrors)
        assertEquals(true, seq.hadErrorsAndClear)
        assertEquals(1, seq.errors)
        assertEquals(false, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
        assertEquals(true, seq.hadErrorsAndClear)
        assertEquals(2, seq.errors)
        assertEquals(false, seq.hadErrors)
        seq.index++
        assertEquals(true, seq.hadErrors)
        assertEquals(true, seq.hadErrorsAndClear)
        assertEquals(3, seq.errors)
        assertEquals(false, seq.hadErrors)
    }

    @Test
    fun safeRawIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (i in start - 3..end + 3) {
                        val safeRawIndex = seq.safeRawIndex(i)
                        var expStart = i.max(0).min(seq.length)
                        assertEquals("Testing: $i for [$start, $end) exp $expStart", expStart, safeRawIndex)
                    }
                }
            }
        }
    }

    @Test
    fun safeRawInclusiveIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (i in start - 3..end + 3) {
                        val safeRawIndex = seq.safeRawInclusiveIndex(i)
                        var expStart = i.max(0).min((seq.length - 1).max(0))
                        assertEquals("Testing: $i for [$start, $end) exp $expStart", expStart, safeRawIndex)
                    }
                }
            }
        }
    }

    @Test
    fun safeRawRange() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (i in start - 3..end + 3) {
                        for (j in start - 3..end + 3) {
                            val pair = seq.safeRawRange(i, j)
                            var expEnd = j.max(0).min(seq.length)
                            var expStart = i.max(0).min(expEnd)

                            assertEquals("Testing: $i, $j for [$start, $end) exp [$expStart, $expEnd)", expStart, pair.first)
                            assertEquals("Testing: $i, $j for [$start, $end) exp [$expStart, $expEnd)", expEnd, pair.second)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun rawCharIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (i in -3..seq.length + 3) {
                        val safeRawIndex = seq.safeRawCharIndex(i)
                        var expStart = start + i.max(0).min(seq.length)
                        assertEquals("Testing: $i for [$start, $end) exp $expStart", expStart, safeRawIndex)
                    }
                }
            }
        }
    }

    @Test
    fun setStartEndIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.endIndex = seqEnd
                            assertEquals("Testing $seqEnd in [0, ${seq.rawLength})", seqEnd.max(0).min(seq.rawLength), seq.endIndex)

                            seq.startIndex = seqStart
                            assertEquals("Testing $seqStart in [0, ${seq.rawLength})", seqStart.max(0).min(seq.rawLength), seq.startIndex)

                            // test end changed to not be before start
                            if (seqEnd.max(0).min(seq.rawLength) < seqStart.max(0).min(seq.rawLength)) assertEquals(seq.startIndex, seq.endIndex)
                            else assertEquals(seqEnd.max(0).min(seq.rawLength), seq.endIndex)

                            seq.startIndex = seqStart
                            assertEquals("Testing $seqStart in [0, ${seq.rawLength})", seqStart.max(0).min(seq.rawLength), seq.startIndex)

                            seq.endIndex = seqEnd
                            assertEquals("Testing $seqEnd in [0, ${seq.rawLength})", seqEnd.max(0).min(seq.rawLength), seq.endIndex)

                            // test start changed to not be after end
                            if (seqStart.max(0).min(seq.rawLength) > seqEnd.max(0).min(seq.rawLength)) assertEquals(seq.endIndex, seq.startIndex)
                            else assertEquals(seqStart.max(0).min(seq.rawLength), seq.startIndex)

                            assertEquals(seq.endIndex - seq.startIndex, seq.length)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun safeIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            for (seqIndex in seq.startIndex - 3..seq.endIndex + 3) {
                                val expIndex = seqIndex.max(seq.startIndex).min(seq.endIndex)
                                assertEquals("Testing $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndex", expIndex, seq.safeIndex(seqIndex))
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun safeInclusiveIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            for (seqIndex in seq.startIndex - 3..seq.endIndex + 3) {
                                val expIndex = seqIndex.max(seq.startIndex).min((seq.endIndex - 1).max(seq.startIndex))
                                assertEquals("Testing $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndex", expIndex, seq.safeInclusiveIndex(seqIndex))
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun safeRange() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            for (seqIndexStart in seq.startIndex - 3..seq.endIndex + 3) {
                                for (seqIndexEnd in seq.startIndex - 3..seq.endIndex + 3) {
                                    val expIndexEnd = seqIndexEnd.max(seq.startIndex).min(seq.endIndex)
                                    val expIndexStart = (seqIndexStart.max(seq.startIndex).min(seq.endIndex)).min(expIndexEnd)
                                    val pair = seq.safeRange(seqIndexStart, seqIndexEnd)
                                    assertEquals("Testing $seqIndexStart, $seqIndexEnd in [${seq.startIndex}, ${seq.endIndex}) exp ($expIndexStart, $expIndexEnd)", expIndexStart, pair.first)
                                    assertEquals("Testing $seqIndexStart, $seqIndexEnd in [${seq.startIndex}, ${seq.endIndex}) exp ($expIndexStart, $expIndexEnd)", expIndexEnd, pair.second)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun setIndex() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            for (seqIndex in seq.startIndex - 3..seq.endIndex + 3) {
                                val expIndex = seqIndex.max(seq.startIndex).min(seq.endIndex)
                                seq.index = seqIndex
                                assertEquals("Testing $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndex", expIndex, seq.index)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun nonChars() {
        val seq = SafeCharSequence(emptySeq)

        assertEquals('\u0000', seq[-1])
        assertEquals('\u0000', seq[0])
        assertEquals('\u0000', seq[1])

        val seq1 = SafeCharSequence(digitSeq)

        assertEquals('\u0000', seq1[-2])
        assertEquals('\u0000', seq1[-1])
        assertEquals('\u0000', seq1[10])
        assertEquals('\u0000', seq1[11])

        val firstNonChars = "!@#"
        val lastNonChars = "!@#"

        for (first in firstNonChars) {
            seq1.beforeStartNonChar = first
            assertEquals(first, seq1.beforeStartNonChar)
            assertEquals('\u0000', seq1.afterEndNonChar)

            assertEquals(first, seq1[-2])
            assertEquals(first, seq1[-1])
            assertEquals('\u0000', seq1[10])
            assertEquals('\u0000', seq1[11])
        }

        val seq2 = SafeCharSequence(digitSeq)
        for (last in lastNonChars) {
            seq2.afterEndNonChar = last
            assertEquals('\u0000', seq2.beforeStartNonChar)
            assertEquals(last, seq2.afterEndNonChar)

            assertEquals('\u0000', seq2[-2])
            assertEquals('\u0000', seq2[-1])
            assertEquals(last, seq2[10])
            assertEquals(last, seq2[11])
        }

        for (first in firstNonChars) {
            seq1.beforeStartNonChar = first
            for (last in lastNonChars) {
                seq1.afterEndNonChar = last
                assertEquals(first, seq1.beforeStartNonChar)
                assertEquals(last, seq1.afterEndNonChar)

                assertEquals(first, seq1[-2])
                assertEquals(first, seq1[-1])
                assertEquals(last, seq1[10])
                assertEquals(last, seq1[11])
            }
        }
    }

    @Test
    fun getChar() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
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

                            for (seqIndex in seq.startIndex - 3..seq.endIndex + 3) {
                                val expIndex = seqIndex.max(seq.startIndex).min(seq.endIndex)
                                val expChar = if (seqIndex < seq.startIndex) '!' else if (seqIndex >= seq.endIndex) '*' else rawSeq[start + expIndex]

                                assertEquals("Testing [] $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expChar", expChar, seq[seqIndex])

                                seq.index = seqIndex
                                val expIndexChar = if (seq.startIndex == seq.endIndex || seqIndex >= seq.endIndex) '*' else rawSeq[start + expIndex]
                                assertEquals("Testing char $seqIndex in [${seq.startIndex}, ${seq.endIndex}) exp $expIndexChar", expIndexChar, seq.char)


                                assertEquals("Testing beforeIndex ${seq.index} in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(rawSeq.subSequence(start, end).subSequence(seq.startIndex, expIndex)).toString(), StringBuilder().append(seq.beforeIndex).toString())
                                assertEquals("Testing afterIndex ${seq.index} in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(rawSeq.subSequence(start, end).subSequence(expIndex, seq.endIndex)).toString(), StringBuilder().append(seq.afterIndex).toString())
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun getFirstChar() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    seq.beforeStartNonChar = '!'
                    seq.afterEndNonChar = '*'
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            val expChar = if (seq.startIndex >= seq.endIndex) '*' else rawSeq[start + seq.startIndex]

                            assertEquals("Testing 0 in [${seq.startIndex}, ${seq.endIndex}) exp $expChar", expChar, seq.firstChar)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun getLastChar() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    seq.beforeStartNonChar = '!'
                    seq.afterEndNonChar = '*'
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            val expChar = if (seq.startIndex > seq.endIndex - 1) '!' else rawSeq[start + seq.endIndex - 1]

                            assertEquals("Testing 0 in [${seq.startIndex}, ${seq.endIndex}) exp $expChar", expChar, seq.lastChar)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun isEmpty() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    seq.beforeStartNonChar = '!'
                    seq.afterEndNonChar = '*'
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    assertEquals("Testing isEmpty in [${seq.startIndex}, ${seq.endIndex})", seq.length == 0, seq.isEmpty)
                }
            }
        }
    }

    @Test
    fun isBlank() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    seq.beforeStartNonChar = '!'
                    seq.afterEndNonChar = '*'
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    assertEquals("Testing isBlank in '${rawSeq.subSequence(start, end)}' [${seq.startIndex}, ${seq.endIndex})", rawSeq.subSequence(start, end).isBlank(), seq.isBlank)
                }
            }
        }
    }

    @Test
    fun getRawChars() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    seq.beforeStartNonChar = '!'
                    seq.afterEndNonChar = '*'
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in -3..seq.rawLength + 3) {
                        for (seqEnd in seqStart - 3..seq.rawLength + 3) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            val rawChars = seq.rawChars
                            assertEquals("Testing rawChars in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(rawSeq.subSequence(start + seq.startIndex, start + seq.endIndex)).toString(), StringBuilder().append(rawChars).toString())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun rawSubSequence() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in 0..seq.rawLength) {
                        for (seqEnd in seqStart..seq.rawLength) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            for (rawSeqStart in seqStart - 3..seqEnd + 3) {
                                for (rawSeqEnd in seqStart - 3..seqEnd + 3) {
                                    val pair = seq.safeRawRange(rawSeqStart, rawSeqEnd)
                                    val expSeq = rawSeq.subSequence(start, end).subSequence(pair.first, pair.second)
                                    assertEquals("Testing rawSubSequence($rawSeqStart,$rawSeqEnd) in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(expSeq).toString(), StringBuilder().append(seq.rawSubSequence(rawSeqStart, rawSeqEnd)).toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun subSequence() {
        for (rawSeq in allCharTestSequences) {
            for (start in 0..rawSeq.length) {
                for (end in start..rawSeq.length) {
                    val seq = SafeCharSequence(rawSeq, start, end)
                    val length = end - start
                    assertEquals("Testing length for $start, $end of ${rawSeq.length} exp ${end - start}", length, seq.rawLength)

                    for (seqStart in 0..seq.rawLength) {
                        for (seqEnd in seqStart..seq.rawLength) {
                            seq.startIndex = seqStart
                            seq.endIndex = seqEnd

                            val expSubSeq = rawSeq.subSequence(start, end).subSequence(seq.startIndex, seq.endIndex)
                            assertEquals("Testing subSequence in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(expSubSeq).toString(), StringBuilder().append(seq.subSequence).toString())

                            for (rawSeqStart in seqStart - 3..seqEnd + 3) {
                                for (rawSeqEnd in seqStart - 3..seqEnd + 3) {
                                    val pair = seq.safeRange(rawSeqStart, rawSeqEnd)
                                    val expSeq = rawSeq.subSequence(start, end).subSequence(pair.first, pair.second)
                                    assertEquals("Testing subSequence($rawSeqStart,$rawSeqEnd) in [${seq.startIndex}, ${seq.endIndex})", StringBuilder().append(expSeq).toString(), StringBuilder().append(seq.subSequence(rawSeqStart, rawSeqEnd)).toString())
                                }
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
