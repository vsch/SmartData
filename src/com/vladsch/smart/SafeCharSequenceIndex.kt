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

class SafeCharSequenceIndex @JvmOverloads constructor(chars: SafeCharSequence, index: Int = 0) : SafeCharSequenceIndexer, SafeCharSequenceError by chars {
    @JvmOverloads constructor(chars: CharSequence, index: Int = 0) : this(SafeCharSequenceRange(chars), index)

    protected val myChars: SafeCharSequence = chars
    protected var myIndex: Int = myChars.safeIndex(index)

    override fun getIndex(): Int = myIndex
    override fun setIndex(index: Int) {
        myIndex = myChars.safeIndex(index)
    }

    override fun toString(): String {
        return startOfLineToIndexChars.asString() + ">|<" + indexToEndOfLineChars.asString()
    }

    override fun getChar(): Char = myChars[myIndex]
    override fun getBeforeIndexChars(): SafeCharSequence = myChars.subSequence(0, myIndex)
    override fun getAfterIndexChars(): SafeCharSequence = myChars.subSequence(myIndex, myChars.length)

    override fun getLineChars(): SafeCharSequence = myChars.subSequence(startOfLine, endOfLine)
    override fun getFirstToLastNonBlankLineChars(): SafeCharSequence = myChars.subSequence(firstNonBlank, afterLastNonBlank)
    override fun getFirstNonBlankToEndOfLineChars(): SafeCharSequence = myChars.subSequence(firstNonBlank, endOfLine)
    override fun getStartOfLineToLastNonBlankChars(): SafeCharSequence = myChars.subSequence(startOfLine, afterLastNonBlank)
    override fun getStartOfLineToIndexChars(): SafeCharSequence = myChars.subSequence(startOfLine, myIndex)
    override fun getFirstNonBlankToIndexChars(): SafeCharSequence = myChars.subSequence(firstNonBlank, myIndex)
    override fun getIndexToEndOfLineChars(): SafeCharSequence = myChars.subSequence(myIndex, endOfLine)
    override fun getAfterIndexToLastNonBlankChars(): SafeCharSequence = myChars.subSequence(myIndex, afterLastNonBlank)

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

    override fun getAfterLastNonBlank(): Int {
        return myChars.safeIndex(myChars.indexTrailing(endOfLine) {
            when (it) {
                ' ', '\t' -> null
                else -> 1
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
        val endLine = myChars.safeIndex(endOfLine)
        if (endLine == myChars.length) addSafeError()
        return endLine
    }

    override fun endOfPreviousSkipLines(lines: Int): Int {
        var skipLines = lines
        val savedIndex = index
        index = endOfPreviousLine
        clearHadSafeErrors()
        while (!hadSafeErrors && skipLines-- > 0) {
            index = endOfPreviousLine
        }
        val lastEndOfLine = if (hadSafeErrors) 0 else index
        index = savedIndex
        return lastEndOfLine
    }

    override fun startOfNextSkipLines(lines: Int): Int {
        var skipLines = lines
        val savedIndex = index
        index = startOfNextLine
        clearHadSafeErrors()
        while (!hadSafeErrors && skipLines-- > 0) {
            index = startOfNextLine
        }
        val lastStartOfLine = if (hadSafeErrors) myChars.length else index
        index = savedIndex
        return lastStartOfLine
    }

    override fun isBlankLine(): Boolean = isEmptyLine || firstNonBlank >= endOfLine - 1
    override fun isEmptyLine(): Boolean = startOfLine >= endOfLine - 1
    override fun getIndent(): Int = firstNonBlank - startOfLine
    override fun getColumn(): Int = columnOf(myIndex)
    override fun columnOf(index: Int): Int = myChars.safeIndex(index) - startOfLine
}
