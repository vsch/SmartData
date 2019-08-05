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

import com.intellij.util.text.CharSequenceBackedByArray
import java.util.*

class SmartReplacedCharSequence(replacedChars: SmartCharSequence, val chars: CharSequence) : SmartTransformedSequence<SmartReplacedCharSequence>(replacedChars) {
    protected val myVersion: SmartVersion
    protected val myChars = chars

    init {
        if (myChars is SmartCharSequence) {
            myVersion = myChars.version
        } else {
            myVersion = SmartImmutableVersion()
        }
    }

    override fun getVersion(): SmartVersion = myVersion

    override val length: Int get() = myChars.length

    // disable proxy copying if replacement is not a smart sequence, use mapping, it's fast enough
    override fun getCachedProxy(): SmartCharSequence {
        if (myChars is SmartCharSequence) {
            return myChars.cachedProxy
        }
        return this
    }

    override fun toString(): String {
        return String(getChars())
    }

    override fun charAtImpl(index: Int): Char {
        return myChars[index]
    }

    override fun getChars(): CharArray {
        if (myChars is CharSequenceBackedByArray) {
            return myChars.chars
        } else {
            return super.getCharsImpl()
        }
    }

    override fun getCharsImpl(dst: CharArray, dstOffset: Int) {
        if (myChars is CharSequenceBackedByArray) {
            myChars.getChars(dst, dstOffset)
        } else {
            val iMax = length
            for (i in 0..iMax-1) {
                dst[dstOffset+i] = myChars[i]
            }
        }
    }

    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartReplacedCharSequence {
        return SmartReplacedCharSequence(myReplacedChars, myChars.subSequence(startIndex, endIndex))
    }

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        checkIndex(index)
        return myReplacedChars.trackedSourceLocation(if (index < myReplacedChars.length) index else myReplacedChars.length - 1)
    }

    override fun getMarkers(id: String?): List<TrackedLocation> {
        if (myChars is SmartCharSequence) {
            val locations = myChars.getMarkers(id)
            if (!locations.isEmpty()) {
                if (length < myReplacedChars.length) {
                    // re-map
                    val markers = ArrayList(locations)
                    for (location in locations) {
                        markers.add(adjustTrackedLocation(location))
                    }
                    return markers
                }
                return locations
            }
        }
        return TrackedLocation.EMPTY_LIST
    }

    protected fun adjustTrackedLocation(location: TrackedLocation?): TrackedLocation? {
        if (location != null) {
            val length = length
            if (length < myReplacedChars.length) {
                @Suppress("NAME_SHADOWING")
                var location = location
                if (location.index > length) location = location.withIndex(length)
                if (location.prevIndex > length) location = location.withPrevClosest(length)
                if (location.prevIndex > length) location = location.withNextClosest(length)
                return location
            }
        }
        return location
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        val location = myReplacedChars.trackedLocation(source, offset)
        return adjustTrackedLocation(location)
    }

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        return null
    }
}
