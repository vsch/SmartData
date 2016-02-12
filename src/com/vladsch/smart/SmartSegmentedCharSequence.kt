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

open class SmartSegmentedCharSequence : SmartCharSequenceBase<SmartCharSequence>, TrackingCharSequenceMarkerHolder {
    protected val myVersion: SmartVersion
    protected val myCacheVersion: SmartDependentRunnableVersionHolder
    private var myLengths = IntArray(0)
    private var myVariableContent: Boolean = false
    val segments: List<SmartCharSequence>

    constructor(vararg charSequences: CharSequence) {
        val smartCharSequences = Companion.smart(charSequences.toList())
        segments = smartCharSequences
        myVersion = SmartDependentVersionHolder(segments)
        myCacheVersion = SmartDependentRunnableVersionHolder(this, Runnable { computeCachedData() })
    }

    constructor(charSequences: Collection<CharSequence>) {
        segments = Companion.smart(charSequences)
        myVersion = SmartDependentVersionHolder(segments)
        myCacheVersion = SmartDependentRunnableVersionHolder(this, Runnable { computeCachedData() })
    }

    internal fun computeCachedData() {
        val lengths = IntArray(segments.size + 1)
        var length = 0
        var i = 1
        lengths[0] = 0
        var variableContent = false
        for (charSequence in segments) {
            length += charSequence.length
            lengths[i++] = length
            if (!variableContent && charSequence is SmartCharSequence) {
                variableContent = charSequence.version.isMutable
            }
        }
        myLengths = lengths
        myVariableContent = variableContent
    }

    val isVariableContent: Boolean
        get() {
            myCacheVersion.nextVersion()
            return myVariableContent
        }

    override val length: Int
        get() {
            myCacheVersion.nextVersion()
            return myLengths[myLengths.size - 1]
        }

    public override fun charAtImpl(index: Int): Char {
        val i = getCharSequenceIndex(index)
        if (i >= 0) return segments[i][index - myLengths[i]]
        throw IndexOutOfBoundsException("charAt(" + index + ") is not within underlying char sequence range [0, " + myLengths[myLengths.size - 1])
    }

    override fun getVersion(): SmartVersion {
        return myVersion
    }

    fun getSequence(segmentIndex: Int): SmartCharSequence {
        myCacheVersion.nextVersion()
        return segments[segmentIndex]
    }

    fun startIndex(segmentIndex: Int): Int {
        myCacheVersion.nextVersion()
        return myLengths[segmentIndex]
    }

    fun endIndex(segmentIndex: Int): Int {
        myCacheVersion.nextVersion()
        return myLengths[segmentIndex + 1]
    }

    fun length(segmentIndex: Int): Int {
        myCacheVersion.nextVersion()
        return myLengths[segmentIndex + 1] - myLengths[segmentIndex]
    }

    fun lastIndex(segmentIndex: Int): Int {
        myCacheVersion.nextVersion()
        return if (myLengths[segmentIndex + 1] > myLengths[segmentIndex]) myLengths[segmentIndex + 1] - 1 else myLengths[segmentIndex]
    }

    override fun getMarkers(id: String?): List<TrackedLocation> {
        myCacheVersion.nextVersion()
        if (myVariableContent) {
            val markers = ArrayList<TrackedLocation>()
            val iMax = segments.size
            for (i in 0..iMax - 1) {
                val charSequence = segments[i]
                if (charSequence is SmartCharSequenceMarker && (id == null || id == charSequence.id)) {
                    val index = myLengths[i]
                    var trackedLocation = TrackedLocation(index, 0, charSequence)
                    if (index > 0) {
                        // have real previous
                        val prevLocation = trackedSourceLocation(index - 1)
                        trackedLocation = trackedLocation.withPrevClosest(prevLocation.index, prevLocation.offset, prevLocation.source)
                    }

                    if (myLengths[i + 1] < length) {
                        // there is a next real segment after the marker
                        val nextIndex = myLengths[i + 1]
                        val nextLocation = trackedSourceLocation(nextIndex)
                        trackedLocation = trackedLocation.withPrevClosest(nextLocation.index, nextLocation.offset, nextLocation.source)
                    }
                    markers.add(trackedLocation)
                } else {
                    val locations = charSequence.getMarkers(id)
                    if (!locations.isEmpty()) {
                        // re-map
                        for (location in locations) {
                            markers.add(location.withIndex(myLengths[i] + location.index).withPrevClosest(myLengths[i] + location.prevIndex).withNextClosest(myLengths[i] + location.nextIndex))
                        }
                    }
                }
            }

            if (!markers.isEmpty()) return markers
        }
        return TrackedLocation.EMPTY_LIST
    }

    override fun getCharsImpl(dst: CharArray, dstOffset: Int) {
        myCacheVersion.nextVersion()
        val iMax = segments.size
        for (i in 0..iMax - 1) {
            segments[i].getChars(dst, dstOffset + myLengths[i])
        }
    }

    protected fun getCharSequenceIndex(index: Int): Int {
        myCacheVersion.nextVersion()
        for (i in 1..myLengths.size - 1) {
            if (index < myLengths[i] || index == myLengths[i] && i + 1 == myLengths.size) {
                return i - 1
            }
        }
        return -1
    }

    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartCharSequence {
        val iStart = getCharSequenceIndex(startIndex)
        val iEnd = getCharSequenceIndex(endIndex)

        if (iStart >= 0 && iEnd >= 0 && iStart <= iEnd) {
            if (iStart == iEnd) {
                // subSequence of one of the sequences
                return segments[iStart].subSequence(startIndex - myLengths[iStart], endIndex - myLengths[iStart])
            } else if (iStart == 0 && startIndex == 0 && iEnd == myLengths.size - 2 && endIndex == myLengths[iEnd + 1]) {
                // just a copy of us
                return this
            } else {
                // partial of our sequence
                val trackingSequences = Companion.trackingSequences(segments, iStart, startIndex - myLengths[iStart], iEnd, endIndex - myLengths[iEnd])
                if (trackingSequences.size == 1) {
                    return trackingSequences[0]
                } else {
                    return SmartSegmentedCharSequence(trackingSequences)
                }
            }
        }

        // won't happen but we leave it in
        throw IndexOutOfBoundsException("subSequence($startIndex, $endIndex) is not within sequence list range of [0, $length)")
    }

    override fun flattened(sequences: ArrayList<SmartCharSequence>) {
        for (charSequence in segments) {
            charSequence.flattened(sequences)
        }
    }

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        val i = getCharSequenceIndex(index)
        if (i >= 0) {
            val trackedLocation = segments[i].trackedSourceLocation(index - myLengths[i])
            return trackedLocation.withIndex(index)
        }
        throw IndexOutOfBoundsException("charAt(" + index + ") is not within underlying char sequence range [0, " + myLengths[myLengths.size - 1])
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        var i = 0
        for (charSequence in segments) {
            val trackedLocation = charSequence.trackedLocation(source, offset)
            if (trackedLocation != null) {
                return trackedLocation.withIndex(trackedLocation.index + myLengths[i])
            }
            i++
        }
        return null
    }

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        if (other == null) return null

        val merged = segments[segments.size - 1].splicedWith(other)
        if (merged != null) {
            val smartCharSequences = ArrayList<SmartCharSequence>(segments.size)
            if (segments.size > 1) System.arraycopy(segments, 0, smartCharSequences, 0, segments.size - 1)
            smartCharSequences[segments.size - 1] = merged
            return SmartSegmentedCharSequence(smartCharSequences)
        }
        return null
    }
}
