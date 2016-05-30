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

fun CharSequence.isAllSame(c: Char): Boolean {
    for (i in 0..this.length - 1) {
        if (this[i] != c) return false
    }
    return this.length > 0
}

fun CharSequence.countLeading(vararg c: Char, index: Int? = null, endIndex: Int? = null): Int {
    @Suppress("NAME_SHADOWING")
    var index = index ?: 0
    @Suppress("NAME_SHADOWING")
    var endIndex = endIndex ?: this.length

    if (endIndex > this.length) endIndex = this.length
    if (index < 0) index = 0
    if (index >= endIndex) return 0

    val origIndex = index
    for (i in index..endIndex - 1) {
        if (this[i] !in c) return i - origIndex
    }
    return endIndex - origIndex
}

fun CharSequence.countTrailing(vararg c: Char, index: Int? = null, startIndex: Int? = null): Int {
    @Suppress("NAME_SHADOWING")
    var index = index ?: length - 1
    @Suppress("NAME_SHADOWING")
    var startIndex = startIndex ?: 0

    if (index > length - 1) index = length - 1
    if (startIndex < 0 || index < 0 || index < startIndex) return 0

    val origIndex = index
    for (i in (startIndex..index).reversed()) {
        if (this[i] !in c) return origIndex - i
    }
    return origIndex - startIndex + 1
}

fun CharSequence.countLeadingNot(vararg c: Char, index: Int? = null, endIndex: Int? = null): Int {
    @Suppress("NAME_SHADOWING")
    var index = index ?: 0
    @Suppress("NAME_SHADOWING")
    var endIndex = endIndex ?: this.length

    if (endIndex > this.length) endIndex = this.length
    if (index < 0) index = 0
    if (index >= endIndex) return 0

    val origIndex = index
    for (i in index..endIndex - 1) {
        if (this[i] in c) return i - origIndex
    }
    return endIndex - origIndex
}

fun CharSequence.countTrailingNot(vararg c: Char, index: Int? = null, startIndex: Int? = null): Int {
    @Suppress("NAME_SHADOWING")
    var index = index ?: length - 1
    @Suppress("NAME_SHADOWING")
    var startIndex = startIndex ?: 0

    if (index > length - 1) index = length - 1
    if (startIndex < 0 || index < 0 || index < startIndex) return 0

    val origIndex = index
    for (i in (startIndex..index).reversed()) {
        if (this[i] in c) return origIndex - i
    }
    return origIndex - startIndex + 1
}

fun CharSequence.countLeading(pattern: Regex, index: Int? = null): Int {
    @Suppress("NAME_SHADOWING")
    var index = index ?: 0
    var count = 0
    var chars = subSequence(index, length)
    while (true) {
        val removed = chars.removePrefix(pattern)
        if (removed === chars) return count
        chars = removed
        count++
    }
}

fun CharSequence.countTrailing(pattern: Regex, index: Int? = null): Int {
    @Suppress("NAME_SHADOWING")
    var index = index ?: length - 1
    if (index > length - 1) index = length - 1

    var count = 0
    var chars = subSequence(0, index)
    while (true) {
        val removed = chars.removeSuffix(pattern)
        if (removed === chars) return count
        chars = removed
        count++
    }
}

fun CharSequence.studyLeading(pattern: Regex, index: Int? = null): StudyResult {
    @Suppress("NAME_SHADOWING")
    var index = index ?: 0
    var count = 0
    var chars = subSequence(index, length)
    val instances = ArrayList<String>()
    val totalPrefix = StringBuilder()
    while (true) {
        val removed = chars.removePrefix(pattern)
        if (removed === chars) break
        val prefix = chars.subSequence(0, chars.length - removed.length)
        instances.add(prefix.asString())
        totalPrefix.append(prefix)
        chars = removed
        count++
    }
    return StudyResult(totalPrefix.toString(), count, instances)
}

fun CharSequence.studyTrailing(pattern: Regex, index: Int? = null): StudyResult {
    @Suppress("NAME_SHADOWING")
    var index = index ?: length - 1
    if (index > length - 1) index = length - 1

    var count = 0
    var chars = subSequence(0, index)
    val instances = ArrayList<String>()
    val totalPrefix = StringBuilder()

    while (true) {
        val removed = chars.removeSuffix(pattern)
        if (removed === chars) break
        val prefix = chars.subSequence(removed.length, chars.length)
        instances.add(prefix.asString())
        totalPrefix.append(prefix)
        chars = removed
        count++
    }
    return StudyResult(totalPrefix.toString(), count, instances)
}

fun CharSequence.trimEnd(s: String = " \t", index: Int? = null): CharSequence {
    @Suppress("NAME_SHADOWING")
    var index = index ?: length - 1
    if (index > length - 1) index = length - 1
    for (i in (0..index).reversed()) {
        if (this[i] !in s) return subSequence(0, i + 1)
    }
    return EMPTY_SEQUENCE;
}

fun CharSequence.trimEnd(vararg s: Char, index: Int? = null): CharSequence {
    @Suppress("NAME_SHADOWING")
    var index = index ?: length - 1
    if (index > length - 1) index = length - 1
    for (i in (0..index).reversed()) {
        if (this[i] !in s) return subSequence(0, i + 1)
    }
    return EMPTY_SEQUENCE;
}

fun CharSequence.trimEOL(index: Int? = null): CharSequence {
    return trimEnd('\n', index = index)
}

fun CharSequence.removePrefix(stripPrefix: CharSequence, removePartial: Boolean): CharSequence {
    var pos = 0
    while (pos < stripPrefix.length && pos < length) {
        if (this[pos] != stripPrefix[pos]) {
            break
        }
        pos++
    }

    if (removePartial || pos == stripPrefix.length) {
        return subSequence(pos, length)
    }
    return this
}

fun CharSequence.removePrefix(strip: Regex?): CharSequence {
    val range = strip?.find(this)?.range ?: return this
    if (range.start != 0) return this
    return subSequence(range.endInclusive + 1, length)
}

fun CharSequence.removeSuffix(strip: Regex?): CharSequence {
    var range = strip?.find(this, 0)?.range ?: return this
    while (range.start + range.endInclusive + 1 < length) {
        range = strip?.find(this, range.start + range.endInclusive + 1)?.range ?: return this
    }
    return subSequence(0, range.start)
}

fun CharSequence.splitParts(char: Char, includeDelimiter: Boolean): ArrayList<CharSequence> {
    return wrapParts(char, includeDelimiter, EMPTY_SEQUENCE, EMPTY_SEQUENCE)
}

fun CharSequence.editable(): EditableCharSequence {
    return SmartCharSequenceBase.editableCharSequence(this)
}

fun CharSequence.smart(): SmartCharSequence {
    return SmartCharSequenceBase.smart(this)
}

fun CharSequence.extractGroups(regex: String): ArrayList<CharSequence>? {
    var matchResult = regex.toRegex().matchEntire(this)
    if (matchResult != null) {
        val segments = ArrayList<CharSequence>(matchResult.groups.size)
        var group = 0
        for (matchGroup in matchResult.groups) {
            if (matchGroup != null && matchGroup.range.start <= matchGroup.range.endInclusive) {
                segments.add(this.subSequence(matchGroup.range.start, matchGroup.range.endInclusive + 1))
            } else {
                segments.add(NULL_SEQUENCE)
            }
            group++
        }
        return segments
    }
    return null
}

fun CharSequence.wrapParts(char: Char, includeDelimiter: Boolean, prefix: CharSequence, suffix: CharSequence): ArrayList<CharSequence> {
    var pos = 0;
    var lastPos = 0;
    val segments = ArrayList<CharSequence>()
    while (lastPos < length) {
        pos = this.indexOf(char, lastPos)
        if (pos < 0) {
            segments.add(this.subSequence(lastPos, this.length))
            break;
        }

        if (prefix.length > 0) segments.add(prefix)
        segments.add(this.subSequence(lastPos, if (includeDelimiter) pos + 1 else pos))
        if (suffix.length > 0) segments.add(suffix)
        lastPos = pos + 1
    }

    return segments
}

fun CharSequence.indexTrailing(index: Int, scanner: (char: Char) -> Int?): Int {
    @Suppress("NAME_SHADOWING")
    var index = index
    if (index > length) index = length
    while (index > 0) {
        index--
        val scanned = scanner(this[index])
        if (scanned != null) return index + scanned;
    }
    return 0
}

fun CharSequence.indexLeading(index: Int, scanner: (char: Char) -> Int?): Int {
    @Suppress("NAME_SHADOWING")
    if (index >= 0) {
        var index = index
        while (index < length) {
            val scanned = scanner(this[index])
            if (scanned != null) return index + scanned;
            index++
        }
    }
    return length
}

fun CharSequence.asString(): String {
    return StringBuilder().append(this).toString()
}

fun CharSequence.asSmart(): SmartCharSequence {
    return SmartCharSequenceBase.smart(this)
}

fun String.asSmart(): SmartCharSequence {
    return SmartCharArraySequence(this)
}

fun CharSequence.asEditable(): EditableCharSequence {
    return SmartCharSequenceBase.editableCharSequence(this)
}

fun SmartCharSequence.trimStart(): SmartCharSequence {
    return trimStart(' ', '\t', '\n')
}

fun SmartCharSequence.trimStart(vararg chars: Char): SmartCharSequence {
    val leading = countLeading(*chars)
    return if (leading == 0) this else if (leading == length) EMPTY_SEQUENCE else subSequence(leading, length)
}

fun SmartCharSequence.trimEnd(): SmartCharSequence {
    return trimEnd(' ', '\t', '\n')
}

fun SmartCharSequence.trimEnd(vararg chars: Char): SmartCharSequence {
    val trailing = countTrailing(*chars)
    return if (trailing == 0) this else if (trailing == length) EMPTY_SEQUENCE else subSequence(0, length - trailing)
}

fun SmartCharSequence.trim(): SmartCharSequence {
    return trim(' ', '\t', '\n')
}

fun SmartCharSequence.trim(vararg chars: Char): SmartCharSequence {
    val leading = countLeading(*chars)
    if (leading == length) return EMPTY_SEQUENCE

    val trailing = countTrailing(*chars)
    return if (trailing == 0) {
        if (leading == 0) this
        else subSequence(leading, length)
    } else subSequence(leading, length - trailing)
}

object LowerCaseMapper : CharSequenceMapper {
    override fun mapChar(charSequence: CharSequence, index: Int): Char {
        return Character.toLowerCase(charSequence[index])
    }

    override fun mapChar(charSequence: CharArray, offset: Int, index: Int): Char {
        return Character.toLowerCase(charSequence[offset + index])
    }
}

object UpperCaseMapper : CharSequenceMapper {
    override fun mapChar(charSequence: CharSequence, index: Int): Char {
        return Character.toUpperCase(charSequence[index])
    }

    override fun mapChar(charSequence: CharArray, offset: Int, index: Int): Char {
        return Character.toUpperCase(charSequence[offset + index])
    }
}

object SpaceToNonBreakSpaceMapper : CharSequenceMapper {
    override fun mapChar(charSequence: CharSequence, index: Int): Char {
        val c = charSequence[index]
        return if (c == ' ') '\u00A0' else c
    }

    override fun mapChar(charSequence: CharArray, offset: Int, index: Int): Char {
        val c = charSequence[offset + index]
        return if (c == ' ') '\u00A0' else c
    }
}

object NonBreakSpaceToSpaceMapper : CharSequenceMapper {
    override fun mapChar(charSequence: CharSequence, index: Int): Char {
        val c = charSequence[index]
        return if (c == '\u00A0') ' ' else c
    }

    override fun mapChar(charSequence: CharArray, offset: Int, index: Int): Char {
        val c = charSequence[offset + index]
        return if (c == '\u00A0') ' ' else c
    }
}

