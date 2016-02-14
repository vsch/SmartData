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

open class SafeCharSequence(chars: CharSequence, startIndex: Int, endIndex: Int) : CharSequence {
    constructor(chars: CharSequence, startIndex: Int) : this(chars, startIndex, chars.length)

    constructor(chars: CharSequence) : this(chars, 0, chars.length)

    protected val myChars: CharSequence = chars
    protected val myStart: Int
    protected val myEnd: Int
    protected var myErrors: Int = 0
    protected var myLastCheckedErrors: Int = 0
    protected var myBeforeStartNonChar: Char = '\u0000'
    protected var myAfterEndNonChar: Char = '\u0000'
    protected var myIndex: Int = 0
    protected var myStartIndex = 0

    init {
        val rawStart = if (startIndex < 0) 0 else if (startIndex > chars.length) chars.length else startIndex
        val rawEnd = if (endIndex < 0) 0 else if (endIndex > chars.length) chars.length else endIndex
        if (rawStart != startIndex) myErrors++
        if (rawEnd != endIndex) myErrors++
        myStart = if (rawStart > rawEnd) rawEnd else rawStart
        myEnd = rawEnd
        if (myStart != rawStart) myErrors++
    }

    protected var myEndIndex = myEnd - myStart

    var startIndex: Int
        get() = myStartIndex
        set(value) {
            myStartIndex = safeRawIndex(value)
            if (myEndIndex < myStartIndex) myEndIndex = myStartIndex
        }

    var endIndex: Int
        get() = myEndIndex
        set(value) {
            myEndIndex = safeRawIndex(value)
            if (myStartIndex > myEndIndex) myStartIndex = myEndIndex
        }

    var index: Int
        get() = myIndex
        set(value) {
            myIndex = safeIndex(value)
        }

    val errors: Int get() = myErrors

    var beforeStartNonChar: Char
        get() = myBeforeStartNonChar
        set(value) {
            myBeforeStartNonChar = value
        }

    var afterEndNonChar: Char
        get() = myAfterEndNonChar
        set(value) {
            myAfterEndNonChar = value
        }

    fun clearHadErrors() {
        myLastCheckedErrors = myErrors
    }

    val hadErrors: Boolean get() {
        if (myLastCheckedErrors > myErrors) myLastCheckedErrors = myErrors
        return myLastCheckedErrors < myErrors
    }

    val hadErrorsAndClear: Boolean get() {
        if (myLastCheckedErrors > myErrors) myLastCheckedErrors = myErrors
        val result = myLastCheckedErrors < myErrors
        myLastCheckedErrors = myErrors
        return result
    }

    open fun clearErrors() {
        myErrors = 0
    }

    val char: Char get() = get(myIndex)
    val firstChar: Char get() = get(myStartIndex)
    val lastChar: Char get() = get(myEndIndex - 1)

    val rawLength: Int
        get() = myEnd - myStart

    override val length: Int
        get() = myEndIndex - myStartIndex

    val isEmpty: Boolean get() = myStartIndex >= myEndIndex
    val isBlank: Boolean get() = isEmpty || countLeading(' ', '\t', '\n') == length

    override fun get(index: Int): Char {
        if (index < myStartIndex) return myBeforeStartNonChar
        else if (index >= myEndIndex) return myAfterEndNonChar
        else return myChars[safeRawCharIndex(index)]
    }

    val rawChars: CharSequence get() = myChars.subSequence(safeRawCharIndex(myStartIndex), safeRawCharIndex(myEndIndex))

    protected fun unsafeSubSequence(startIndex: Int, endIndex: Int): SafeCharSequence {
        val result = SafeCharSequence(myChars, rawCharIndex(startIndex), rawCharIndex(endIndex))
        // transfer some relevant settings
        result.myBeforeStartNonChar = myBeforeStartNonChar
        result.myAfterEndNonChar = myAfterEndNonChar
        return result
    }

    fun rawSubSequence(startIndex: Int, endIndex: Int): SafeCharSequence {
        val (safeStart, safeEnd) = safeRawRange(startIndex, endIndex)
        return unsafeSubSequence(safeStart, safeEnd)
    }

    override fun subSequence(startIndex: Int, endIndex: Int): SafeCharSequence {
        val (safeStart, safeEnd) = safeRange(startIndex, endIndex)
        return SafeCharSequence(myChars, rawCharIndex(safeStart), rawCharIndex(safeEnd))
    }

    val subSequence: SafeCharSequence get() = unsafeSubSequence(myStartIndex, myEndIndex)
    val beforeStart: SafeCharSequence get() = unsafeSubSequence(0, myStartIndex)
    val beforeIndex: SafeCharSequence get() = unsafeSubSequence(myStartIndex, myIndex)
    val afterIndex: SafeCharSequence get() = unsafeSubSequence(myIndex, myEndIndex)
    val afterEnd: SafeCharSequence get() = unsafeSubSequence(myEndIndex, rawLength)
    val line: SafeCharSequence get() = unsafeSubSequence(startOfLine, endOfLine)

    val startOfLine: Int get() {
        return safeIndex(indexTrailing(myIndex) {
            when (it) {
                '\n' -> 1
                else -> null
            }
        })
    }

    val endOfLine: Int get() {
        return safeInclusiveIndex(indexLeading(index) {
            when (it) {
                '\n' -> 1
                else -> null
            }
        })
    }

    val lastNonBlank: Int get() {
        return safeIndex(indexTrailing(endOfLine) {
            when (it) {
                ' ', '\t' -> null
                else -> 0
            }
        })
    }

    val firstNonBlank: Int get() {
        return safeIndex(indexLeading(startOfLine) {
            when (it) {
                ' ', '\t' -> null
                else -> 0
            }
        })
    }

    val isBlankLine: Boolean get() = isEmptyLine || firstNonBlank >= endOfLine - 1
    val isEmptyLine: Boolean get() = startOfLine >= endOfLine - 1
    val indent: Int get() = firstNonBlank - startOfLine

    val column: Int get() = columnOf(myIndex)
    fun columnOf(index: Int): Int = safeIndex(index) - startOfLine

    protected fun rawCharIndex(index: Int): Int {
        return myStart + index
    }

    fun safeRawCharIndex(index: Int): Int {
        return myStart + safeRawIndex(index)
    }

    fun safeRawIndex(index: Int): Int {
        val result = if (index < 0 || myStart == myEnd) 0 else if (index > rawLength) rawLength else index
        if (result != index) myErrors++
        return result
    }

    fun safeRawInclusiveIndex(index: Int): Int {
        val result = if (index < 0 || myStart == myEnd) 0 else if (index >= rawLength) rawLength - 1 else index
        if (result != index) myErrors++
        return result
    }

    fun safeRawRange(startIndex: Int, endIndex: Int): Pair<Int, Int> {
        val safeStart = safeRawIndex(startIndex)
        val safeEnd = safeRawIndex(endIndex)
        if (safeStart <= safeEnd) return Pair(safeStart, safeEnd)
        myErrors++
        return Pair(safeEnd, safeEnd)
    }

    fun safeIndex(index: Int): Int {
        val safeIndex = safeRawIndex(index)
        val result = if (safeIndex < myStartIndex || myStartIndex == myEndIndex) myStartIndex else if (safeIndex > myEndIndex) myEndIndex else safeIndex
        if (result != safeIndex) myErrors++
        return result
    }

    fun safeInclusiveIndex(index: Int): Int {
        val safeIndex = safeRawInclusiveIndex(index)
        val result = if (safeIndex < myStartIndex || myStartIndex == myEndIndex) myStartIndex else if (safeIndex >= myEndIndex) myEndIndex - 1 else safeIndex
        if (result != safeIndex) myErrors++
        return result
    }

    fun safeRange(startIndex: Int, endIndex: Int): Pair<Int, Int> {
        val safeStart = safeIndex(startIndex)
        val safeEnd = safeIndex(endIndex)
        if (safeStart < safeEnd) return Pair(safeStart, safeEnd)
        myErrors++
        return Pair(safeEnd, safeEnd)
    }

}
