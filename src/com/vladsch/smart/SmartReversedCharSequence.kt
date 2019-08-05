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

import java.util.*

class SmartReversedCharSequence(chars: SmartCharSequence) : SmartCharSequenceBase<SmartReversedCharSequence>() {
    protected val myVersion: SmartVersion = chars.version
    protected val myChars = chars

    override fun addStats(stats: SmartCharSequence.Stats) {
        myChars.addStats(stats)
        stats.nesting++
    }

    constructor(chars: CharArray, start: Int, end: Int) : this(SmartCharArraySequence(chars, start, end))

    constructor(chars: CharArray) : this(SmartCharArraySequence(chars, 0, chars.size))

    constructor(chars: CharSequence) : this(SmartCharSequenceWrapper(chars))

    override fun getVersion(): SmartVersion = myVersion

    override fun reversed(): SmartCharSequence = myChars

    override val length: Int get() = myChars.length

    override fun charAtImpl(index: Int): Char = myChars.cachedProxy[reversedOffset(index)]
//    override fun charAtImpl(index: Int): Char = myChars[reversedOffset(index)]

    override fun getCharsImpl(dst: CharArray, dstOffset: Int) {
        val iMax = length
        for (i in 0..iMax - 1) {
            dst[dstOffset + reversedOffset(i)] = myChars[i]
        }
    }

    override fun getCachedProxy(): SmartCharSequence = this
    override fun toString(): String {
        return String(chars)
    }

    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartReversedCharSequence = SmartReversedCharSequence(myChars.subSequence(startIndex, endIndex))

    protected fun reversedOffset(offset: Int): Int = length - offset - 1

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        checkIndex(index)

        val location = myChars.trackedSourceLocation(reversedOffset(index))
        return adjustTrackedSourceLocation(location)
    }

    override fun getMarkers(id: String?): List<TrackedLocation> {
        val locations = myChars.getMarkers(id)
        if (!locations.isEmpty()) {
            // re-map
            val markers = ArrayList(locations)
            for (location in locations) {
                markers.add(adjustTrackedLocation(location))
            }
            return markers
        }
        return TrackedLocation.EMPTY_LIST
    }

    protected fun adjustTrackedLocation(location: TrackedLocation?): TrackedLocation? {
        if (location != null) {
            return adjustTrackedSourceLocation(location)
        }
        return null
    }

    protected fun adjustTrackedSourceLocation(location: TrackedLocation): TrackedLocation {
        return location.withIndex(reversedOffset(location.index)).withPrevClosest(reversedOffset(location.prevIndex)).withNextClosest(reversedOffset(location.nextIndex))
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        val location = myChars.trackedLocation(source, offset)
        return adjustTrackedLocation(location)
    }

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        if (other is SmartReversedCharSequence) {
            val spliced = other.splicedWith(myChars)
            if (spliced != null) {
                return SmartReversedCharSequence(spliced)
            }
        }
        return null
    }
}
