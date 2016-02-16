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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartDataScopeManagerTest {
    val manager = SmartDataScopeManager

    @Test
    fun test_DataKey_registerVolatileBasic() {
        val INDENT1 = SmartVolatileDataKey("INDENT", 0)

        assertFalse(manager.dependentKeys.containsKey(INDENT1))
    }

    @Test
    fun test_DataKey_registerParentComputedBasic() {
        val INDENT2 = SmartParentComputedDataKey("INDENT2", 0, { it + 4 })

        assertFalse(manager.dependentKeys.containsKey(INDENT2))
    }

    @Test
    fun test_DataKey_resolveDependenciesBasic() {
        val INDENT = SmartParentComputedDataKey("INDENT", 0, { it + 4 })
        val MAX_INDENT = SmartAggregatedScopesDataKey("MAX_INDENT", 0, INDENT, setOf(SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), { it.max() ?: 0 })
        val ADD_INDENT = SmartTransformedDataKey("ADD_INDENT", 0, MAX_INDENT, SmartScopes.SELF, { it + 4 })

        assertTrue(manager.dependentKeys.containsKey(INDENT))
        assertTrue(manager.dependentKeys.containsKey(MAX_INDENT))
        assertFalse(manager.dependentKeys.containsKey(ADD_INDENT))

        manager.resolveDependencies()

        assertEquals(0, manager.keyComputeLevel[INDENT])
        assertEquals(1, manager.keyComputeLevel[MAX_INDENT])
        assertEquals(2, manager.keyComputeLevel[ADD_INDENT])
    }
}
