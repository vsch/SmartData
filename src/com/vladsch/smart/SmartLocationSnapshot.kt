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

internal class SmartSourceLocation(val source: Any, val locations: ArrayList<Range>, val sourceLocations: ArrayList<Range>) : SmartSourceLocationTracker {
    var optimizedSource: Boolean = false
    var sourceIndices = IntArray(0)

    fun optimizeSource() {
        // optimize smart source locations
        // TODO: sort ranges by sourceLocation
        // now we remove any contained and overlapped ranges
        if (sourceLocations.size > 0) {
            val sortIndices = IntArray(sourceLocations.size, { it })
            sourceIndices = sortIndices.sortedWith(Comparator { s, o -> sourceLocations[s].compare(sourceLocations[o]) }).toIntArray()
        } else {
            sourceIndices = IntArray(0)
        }

        optimizedSource = true
    }

    fun addEntry(location: Range, sourceLocation: Range) {
        optimizedSource = false
        locations.add(location)
        sourceLocations.add(sourceLocation)
    }

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        throw UnsupportedOperationException()
    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        if (source != this.source) return null
        if (!optimizedSource) optimizeSource()

        // TODO: find source and return TrackedLocation
        var span = (sourceIndices.size + 1) / 2
        var pos = span
        var found: Int? = null

        while (span > 0) {
            val range = sourceLocations[sourceIndices[pos]]

            if (offset < range.start) {
                pos -= span
                span /= 2
            } else {
                if (offset < range.end) {
                    found = sourceIndices[pos]
                    break
                }

                pos += span
                span /= 2
                if (pos >= sourceIndices.size) {
                    if (span == 0) break
                    pos -= span
                    span /= 2
                }
            }
        }

        // found != null if we have the exact
        // pos is the closest to given offset
        if (found != null) {
            val location = locations[found]
            val sourceLocation = sourceLocations[found]

            if (sourceLocation.span == location.span) {
                // exact mapping
                return TrackedLocation(offset, offset - sourceLocation.start + location.start, source)
            } else if (sourceLocation.span != 0 && location.span != 0) {
                // almost an exact match
                val indexDelta = (offset - sourceLocation.start).toDouble() / sourceLocation.span.toDouble() * location.span.toDouble()
                val index = Math.round(indexDelta).toInt() + location.start
                val nextIndex = if (index + 1 <= location.end) index + 1 else index
                val prevIndex = if (index - 1 >= location.start) index + 1 else index
                val nextOffset = Math.round((nextIndex - location.start).toDouble() / location.span.toDouble() * sourceLocation.span.toDouble()).toInt() + sourceLocation.start
                val prevOffset = Math.round((nextIndex - location.start).toDouble() / location.span.toDouble() * sourceLocation.span.toDouble()).toInt() + sourceLocation.start
                return TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, source, source)
            }
        }

        return null
    }
}

class SmartLocationSnapshot private constructor(val sources: Map<Any, SmartSourceLocation>) : SmartSourceLocationTracker {
    //    var optimizedDestination: Boolean = false
    //    val locationIndices = IntArray(0)

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        throw UnsupportedOperationException()
        //        if (!optimizedDestination) optimizeDestination()
        //
        //        // TODO: find location and return TrackedLocation
        //        return TrackedLocation(0,0,this)
    }

    //    fun optimizeDestination() {
    //        // optimize smart source locations
    //        // TODO: sort ranges by location
    //
    //        optimizedDestination = true
    //    }

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        if (source == null) return null

        val sourceLocation = sources[source] ?: return null
        return sourceLocation.trackedLocation(source, offset)
    }

    companion object {
        fun create(sourceSequence: SmartCharSequence): SmartLocationSnapshot {
            val sources = ArrayList<Any>()
            val locations = ArrayList<Range>()
            val sourceLocations = ArrayList<Range>()

            sourceSequence.getSourceLocations(sources, locations, sourceLocations)
            assert(sources.size == locations.size)
            assert(sources.size == sourceLocations.size)

            val sourceMap = HashMap<Any, SmartSourceLocation>()

            for (i in 0..sources.size - 1) {
                val aSource = sources[i]
                val sourceItem = sourceMap[aSource]

                var source = sourceItem ?: SmartSourceLocation(aSource, ArrayList(), ArrayList())
                source.addEntry(locations[i], sourceLocations[i])
            }

            return SmartLocationSnapshot(sourceMap)
        }
    }
}
