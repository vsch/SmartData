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

class SmartTableColumnBalancerTest {

    @Test
    fun test_basic() {
        val tableBalancer = SmartTableColumnBalancer(CharWidthProvider.UNITY_PROVIDER)

        val row1col1 = SmartVolatileData(5)
        val row1col2 = SmartVolatileData(15)
        val row1col3 = SmartVolatileData(20)

        val row2col1 = SmartVolatileData(2)
        val row2col2 = SmartVolatileData(12)
        val row2col3 = SmartVolatileData(10)

        val col1Align = SmartVolatileData(TextAlignment.LEFT)
        val col2Align = SmartVolatileData(TextAlignment.CENTER)
        val col3Align = SmartVolatileData(TextAlignment.RIGHT)

        tableBalancer.alignment(0, col1Align)
        tableBalancer.alignment(1, col2Align)
        tableBalancer.alignment(2, col3Align)

        val widthRow1Col1 = tableBalancer.width(0, row1col1)
        val widthRow1Col2 = tableBalancer.width(1, row1col2)
        val widthRow1Col3 = tableBalancer.width(2, row1col3)

        val widthRow2Col1 = tableBalancer.width(0, row2col1)
        val widthRow2Col2 = tableBalancer.width(1, row2col2)
        val widthRow2Col3 = tableBalancer.width(2, row2col3)

        tableBalancer.finalizeTable()

        assertEquals(5, widthRow1Col1.get())
        assertEquals(15, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        // change some widths, should change results
        row2col1.set(6)

        assertEquals(6, widthRow1Col1.get())
        assertEquals(15, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        row2col1.set(3)

        assertEquals(5, widthRow1Col1.get())
        assertEquals(15, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        row1col2.set(17)

        assertEquals(5, widthRow1Col1.get())
        assertEquals(17, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())
    }

    @Test
    fun test_simpleSpan() {
        val tableBalancer = SmartTableColumnBalancer(CharWidthProvider.UNITY_PROVIDER)

        val row1col1 = SmartVolatileData(5)
        val row1col2 = SmartVolatileData(15)
        val row1col3 = SmartVolatileData(20)

        val row2col1 = SmartVolatileData(2)
        val row2col2 = SmartVolatileData(12)
        val row2col3 = SmartVolatileData(10)

        val row3col1 = SmartVolatileData(15)
        val row3col3 = SmartVolatileData(10)

        val col1Align = SmartVolatileData(TextAlignment.LEFT)
        val col2Align = SmartVolatileData(TextAlignment.CENTER)
        val col3Align = SmartVolatileData(TextAlignment.RIGHT)

        tableBalancer.alignment(0, col1Align)
        tableBalancer.alignment(1, col2Align)
        tableBalancer.alignment(2, col3Align)

        val widthRow1Col1 = tableBalancer.width(0, row1col1)
        val widthRow1Col2 = tableBalancer.width(1, row1col2)
        val widthRow1Col3 = tableBalancer.width(2, row1col3)

        val widthRow2Col1 = tableBalancer.width(0, row2col1)
        val widthRow2Col2 = tableBalancer.width(1, row2col2)
        val widthRow2Col3 = tableBalancer.width(2, row2col3)

        val widthRow3Col1 = tableBalancer.width(0, row3col1, 2, 0)
        val widthRow3Col3 = tableBalancer.width(2, row3col3)

        tableBalancer.finalizeTable()

        assertEquals(5, widthRow1Col1.get())
        assertEquals(15, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(20, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        // change some widths, should change results
        row2col1.set(6)

        assertEquals(6, widthRow1Col1.get())
        assertEquals(15, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(21, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        row2col1.set(3)

        assertEquals(5, widthRow1Col1.get())
        assertEquals(15, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(20, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        row1col2.set(17)

        assertEquals(5, widthRow1Col1.get())
        assertEquals(17, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(22, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

    }

    @Test
    fun test_simpleAddWidthSpan() {
        val tableBalancer = SmartTableColumnBalancer(CharWidthProvider.UNITY_PROVIDER)

        val row1col1 = SmartVolatileData(5)
        val row1col2 = SmartVolatileData(15)
        val row1col3 = SmartVolatileData(20)

        val row2col1 = SmartVolatileData(2)
        val row2col2 = SmartVolatileData(12)
        val row2col3 = SmartVolatileData(10)

        val row3col1 = SmartVolatileData(30)
        val row3col3 = SmartVolatileData(10)

        val col1Align = SmartVolatileData(TextAlignment.LEFT)
        val col2Align = SmartVolatileData(TextAlignment.CENTER)
        val col3Align = SmartVolatileData(TextAlignment.RIGHT)

        tableBalancer.alignment(0, col1Align)
        tableBalancer.alignment(1, col2Align)
        tableBalancer.alignment(2, col3Align)

        val widthRow1Col1 = tableBalancer.width(0, row1col1)
        val widthRow1Col2 = tableBalancer.width(1, row1col2)
        val widthRow1Col3 = tableBalancer.width(2, row1col3)

        val widthRow2Col1 = tableBalancer.width(0, row2col1)
        val widthRow2Col2 = tableBalancer.width(1, row2col2)
        val widthRow2Col3 = tableBalancer.width(2, row2col3)

        val widthRow3Col1 = tableBalancer.width(0, row3col1, 2, 0)
        val widthRow3Col3 = tableBalancer.width(2, row3col3)

        tableBalancer.finalizeTable()

        assertEquals(10, widthRow1Col1.get())
        assertEquals(20, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(30, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        // change some widths, should change results
        row2col1.set(6)

        assertEquals(10, widthRow1Col1.get())
        assertEquals(20, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(30, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        row2col1.set(3)

        assertEquals(10, widthRow1Col1.get())
        assertEquals(20, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(30, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

        row1col2.set(21)

        assertEquals(7, widthRow1Col1.get())
        assertEquals(23, widthRow1Col2.get())
        assertEquals(20, widthRow1Col3.get())
        assertEquals(30, widthRow3Col1.get())
        assertEquals(20, widthRow3Col3.get())

        assertEquals(widthRow1Col1, widthRow2Col1)
        assertEquals(widthRow1Col2, widthRow2Col2)
        assertEquals(widthRow1Col3, widthRow2Col3)

        assertEquals(col1Align.get(), tableBalancer.alignmentDataPoint(0).get())
        assertEquals(col2Align.get(), tableBalancer.alignmentDataPoint(1).get())
        assertEquals(col3Align.get(), tableBalancer.alignmentDataPoint(2).get())

    }
}
