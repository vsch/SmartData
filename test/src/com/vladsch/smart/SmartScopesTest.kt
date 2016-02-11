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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartScopesTest {
    @Test
    fun test_SmartScopes_isAncestorsSet() {
        assertFalse(SmartScopes.Companion.isAncestorsSet(SmartScopes.SELF.flags))
        assertTrue(SmartScopes.Companion.isAncestorsSet(SmartScopes.PARENT.flags))
        assertTrue(SmartScopes.Companion.isAncestorsSet(SmartScopes.ANCESTORS.flags))
        assertFalse(SmartScopes.Companion.isAncestorsSet(SmartScopes.CHILDREN.flags))
        assertFalse(SmartScopes.Companion.isAncestorsSet(SmartScopes.DESCENDANTS.flags))
        assertFalse(SmartScopes.Companion.isAncestorsSet(SmartScopes.INDICES.flags))

        assertTrue(SmartScopes.Companion.isAncestorsSet(setOf(SmartScopes.PARENT, SmartScopes.ANCESTORS)))
        assertTrue(SmartScopes.Companion.isAncestorsSet(setOf(SmartScopes.PARENT, SmartScopes.ANCESTORS, SmartScopes.SELF)))
    }

    @Test
    fun test_SmartScopes_isDescendantsSet() {
        assertFalse(SmartScopes.Companion.isDescendantSet(SmartScopes.SELF.flags))
        assertFalse(SmartScopes.Companion.isDescendantSet(SmartScopes.PARENT.flags))
        assertFalse(SmartScopes.Companion.isDescendantSet(SmartScopes.ANCESTORS.flags))
        assertTrue(SmartScopes.Companion.isDescendantSet(SmartScopes.CHILDREN.flags))
        assertTrue(SmartScopes.Companion.isDescendantSet(SmartScopes.DESCENDANTS.flags))
        assertFalse(SmartScopes.Companion.isDescendantSet(SmartScopes.INDICES.flags))

        assertTrue(SmartScopes.Companion.isDescendantSet(setOf(SmartScopes.CHILDREN, SmartScopes.DESCENDANTS)))
        assertTrue(SmartScopes.Companion.isDescendantSet(setOf(SmartScopes.CHILDREN, SmartScopes.DESCENDANTS, SmartScopes.SELF)))
    }

    @Test
    fun test_SmartScopes_isValidSet() {
        assertTrue(SmartScopes.Companion.isValidSet(SmartScopes.SELF.flags))
        assertTrue(SmartScopes.Companion.isValidSet(SmartScopes.PARENT.flags))
        assertTrue(SmartScopes.Companion.isValidSet(SmartScopes.ANCESTORS.flags))
        assertTrue(SmartScopes.Companion.isValidSet(SmartScopes.CHILDREN.flags))
        assertTrue(SmartScopes.Companion.isValidSet(SmartScopes.DESCENDANTS.flags))
        assertFalse(SmartScopes.Companion.isValidSet(SmartScopes.INDICES.flags))

        assertTrue(SmartScopes.Companion.isValidSet(setOf(SmartScopes.PARENT, SmartScopes.ANCESTORS)))
        assertTrue(SmartScopes.Companion.isValidSet(setOf(SmartScopes.PARENT, SmartScopes.ANCESTORS, SmartScopes.SELF)))
        assertTrue(SmartScopes.Companion.isValidSet(setOf(SmartScopes.CHILDREN, SmartScopes.DESCENDANTS)))
        assertTrue(SmartScopes.Companion.isValidSet(setOf(SmartScopes.CHILDREN, SmartScopes.DESCENDANTS, SmartScopes.SELF)))

        assertFalse(SmartScopes.Companion.isValidSet(setOf(SmartScopes.CHILDREN, SmartScopes.PARENT, SmartScopes.ANCESTORS, SmartScopes.SELF)))
        assertFalse(SmartScopes.Companion.isValidSet(setOf(SmartScopes.DESCENDANTS, SmartScopes.PARENT, SmartScopes.ANCESTORS, SmartScopes.SELF)))
        assertFalse(SmartScopes.Companion.isValidSet(setOf(SmartScopes.PARENT, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS, SmartScopes.SELF)))
        assertFalse(SmartScopes.Companion.isValidSet(setOf(SmartScopes.ANCESTORS, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS, SmartScopes.SELF)))
    }

}
