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

class CharSequenceInfoTest {
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
Line15"""

    val chars = string.toCharArray()
    val charLines = SmartCharArraySequence(chars).splitPartsSegmented('\n', true)

    @Test
    fun test_startOfLine() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.startIndex(i), csInfo.startOfLine)
            }
        }
    }

    @Test
    fun test_startOfLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", 0, csInfo.startOfLine)
            }
        }
    }

    @Test
    fun test_endOfLine() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.endIndex(i), csInfo.endOfLine)
            }
        }
    }

    @Test
    fun test_endOfLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.endIndex(i) - charLines.startIndex(i), csInfo.endOfLine)
            }
        }
    }

    @Test
    fun test_firstNonBlank() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.startIndex(i) + charLines.segments[i].countLeading(' '), csInfo.firstNonBlank)
            }
        }
    }

    @Test
    fun test_firstNonBlankSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo.firstNonBlank)
            }
        }
    }

    @Test
    fun test_indent() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo.indent)
            }
        }
    }

    @Test
    fun test_indentSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.segments[i].countLeading(' '), csInfo.indent)
            }
        }
    }

    @Test
    fun test_lastNonBlank() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.lastIndex(i) - charLines.segments[i].countTrailing(' '), csInfo.lastNonBlank)
            }
        }
    }

    @Test
    fun test_lastNonBlankSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", charLines.lastIndex(i) - charLines.startIndex(i) - charLines.segments[i].countTrailing(' '), csInfo.lastNonBlank)
            }
        }
    }

    @Test
    fun test_isBlankLine() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..14, csInfo.isBlankLine)
            }
        }
    }

    @Test
    fun test_isBlankLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..14, csInfo.isBlankLine)
            }
        }
    }

    @Test
    fun test_isEmptyLine() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..11, csInfo.isEmptyLine)
            }
        }
    }

    @Test
    fun test_isEmptyLineSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", i in 11..11, csInfo.isEmptyLine)
            }
        }
    }

    @Test
    fun test_columnOf() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines, charLines.startIndex(i) + j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.column)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.columnOf(charLines.startIndex(i) + j))
            }
        }
    }

    @Test
    fun test_columnOfSingle() {
        for (i in 0..charLines.segments.lastIndex) {
            for (j in 0..(charLines.lastIndex(i)-charLines.startIndex(i))) {
                val csInfo = SafeCharSequenceIndexImpl(charLines.getSequence(i), j)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.column)
                assertEquals("i:$i, j:$j index:${csInfo.index} start:${csInfo.startOfLine} end:${csInfo.endOfLine} first:${csInfo.firstNonBlank} last:${csInfo.lastNonBlank}", j, csInfo.columnOf(j))
            }
        }
    }

}
