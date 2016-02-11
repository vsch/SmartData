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

class CharSequenceInfo(val chars: CharSequence, val index: Int) {
    val before: CharSequence get() = chars.subSequence(0, index)
    val after: CharSequence get() = chars.subSequence(index, chars.length)

    val startOfLine: Int by lazy {
        chars.indexTrailing(index) {
            when (it) {
                '\n' -> 1
                else -> null
            }
        }
    }

    val endOfLine: Int by lazy {
        chars.indexLeading(index) {
            when (it) {
                '\n' -> 1
                else -> null
            }
        }
    }

    val lastNonBlank: Int by lazy {
        chars.indexTrailing(endOfLine) {
            when (it) {
                ' ', '\t' -> null
                else -> 0
            }
        }
    }

    val firstNonBlank: Int by lazy {
        chars.indexLeading(startOfLine) {
            when (it) {
                ' ', '\t' -> null
                else -> 0
            }
        }
    }

    val isBlankLine: Boolean get() = isEmptyLine || firstNonBlank == endOfLine - 1
    val isEmptyLine: Boolean get() = startOfLine + 1 == endOfLine
    val indent: Int get() = firstNonBlank - startOfLine

    val column: Int get() = columnOf(index)

    fun columnOf(index: Int): Int = index - startOfLine
}
