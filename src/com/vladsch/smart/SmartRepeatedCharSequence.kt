/*
 * Copyright (c) 2015-2020 Vladimir Schneider <vladimir.schneider@gmail.com>
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
import java.util.function.Supplier

class SmartRepeatedCharSequence(replacedChars: SmartCharSequence, chars: CharSequence, startIndex: Int, endIndex: Int) : SmartCharSequenceBase<SmartCharSequence>() {

    constructor(replacedChars: SmartCharSequence, startIndex: Int, endIndex: Int) : this(replacedChars, replacedChars, startIndex, endIndex)

    @JvmOverloads constructor(replacedChars: SmartCharSequence, chars: CharSequence, repeatCount: Int = 1) : this(replacedChars, chars, 0, chars.length * repeatCount)

    // CharSequence based
    constructor(chars: CharSequence, startIndex: Int, endIndex: Int) : this(EMPTY_SEQUENCE, chars, startIndex, endIndex)

    @JvmOverloads constructor(chars: CharSequence, repeatCount: Int = 1) : this(EMPTY_SEQUENCE, chars, 0, chars.length * repeatCount)

    // char[] based
    constructor(chars: CharArray, startIndex: Int, endIndex: Int) : this(EMPTY_SEQUENCE, RepeatedCharSequence(chars, startIndex, endIndex), 0, endIndex - startIndex)

    @JvmOverloads constructor(chars: CharArray, repeatCount: Int = 1) : this(chars, 0, chars.size * repeatCount)

    @JvmOverloads constructor(char: Char, repeatCount: Int = 1) : this(charArrayOf(char), repeatCount)

    // String based
    constructor(chars: String, startIndex: Int, endIndex: Int) : this(chars.toCharArray(), startIndex, endIndex)

    @JvmOverloads constructor(chars: String, repeatCount: Int = 1) : this(chars, 0, chars.length * repeatCount)

    protected val myReplacedChars = replacedChars

    override fun addStats(stats: SmartCharSequence.Stats) {
        myReplacedChars.addStats(stats)
        stats.nesting++
    }

    // versioned properties
    protected val myVariableChars = SmartVersionedProperty(chars)
    protected var myStartIndex = SmartVersionedProperty(startIndex)
    protected var myEndIndex = SmartVersionedProperty(endIndex)
    protected var myResultSequence = SmartDependentData(listOf(myVariableChars, myStartIndex, myEndIndex), Supplier { computeResultSequence() })
    protected var myLength = SmartDependentData(myResultSequence, Supplier { myResultSequence.get().length })

    protected val myVersion = SmartDependentVersion(listOf(myResultSequence, myReplacedChars.version))

    var startIndex: Int get() = myStartIndex.get()
        set(value) {
            myStartIndex.set(value)
        }

    var endIndex: Int get() = myEndIndex.get()
        set(value) {
            myEndIndex.set(value)
        }

    var variableChars: CharSequence
        get() = myVariableChars.get()
        set(chars) {
            myVariableChars.set(SmartReplacedCharSequence(myReplacedChars, chars))
        }

    override fun getVersion(): SmartVersion {
        return myVersion
    }

    val lengthDataPoint: SmartVersionedDataHolder<Int>
        get() = myLength

    var variableCharsDataPoint: SmartVersionedProperty<CharSequence>
        get() = myVariableChars
        set(value) {
            myVariableChars.connect(value)
        }

    var startIndexDataPoint: SmartVersionedProperty<Int>
        get() = myStartIndex
        set(value) {
            myStartIndex.connect(value)
        }

    var endIndexDataPoint: SmartVersionedProperty<Int>
        get() = myEndIndex
        set(value) {
            myEndIndex.connect(value)
        }

    protected val resultSequence: SmartCharSequence
        get() = myResultSequence.get()

    protected fun computeResultSequence(): SmartCharSequence {
        val chars = StringBuilder().append(myVariableChars.get()).toString().toCharArray()
//        return SmartCharSequenceWrapper(RepeatedCharSequence(chars, myStartIndex.value, myEndIndex.value)).cachedProxy
        return SmartCharSequenceWrapper(RepeatedCharSequence(chars, myStartIndex.get(), myEndIndex.get()))
    }

    override var length: Int get() = myLength.get()
        set(value) {
            if (value >= 0 && myStartIndex.get() + value != myEndIndex.get()) {
                myEndIndex.set(myStartIndex.get() + value)
            }
        }

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
        //        if (location != null) {
        //            val leadPadding = myLeftPadding.length + myPrefix.value.length
        //            if (leadPadding > 0) {
        //                return adjustTrackedSourceLocation(location)
        //            }
        //        }
        return location
    }

    protected fun adjustTrackedSourceLocation(location: TrackedLocation): TrackedLocation {
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
