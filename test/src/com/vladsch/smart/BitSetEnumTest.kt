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

import com.vladsch.smart.TestUtils.assertEqualsMessage
import com.vladsch.smart.TestUtils.compareUnorderedLists
import org.junit.Test


class BitSetEnumTest() {
    enum class MyEnum(val flags:Int) {
        MEMBER_1(1),
        MEMBER_2(2),
        MEMBER_3(4),
        MEMBER_4(8);

        fun isIn(flags:Int):Boolean = this in flags
        companion object : BitSetEnum<MyEnum>(MyEnum::class.java, {it.flags}) {
            fun containsSome(flags:Int, vararg enumConstants: MyEnum):Boolean = flags.someIn(*enumConstants)
            fun containsAll(flags:Int, vararg enumConstants: MyEnum):Boolean = flags.allIn(*enumConstants)
        }
    }

    @Test
    fun testTest_asSet() {
        val enumSet = MyEnum.asSet(7)
        val manualEnumSet = setOf(MyEnum.MEMBER_1, MyEnum.MEMBER_2, MyEnum.MEMBER_3)

        compareUnorderedLists("", manualEnumSet.toTypedArray(), enumSet.toTypedArray())
        assertEqualsMessage("", MyEnum.asFlags(enumSet), 7)
    }

    @Test
    fun testTest_asList() {
        val enumSet = MyEnum.asList(7)
        val manualEnumSet = listOf(MyEnum.MEMBER_1, MyEnum.MEMBER_2, MyEnum.MEMBER_3)

        compareUnorderedLists("", manualEnumSet.toTypedArray(), enumSet.toTypedArray())
        assertEqualsMessage("", MyEnum.asFlags(enumSet), 7)
    }

    @Test
    fun testTest_asArray() {
        val enumSet = MyEnum.asArray<MyEnum>(7)
        val manualEnumSet = arrayOf(MyEnum.MEMBER_1, MyEnum.MEMBER_2, MyEnum.MEMBER_3)

        compareUnorderedLists("", manualEnumSet, enumSet)
        assertEqualsMessage("", MyEnum.asFlags(enumSet), 7)
    }

    @Test
    fun testTest_isIn() {
//        val enumSet = MyEnum.asArray<MyEnum>(7)
//        val manualEnumSet = arrayOf(MyEnum.MEMBER_1, MyEnum.MEMBER_2, MyEnum.MEMBER_3)

        assertEqualsMessage("", true, MyEnum.MEMBER_1.isIn(7))
        assertEqualsMessage("", true, MyEnum.MEMBER_1.isIn(5))
        assertEqualsMessage("", false, MyEnum.MEMBER_1.isIn(6))
        assertEqualsMessage("", false, MyEnum.MEMBER_1.isIn(14))
        assertEqualsMessage("", false, MyEnum.MEMBER_1.isIn(2))
    }

    @Test
    fun testTest_containsSome() {
//        val enumSet = MyEnum.asArray<MyEnum>(7)
//        val manualEnumSet = arrayOf(MyEnum.MEMBER_1, MyEnum.MEMBER_2, MyEnum.MEMBER_3)

        assertEqualsMessage("", true, MyEnum.containsSome(7, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
        assertEqualsMessage("", true, MyEnum.containsSome(5, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
        assertEqualsMessage("", true, MyEnum.containsSome(3, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
        assertEqualsMessage("", false, MyEnum.containsSome(10, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
    }

    @Test
    fun testTest_containsAll() {
//        val enumSet = MyEnum.asArray<MyEnum>(7)
//        val manualEnumSet = arrayOf(MyEnum.MEMBER_1, MyEnum.MEMBER_2, MyEnum.MEMBER_3)

        assertEqualsMessage("", true, MyEnum.containsAll(7, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
        assertEqualsMessage("", true, MyEnum.containsAll(5, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
        assertEqualsMessage("", false, MyEnum.containsAll(3, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
        assertEqualsMessage("", false, MyEnum.containsAll(10, MyEnum.MEMBER_1, MyEnum.MEMBER_3))
    }
}
