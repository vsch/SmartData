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

import org.junit.Assert.*
import org.junit.Test
import java.util.function.Supplier

class SmartVersionTest {
    @Test
    fun test_immutable() {
        val v1 = SmartImmutableVersion()

        assertFalse(v1.isStale)
        assertFalse(v1.isMutable)
    }

    @Test
    fun test_volatile() {
        val v1 = SmartVolatileVersion()
        val v2 = SmartVolatileVersion()

        assertFalse(v1.isStale)
        assertFalse(v2.isStale)
        assertTrue(v1.isMutable)

        assertTrue(v1.versionSerial < v2.versionSerial)
        v1.nextVersion()
        assertFalse(v1.versionSerial < v2.versionSerial)

        SmartVersionManager.groupedUpdate(Runnable {
            v1.nextVersion()
            v2.nextVersion()
        })

        assertEquals(v1.versionSerial, v2.versionSerial)

        v1.nextVersion()
        v2.nextVersion()
        assertTrue(v1.versionSerial < v2.versionSerial)

        v1.nextVersion()
        v2.nextVersion()
        assertTrue(v1.versionSerial < v2.versionSerial)

        SmartVersionManager.groupedUpdate(Runnable {
            v1.nextVersion()
            v2.nextVersion()
        })

        assertEquals(v1.versionSerial, v2.versionSerial)

        v1.nextVersion()
        v2.nextVersion()

        SmartVersionManager.groupedCompute(Supplier {
            v1.nextVersion()
            v2.nextVersion()
        })

        assertEquals(v1.versionSerial, v2.versionSerial)
    }

    @Test
    fun test_dependent() {
        var v1 = SmartVolatileVersion()
        var v2 = SmartVolatileVersion()
        var v3 = SmartVolatileVersion()
        var vc = SmartDependentVersion(v3)
        var dv = SmartDependentVersion(listOf(v1, v2, vc))
        var dvn = SmartDependentVersion(listOf(SmartImmutableVersion()))

        assertTrue(dv.isMutable)
        assertFalse(dv.isStale)
        v2.nextVersion()
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)


        assertFalse(dvn.isMutable)
        assertFalse(dvn.isStale)

        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)

        v3.nextVersion()
        assertTrue(vc.isStale)
        assertTrue(dv.isStale)
        assertNotEquals(v3.versionSerial, vc.versionSerial)
        vc.nextVersion()
        assertEquals(v3.versionSerial, vc.versionSerial)
        assertTrue(dv.isStale)

        //        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, v3: ${v3.versionSerial}, vc: ${vc.versionSerial} : dv: ${dv.versionSerial} ${dv.isStale}")
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)

        v3.nextVersion()
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
    }

    @Test
    fun test_snapshot() {
        var v1 = SmartImmutableVersion()
        var v2 = SmartVolatileVersion()
        var vs1 = SmartCacheVersion(v1)
        var vs2 = SmartCacheVersion(v2)

        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, vs1: ${vs1.versionSerial}, vs2: ${vs2.versionSerial}")

        assertFalse(vs1.isMutable)

        assertFalse(vs1.isStale)
        assertEquals(v1.versionSerial, vs1.versionSerial)

        assertFalse(vs2.isMutable)
        assertFalse(vs2.isStale)
        assertEquals(v2.versionSerial, vs2.versionSerial)

        v2.nextVersion()

        assertFalse(vs2.isMutable)
        assertTrue(vs2.isStale)
        assertNotEquals(v2.versionSerial, vs2.versionSerial)
    }

    @Test
    fun test_dependentRunnable() {
        var v1 = SmartImmutableVersion()
        var v2 = SmartVolatileVersion()
        var called = 0

        var vs1 = SmartDependentRunnableVersion(v1, Runnable { called++ })
        assertEquals(1, called)

        var vs2 = SmartDependentRunnableVersion(v2, Runnable { called++ })
        assertEquals(2, called)

        assertFalse(vs1.isMutable)
        assertFalse(vs1.isStale)
        assertEquals(v1.versionSerial, vs1.versionSerial)
        assertEquals(2, called)

        assertTrue(vs2.isMutable)
        assertFalse(vs2.isStale)
        assertEquals(v2.versionSerial, vs2.versionSerial)
        assertEquals(2, called)

        v2.nextVersion()

        assertEquals(2, called)
        assertTrue(vs2.isMutable)

        assertTrue(vs2.isStale)
        vs2.nextVersion()
        assertFalse(vs2.isStale)
        assertEquals(3, called)
        assertEquals(v2.versionSerial, vs2.versionSerial)
        assertEquals(3, called)
    }
}
