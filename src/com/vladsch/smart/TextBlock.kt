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

open class TextBlock(val chars: SmartCharSequence, expandTabs: Int, val parent: TextBlock?) : SmartCharSequence {
    // stored as array of char sequences and if they all come from the same TrackingCharArraySequence they have start/end into original char[]
    // giving us original location tracking for free
    var lines: SmartSegmentedCharSequence      // char sequence of adjusted block text

    constructor(chars: SmartCharSequence, expandTabs: Int) : this(chars, expandTabs, null)

    constructor(chars: CharSequence, expandTabs: Int) : this(SmartCharSequenceWrapper(chars), expandTabs, null)

    init {
        if (chars is SmartSegmentedCharSequence) {
            lines = if (expandTabs > 0) chars.expandTabs(expandTabs).segmented() else chars
        } else {
            lines = (if (expandTabs > 0) chars.expandTabs(expandTabs).segmented() else chars).splitPartsSegmented('\n', true).segmented();
        }
    }

    fun recomputeLines() {
        lines = lines.splitPartsSegmented('\n', true)
    }

    override val length: Int get() = lines.length

    override fun get(index: Int): Char {
        return lines[index]
    }

    fun childBlock(start: Int, end: Int): TextBlock = childBlock(0, lines.length, EMPTY_SEQUENCE)

    fun childBlock(stripPrefix: CharSequence): TextBlock = childBlock(0, lines.length, stripPrefix)

    fun childBlock(start: Int, end: Int, stripPrefix: CharSequence): TextBlock {
        if (stripPrefix.isEmpty()) {
            return TextBlock(lines.subSequence(start, end), 0, this)
        } else {
            return childBlock(start, end) { it.removePrefix(stripPrefix, true) as SmartCharSequence }
        }
    }

    fun getOffset(index:Int) : Int = trackedSourceLocation(index).offset

    fun getIndex(offset:Int) : Int = trackedLocation(null, offset)?.index ?: -1

    fun childBlock(start: Int, end: Int, lineProcessor: (line: SmartCharSequence) -> SmartCharSequence): TextBlock {
        val lineSequences = ArrayList<SmartCharSequence>()
        var subLines = if (start == 0 && end == lines.length) lines else lines.subSequence(start, end).splitPartsSegmented('\n', true).segmented()

        for (line in subLines.segments) {
            lineSequences.add(lineProcessor(line))
        }

        return TextBlock(SmartSegmentedCharSequence(lineSequences), 0, this)
    }

    override fun trackedSourceLocation(index: Int): TrackedLocation = lines.trackedSourceLocation(index)

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? = lines.trackedLocation(source, offset)

    override fun subSequence(startIndex: Int, endIndex: Int): SmartCharSequence = lines.subSequence(startIndex, endIndex)

    override fun splicedWith(other: CharSequence?): SmartCharSequence? = lines.splicedWith(other)

    override fun flattened(sequences: ArrayList<SmartCharSequence>) = lines.flattened(sequences)

    override fun equivalent(other: CharSequence): Boolean = lines.equivalent(other)

    override fun reversed(): SmartCharSequence = lines.reversed()

    override fun insert(charSequence: CharSequence, startIndex: Int): SmartCharSequence = lines.insert(charSequence, startIndex)

    override fun delete(startIndex: Int, endIndex: Int): SmartCharSequence = lines.delete(startIndex, endIndex)

    override fun replace(charSequence: CharSequence, startIndex: Int, endIndex: Int): SmartCharSequence = lines.replace(charSequence, startIndex, endIndex)

    override fun expandTabs(tabSize: Int): SmartCharSequence = lines.expandTabs(tabSize)

    override fun wrapParts(delimiter: Char, includeDelimiter: Boolean, prefix: CharSequence, suffix: CharSequence): SmartCharSequence = lines.wrapParts(delimiter, includeDelimiter, prefix, suffix)

    override fun lowercase(): SmartCharSequence = lines.lowercase()

    override fun uppercase(): SmartCharSequence = lines.uppercase()

    override fun editable(): EditableCharSequence = lines.editable()

    override fun segmented(): SmartSegmentedCharSequence = lines

    override fun variable(): SmartVariableCharSequence = lines.variable()

    override fun append(vararg others: CharSequence): SmartCharSequence = lines.append(*others)

    override fun getChars(): CharArray = lines.chars

    override fun getChars(dst: CharArray, dstOffset: Int) = lines.getChars(dst, dstOffset)

    override fun getVersion(): SmartVersion = lines.version

    override fun getCachedProxy(): SmartCharSequence = lines.cachedProxy

    override fun getMarkers(id: String?): List<TrackedLocation> = lines.getMarkers(id)

    override fun getOriginal(): SmartCharSequence = lines.original
    override fun getContents(): SmartCharSequence = lines

    override fun mapped(mapper: CharSequenceMapper): SmartCharSequence = lines.mapped(mapper)

    override fun splitParts(delimiter: Char, includeDelimiter: Boolean): List<SmartCharSequence> = lines.splitParts(delimiter, includeDelimiter)

    override fun splitPartsSegmented(delimiter: Char, includeDelimiter: Boolean): SmartSegmentedCharSequence = lines.splitPartsSegmented(delimiter, includeDelimiter)

    override fun extractGroups(regex: String): List<SmartCharSequence>? = lines.extractGroups(regex)

    override fun extractGroupsSegmented(regex: String): SmartSegmentedCharSequence? = lines.extractGroupsSegmented(regex)
}

