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

class SmartParagraphCharSequence(replacedChars: SmartCharSequence) : SmartCharSequenceBase<SmartCharSequence>() {

    companion object {
        @JvmStatic val MARKDOWN_START_LINE_CHAR = '\u2028'     // this one is not preserved but will cause a line break if not already at beginning of line
        @JvmStatic val MARKDOWN_START_LINE = SmartRepeatedCharSequence('\u2028')     // this one is not preserved but will cause a line break if not already at beginning of line
    }

    constructor(chars: CharSequence) : this(SmartCharSequenceWrapper(chars))

    constructor(chars: CharArray) : this(SmartCharArraySequence(chars))

    constructor(chars: String) : this(SmartCharArraySequence(chars.toCharArray()))

    // TODO: need a SmartDependentVersionHolder to SmartVersionedDataHolder adapter class so that smar sequences can be included
    // in list of dependents of properties
    protected val myReplacedChars = replacedChars
    protected var myFirstIndent = SmartVersionedProperty("paraCharSeq:FirstIndent", 0)
    protected var myIndent = SmartVersionedProperty("paraCharSeq:Indent", 0)
    protected var myFirstWidthOffset = SmartVersionedProperty("varCharSeq:FirstWidth", 0)
    protected var myWidth = SmartVersionedProperty("varCharSeq:Width", 0)
    protected var myAlignment = SmartVersionedProperty("varCharSeq:Alignment", TextAlignment.LEFT)
    protected var myKeepMarkdownHardBreaks = SmartVersionedProperty("varCharSeq:keepMarkdownHardBreaks", true)
    protected var myKeepLineBreaks = SmartVersionedProperty("varCharSeq:keepLineBreaks", false)

    protected var myResultSequence = SmartDependentData(listOf(myFirstIndent, myIndent, myAlignment, myFirstWidthOffset, myWidth, myKeepMarkdownHardBreaks), DataComputable { computeResultSequence() })
    protected val myVersion = SmartDependentVersion(listOf(myResultSequence, myReplacedChars.version))

    var alignment: TextAlignment get() = myAlignment.value
        set(value) {
            myAlignment.value = value
        }

    var width: Int get() = myWidth.value
        set(value) {
            myWidth.value = value.minBound(0)
        }

    val firstWidth: Int get() = if (myWidth.value == 0) 0 else (myWidth.value + myFirstWidthOffset.value).minBound(0)

    var firstWidthOffset: Int get() = myFirstWidthOffset.value
        set(value) {
            myFirstWidthOffset.value = value
        }

    var indent: Int get() = myIndent.value
        set(value) {
            myIndent.value = value.minBound(0)
        }

    var firstIndent: Int get() = myFirstIndent.value
        set(value) {
            myFirstIndent.value = value.minBound(0)
        }

    var keepMarkdownHardBreaks: Boolean get() = myKeepMarkdownHardBreaks.value
        set(value) {
            myKeepMarkdownHardBreaks.value = value
        }

    var keepLineBreaks: Boolean get() = myKeepLineBreaks.value
        set(value) {
            myKeepLineBreaks.value = value
        }

    override fun getVersion(): SmartVersion {
        return myVersion
    }

    var alignmentDataPoint: SmartVersionedDataHolder<TextAlignment>
        get() = myAlignment
        set(value) {
            myAlignment.connect(value)
        }

    var widthDataPoint: SmartVersionedDataHolder<Int>
        get() = myWidth
        set(value) {
            myWidth.connect(value)
        }

    protected val resultSequence: SmartCharSequence
        get() = myResultSequence.value

    protected data class Token<V : Enum<V>>(val type: Enum<V>, val range: Range) {
        override fun toString(): String {
            return "token: $type $range"
        }

        fun subSequence(charSequence: SmartCharSequence): SmartCharSequence {
            return charSequence.subSequence(range.start, range.end)
        }

        fun subSequence(charSequence: CharSequence): CharSequence {
            return charSequence.subSequence(range.start, range.end)
        }
    }

    protected enum class TextType {
        WORD,
        SPACE,
        BREAK,
        MARKDOWN_BREAK,
        MARKDOWN_START_LINE;
    }

    protected fun tokenizeSequence(chars: CharSequence): List<Token<TextType>> {
        var pos = 0
        var maxPos = chars.length
        var lastPos = 0
        var inWord = false
        val tokenList = ArrayList<Token<TextType>>()
        var lastConsecutiveSpaces = 0

        while (pos < maxPos) {
            val c = chars[pos]
            if (inWord) {
                if (c == ' ' || c == '\t' || c == '\n' || c == MARKDOWN_START_LINE_CHAR) {
                    inWord = false
                    if (lastPos < pos) {
                        // have a word
                        tokenList.add(Token(TextType.WORD, Range(lastPos, pos)))
                        lastPos = pos
                    }
                } else {
                    pos++
                }
            } else {
                // in white space
                if (c != ' ' && c != '\t' && c != '\n' && c != MARKDOWN_START_LINE_CHAR) {
                    if (lastPos < pos) {
                        tokenList.add(Token(TextType.SPACE, Range(lastPos, pos)))
                        lastPos = pos
                    }
                    inWord = true
                    lastConsecutiveSpaces = 0
                } else {
                    if (c == '\n') {
                        if (lastConsecutiveSpaces >= 2) {
                            tokenList.add(Token(TextType.MARKDOWN_BREAK, Range(pos - lastConsecutiveSpaces, pos + 1)))
                        } else {
                            tokenList.add(Token(TextType.BREAK, Range(pos, pos + 1)))
                        }

                        lastPos = pos + 1
                    } else if (c == MARKDOWN_START_LINE_CHAR) {
                        tokenList.add(Token(TextType.MARKDOWN_START_LINE, Range(pos, pos + 1)))
                        lastPos = pos + 1
                    }

                    if (c == ' ') lastConsecutiveSpaces++
                    else lastConsecutiveSpaces = 0
                    pos++
                }
            }
        }

        if (lastPos < pos) {
            tokenList.add(Token(if (inWord) TextType.WORD else TextType.SPACE, Range(lastPos, pos)))
        }

        return tokenList
    }

    protected fun computeResultSequence(): SmartCharSequence {
        if (firstWidth <= 0) return myReplacedChars.cachedProxy

        val chars = myReplacedChars.cachedProxy
        val tokens = tokenizeSequence(chars)
        var iMax = tokens.size
        val lineBreak = RepeatedCharSequence("\n")
        val hardBreak = RepeatedCharSequence("  \n")
        var pos = 0
        var i = 0
        var lineCount = 0
        val lineWords = ArrayList<Token<TextType>>()
        var result = ArrayList<CharSequence>()
        var lineIndent = firstIndent
        var lineWidth = firstWidth
        val nextWidth = if (myWidth.value <= 0) Integer.MAX_VALUE else myWidth.value
        var wordsOnLine = 0

        fun lineBreak(spaceToken: Token<TextType>?, lineBreak: CharSequence, lastLine: Boolean) {
            addLine(result, chars, lineWords, wordsOnLine, lineCount, lineWidth - pos - lineIndent, lastLine)
            if (spaceToken != null) {
                result.add(SmartReplacedCharSequence(spaceToken.subSequence(chars), lineBreak))
            } else {
                result.add(lineBreak)
            }

            lineWords.clear()
            pos = 0
            wordsOnLine = 0
            lineCount++
            lineIndent = indent
            lineWidth = nextWidth
        }

        while (i < iMax) {
            val token = tokens[i]
            //                        println("token[$i] = $token (${chars.subSequence(token.range.start, token.range.end)})")

            when (token.type) {
                TextType.SPACE -> {
                    if (pos > 0) lineWords.add(token)
                    i++
                }
                TextType.WORD -> {
                    if (pos == 0 || lineIndent + pos + token.range.span + 1 <= lineWidth) {
                        // fits, add it
                        if (pos > 0) pos++
                        lineWords.add(token)
                        pos += token.range.span
                        wordsOnLine++
                        i++
                    } else {
                        // need to insert a line break and repeat
                        val lineBreakToken = lineWords[lineWords.lastIndex]

                        if (lineBreakToken.type == TextType.WORD) {
                            lineBreak(null, lineBreak, false)
                        } else {
                            lineBreak(lineBreakToken, lineBreak, false)
                        }
                    }
                }
                TextType.MARKDOWN_BREAK -> {
                    if (pos > 0) {
                        if (myKeepMarkdownHardBreaks.value) {
                            lineBreak(token, hardBreak, true)
                        } else if (myKeepLineBreaks.value) {
                            lineWords.add(token)
                            lineBreak(token, lineBreak, true)
                        }
                    }
                    i++
                }
                TextType.BREAK -> {
                    if (pos > 0 && myKeepLineBreaks.value) {
                        lineBreak(token, lineBreak, true)
                    }
                    i++
                }
                TextType.MARKDOWN_START_LINE -> {
                    if (wordsOnLine > 0) {
                        lineBreak(null, lineBreak, false)
                    }
                    i++
                }
            }
        }

        if (wordsOnLine > 0) {
            addLine(result, chars, lineWords, wordsOnLine, lineCount, lineWidth - pos - lineIndent, true)
        }

        return SmartCharSequenceBase.smart(result).cachedProxy
    }

    private fun addLine(result: ArrayList<CharSequence>, charSequence: SmartCharSequence, lineWords: ArrayList<Token<TextType>>, wordsOnLine: Int, lineCount: Int, extraSpaces: Int, lastLine: Boolean) {
        var leadSpaces = 0
        var addSpaces = 0
        var remSpaces = 0
        val distributeSpaces = if (extraSpaces > 0) extraSpaces else 0

        val indent = if (lineCount > 0) myIndent.value else myFirstIndent.value
        when (myAlignment.value) {
            TextAlignment.LEFT -> {
                leadSpaces = indent
            }
            TextAlignment.RIGHT -> {
                leadSpaces = indent + distributeSpaces
            }
            TextAlignment.CENTER -> {
                leadSpaces = indent + distributeSpaces / 2
            }
            TextAlignment.JUSTIFIED -> {
                leadSpaces = indent
                if (!lastLine && wordsOnLine > 1 && distributeSpaces > 0) {
                    addSpaces = distributeSpaces / (wordsOnLine - 1)
                    remSpaces = distributeSpaces - addSpaces * (wordsOnLine - 1)
                    if (addSpaces * (wordsOnLine - 1) + remSpaces != distributeSpaces) {
                        val tmp = 0
                    }
                }
            }
        }

        if (leadSpaces > 0) result.add(RepeatedCharSequence(' ', leadSpaces))
        var firstWord = true
        var hadSpace = true

        for (word in lineWords) {
            if (word.type == TextType.WORD) {
                if (firstWord) firstWord = false
                else if (!hadSpace) {
                    var spcSize = if (remSpaces > 0) 1 else 0
                    result.add(RepeatedCharSequence(' ', addSpaces + 1 + spcSize))
                    remSpaces -= spcSize
                }
                result.add(word.subSequence(charSequence))
                hadSpace = false
            } else {
                var spcSize = if (remSpaces > 0) 1 else 0
                result.add(SmartReplacedCharSequence(word.subSequence(charSequence), RepeatedCharSequence(' ', addSpaces + 1 + spcSize)))
                remSpaces -= spcSize
                hadSpace = true
            }
        }

        if (remSpaces > 0) {
            val tmp = 0
        }
    }

    fun leftAlign(width: Int) {
        alignment = TextAlignment.LEFT
        this.width = width
    }

    fun rightAlign(width: Int) {
        alignment = TextAlignment.RIGHT
        this.width = width
    }

    fun centerAlign(width: Int) {
        alignment = TextAlignment.CENTER
        this.width = width
    }

    fun justifyAlign(width: Int) {
        alignment = TextAlignment.JUSTIFIED
        this.width = width
    }

    override val length: Int get() = resultSequence.length
    override fun getCharsImpl(dst: CharArray, dstOffset: Int) = resultSequence.getChars(dst, dstOffset)
    override fun charAtImpl(index: Int): Char = resultSequence[index]
    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartCharSequence = resultSequence.subSequence(startIndex, endIndex)
    override fun getCharsImpl(): CharArray = resultSequence.chars
    override fun getCachedProxy(): SmartCharSequence = resultSequence.cachedProxy

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        checkIndex(index)
        val location = resultSequence.trackedSourceLocation(index)
        return adjustTrackedSourceLocation(location)
    }

    protected fun adjustTrackedLocation(location: TrackedLocation?): TrackedLocation? {
        // TODO: adjust tracking location as needed
        //        if (location != null) {
        //            val leadPadding = myLeftPadding.length + myPrefix.value.length
        //            if (leadPadding > 0) {
        //                return adjustTrackedSourceLocation(location)
        //            }
        //        }
        return location
    }

    protected fun adjustTrackedSourceLocation(location: TrackedLocation): TrackedLocation {
        // TODO: adjust tracking location as needed
        //        val leadPadding = myLeftPadding.length + myPrefix.value.length
        //        if (leadPadding > 0) {
        //            return location.withIndex(leadPadding + location.index).withPrevClosest(leadPadding + location.prevIndex).withNextClosest(leadPadding + location.nextIndex)
        //        }
        return location
    }

    override fun getMarkers(id: String?): List<TrackedLocation> {
        val locations = myReplacedChars.getMarkers(id)
        if (!locations.isEmpty()) {
            // re-map
            val markers = ArrayList(locations)
            for (location in locations) {
                markers.add(adjustTrackedLocation(location))
            }
            if (!markers.isEmpty()) return markers
        }
        return TrackedLocation.EMPTY_LIST
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        val location = resultSequence.trackedLocation(source, offset)
        return adjustTrackedLocation(location)
    }

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        return null
    }
}

