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

abstract class SmartCharSequenceBase<T : SmartCharSequence> : SmartCharSequence {
    protected var myCachedProxy: SmartCharSequence? = null

    /*
     * cached proxy should return original sequence for editing purposes
     */
    override fun getOriginal(): SmartCharSequenceBase<T> = this

    override fun getContents(): SmartCharSequenceBase<T> = this
    override fun getContents(startIndex: Int, endIndex: Int): SmartCharSequence = subSequence(startIndex, endIndex)

    /*
             *  raw access, never via proxy or in proxy via original
             */
    internal abstract fun properSubSequence(startIndex: Int, endIndex: Int): T

    internal abstract fun charAtImpl(index: Int): Char
    internal abstract fun getCharsImpl(dst: CharArray, dstOffset: Int)
    //    internal fun getCharsImpl(dst: CharArray, dstOffset: Int) {
    //        val iMax = length
    //        for (i in 0..iMax-1) {
    //            dst[dstOffset+i] = charAtImpl(i)
    //        }
    //    }

    internal open fun getCharsImpl(): CharArray {
        val iMax = length
        val chars = CharArray(iMax)
        getCharsImpl(chars, 0)
        return chars
    }

    /*
     *  use proxy if fresh otherwise raw access
     */
    open val freshProxyOrNull: SmartCharSequence? get() {
        var cachedProxy = myCachedProxy
        if (cachedProxy == null || cachedProxy.version.isStale) return null
        return cachedProxy
    }

    override fun get(index: Int): Char {
        return freshProxyOrNull?.get(index) ?: charAtImpl(index)
    }

    override fun subSequence(startIndex: Int, endIndex: Int): T {
        checkBounds(startIndex, endIndex)
        if (startIndex == 0 && endIndex == length) return original.contents as T
        val subSequence = original.properSubSequence(startIndex, endIndex)
        val cachedProxy = freshProxyOrNull
        if (subSequence is SmartCharSequenceBase<*> && cachedProxy is SmartCharSequenceBase<*>) {
            // grab a copy of the cached proxy if it is not stale
            subSequence.myCachedProxy = cachedProxy.properSubSequence(startIndex, endIndex)
        }
        assert(subSequence === subSequence.contents)
        return subSequence
    }

    override fun getChars(): CharArray {
        return freshProxyOrNull?.chars ?: getCharsImpl()
    }

    override fun getChars(dst: CharArray, dstOffset: Int) {
        freshProxyOrNull?.getChars(dst, dstOffset) ?: getCharsImpl(dst, dstOffset)
    }

    override fun equivalent(other: CharSequence): Boolean {
        if (this === other) return true

        if (length != other.length) return false

        val ourProxy: CharSequence = freshProxyOrNull ?: this
        val otherProxy: CharSequence = if (other is SmartCharSequenceBase<*>) other.freshProxyOrNull ?: other else other

        val iMax = length
        for (i in 0..iMax - 1) {
            if (ourProxy[i] != otherProxy[i]) return false
        }

        return true
    }

    /*
     * full sequence scanning, proxy refreshing functions
     */
    override fun getCachedProxy(): SmartCharSequence {
        var cachedProxy = freshProxyOrNull
        if (cachedProxy == null) {
            cachedProxy = SmartCharArraySequence(this, getCharsImpl())
            myCachedProxy = cachedProxy
        }
        return cachedProxy
    }

    override fun extractGroupsSegmented(regex: String): SmartSegmentedCharSequence? {
        val segments = extractGroups(regex) ?: return null
        return SmartSegmentedCharSequence(segments)
    }

    override fun extractGroups(regex: String): List<SmartCharSequence>? {
        var matchResult = regex.toRegex().matchEntire(cachedProxy)
        if (matchResult != null) {
            val segments = ArrayList<SmartCharSequence>(matchResult.groups.size)
            var group = 0
            for (matchGroup in matchResult.groups) {
                if (matchGroup != null && matchGroup.range.start <= matchGroup.range.endInclusive) {
                    segments.add(original.subSequence(matchGroup.range.start, matchGroup.range.endInclusive + 1))
                } else {
                    segments.add(NULL_SEQUENCE)
                }
                group++
            }
            return segments
        }
        return null
    }

    override fun splitParts(delimiter: Char, includeDelimiter: Boolean): List<SmartCharSequence> {
        return (cachedProxy as CharSequence).splitParts(delimiter, includeDelimiter) as List<SmartCharSequence>
    }

    override fun splitPartsSegmented(delimiter: Char, includeDelimiter: Boolean): SmartSegmentedCharSequence {
        return segmentedFlat(splitParts(delimiter, includeDelimiter))
    }

    override fun wrapParts(delimiter: Char, includeDelimiter: Boolean, prefix: CharSequence, suffix: CharSequence): SmartCharSequence {
        return SmartSegmentedCharSequence(cachedProxy.wrapParts(delimiter, includeDelimiter, prefix, suffix))
    }

    override fun expandTabs(tabSize: Int): SmartCharSequence {
        var lastPos = 0
        val parts = ArrayList<SmartCharSequence>()
        val length = length
        var col = 0
        val tabExpansion = RepeatedCharSequence(' ', tabSize)
        val proxy = cachedProxy.chars

        for (i in 0..length - 1) {
            val c = proxy[i]
            if (c == '\t') {
                parts.add(subSequence(lastPos, i))
                parts.add(SmartReplacedCharSequence(subSequence(i, i + 1), tabExpansion.subSequence(0, tabSize - col % tabSize)))
                lastPos = i + 1
            } else if (c == '\n') {
                col = 0
            } else {
                col++
            }
        }

        if (lastPos < length) parts.add(this.subSequence(lastPos, length))
        return SmartSegmentedCharSequence(parts)
    }

    // IMPORTANT: if overriding getCachedProxy or freshProxyOrNull to return `this` then need to override this method otherwise will get infinite recursion
    override fun toString(): String {
        return cachedProxy.toString()
    }

    /*
     * convenience type change functions
     */

    override fun mapped(mapper: CharSequenceMapper): SmartMappedCharSequence {
        val base = original
        return if (base is SmartMappedCharSequence && base.myMapper === mapper) base else SmartMappedCharSequence(base, mapper)
    }

    override fun lowercase(): SmartCharSequence {
        return mapped(LowerCaseMapper)
    }

    override fun uppercase(): SmartCharSequence {
        return mapped(UpperCaseMapper)
    }

    override fun reversed(): SmartCharSequence {
        val base = original
        return if (base is SmartReversedCharSequence) base else SmartReversedCharSequence(base)
    }

    override fun editable(): EditableCharSequence {
        return EditableCharSequence(this)
    }

    override fun segmented(): SmartSegmentedCharSequence {
        val base = original
        return if (base is SmartSegmentedCharSequence) base else SmartSegmentedCharSequence(base)
    }

    override fun variable(): SmartVariableCharSequence {
        val base = original
        return if (base is SmartVariableCharSequence) base else SmartVariableCharSequence(base)
    }

    /*
     * convenience implementation functions
     */

    override fun insert(charSequence: CharSequence, startIndex: Int): SmartCharSequence {
        return replace(charSequence, startIndex, startIndex)
    }

    override fun delete(startIndex: Int, endIndex: Int): SmartCharSequence {
        return replace(EMPTY_SEQUENCE, startIndex, endIndex)
    }

    override fun append(vararg others: CharSequence): SmartCharSequence {
        val segments = ArrayList<SmartCharSequence>(others.size + 1)

        if (this is SmartSegmentedCharSequence) segments.addAll(this.segments)
        else flattened(segments)

        for (charSequence in others) {
            segments.add(smart(charSequence))
        }

        return SmartSegmentedCharSequence(segments)
    }

    override fun appendOptimized(vararg others: CharSequence): SmartCharSequence {
        val segments = ArrayList<SmartCharSequence>(others.size + 1)

        flattened(segments)

        for (charSequence in others) {
            if (!charSequence.isEmpty() || charSequence is SmartCharSequence && charSequence.version.isMutable) {
                segments.add(smart(charSequence))
            }
        }

        return SmartSegmentedCharSequence(spliceSequences(segments))
    }

    override fun replace(charSequence: CharSequence, startIndex: Int, endIndex: Int): SmartCharSequence {
        if (startIndex < 0 || startIndex > length) {
            throw IndexOutOfBoundsException()
        }

        val canEliminate = charSequence.length == 0 && (charSequence !is SmartCharSequence || !charSequence.version.isMutable)

        if (startIndex == endIndex) {
            // pure insert
            if (canEliminate) {
                return original.contents
            }

            if (startIndex == 0) {
                return SmartSegmentedCharSequence(charSequence, original.contents)
            } else if (startIndex == length) {
                return segmentedFlat(original.contents, smart(charSequence))
            } else {
                return segmentedFlat(subSequence(0, startIndex), smart(charSequence), subSequence(startIndex, length))
            }
        } else if (canEliminate) {
            // pure delete
            if (startIndex == 0) {
                if (endIndex == length) {
                    return EMPTY_SEQUENCE
                } else {
                    return subSequence(endIndex, length)
                }
            } else if (endIndex == length) {
                return subSequence(0, startIndex)
            } else {
                return segmentedFlat(subSequence(0, startIndex), subSequence(endIndex, length))
            }
        } else {
            // a mix of delete and insert
            if (startIndex == 0) {
                if (endIndex == length) {
                    return smart(charSequence)
                } else {
                    return segmentedFlat(smart(charSequence), subSequence(endIndex, length))
                }
            } else if (startIndex == length) {
                return segmentedFlat(this, smart(charSequence))
            } else {
                return segmentedFlat(subSequence(0, startIndex), smart(charSequence), subSequence(endIndex, length))
            }
        }
    }

    protected fun checkBounds(start: Int, end: Int) {
        if (start < 0 || start > end || end > length) {
            throw IndexOutOfBoundsException("subSequence($start, $end) is not within range [0, $length)")
        }
    }

    protected fun checkIndex(index: Int) {
        if (index < 0 || index >= length) {
            throw IndexOutOfBoundsException("index $index is not within range [0, $length)")
        }
    }

    override fun flattened(sequences: ArrayList<SmartCharSequence>) {
        sequences.add(original.contents)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CharSequence) return false

        if (other !is SmartCharSequenceBase<*>) {
            return equivalent(other)
        }

        if (!cachedProxy.equivalent(other.cachedProxy)) return false
        return true
    }

    override fun hashCode(): Int {
        return myCachedProxy?.hashCode() ?: 0
    }

    companion object {
        @JvmStatic fun segmentedFlat(vararg others: SmartCharSequence): SmartSegmentedCharSequence {
            if (others.size == 0)
                return EMPTY_SEGMENTED_SEQUENCE
            else {
                val segments = ArrayList<SmartCharSequence>(others.size)
                for (other in others) {
                    other.flattened(segments)
                }
                return SmartSegmentedCharSequence(segments)
            }
        }

        @JvmStatic fun segmentedFlat(others: Iterable<SmartCharSequence>): SmartSegmentedCharSequence {
            val segments = ArrayList<SmartCharSequence>()
            for (other in others) {
                other.flattened(segments)
            }
            return if (segments.isEmpty()) EMPTY_SEGMENTED_SEQUENCE else SmartSegmentedCharSequence(segments)
        }

        @JvmStatic fun smart(other: CharSequence): SmartCharSequence {
            return if (other is SmartCharSequenceContainer) other.contents else SmartCharSequenceWrapper(other, 0, other.length)
        }

        @JvmStatic fun smartList(others: Collection<CharSequence>): List<SmartCharSequence> {
            val smartCharSequences = ArrayList<SmartCharSequence>(others.size)
            for (other in others) {
                smartCharSequences.add(smart(other))
            }
            return smartCharSequences
        }

        @JvmStatic fun smart(vararg others: SmartCharSequence): SmartCharSequence {
            val list = spliceSequences(*others)
            return if (list.size == 1) list[0] else SmartSegmentedCharSequence(list)
        }

        @JvmStatic fun smart(vararg others: CharSequence): SmartCharSequence {
            val list = spliceSequences(others.asList())
            return if (list.size == 1) list[0] else SmartSegmentedCharSequence(list)
        }

        @JvmStatic fun smart(others: List<CharSequence>): SmartCharSequence {
            return if (others.size == 1) others[0].asSmart() else SmartSegmentedCharSequence(others)
        }

        @Suppress("NAME_SHADOWING")
        @JvmStatic fun trackingSequences(charSequences: List<SmartCharSequence>, startIndex: Int, startOffset: Int, endIndex: Int, endOffset: Int): List<SmartCharSequence> {
            var endIndex = endIndex
            var endOffset = endOffset
            // adjust parameters if endIndex is one past all sequences
            if (endIndex == charSequences.size) {
                endIndex = charSequences.size - 1
                endOffset = charSequences[endIndex].length
            }

            if (startIndex == endIndex) {
                // just one
                return listOf(charSequences[startIndex].subSequence(startOffset, endOffset))
            } else {
                // partial of first and last and in between
                val smartCharSequences = ArrayList<SmartCharSequence>(endIndex - startIndex + 1)

                smartCharSequences.add(charSequences[startIndex].subSequence(startOffset, charSequences[startIndex].length))
                if (endIndex - startIndex > 1) smartCharSequences.addAll(charSequences.subList(startIndex + 1, endIndex - 1))
                smartCharSequences.add(charSequences[endIndex].subSequence(0, endOffset))
                return smartCharSequences
            }
        }

        @JvmStatic fun spliceSequences(vararg charSequences: SmartCharSequence): ArrayList<SmartCharSequence> {
            if (charSequences.size == 0) {
                return ArrayList()
            }

            val smartCharSequences = ArrayList<SmartCharSequence>()

            var charSequence = charSequences[0].contents
            val iMax = charSequences.size
            for (i in 1..iMax - 1) {
                val other = charSequences[i]
                val merged = charSequence.splicedWith(other.contents)
                if (merged == null) {
                    smartCharSequences.add(charSequence)
                    charSequence = other.contents
                } else {
                    charSequence = merged
                }
            }

            smartCharSequences.add(charSequence)
            return smartCharSequences
        }

        @JvmStatic fun spliceSequences(charSequences: Collection<CharSequence>): ArrayList<SmartCharSequence> {
            if (charSequences.size == 0) {
                return ArrayList()
            }

            val smartCharSequences = ArrayList<SmartCharSequence>()

            var charSequence: SmartCharSequence? = null
            for (other in charSequences) {
                if (other.isEmpty() && !(other is SmartCharSequence && other.version.isMutable)) continue

                if (charSequence == null) {
                    charSequence = smartContents(other)
                } else {
                    val merged = charSequence.splicedWith(smartContents(other))
                    if (merged == null) {
                        smartCharSequences.add(charSequence)
                        charSequence = smartContents(other)
                    } else {
                        charSequence = merged
                    }
                }
            }

            if (charSequence != null) smartCharSequences.add(charSequence)
            return smartCharSequences
        }

        @JvmStatic fun smartContents(charSequence: CharSequence): SmartCharSequence {
            if (charSequence !is SmartCharSequenceContainer) return SmartCharSequenceWrapper(charSequence)
            return charSequence.contents
        }

        @JvmStatic fun editableCharSequence(charSequence: CharSequence): EditableCharSequence {
            return if (charSequence is EditableCharSequence) charSequence else EditableCharSequence(charSequence)
        }
    }
}
