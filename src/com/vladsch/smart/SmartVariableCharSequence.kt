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

    protected val myVersion = SmartVolatileVersion()
    protected val myReplacedChars = replacedChars
    protected var myVariableChars = SmartReplacedCharSequence(myReplacedChars, chars)
    protected var myLeftPadding: CharSequence = EMPTY_SEQUENCE
    protected var myRightPadding: CharSequence = EMPTY_SEQUENCE
    protected var myResultSequence: SmartSegmentedCharSequence? = null
    protected var myWidth: Int = 0
    protected var myAlignment = TextAlignment.LEFT
    protected var myPrefix: CharSequence = EMPTY_SEQUENCE
    protected var mySuffix: CharSequence = EMPTY_SEQUENCE
    protected var myLeftPadChar = ' '
    protected var myRightPadChar = ' '
    protected var myColumnSpan = 1

    var alignment = myAlignment
        set(alignment) = setAlignment(alignment, myWidth)

    var columnSpan: Int get() = myColumnSpan
        set(value) {
            if (value < 1 || value > 1000) {
                throw IllegalArgumentException("columnSpan $value must be in range [1, 1000]")
            }
            myColumnSpan = value
        }

    var width: Int get() = myWidth
        set(width) = setAlignment(myAlignment, width)

    var prefix: CharSequence get() = myPrefix
        set(prefix) = setWrapping(prefix, mySuffix)

    var suffix: CharSequence = mySuffix
        set(suffix) = setWrapping(myPrefix, suffix)

    var leftPadChar = myLeftPadChar
        set(leftPadChar) = setPaddingChars(leftPadChar, myRightPadChar)

    var rightPadChar = myRightPadChar
        set(rightPadChar) = setPaddingChars(myLeftPadChar, rightPadChar)

    var variableChars: CharSequence
        get() = myVariableChars
        set(chars) {
            myVariableChars = SmartReplacedCharSequence(myReplacedChars, chars)
            invalidateResultSequence()
        }

    val fixedLength: Int get() = myVariableChars.length + myPrefix.length + mySuffix.length

    override fun getVersion(): SmartVersion {
        return myVersion
    }

    fun alignmentFrom(variable: SmartVariable<TextColumnAlignment>) {
        alignmentFrom(variable.data, variable.key, variable.index)
    }

    fun alignmentFrom(cache: SmartData, key: SmartValueKey<TextColumnAlignment>, index: Int) {
        if (myColumnSpan == 1) {
            cache.registerListener(key, index, SmartIndexedValueListener<TextColumnAlignment> {
                //                println("${cache.name} got alignment ${it.alignment} width ${it.width}")
                setAlignment(it)
            })
        } else {
            cache.registerListener(key, SmartValueListener<TextColumnAlignment>
            {
                var width = 0
                var alignment = (it[index] ?: key.nullValue).alignment
                for (i in index..index + myColumnSpan - 1) {
                    val textAlign = it[i] ?: continue
                    width += textAlign.width
                }

                //                println("got alignment $alignment width $width")
                setAlignment(alignment, width)
            })
        }
    }

    fun provideLength(variable: SmartVariable<Int>) {
        provideLength(variable.data, variable.key, variable.index)
    }

    fun provideLength(cache: SmartData, key: SmartValueKey<Int>, index: Int) {
        if (myColumnSpan == 1) {
            //                println("providing $key [${index}, ${index + myColumnSpan - 1}] to $width rem $over")
            cache.registerProvider(SmartValueProvider(arrayOf(), arrayOf(key), ValueProvider { it.setValue(key, fixedLength, index) }))
        } else {
            cache.registerProvider(SmartValueProvider(arrayOf(), arrayOf(key), ValueProvider {
                val width = (fixedLength / myColumnSpan).toInt()
                var over = fixedLength - width * myColumnSpan

                //                println("providing $key [${index}, ${index + myColumnSpan - 1}] to $width rem $over")

                for (i in index..index + myColumnSpan - 1) {
                    val value = width + (if (over > 0 ) 1 else 0)
                    it.setValue(key, value, index + i)
                    if (over > 0) over--
                }
            }))
        }
    }

    fun setAlignment(columnAlignment: TextColumnAlignment) {
        setAlignment(columnAlignment.alignment, columnAlignment.width)
    }

    fun setAlignment(alignment: TextAlignment, width: Int) {
        if (myAlignment != alignment || myWidth != width) {
            myAlignment = alignment
            myWidth = width
            invalidateResultSequence()
        }
    }

    fun setPaddingChars(leftPadding: Char, rightPadding: Char) {
        if (leftPadding != myLeftPadChar || rightPadding != myRightPadChar) {
            myLeftPadChar = leftPadding
            myRightPadChar = rightPadding
            invalidateResultSequence()
        }
    }

    fun setWrapping(prefix: CharSequence, suffix: CharSequence) {
        if (myPrefix !== prefix || mySuffix !== suffix) {
            myPrefix = prefix
            mySuffix = suffix
            invalidateResultSequence()
        }
    }

    private fun invalidateResultSequence() {
        myVersion.nextVersion()
        myResultSequence = null
    }

    protected val resultSequence: SmartSegmentedCharSequence
        get() {
            if (myResultSequence == null) updateResultSequence()
            return myResultSequence!!
        }

    protected fun updateResultSequence() {
        myRightPadding = EMPTY_SEQUENCE
        myLeftPadding = EMPTY_SEQUENCE

        val paddingSize = myWidth - fixedLength
        var leftPadding = 0
        var rightPadding = 0

        if (paddingSize > 0) {
            when (myAlignment) {
                TextAlignment.RIGHT -> leftPadding = paddingSize
                TextAlignment.CENTER -> {
                    val even = paddingSize shr 1
                    if (even > 0) leftPadding = even
                    rightPadding = paddingSize - even
                }
                else -> rightPadding = paddingSize
            }
        }

        if (leftPadding > 0) myLeftPadding = RepeatedCharSequence(myLeftPadChar, leftPadding)
        if (rightPadding > 0) myRightPadding = RepeatedCharSequence(myRightPadChar, rightPadding)
        myResultSequence = SmartSegmentedCharSequence(myPrefix, myLeftPadding, myVariableChars, myRightPadding, mySuffix)
    }

    fun leftAlign(width: Int) {
        myAlignment = TextAlignment.LEFT
        myWidth = width
        invalidateResultSequence()
    }

    fun rightAlign(width: Int) {
        myAlignment = TextAlignment.RIGHT
        myWidth = width
        invalidateResultSequence()
    }

    fun centerAlign(width: Int) {
        myAlignment = TextAlignment.CENTER
        myWidth = width
        invalidateResultSequence()
    }

    override val length: Int get() = resultSequence.length
    override fun getCharsImpl(dst: CharArray, dstOffset: Int) = resultSequence.getCharsImpl(dst, dstOffset)
    override fun charAtImpl(index: Int): Char = resultSequence[index]
    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartCharSequence = resultSequence.subSequence(startIndex, endIndex)
    override fun getCharsImpl(): CharArray = resultSequence.getCharsImpl()
    override fun getCachedProxy(): SmartCharSequence = resultSequence.cachedProxy

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        checkIndex(index)
        val location = resultSequence.trackedSourceLocation(index)
        return adjustTrackedSourceLocation(location)
    }

    protected fun adjustTrackedLocation(location: TrackedLocation?): TrackedLocation? {
        if (location != null) {
            val leadPadding = myLeftPadding.length + myPrefix.length
            if (leadPadding > 0) {
                return adjustTrackedSourceLocation(location)
            }
        }
        return location
    }

    protected fun adjustTrackedSourceLocation(location: TrackedLocation): TrackedLocation {
        val leadPadding = myLeftPadding.length + myPrefix.length
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

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? {
        val location = resultSequence.trackedLocation(source, offset)
        return adjustTrackedLocation(location)
    }

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        return null
    }
}
