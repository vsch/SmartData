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

class SmartVariableCharSequence(replacedChars: SmartCharSequence, chars: CharSequence) : SmartCharSequenceBase<SmartCharSequence>() {

    constructor(replacedChars: SmartCharSequence) : this(replacedChars, replacedChars)

    protected val myReplacedChars = replacedChars
    protected var myLeftPadding:CharSequence = EMPTY_SEQUENCE
    protected var myRightPadding:CharSequence = EMPTY_SEQUENCE

    // versioned properties
    protected val myLeftPadChar = SmartVolatileData(' ')
    protected val myRightPadChar = SmartVolatileData(' ')

    protected val myPrefix = SmartVolatileData<CharSequence>(EMPTY_SEQUENCE)
    protected val mySuffix = SmartVolatileData<CharSequence>(EMPTY_SEQUENCE)
    protected val myVariableChars = SmartVolatileData(SmartReplacedCharSequence(myReplacedChars, chars))
    protected val myFixedLength = SmartDependentData(listOf(myPrefix, myVariableChars, mySuffix), { myVariableChars.value.length + myPrefix.value.length + mySuffix.value.length })

    protected var myWidth = SmartVersionedProperty("varCharSeq:Width", 0)
    protected var myAlignment = SmartVersionedProperty("varCharSeq:Alignment",TextAlignment.LEFT)

    protected var myResultSequence = SmartDependentData(listOf(myFixedLength, myAlignment, myWidth, myLeftPadChar, myRightPadChar), DataComputable { computeResultSequence() })
    protected val myVersion = SmartDependentVersion(listOf(myResultSequence, myReplacedChars.version))

    var alignment: TextAlignment get() = myAlignment.value
        set(value) {
            myAlignment.value = value
        }

    var width: Int get() = myWidth.value
        set(value) {
            myWidth.value = value
        }

    var prefix: CharSequence get() = myPrefix.value
        set(value) {
            myPrefix.value = value
        }

    var suffix: CharSequence get() = mySuffix.value
        set(value) {
            mySuffix.value = value
        }

    var leftPadChar: Char get() = myLeftPadChar.value
        set(value) {
            myLeftPadChar.value = value
        }

    var rightPadChar: Char get() = myRightPadChar.value
        set(value) {
            myRightPadChar.value = value
        }

    var variableChars: CharSequence
        get() = myVariableChars.value
        set(chars) {
            myVariableChars.value = SmartReplacedCharSequence(myReplacedChars, chars)
        }

    override fun getVersion(): SmartVersion {
        return myVersion
    }

    var alignmentDataPoint: SmartVersionedDataHolder<TextAlignment>
        get() = myAlignment
        set(value) {
            myAlignment.connect(value)
        }

    var widthDataPoint: SmartVersionedDataHolder<Int>
        get() = myWidth
        set(value) {
            myWidth.connect(value)
        }

    val lengthDataPoint: SmartVersionedDataHolder<Int>
        get() = myFixedLength

    protected val resultSequence: SmartCharArraySequence
        get() = myResultSequence.value

    protected fun computeResultSequence(): SmartCharArraySequence {
        val paddingSize = myWidth.value - myFixedLength.value
        var leftPadding = 0
        var rightPadding = 0

        if (paddingSize > 0) {
            when (myAlignment.value) {
                TextAlignment.RIGHT -> leftPadding = paddingSize
                TextAlignment.CENTER -> {
                    var leftPrePad = myPrefix.value.countLeading(' ')
                    var rightPrePad = mySuffix.value.countTrailing(' ')
                    val commonPrePad = leftPrePad.min(rightPrePad)
                    rightPrePad -= commonPrePad
                    leftPrePad -= commonPrePad - rightPrePad
                    val even = paddingSize / 2
                    if (even > leftPrePad) leftPadding = even - leftPrePad
                    rightPadding = paddingSize - leftPadding
                }
                TextAlignment.LEFT -> rightPadding = paddingSize
                TextAlignment.JUSTIFIED -> rightPadding = paddingSize
                else -> throw IllegalArgumentException("Unrecognized TextAlignment value")
            }
        }

        if (leftPadding > 0) myLeftPadding = RepeatedCharSequence(myLeftPadChar.value, leftPadding)
        else myLeftPadding = EMPTY_SEQUENCE

        if (rightPadding > 0) myRightPadding = RepeatedCharSequence(myRightPadChar.value, rightPadding)
        else myRightPadding = EMPTY_SEQUENCE

        return SmartSegmentedCharSequence(myPrefix.value, myLeftPadding, myVariableChars.value, myRightPadding, mySuffix.value).cachedProxy as SmartCharArraySequence
    }

    fun leftAlign(width: Int) {
        alignment = TextAlignment.LEFT
        this.width = width
    }

    fun rightAlign(width: Int) {
        alignment = TextAlignment.RIGHT
        this.width = width
    }

    fun centerAlign(width: Int) {
        alignment = TextAlignment.CENTER
        this.width = width
    }

    override val length: Int get() = resultSequence.length
    override fun getCharsImpl(dst: CharArray, dstOffset: Int) = resultSequence.getCharsImpl(dst, dstOffset)
    override fun charAtImpl(index: Int): Char = resultSequence[index]
    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartCharSequence = resultSequence.subSequence(startIndex, endIndex)
    override fun getCharsImpl(): CharArray = resultSequence.getCharsImpl()
    override fun getCachedProxy(): SmartCharSequence = resultSequence.cachedProxy

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        val location = resultSequence.trackedLocation(source, offset)
        return location
    }

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        checkIndex(index)
        val location = resultSequence.trackedSourceLocation(index)
        return location
    }

    protected fun adjustTrackedLocation(location: TrackedLocation): TrackedLocation {
        val leadPadding = myLeftPadding.length + myPrefix.value.length
        if (leadPadding > 0) {
            return location.withIndex(leadPadding + location.index).withPrevClosest(leadPadding + location.prevIndex).withNextClosest(leadPadding + location.nextIndex)
        }
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

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        return null
    }
}
