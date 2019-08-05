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

import org.junit.Assert.assertEquals
import org.junit.Test

class CharSequenceErrorTest {

    @Test
    fun getErrors() {
        val err = SafeCharSequenceErrorImpl()

        assertEquals(0, err.safeErrors)
        err.addSafeError()
        assertEquals(1, err.safeErrors)
        err.addSafeError()
        assertEquals(2, err.safeErrors)
    }

    @Test
    fun clearErrors() {
        val err = SafeCharSequenceErrorImpl()
        assertEquals(0, err.safeErrors)
        err.addSafeError()
        assertEquals(1, err.safeErrors)
        err.addSafeError()
        assertEquals(2, err.safeErrors)
        err.clearSafeErrors()
        assertEquals(0, err.safeErrors)
        err.addSafeError()
        assertEquals(1, err.safeErrors)
        err.addSafeError()
        assertEquals(2, err.safeErrors)
    }

    @Test
    fun getHadErrors() {
        val err = SafeCharSequenceErrorImpl()
        assertEquals(false, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
    }

    @Test
    fun clearHadErrors() {
        val err = SafeCharSequenceErrorImpl()
        assertEquals(false, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        err.clearHadSafeErrors()
        assertEquals(2, err.safeErrors)
        assertEquals(false, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
    }

    @Test
    fun getHadErrorsAndClear() {
        val err = SafeCharSequenceErrorImpl()
        assertEquals(false, err.hadSafeErrors)
        assertEquals(false, err.hadSafeErrorsAndClear)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(1, err.safeErrors)
        assertEquals(false, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(2, err.safeErrors)
        assertEquals(false, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(3, err.safeErrors)
        assertEquals(false, err.hadSafeErrors)
    }

    @Test
    fun getSafeErrorSnapshot() {
        val err = SafeCharSequenceErrorImpl()
        assertEquals(false, err.hadSafeErrors)
        assertEquals(false, err.hadSafeErrorsAndClear)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(1, err.safeErrors)
        assertEquals(false, err.hadSafeErrors)
        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(2, err.safeErrors)
        assertEquals(false, err.hadSafeErrors)

        var snapshot = err.safeErrorSnapshot

        err.addSafeError()
        assertEquals(true, err.hadSafeErrors)
        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(3, err.safeErrors)
        assertEquals(2, snapshot.safeErrors)

        err.safeErrorSnapshot = snapshot
        assertEquals(2, err.safeErrors)
        assertEquals(false, err.hadSafeErrors)

        err.addSafeError()
        assertEquals(3, err.safeErrors)
        assertEquals(true, err.hadSafeErrors)
        snapshot = err.safeErrorSnapshot
        assertEquals(3, snapshot.safeErrors)

        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(false, err.hadSafeErrors)
        assertEquals(true, snapshot.hadSafeErrors)

        err.safeErrorSnapshot = snapshot
        assertEquals(true, err.hadSafeErrorsAndClear)
        assertEquals(false, err.hadSafeErrors)
        assertEquals(true, snapshot.hadSafeErrors)
        assertEquals(3, err.safeErrors)
    }

}
