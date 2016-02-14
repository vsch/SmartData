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

class SafeCharSequenceIndexImpl @JvmOverloads constructor(chars:SafeCharSequence, index: Int = 0) : SafeCharSequenceIndex, SafeCharSequenceError by chars {
    @JvmOverloads constructor(chars: CharSequence, index: Int = 0):this(SafeCharSequenceRangeImpl(chars), index)

    protected val myChars:SafeCharSequence = chars
    protected var myIndex:Int = myChars.safeIndex(index)

    override fun getIndex(): Int = myIndex
    override fun setIndex(index: Int) {
        myIndex = myChars.safeIndex(index)
    }

    override fun getChar(): Char = myChars[myIndex]
    override fun getBeforeIndex(): SafeCharSequence = myChars.subSequence(0, myIndex)
    override fun getAfterIndex(): SafeCharSequence = myChars.subSequence(myIndex, myChars.length)
    override fun getLine(): SafeCharSequence = myChars.subSequence(startOfLine, endOfLine)

    override fun getStartOfLine(): Int {
        return myChars.safeIndex(myChars.indexTrailing(myIndex) {
            when (it) {
                '\n' -> 1
                else -> null
            }
        })
    }

    override fun getEndOfLine(): Int {
        return myChars.safeIndex(myChars.indexLeading(myIndex) {
            when (it) {
                '\n' -> 1
                else -> null
            }
        })
    }

    override fun getLastNonBlank(): Int {
        return myChars.safeIndex(myChars.indexTrailing(endOfLine) {
            when (it) {
                ' ', '\t' -> null
                else -> 0
            }
        })
    }

    override fun getFirstNonBlank(): Int {
        return myChars.safeIndex(myChars.indexLeading(startOfLine) {
            when (it) {
                ' ', '\t' -> null
                else -> 0
            }
        })
    }

    override fun getEndOfPreviousLine(): Int {
        return myChars.safeIndex(startOfLine - 1)
    }

    override fun getStartOfNextLine(): Int {
        return myChars.safeIndex(endOfLine)
    }

    override fun endOfPreviousSkipLines(lines: Int): Int {
        var lastEndOfLine = endOfLine
        var skipLines = lines
        clearHadSafeErrors()
        while (!hadSafeErrors && skipLines-- > 0) lastEndOfLine = endOfPreviousLine
        return lastEndOfLine
    }

    override fun startOfNextSkipLines(lines: Int): Int {
        var lastStartOfLine = startOfLine
        var skipLines = lines
        clearHadSafeErrors()
        while (!hadSafeErrors && skipLines-- > 0) lastStartOfLine = startOfNextLine
        return lastStartOfLine
    }

    override fun isBlankLine(): Boolean = isEmptyLine || firstNonBlank >= endOfLine - 1
    override fun isEmptyLine(): Boolean = startOfLine >= endOfLine - 1
    override fun getIndent(): Int = firstNonBlank - startOfLine
    override fun getColumn(): Int = columnOf(myIndex)
    override fun columnOf(index: Int): Int = myChars.safeIndex(index) - startOfLine
}
