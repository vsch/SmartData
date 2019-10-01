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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartParagraphCharSequenceTest {

    val simplePar = """Lorem ipsum dolor sit amet, consectetaur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."""

    val multiLinePar = """Lorem ipsum dolor sit amet, consectetaur adipisicing elit,
sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris
nisi ut aliquip ex ea commodo consequat.

Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum Et harumd und lookum like Greek to me, dereud facilis est er expedit distinct.
Nam liber te conscient to factor tum poen legum odioque civiuda.
Et tam neque pecun modut est neque nonor et imper ned libidig met,
consectetur adipiscing elit, sed ut labore et dolore magna aliquam makes one wonder who would ever read this stuff?
Bis nostrud exercitation ullam mmodo consequet.
Duis aute in voluptate velit esse cillum dolore eu fugiat nulla pariatur."""

    val indentedMultiLinePar = """    Lorem ipsum dolor sit amet, consectetaur adipisicing elit,
    sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
    Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris
    nisi ut aliquip ex ea commodo consequat.

    Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
    Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum Et harumd und lookum like Greek to me, dereud facilis est er expedit distinct.
    Nam liber te conscient to factor tum poen legum odioque civiuda.
    Et tam neque pecun modut est neque nonor et imper ned libidig met,
    consectetur adipiscing elit, sed ut labore et dolore magna aliquam makes one wonder who would ever read this stuff?
    Bis nostrud exercitation ullam mmodo consequet.
    Duis aute in voluptate velit esse cillum dolore eu fugiat nulla pariatur."""

    val hard = "  "
    val multiLineHardBreaksPar = """Lorem ipsum dolor sit amet, consectetaur adipisicing elit,
sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.$hard
Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris
nisi ut aliquip ex ea commodo consequat.$hard

Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.$hard
Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum Et harumd und lookum like Greek to me, dereud facilis est er expedit distinct.$hard
Nam liber te conscient to factor tum poen legum odioque civiuda.$hard
Et tam neque pecun modut est neque nonor et imper ned libidig met,
consectetur adipiscing elit, sed ut labore et dolore magna aliquam makes one wonder who would ever read this stuff?
Bis nostrud exercitation ullam mmodo consequet.$hard
Duis aute in voluptate velit esse cillum dolore eu fugiat nulla pariatur."""

    val logOutput = true

    fun ensureAligned(alignment: TextAlignment, chars: SmartCharSequence, firstIndent: Int, indent: Int, width: Int, respectHardBreaks: Boolean) {
        val lines = chars.splitParts('\n', false)
        var lineCount = 0
        for (line in lines) {
            val indentSize = if (lineCount == 0) firstIndent else indent
            val trimmed = line.trim()

            val padding = (width - indentSize - trimmed.length).minLimit(0)

            val leftPad: Int
            val rightPad: Int
            var testAlignment = alignment
            var isHardBreakLine = lineCount == lines.lastIndex || trimmed.endsWith('.') && respectHardBreaks

            when (alignment) {
                TextAlignment.DEFAULT, TextAlignment.LEFT -> {
                    leftPad = 0
                    rightPad = padding - leftPad
                }
                TextAlignment.CENTER -> {
                    leftPad = padding / 2
                    rightPad = padding - leftPad
                }
                TextAlignment.RIGHT -> {
                    leftPad = padding
                    rightPad = padding - leftPad
                }
                TextAlignment.JUSTIFIED -> {
                    if (!isHardBreakLine) {
                        leftPad = 0
                        rightPad = 0
                    } else {
                        leftPad = 0
                        rightPad = padding - leftPad
                        testAlignment = TextAlignment.LEFT
                    }
                }
            }

            assertEquals(leftPad + indentSize, line.countLeading(' '), "Testing left padding on line '$line' $lineCount $leftPad")

            val actualRightPad = width - leftPad - indentSize - trimmed.length

            when (testAlignment) {
                TextAlignment.DEFAULT,
                TextAlignment.LEFT,
                TextAlignment.CENTER -> {
                    if (trimmed.contains(' ')) {
//                        if (rightPad != actualRightPad) {
//                            val tmp = 0
//                        }
                        assertEquals(rightPad, actualRightPad, "Testing right padding on line '$line' $lineCount $rightPad")
                        assertTrue(trimmed.length <= width - indentSize, "Testing fit on line $lineCount, '$trimmed'.length ${line.length} < $width")
                    } else if (trimmed.length < width - indentSize) {
                        assertEquals(true, rightPad <= actualRightPad, "Testing right padding on line '$line' $lineCount $rightPad")
                    }
                }
                TextAlignment.RIGHT -> {
                    if (trimmed.contains(' ')) {
//                        if (rightPad != actualRightPad) {
//                            val tmp = 0
//                        }
                        assertEquals(rightPad, actualRightPad, "Testing right padding on line '$line' $lineCount $rightPad")
                        assertTrue(trimmed.length <= width - indentSize, "Testing fit on line $lineCount, '$trimmed'.length ${line.length} < $width")
                    } else if (trimmed.length <= width - indentSize) {
                        assertEquals(true, rightPad <= actualRightPad, "Testing right padding on line '$line' $lineCount $rightPad")
                    }
                }
                TextAlignment.JUSTIFIED -> {
                    if (trimmed.contains(' ')) {
//                        if (rightPad != actualRightPad) {
//                            val tmp = 0
//                        }
                        assertEquals(rightPad, actualRightPad, "Testing right padding on line '$line' $lineCount $rightPad")
                        assertTrue(trimmed.length <= width - indentSize, "Testing fit on line $lineCount, '$trimmed'.length ${line.length} < $width")
                    } else if (trimmed.length < width - indentSize) {
                        assertEquals(true, rightPad <= actualRightPad, "Testing right padding on line '$line' $lineCount $rightPad")
                    }
                }
            }

            lineCount++
        }
    }

    @Test
    fun test_align() {
        val par = SmartParagraphCharSequence(simplePar)
        assertEquals(simplePar, par.asString())

        for (alignment in TextAlignment.values()) {
            par.alignment = alignment
            for (ind in 0..4 step 4) {
                par.indent = ind
                for (fInd in 0..8 step 4) {
                    par.firstIndent = fInd
                    for (i in 0..50 step 10) {
                        par.width = i
                        if (logOutput || par.indent == 4 && par.firstIndent == 8 && par.width == 50) {
                            println("reformat simplePar to $i, align: ${par.alignment} first: ${par.firstIndent} ind: ${par.indent}")
                            println(par)
                            println()
                        }
                        if (i > 0) ensureAligned(par.alignment, par, par.firstIndent, par.indent, i, par.keepMarkdownHardBreaks)
                    }
                }
            }
        }
    }

    @Test
    fun test_alignMultiLine() {
        val par = SmartParagraphCharSequence(multiLinePar)
        assertEquals(multiLinePar, par.asString())

        for (alignment in TextAlignment.values()) {
            par.alignment = alignment
            for (ind in 0..4 step 4) {
                par.indent = ind
                for (fInd in 0..8 step 4) {
                    par.firstIndent = fInd
                    for (i in 0..50 step 10) {
                        par.width = i
                        if (logOutput || par.indent == 4 && par.firstIndent == 8 && par.width == 50) {
                            println("reformat multiLinePar to $i, align: ${par.alignment} first: ${par.firstIndent} ind: ${par.indent}")
                            println(par)
                            println()
                        }
                        if (i > 0) ensureAligned(par.alignment, par, par.firstIndent, par.indent, i, par.keepMarkdownHardBreaks)
                    }
                }
            }
        }
    }

    @Test
    fun test_alignIndentedMultiLine() {
        val par = SmartParagraphCharSequence(indentedMultiLinePar)
        assertEquals(indentedMultiLinePar, par.asString())

        for (alignment in TextAlignment.values()) {
            par.alignment = alignment
            for (ind in 0..4 step 4) {
                par.indent = ind
                for (fInd in 0..8 step 4) {
                    par.firstIndent = fInd
                    for (i in 0..50 step 10) {
                        par.width = i
                        if (logOutput || par.indent == 4 && par.firstIndent == 8 && par.width == 50) {
                            println("reformat indentedMultiLinePar to $i, align: ${par.alignment} first: ${par.firstIndent} ind: ${par.indent}")
                            println(par)
                            println()
                        }
                        if (i > 0) ensureAligned(par.alignment, par, par.firstIndent, par.indent, i, par.keepMarkdownHardBreaks)
                    }
                }
            }
        }
    }

    //    @Test
    fun test_SingleDebug() {
        val par = SmartParagraphCharSequence(multiLineHardBreaksPar)
        assertEquals(multiLineHardBreaksPar, par.asString())

        par.alignment = TextAlignment.JUSTIFIED
        par.indent = 4
        par.firstIndent = 8
        par.width = 40
        par.keepMarkdownHardBreaks = true
        println("reformat to ${par.width}, to ${par.width} align: ${par.alignment} first: ${par.firstIndent} ind: ${par.indent}")
        println(par)
        println()
        if (par.width > 0) ensureAligned(par.alignment, par, par.firstIndent, par.indent, par.width, par.keepMarkdownHardBreaks)
    }
}

