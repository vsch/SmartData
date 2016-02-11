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

class SmartCharSequenceMarker(val id: String, val selectedSequence: SmartCharSequence) : SmartTransformedSequence<SmartCharSequenceMarker>(selectedSequence) {
    constructor(id: String) : this(id, EMPTY_SEQUENCE)

    protected val myVersion: SmartVersion = selectedSequence.version

    override fun getVersion(): SmartVersion = myVersion

    override fun getMarkers(id: String?): List<TrackedLocation> = myReplacedChars.getMarkers(id)

    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartCharSequenceMarker = SmartCharSequenceMarker(id, myReplacedChars.subSequence(startIndex, endIndex))

    override fun charAtImpl(index: Int): Char = myReplacedChars[index]

    override fun getCharsImpl(dst: CharArray, dstOffset: Int) = myReplacedChars.getChars(dst, dstOffset)

    override val length: Int get() = myReplacedChars.length

    override fun getCachedProxy(): SmartCharSequence = myReplacedChars.cachedProxy

    override fun trackedSourceLocation(index: Int): TrackedLocation = myReplacedChars.trackedSourceLocation(index)

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? = myReplacedChars.trackedLocation(source, offset)

    override fun flattened(sequences: ArrayList<SmartCharSequence>) {
        sequences.add(this)
    }

    override fun equivalent(other: CharSequence): Boolean = myReplacedChars.equivalent(other)

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        val spliced = myReplacedChars.splicedWith(other)
        return if (spliced == null) null else SmartCharSequenceMarker(id, spliced)
    }
}
