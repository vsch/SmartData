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

class SmartMappedCharSequence(chars: SmartCharSequence, mapper: CharSequenceMapper) : SmartTransformedSequence<SmartMappedCharSequence>(chars) {
    internal val myMapper = mapper
    internal val myVersion = myReplacedChars.version

    override fun getVersion(): SmartVersion = myVersion

    override val length: Int get() = myReplacedChars.length

    override fun charAtImpl(index: Int): Char = myMapper.mapChar(myReplacedChars, index)
    override fun getCharsImpl(dst: CharArray, dstOffset: Int) {
        val iMax = length
        for (i in 0..iMax-1) {
            dst[dstOffset+i] = myMapper.mapChar(myReplacedChars, i)
        }
    }

    // disable proxy copying, use mapping, it's fast enough
    override fun getCachedProxy(): SmartCharSequence = this
    override fun toString(): String {
//        val charArr = CharArray(length)
//        getCharsImpl(charArr, 0)
//        return String(charArr)
        return String(chars)
    }

    override fun properSubSequence(startIndex: Int, endIndex: Int): SmartMappedCharSequence = SmartMappedCharSequence(myReplacedChars.subSequence(startIndex, endIndex), myMapper)

    override fun trackedSourceLocation(index: Int): TrackedLocation = myReplacedChars.trackedSourceLocation(index)

    override fun trackedLocation(source: Any?, offset: Int): TrackedLocation? = myReplacedChars.trackedLocation(source, offset)

    override fun getMarkers(id: String?): List<TrackedLocation> = myReplacedChars.getMarkers(id)

    override fun splicedWith(other: CharSequence?): SmartCharSequence? {
        if (other is SmartMappedCharSequence) {
            if (myMapper === other.myMapper) {
                val splicedChars = other.myReplacedChars.splicedWith(myReplacedChars)
                if (splicedChars != null) {
                    return SmartMappedCharSequence(splicedChars, myMapper)
                }
            }
        }
        return null
    }
}
