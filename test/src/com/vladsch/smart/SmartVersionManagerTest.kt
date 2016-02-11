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

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SmartVersionManagerTest {
    @Test
    fun test_basicNextVersion() {
        var version1 = SmartVersionManager.nextVersion
        var version2 = SmartVersionManager.nextVersion

        assertNotEquals(version1, version2)
        assertTrue(version1 < version2)
    }

    @Test
    fun test_groupedNextVersion() {
        var version1: Int = 1
        var version2: Int = 2

        SmartVersionManager.groupedUpdate(Runnable {
            version1 = SmartVersionManager.nextVersion
            version2 = SmartVersionManager.nextVersion
        })

        assertEquals(version1, version2)

        SmartVersionManager.groupedUpdate(Runnable {
            version1 = SmartVersionManager.nextVersion
        })

        SmartVersionManager.groupedUpdate(Runnable {
            version2 = SmartVersionManager.nextVersion
        })

        assertNotEquals(version1, version2)
    }

}
