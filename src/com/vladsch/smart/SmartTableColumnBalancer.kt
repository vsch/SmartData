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

import java.util.*

class SmartTableColumnBalancer() {
    companion object {
        private val IMMUTABLE_ZERO = SmartImmutableData(0)
        private val IMMUTABLE_LEFT_ALIGNMENT = SmartImmutableData(TextAlignment.LEFT)
    }

    // inputs
    protected val myColumnLengths = HashMap<Int, ArrayList<SmartVersionedDataHolder<Int>>>()
    protected val myColumnSpans = ArrayList<TableColumnSpan>()

    // values during balancing computation
    protected var myColumnWidths = Array(0, { 0 })
    protected var myAdditionalColumnWidths = Array(0, { 0 })

    // these are outputs
    protected val myAlignmentDataPoints = ArrayList<SmartVersionedDataAlias<TextAlignment>>()
    protected val myColumnWidthDataPoints = ArrayList<SmartVersionedDataAlias<Int>>()
    protected val myVersionData: SmartVersionedDataAlias<Int> = SmartVersionedDataAlias(IMMUTABLE_ZERO)
    protected var myDependencies: List<SmartVersionedDataHolder<Int>> = listOf()

    fun width(index: Int, textLength: SmartVersionedDataHolder<Int>): SmartVersionedDataHolder<Int> {
        return width(index, textLength, 1)
    }

    fun width(index: Int, textLength: SmartVersionedDataHolder<Int>, columnSpan: Int): SmartVersionedDataHolder<Int> {
        if (columnSpan < 1) throw IllegalArgumentException("columnSpan must be >= 1")

        ensureColumnDataPoints(index + columnSpan - 1)

        if (columnSpan == 1) {
            // regular column
            addColumnLength(index, textLength)
            return myColumnWidthDataPoints[index]
        } else {
            // this one is a span
            val colSpan = TableColumnSpan(this, index, columnSpan, textLength)
            myColumnSpans.add(colSpan)
            return colSpan.widthDataPoint
        }
    }

    protected fun addColumnLength(index: Int, textLength: SmartVersionedDataHolder<Int>) {
        val list = myColumnLengths[index]

        if (list == null) {
            myColumnLengths.put(index, arrayListOf(textLength))
        } else {
            list.add(textLength)
        }
    }

    protected fun ensureColumnDataPoints(index: Int) {
        while (myAlignmentDataPoints.size < index + 1) {
            myAlignmentDataPoints.add(SmartVersionedDataAlias(IMMUTABLE_LEFT_ALIGNMENT))
        }

        while (myColumnWidthDataPoints.size < index + 1) {
            myColumnWidthDataPoints.add(SmartVersionedDataAlias(IMMUTABLE_ZERO))
        }
    }

    fun alignmentDataPoint(index: Int): SmartVersionedDataHolder<TextAlignment> {
        ensureColumnDataPoints(index)
        return myAlignmentDataPoints[index]
    }

    fun alignment(index: Int, alignment: SmartVersionedDataHolder<TextAlignment>) {
        ensureColumnDataPoints(index)
        if (myAlignmentDataPoints[index].alias !== IMMUTABLE_LEFT_ALIGNMENT) throw IllegalStateException("Alignment provider for index $index is already defined ${myAlignmentDataPoints[index]}, new one is in error $alignment")
        myAlignmentDataPoints[index].alias = alignment
    }


    // here is the meat of the wiring
    fun finalizeTable() {
        // column width data points depend on all the lengths of columns and all the spans that span those columns
        val dependencies = arrayListOf<SmartVersionedDataHolder<Int>>()
        for (index in 0..myColumnWidthDataPoints.lastIndex) {
            val lengths = myColumnLengths[index]
            if (lengths != null) {
                for (length in lengths) {
                    if (length !== IMMUTABLE_ZERO) dependencies.add(length)
                }
            }
        }

        // now add any spanning columns
        for (columnSpan in myColumnSpans) {
            dependencies.add(columnSpan.textLengthDataPoint)
        }

        myDependencies = dependencies

        myColumnWidths = Array(myColumnLengths.size, { 0 })
        myAdditionalColumnWidths = Array(myColumnLengths.size, { 0 })

        for (index in 0..myColumnWidthDataPoints.lastIndex) {
            myColumnWidthDataPoints[index].alias = SmartDependentData(myVersionData, { columnWidth(index) })
        }

        for (columnSpan in myColumnSpans) {
            columnSpan.widthDataPoint.alias = SmartDependentData(myVersionData, { spanWidth(columnSpan.startIndex, columnSpan.endIndex) })
        }

        // we now set our version alias to dependent data that will do the column balancing computations
        myVersionData.alias = SmartDependentData(dependencies, { balanceColumns(); 0 })
    }

    private fun additionalColumnWidth(index: Int): Int {
        return myAdditionalColumnWidths[index]
    }

    // called when something has changed in the dependencies, so here we recompute everything
    fun balanceColumns() {
        // get all the single column text lengths
        for (index in 0..myColumnWidthDataPoints.lastIndex) {
            var width = 0
            val lengths = myColumnLengths[index]
            if (lengths != null) {
                for (length in lengths) {
                    if (width < length.value) width = length.value
                }
            }
            myColumnWidths[index] = width
        }

        // get all the span lengths
        for (columnSpan in myColumnSpans) {
            columnSpan.textLength = columnSpan.textLengthDataPoint.value
        }

        // compute unfixed columns, which are all columns at first
        var haveUnfixedColumns = myColumnSpans.size > 0
        while (haveUnfixedColumns) {
            haveUnfixedColumns = false

            for (columnSpan in myColumnSpans) {
                columnSpan.distributeLength()
            }

            var fixedAdditionalWidth = 0
            var unfixedColumns = setOf<Int>()
            for (columnSpan in myColumnSpans) {
                unfixedColumns = unfixedColumns.union(columnSpan.unfixedColumns)
            }

            // need to reset all unfixed additional columns so we recompute them
            for (index in unfixedColumns) {
                myAdditionalColumnWidths[index] = 0
            }

            for (columnSpan in myColumnSpans) {
                if (!columnSpan.unfixedColumns.isEmpty()) {
                    for (index in columnSpan.unfixedColumns) {
                        val additionalWidth = columnSpan.additionalWidths[index - columnSpan.startIndex]
                        if (myAdditionalColumnWidths[index] < additionalWidth) {
                            myAdditionalColumnWidths[index] = additionalWidth
                            if (fixedAdditionalWidth < additionalWidth) {
                                fixedAdditionalWidth = additionalWidth
                            }
                        }
                    }
                }
            }

            for (columnSpan in myColumnSpans) {
                if (!columnSpan.unfixedColumns.isEmpty()) {
                    columnSpan.fixLengths(fixedAdditionalWidth)

                    if (!columnSpan.unfixedColumns.isEmpty()) haveUnfixedColumns = true
                }
            }
        }

        // we now have everything computed, can clear the
        for (columnSpan in myColumnSpans) {
            columnSpan.clearIterationData()
        }
    }

    internal fun spanWidth(startIndex: Int, endIndex: Int): Int {
        var width = 0
        for (index in startIndex..endIndex - 1) {
            width += columnWidth(index)
        }
        return width
    }

    internal fun columnWidth(index: Int) = myColumnWidths[index] + myAdditionalColumnWidths[index]

    internal fun columnLength(index: Int): Int = myColumnWidths[index]

    class TableColumnSpan(tableColumnBalancer: SmartTableColumnBalancer, index: Int, columnSpan: Int, textLength: SmartVersionedDataHolder<Int>) {
        protected val myTableColumnBalancer = tableColumnBalancer
        protected val myStartIndex = index
        protected val myEndIndex = index + columnSpan
        protected val myTextLengthDataPoint = textLength
        protected val myWidthDataPoint = SmartVersionedDataAlias(IMMUTABLE_ZERO)
        protected val myColumns: Set<Int>

        // used during balancing
        protected var myFixedColumns = setOf<Int>()
        protected var myUnfixedColumns = setOf<Int>()
        protected val myAddColumnWidth: Array<Int>
        protected var myTextLength: Int = 0

        init {
            val columns = HashSet<Int>()
            for (span in startIndex..lastIndex) {
                columns.add(span)
            }

            myColumns = columns
            myAddColumnWidth = Array(columnSpan, { 0 })
        }

        val startIndex: Int get() = myStartIndex
        val endIndex: Int get() = myEndIndex
        val lastIndex: Int get() = myEndIndex - 1

        val textLengthDataPoint: SmartVersionedDataHolder<Int> get() = myTextLengthDataPoint
        val widthDataPoint: SmartVersionedDataAlias<Int> get() = myWidthDataPoint
        val additionalWidths: Array<Int> get() = myAddColumnWidth
        var textLength: Int get() = myTextLength
            set(value) {
                myTextLength = value
            }

        val fixedColumns: Set<Int> get() = myFixedColumns
        val unfixedColumns: Set<Int> get() = myUnfixedColumns

        // distribute length difference to unfixed columns
        fun distributeLength() {
            val unfixed = HashSet<Int>()
            var fixedWidth = 0
            for (index in startIndex..lastIndex) {
                if (fixedColumns.contains(index)) {
                    // this one is fixed
                    fixedWidth += myTableColumnBalancer.columnWidth(index)
                } else {
                    fixedWidth += myTableColumnBalancer.columnLength(index)
                    unfixed.add(index)
                }
            }

            myUnfixedColumns = unfixed

            if (!unfixed.isEmpty()) {
                val extraLength = (myTextLength - fixedWidth).minBound(0)
                val whole = extraLength / unfixed.size
                var remainder = extraLength - whole * unfixed.size

                for (index in unfixed.sortedByDescending { myTableColumnBalancer.columnLength(it) }) {
                    additionalWidths[index - myStartIndex] = whole + if (remainder > 0) 1 else 0
                    if (remainder > 0) remainder--
                }
            }
        }

        fun fixLengths(fixedLength: Int) {
            val fixedColumns = HashSet<Int>()
            val unfixedColumns = HashSet<Int>()
            fixedColumns.addAll(myFixedColumns)

            for (index in myUnfixedColumns) {
                val additionalColumnWidth = myTableColumnBalancer.additionalColumnWidth(index)

                if (additionalColumnWidth == fixedLength) {
                    // this one is fixed
                    myAddColumnWidth[index - myStartIndex] = fixedLength
                    fixedColumns.add(index)
                } else {
                    if (fixedLength < additionalColumnWidth) {
                        fixedColumns.add(index)
                    } else {
                        unfixedColumns.add(index)
                    }
                }
            }

            myFixedColumns = fixedColumns
            myUnfixedColumns = unfixedColumns
        }

        fun clearIterationData() {
            myFixedColumns = setOf()
            myUnfixedColumns = setOf()
        }
    }

}
