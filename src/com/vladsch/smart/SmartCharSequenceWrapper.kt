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

open class SmartCharSequenceWrapper(chars: CharSequence, startIndex: Int = 0, endIndex: Int = chars.length) : SmartCharSequenceBase<SmartCharSequenceWrapper>() {

    val myVersion: SmartVersion = if (chars is SmartCharSequence) chars.version else SmartImmutableVersion()
    val myChars: CharSequence = chars
    val myStart: Int = startIndex
    val myEnd: Int = endIndex

    init {
        if (myStart < 0 || myEnd > myChars.length) {
            throw IllegalArgumentException("TrackingCharArraySequence(chars, " + myStart + ", " + myEnd + ") is outside data source range [0, " + myChars.length + ")")
        }
    }

    override fun addStats(stats: SmartCharSequence.Stats) {
        if (myChars is SmartCharSequence) {
            myChars.addStats(stats)
        } else {
            stats.segments++
        }
        stats.nesting++
    }

    /*
     *  raw access, never via proxy or in proxy via original
     */
    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartCharSequenceWrapper {
        return SmartCharSequenceWrapper(myChars, myStart + startIndex, myStart + endIndex)
    }

    override fun charAtImpl(index: Int): Char = myChars[myStart + index]
    override fun getCharsImpl(dst: CharArray, dstOffset: Int) {
        if (myChars is SmartCharSequence) myChars.subSequence(myStart, myEnd).getChars(dst, dstOffset)
        else {
            val iMax = length
            for (i in 0 .. iMax - 1) {
                dst[dstOffset + i] = charAtImpl(i)
            }
        }
    }

    // disable proxy copying, use mapping, it's fast enough
    //    override fun getCachedProxy(): SmartCharSequence = this

    override fun toString(): String = myChars.subSequence(myStart, myEnd).toString()

    override fun get(index: Int): Char {
        // RELEASE : remove test for release
        //        if (myStart + index < 0 || myStart + index >= myChars.length) {
        //            val tmp = 0;
        //        }
        return myChars[myStart + index]
    }

    override fun getVersion(): SmartVersion = myVersion

    override val length: Int get() = myEnd - myStart

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        if (myChars is SmartSourceLocationTracker) {
            val trackedLocation = myChars.trackedSourceLocation(index + myStart)

            if (myStart == 0) return trackedLocation

            return trackedLocation.withIndex(trackedLocation.index - myStart)
                .withPrevClosest(trackedLocation.prevIndex - myStart)
                .withNextClosest(trackedLocation.nextIndex - myStart)
        }
        checkIndex(index)
        return TrackedLocation(index, myStart + index, myChars)
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        if (myChars is SmartSourceLocationTracker) {
            val trackedLocation = myChars.trackedLocation(source, offset)

            if (trackedLocation != null && trackedLocation.index >= myStart && trackedLocation.index < myEnd) {
                return trackedLocation.withIndex(trackedLocation.index - myStart)
                    .withPrevClosest(trackedLocation.prevIndex - myStart)
                    .withNextClosest(trackedLocation.nextIndex - myStart)
            }
        }
        return if ((source == null || source === myChars) && offset >= myStart && offset < myEnd) TrackedLocation(offset - myStart, offset, myChars) else null
    }

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        if (other is SmartCharSequenceWrapper) {
            if (myChars == other.myChars && myEnd == other.myStart) {
                return SmartCharSequenceWrapper(myChars, myStart, other.myEnd)
            }
        }
        return null
    }

    override fun getMarkers(id: String?): List<TrackedLocation> {
        return TrackedLocation.EMPTY_LIST
    }
}
