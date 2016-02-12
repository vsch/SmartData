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

import com.intellij.openapi.util.text.CharSequenceWithStringHash
import com.intellij.util.text.CharSequenceBackedByArray

/**
 * IMPORTANT: if original is not null then editing and non-raw access is directed to it though super class calls so that this
 * class doubles as a fast access proxy with tracking preserved because all affected requests will be directed to the original
 *
 */

open class SmartCharArraySequence(original: SmartCharSequenceBase<*>?, chars: CharArray, startIndex: Int = 0, endIndex: Int = chars.size) : SmartCharSequenceBase<SmartCharArraySequence>(), CharSequenceBackedByArray, CharSequenceWithStringHash {

    @JvmOverloads constructor(chars: CharArray, start: Int = 0, end: Int = chars.size) : this(null, chars, start, end)

    final protected val myOriginal: SmartCharSequenceBase<*>? = original
    final protected val myVersion: SmartVersion = if (original != null ) SmartCacheVersion(original.version) else SmartImmutableVersion()
    final protected val myChars: CharArray = chars
    final protected val myStart: Int = startIndex
    final protected val myEnd: Int = endIndex

    init {
        if (myStart < 0 || myEnd > myChars.size) {
            throw IllegalArgumentException("TrackingCharArraySequence(chars, " + myStart + ", " + myEnd + ") is outside data source range [0, " + myChars.size + ")")
        }
    }

    /*
     *  raw access, never via proxy or in proxy via original
     */
    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartCharArraySequence {
        return SmartCharArraySequence(myOriginal, myChars, myStart + startIndex, myStart + endIndex)
    }

    override fun charAtImpl(index: Int): Char = myChars[myStart + index]
    override fun getCharsImpl(dst: CharArray, dstOffset: Int) = getChars(dst, dstOffset)
    override fun getCharsImpl(): CharArray = chars

    override fun toString(): String {
        return String(myChars, myStart, myEnd - myStart)
    }

    // always on original
    override fun getCachedProxy(): SmartCharSequence = if (myOriginal != null) super.getCachedProxy() else this

    /*
     *  use proxy if fresh otherwise raw access
     */
    override val freshProxyOrNull: SmartCharArraySequence? get() = this

    override fun getChars(): CharArray {
        if (myStart == 0) return myChars
        val chars = CharArray(length)
        System.arraycopy(myChars, myStart, chars, 0, length)
        return chars
    }

    override fun getChars(dst: CharArray, dstOffset: Int) {
        if (dstOffset + length > dst.size) {
            val tmp = 0
        }
        System.arraycopy(myChars, myStart, dst, dstOffset, length)
    }

    override fun get(index: Int): Char = myChars[myStart + index]

    override fun subSequence(startIndex: Int, endIndex: Int): SmartCharArraySequence {
        if (myOriginal != null) return super.subSequence(startIndex, endIndex) as SmartCharArraySequence

        checkBounds(startIndex, endIndex)
        if (startIndex == 0 && endIndex == length) return this
        return properSubSequence(startIndex, endIndex)
    }

    /*
     * Implementation
     */
    override fun getVersion(): SmartVersion = myVersion

    override val length: Int get() = myEnd - myStart

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        checkIndex(index)
        return TrackedLocation(index, myStart + index, myChars)
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        return if ((source == null || source === myChars) && offset >= myStart && offset < myEnd) TrackedLocation(offset - myStart, offset, myChars) else null
    }

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        if (myOriginal != null) return myOriginal.splicedWith(other)

        if (other is SmartCharArraySequence) {
            if (myChars == other.myChars && myEnd == other.myStart) {
                return SmartCharArraySequence(myChars, myStart, other.myEnd)
            }
        }
        return null
    }

    override fun getMarkers(id: String?): List<TrackedLocation> {
        if (myOriginal != null) return myOriginal.getMarkers(id)

        return TrackedLocation.EMPTY_LIST
    }

    override fun reversed(): SmartCharSequence {
        if (myOriginal != null) return myOriginal.reversed()

        return SmartReversedCharSequence(myChars, myStart, myEnd)
    }

}
