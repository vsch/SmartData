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

class EditableCharSequence(chars: CharSequence) : SmartCharSequence {
    constructor() : this(EMPTY_SEGMENTED_SEQUENCE)

    private var myVariableChars: SmartSegmentedCharSequence = SmartCharSequenceBase.segmentedFlat(SmartCharSequenceBase.smart(chars))
    private var myVersion = SmartVolatileVersion()

    private var myChars: SmartCharSequence
        get() = myVariableChars
        private set(value) {
            myVariableChars = value.segmented()
            myVersion.nextVersion()
        }

    override fun addStats(stats: SmartCharSequence.Stats) {
        myChars.addStats(stats)
        stats.nesting++
    }

    override val length: Int get() = myChars.length

    override fun get(index: Int): Char = myChars[index]

    operator fun get(range: IntRange): SmartCharSequence = myChars.subSequence(range.start, range.endInclusive + 1)

    operator fun set(index: Int, c: Char) {
        myChars = myChars.replace(RepeatedCharSequence(c), index, index + 1)
    }

    operator fun set(index: Int, chars: CharSequence) {
        myChars = myChars.replace(chars, index, index + 1)
    }

    operator fun set(range: IntRange, c: Char) {
        myChars = myChars.replace(RepeatedCharSequence(c), range.start, range.endInclusive + 1)
    }

    operator fun set(range: IntRange, chars: CharSequence) {
        myChars = myChars.replace(chars, range.start, range.endInclusive + 1)
    }

    override fun getVersion(): SmartVersion = myVersion

    override fun subSequence(startIndex: Int, endIndex: Int): SmartCharSequence = myChars.subSequence(startIndex, endIndex)

    override fun getCachedProxy(): SmartCharSequence = myChars.cachedProxy

    override fun variable(): SmartVariableCharSequence = myChars.variable()

    override fun toString(): String {
        return myChars.toString()
    }

    fun flatten(): EditableCharSequence {
        val smartCharSequences = ArrayList<SmartCharSequence>()
        myChars.flattened(smartCharSequences)
        myChars = if (smartCharSequences.size == 1) smartCharSequences[0] else SmartCharSequenceBase.smart(smartCharSequences)
        return this
    }

    override fun wrapParts(delimiter: Char, includeDelimiter: Boolean, prefix: CharSequence, suffix: CharSequence): EditableCharSequence {
        myChars = myChars.wrapParts(delimiter, includeDelimiter, prefix, suffix)
        return this
    }

    fun toLowercase(): EditableCharSequence {
        myChars = myChars.lowercase()
        return this
    }

    fun toUppercase(): EditableCharSequence {
        myChars = myChars.uppercase()
        return this
    }

    override fun insert(charSequence: CharSequence, startIndex: Int): EditableCharSequence {
        this.myChars = this.myChars.insert(charSequence, startIndex)
        return this
    }

    fun clear(): EditableCharSequence {
        myChars = EMPTY_SEQUENCE
        return this
    }

    fun getAndClear(): SmartCharSequence {
        val accumulatedCharSequence = myChars
        myChars = EMPTY_SEQUENCE
        return accumulatedCharSequence
    }

    override fun delete(startIndex: Int, endIndex: Int): EditableCharSequence {
        myChars = myChars.delete(startIndex, endIndex)
        return this
    }

    override fun trackedSourceLocation(index: Int): TrackedLocation {
        return myChars.trackedSourceLocation(index)
    }

    override fun replace(charSequence: CharSequence, startIndex: Int, endIndex: Int): EditableCharSequence {
        this.myChars = this.myChars.replace(charSequence, startIndex, endIndex)
        return this
    }

    fun append(text: String): EditableCharSequence {
        return append(RepeatedCharSequence(text))
    }

    fun append(text: String, count: Int): EditableCharSequence {
        return append(RepeatedCharSequence(text, count))
    }

    fun append(c: Char): EditableCharSequence {
        return append(RepeatedCharSequence(c, 1))
    }

    fun append(c: Char, count: Int): EditableCharSequence {
        return append(RepeatedCharSequence(c, count))
    }

    override fun expandTabs(tabSize: Int): EditableCharSequence {
        myChars = myChars.expandTabs(tabSize)
        return this
    }

    override fun append(vararg others: CharSequence?): EditableCharSequence {
        myChars = myChars.append(*others)
        return this
    }

    override fun appendOptimized(vararg others: CharSequence?): EditableCharSequence {
        myChars = myChars.appendOptimized(*others)
        return this
    }

    override fun segmented(): SmartSegmentedCharSequence {
        return myChars.segmented()
    }

    override fun asEditable(): EditableCharSequence = this

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? = myChars.trackedLocation(source, offset)

    override fun splicedWith(other: CharSequence?): SmartCharSequence? = myChars.splicedWith(other)

    override fun flattened(sequences: ArrayList<SmartCharSequence>) = myChars.flattened(sequences)

    override fun equivalent(other: CharSequence): Boolean = myChars.equivalent(other)

    override fun reversed(): SmartCharSequence = myChars.reversed()

    override fun lowercase(): SmartCharSequence = myChars.lowercase()

    override fun uppercase(): SmartCharSequence = myChars.uppercase()

    override fun getChars(): CharArray = myChars.chars

    override fun getChars(dst: CharArray, dstOffset: Int) = myChars.getChars(dst, dstOffset)

    override fun getMarkers(id: String?): MutableList<TrackedLocation> = myChars.getMarkers(id)

    override fun getOriginal(): SmartCharSequence = myChars.original
    override fun getContents(): SmartCharSequence = myChars.contents
    override fun getContents(startIndex: Int, endIndex: Int): SmartCharSequence = myChars.getContents(startIndex, endIndex)

    override fun mapped(mapper: CharSequenceMapper): SmartCharSequence = myChars.mapped(mapper)

    override fun splitParts(delimiter: Char, includeDelimiter: Boolean): MutableList<SmartCharSequence> = myChars.splitParts(delimiter,includeDelimiter)

    override fun splitPartsSegmented(delimiter: Char, includeDelimiter: Boolean): SmartSegmentedCharSequence = myChars.splitPartsSegmented(delimiter,includeDelimiter)

    override fun extractGroups(regex: String): MutableList<SmartCharSequence>?  = myChars.extractGroups(regex)

    override fun extractGroupsSegmented(regex: String): SmartSegmentedCharSequence?  = myChars.extractGroupsSegmented(regex)

    override fun getSourceLocations(sources: ArrayList<Any>, locations: ArrayList<Range>, sourceLocations: ArrayList<Range>) {
        myChars.getSourceLocations(sources, locations, sourceLocations)
    }
}
