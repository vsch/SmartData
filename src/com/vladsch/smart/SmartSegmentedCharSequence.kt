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

open class SmartSegmentedCharSequence : SmartCharSequenceBase<SmartCharSequence>, TrackingCharSequenceMarkerHolder {
    protected val myVersion: SmartVersion
    protected val myCacheVersion: SmartDependentRunnableVersionHolder
    private var myLengths = IntArray(0)
    private var myVariableContent: Boolean = false
    val segments: List<SmartCharSequence>
    private var myLastSegment: Int? = null
    private var myLengthSpan: Int = 0

    override fun addStats(stats: SmartCharSequence.Stats) {
        var maxNesting = 0

        for (segment in segments) {
            var childStats = SmartCharSequence.Stats()
            segment.addStats(childStats)
            stats.segments += childStats.segments
            maxNesting.minLimit(childStats.nesting)
        }

        stats.nesting = maxNesting + 1
    }

    constructor(vararg charSequences: CharSequence) {
        val smartCharSequences = smartList(charSequences.toList())
        segments = smartCharSequences
        myVersion = SmartDependentVersionHolder(segments)
        myCacheVersion = SmartDependentRunnableVersionHolder(this, Runnable { computeCachedData() })
    }

    constructor(charSequences: Collection<CharSequence>) {
        segments = smartList(charSequences)
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
            if (!variableContent) {
                variableContent = charSequence.version.isMutable
            }
        }

        myLengths = lengths
        myVariableContent = variableContent
        myLastSegment = 0

        i = 1
        while (i < lengths.size) i = i.shl(1)
        myLengthSpan = i
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
        if (i >= 0) {
            myLastSegment = i
            return segments[i][index - myLengths[i]]
        }
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

    override fun getSourceLocations(sources: ArrayList<Any>, locations: ArrayList<Range>, sourceLocations: ArrayList<Range>) {
        super.getSourceLocations(sources, locations, sourceLocations)
    }

    protected fun getCharSequenceIndex(index: Int): Int {
        var lastSegment = myLastSegment

        if (lastSegment != null) {
            if (index >= myLengths[lastSegment] && index < myLengths[lastSegment + 1]) return lastSegment
            // see if it is next or previous
            if (lastSegment + 2 <= myLengths.lastIndex && index >= myLengths[lastSegment + 1] && index < myLengths[lastSegment + 2]) {
                lastSegment++
                myLastSegment = lastSegment
                return lastSegment
            }

            if (lastSegment > 0 && index >= myLengths[lastSegment - 1] && index < myLengths[lastSegment]) {
                lastSegment--
                myLastSegment = lastSegment
                return lastSegment
            }
        } else {
            // need to update indices
            myCacheVersion.nextVersion()
        }

        return adjustBinaryIndex(getIndexBinary(index))
        //        return getIndexLinear(index)

        //        val result = getIndexLinear(index)
        //        var fastResult = getIndexBinary(index)
        //
        //        if (result != fastResult) {
        //            println("result:$result != fastResult:$fastResult")
        //            assert(result == fastResult, { "result:$result != fastResult:$fastResult" })
        //        }
        //
        //        return result
    }

    internal fun getIndexLinear(index: Int): Int {
        for (i in 1..myLengths.size - 1) {
            if (index < myLengths[i] || index == myLengths[i] && i + 1 == myLengths.size) {
                return i - 1
            }
        }
        return -1
    }

    internal fun adjustBinaryIndex(index: Int): Int {
        @Suppress("NAME_SHADOWING")
        var index = index
        // since we can have 0 length segments we need to skip them
        if (index < 0) return index
        while (index + 1 < myLengths.size - 1 && myLengths[index] == myLengths[index + 1]) index++
        return if (index >= myLengths.size - 1) -1 else index
    }

    internal fun getIndexBinary(index: Int): Int {
        val iMax = myLengths.size

        if (iMax >= 0) {
            var i = myLengthSpan.shr(1) - 1
            var span = myLengthSpan.shr(2)

            loop@
            while (true) {
                val len = myLengths[i]

                if (index <= len) {
                    if (span == 0 || index == len) {
                        return if (index == len && i < iMax - 1) i else i - 1
                    }

                    i -= span
                    span /= 2
                } else {
                    if (span == 0) {
                        return i
                    }

                    i += span
                    span /= 2

                    while (i >= iMax) {
                        if (span == 0) break@loop
                        i -= span
                        span /= 2
                    }
                }
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
                val trackingSequences = trackingSequences(segments, iStart, startIndex - myLengths[iStart], iEnd, endIndex - myLengths[iEnd])
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
            if (myLengths[i] == 0) return trackedLocation
            return trackedLocation.withIndex(trackedLocation.index + myLengths[i])
                    .withPrevClosest(trackedLocation.prevIndex + myLengths[i])
                    .withNextClosest(trackedLocation.nextIndex + myLengths[i])
        }
        throw IndexOutOfBoundsException("charAt(" + index + ") is not within underlying char sequence range [0, " + myLengths[myLengths.size - 1])
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        var i = 0
        myCacheVersion.nextVersion()
        for (charSequence in segments) {
            val trackedLocation = charSequence.trackedLocation(source, offset)
            if (trackedLocation != null) {
                if (myLengths[i] == 0) return trackedLocation
                else return trackedLocation.withIndex(trackedLocation.index + myLengths[i])
                        .withPrevClosest(trackedLocation.prevIndex + myLengths[i])
                        .withNextClosest(trackedLocation.nextIndex + myLengths[i])
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
