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

open class SafeCharSequenceRange @JvmOverloads constructor(charSequence: CharSequence, startIndex: Int = 0, endIndex: Int = charSequence.length, safeError: SafeCharSequenceError = SafeCharSequenceErrorImpl()) : SafeCharSequenceRanger, SafeCharSequenceError by safeError, /*SmartSourceLocationTracker,*/ SmartCharSequenceContainer {

    protected val myError = safeError
    protected val myChars: CharSequence = charSequence
    protected val myStart: Int
    protected val myEnd: Int
    protected var myBeforeStartNonChar: Char = '\u0000'
    protected var myAfterEndNonChar: Char = '\u0000'
    protected var myStartIndex = 0

    init {
        val rawStart = if (startIndex < 0) 0 else if (startIndex > charSequence.length) charSequence.length else startIndex
        val rawEnd = if (endIndex < 0) 0 else if (endIndex > charSequence.length) charSequence.length else endIndex
        if (rawStart != startIndex) addSafeError()
        if (rawEnd != endIndex) addSafeError()
        myStart = if (rawStart > rawEnd) rawEnd else rawStart
        myEnd = rawEnd
        if (myStart != rawStart) addSafeError()
    }

    override fun toString(): String {
        return asString()
    }

    protected var myEndIndex = myEnd - myStart

    override fun getCharSequenceError(): SafeCharSequenceError {
        return myError
    }

    override val length: Int
        get() = myEndIndex - myStartIndex

    override fun getStartIndex(): Int {
        return myStartIndex
    }

    override fun setStartIndex(startIndex: Int) {
        myStartIndex = safeRawIndex(startIndex)
        if (myEndIndex < myStartIndex) myEndIndex = myStartIndex
    }

    override fun getEndIndex(): Int {
        return myEndIndex
    }

    override fun setEndIndex(endIndex: Int) {
        myEndIndex = safeRawIndex(endIndex)
        if (myStartIndex > myEndIndex) myStartIndex = myEndIndex
    }

    override fun getBeforeStartNonChar(): Char = myBeforeStartNonChar
    override fun setBeforeStartNonChar(value: Char) {
        myBeforeStartNonChar = value
    }

    override fun getAfterEndNonChar(): Char = myAfterEndNonChar
    override fun setAfterEndNonChar(value: Char) {
        myAfterEndNonChar = value
    }

    override fun getFirstChar(): Char = get(0)
    override fun getLastChar(): Char = get(length - 1)
    override fun getRawLength(): Int = myEnd - myStart
    override fun isEmpty(): Boolean = myStartIndex >= myEndIndex
    override fun isBlank(): Boolean = isEmpty || countLeading(' ', '\t', '\n') == length

    override fun get(index: Int): Char {
        if (index < 0) {
            addSafeError()
            return myBeforeStartNonChar
        } else if (index >= length) {
            addSafeError()
            return myAfterEndNonChar
        } else return myChars[rawCharIndex(myStartIndex + index)]
    }

    override fun getCharSequence(): CharSequence {
        return myChars.subSequence(rawCharIndex(myStartIndex), rawCharIndex(myEndIndex))
    }

    private fun unsafeSubSequence(startIndex: Int, endIndex: Int): SafeCharSequenceRanger {
        val result = SafeCharSequenceRange(myChars, rawCharIndex(startIndex), rawCharIndex(endIndex))
        // transfer some relevant settings
        result.myBeforeStartNonChar = myBeforeStartNonChar
        result.myAfterEndNonChar = myAfterEndNonChar
        return result
    }

    override fun rawSubSequence(startIndex: Int, endIndex: Int): SafeCharSequenceRanger {
        val safe = safeRawRange(startIndex, endIndex)
        return unsafeSubSequence(safe.start, safe.end)
    }

    override fun subSequence(startIndex: Int, endIndex: Int): SafeCharSequenceRanger {
        val safe = safeRange(startIndex, endIndex)
        return SafeCharSequenceRange(myChars, rawCharIndex(myStartIndex + safe.start), rawCharIndex(myStartIndex + safe.end))
    }

    override fun getSubSequence(): SafeCharSequenceRanger = unsafeSubSequence(myStartIndex, myEndIndex)
    override fun getBeforeStart(): SafeCharSequenceRanger = unsafeSubSequence(0, myStartIndex)
    override fun getAfterEnd(): SafeCharSequenceRanger = unsafeSubSequence(myEndIndex, rawLength)

    protected fun rawCharIndex(index: Int): Int {
        return myStart + index
    }

    fun safeRawCharIndex(index: Int): Int {
        return myStart + safeRawIndex(index)
    }

    override fun safeRawIndex(index: Int): Int {
        val result = if (index < 0 || myStart == myEnd) 0 else if (index > rawLength) rawLength else index
        if (result != index) addSafeError()
        return result
    }

    override fun safeRawInclusiveIndex(index: Int): Int {
        val result = if (index < 0 || myStart == myEnd) 0 else if (index >= rawLength) rawLength - 1 else index
        if (result != index) addSafeError()
        return result
    }

    override fun safeRawRange(startIndex: Int, endIndex: Int): Range {
        val safeStart = safeRawIndex(startIndex)
        val safeEnd = safeRawIndex(endIndex)
        if (safeStart <= safeEnd) return Range(safeStart, safeEnd)
        addSafeError()
        return Range(safeEnd, safeEnd)
    }

    override fun safeIndex(index: Int): Int {
        val result = if (index < 0 || myStartIndex == myEndIndex) 0 else if (index > length) length else index
        if (result != index) addSafeError()
        return result
    }

    override fun safeInclusiveIndex(index: Int): Int {
        val result = if (index < 0 || myStartIndex == myEndIndex) 0 else if (index >= length) length - 1 else index
        if (result != index) addSafeError()
        return result
    }

    override fun safeRange(startIndex: Int, endIndex: Int): Range {
        val fixedStartIndex = if (startIndex <= endIndex) startIndex else endIndex
        val safeStart = safeIndex(fixedStartIndex)
        val safeEnd = safeIndex(endIndex)
        if (fixedStartIndex != startIndex) addSafeError()
        return Range(safeStart, safeEnd)
    }

//    override fun trackedSourceLocation(index: Int): TrackedLocation {
//        if (myChars is SmartSourceLocationTracker) {
//            val trackedLocation = myChars.trackedSourceLocation(index + myStart)
//
//            if (myStart == 0) return trackedLocation
//
//            return trackedLocation.withIndex(trackedLocation.index - myStart)
//                    .withPrevClosest(trackedLocation.prevIndex - myStart)
//                    .withNextClosest(trackedLocation.nextIndex - myStart)
//        }
//
//        val safeIndex = safeInclusiveIndex(index)
//        return TrackedLocation(safeIndex, myStart + myStartIndex + safeIndex, myChars)
//    }
//
//    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
//        if (myChars is SmartSourceLocationTracker) {
//            val trackedLocation = myChars.trackedLocation(source, offset)
//
//            if (trackedLocation != null && trackedLocation.index >= myStart && trackedLocation.index < myEnd) {
//                if (myStart == 0) return trackedLocation
//                return trackedLocation.withIndex(trackedLocation.index - myStart)
//                        .withPrevClosest(trackedLocation.prevIndex - myStart)
//                        .withNextClosest(trackedLocation.nextIndex - myStart)
//            }
//        }
//        return if ((source == null || source === myChars) && offset >= myStart + myStartIndex && offset < myEnd + myEndIndex) TrackedLocation(offset - myStart - myStartIndex, offset, myChars) else null
//    }

    override fun getContents(): SmartCharSequence {
        if (myChars is SmartCharSequenceContainer) return myChars.getContents(myStart + myStartIndex, myStart + myEndIndex)
        return myChars.subSequence(myStart + myStartIndex, myStart + myEndIndex).asSmart()
    }

    override fun getContents(startIndex: Int, endIndex: Int): SmartCharSequence {
        if (myChars is SmartCharSequenceContainer) return myChars.getContents(myStart + myStartIndex + startIndex, myStart + myStartIndex + endIndex)
        return myChars.subSequence(myStart + myStartIndex + startIndex, myStart + myStartIndex + endIndex).asSmart()
    }
}
