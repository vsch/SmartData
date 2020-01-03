/*
 * Copyright (c) 2015-2020 Vladimir Schneider <vladimir.schneider@gmail.com>
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

@file:Suppress("UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_VALUE", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package com.vladsch.smart

import org.junit.Assert.*
import org.junit.Test

class SmartVersionedDataTest {
    @Test
    fun test_immutable() {
        for (i in -10..10) {
            val v1 = SmartImmutableData(i)

            assertFalse(v1.isStale)
            assertFalse(v1.isMutable)
            assertEquals(i, v1.get())
        }
    }

    @Test
    fun test_volatile() {
        val v1 = SmartVolatileData(0)
        val v2 = SmartVolatileData(1)

        assertFalse(v1.isStale)
        assertFalse(v2.isStale)
        assertTrue(v1.isMutable)

        assertTrue(v1.versionSerial < v2.versionSerial)
        v1.set(5)
        assertFalse(v1.versionSerial < v2.versionSerial)
        assertEquals(5, v1.get())
        val v1Serial = v1.versionSerial
        v1.set(5)
        assertEquals(v1Serial, v1.versionSerial)

        SmartVersionManager.groupedUpdate(Runnable {
            v1.set(10)
            v2.set(11)
        })

        assertEquals(v1.versionSerial, v2.versionSerial)
        assertEquals(10, v1.get())
        assertEquals(11, v2.get())

        val v1Version = v1.versionSerial
        v1.nextVersion()
        assertEquals(v1Version, v1.versionSerial)
        val v2Version = v2.versionSerial
        v2.nextVersion()
        assertEquals(v2Version, v2.versionSerial)

        v1.set(2)
        v2.set(3)
        assertTrue(v1.versionSerial < v2.versionSerial)

        SmartVersionManager.groupedUpdate(Runnable {
            v1.set(4)
            v2.set(5)
        })

        assertEquals(v1.versionSerial, v2.versionSerial)

        v1.set(6)
        v2.set(7)

        SmartVersionManager.groupedCompute {
            v1.set(8)
            v2.set(9)
        }

        assertEquals(v1.versionSerial, v2.versionSerial)
    }

    @Test
    fun test_updateDependent() {
        val v1 = SmartVolatileData(1)
        val v2 = SmartVolatileData(2)
        val v3 = SmartVolatileData(3)
        val vc = SmartDependentData(v3) { v3.get() }
        val sum: () -> Int = { v1.get() + v2.get() + vc.get() }
        val sumEq = { v1.get() + v2.get() + v3.get() }
        val dv = SmartUpdateDependentData(listOf(v1, v2, vc), sum)
        var dv2 = SmartUpdateDependentData(listOf(vc), sum)
        val vi = SmartImmutableData(10)
        val dvn = SmartLatestDependentData(listOf(vi))

        assertTrue(dv.isMutable)
        assertFalse(dv.isStale)
        v2.set(21)
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(sumEq(), dv.get())

        assertFalse(dvn.isMutable)
        assertFalse(dvn.isStale)
        assertEquals(10, dvn.get())

        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())

        v3.set(31)
        vc.get()
        assertEquals(v3.versionSerial, vc.versionSerial)
        assertEquals(v3.get(), vc.get())

        //        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, v3: ${v3.versionSerial}, vc: ${vc.versionSerial} : dv: ${dv.versionSerial} ${dv.isStale}")
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())

        v3.set(32)
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())
    }

    @Test
    fun test_dependent() {
        val v1 = SmartVolatileData(1)
        val v2 = SmartVolatileData(2)
        val v3 = SmartVolatileData(3)
        val vc = SmartDependentData(v3) { v3.get() }
        val sum = { v1.get() + v2.get() + vc.get() }
        val sumEq = { v1.get() + v2.get() + v3.get() }
        val dv = SmartDependentData(listOf(v1, v2, vc), sum)
        val vi = SmartImmutableData(10)
        val dvn = SmartIterableData(listOf(vi)) { it -> vi.get() }

        assertTrue(dv.isMutable)
        assertFalse(dv.isStale)
        v2.set(21)
        dv.get()
        assertFalse(dv.isStale)
        assertEquals(sumEq(), dv.get())

        assertFalse(dvn.isMutable)
        assertFalse(dvn.isStale)
        assertEquals(10, dvn.get())

        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())

        v3.set(31)
        dv.get()
        assertEquals(v3.versionSerial, vc.versionSerial)
        assertEquals(v3.get(), vc.get())

        //        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, v3: ${v3.versionSerial}, vc: ${vc.versionSerial} : dv: ${dv.versionSerial} ${dv.isStale}")
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())

        v3.set(32)
        dv.get()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())
    }

    @Test
    fun test_dependentIterable() {
        val v1 = SmartVolatileData(1)
        val v2 = SmartVolatileData(2)
        val v3 = SmartVolatileData(3)
        val vc = SmartDependentData(v3) { v3.get() }
        val sum = IterableDataComputable<Int> { it.sumBy { it } }
        val sumEq = { v1.get() + v2.get() + v3.get() }
        val dv = SmartVectorData(listOf(v1, v2, vc), sum)
        val vi = SmartImmutableData(10)
        val dvn = SmartIterableData(listOf(vi)) { it -> vi.get() }

        assertTrue(dv.isMutable)
        assertFalse(dv.isStale)
        v2.set(21)
        dv.get()
        assertFalse(dv.isStale)
        assertEquals(sumEq(), dv.get())

        assertFalse(dvn.isMutable)
        assertFalse(dvn.isStale)
        assertEquals(10, dvn.get())

        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())

        v3.set(31)
        dv.get()
        assertEquals(v3.versionSerial, vc.versionSerial)
        assertEquals(v3.get(), vc.get())

        //        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, v3: ${v3.versionSerial}, vc: ${vc.versionSerial} : dv: ${dv.versionSerial} ${dv.isStale}")
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())

        v3.set(32)
        dv.get()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.get())
    }

    @Test
    fun test_snapshot() {
        val v1 = SmartImmutableData("v1", 1)
        val v2 = SmartVolatileData("v2", 2)
        val vs1 = SmartCachedData("vs1", v1)
        val vs2 = SmartCachedData("vs2", v2)

        println("$v1, $v2, $vs1, $vs2")

        assertFalse(vs1.isMutable)

        assertFalse(vs1.isStale)
        assertEquals(v1.versionSerial, vs1.versionSerial)
        assertEquals(v1.get(), vs1.get())

        assertFalse(vs2.isMutable)
        assertFalse(vs2.isStale)
        assertEquals(v2.versionSerial, vs2.versionSerial)
        assertEquals(v2.get(), vs2.get())

        v2.set(22)

        assertFalse(vs2.isMutable)
        assertTrue(vs2.isStale)
        assertNotEquals(v2.versionSerial, vs2.versionSerial)
        assertNotEquals(v2.get(), vs2.get())
    }

    @Test
    fun test_latest() {
        val v1 = SmartImmutableData("v1", 1)
        val v2 = SmartVolatileData("v2", 2)
        val v3 = SmartVolatileData("v3", 3)
        var vs: SmartVersionedDataHolder<Int> = v1
        vs = SmartLatestDependentData("vs2", listOf(v1, v2, v3)) { println("$v1, $v2, $v3, $vs") }
        var va: SmartVersionedDataHolder<Int> = v1

        va = v3
        assertTrue(vs.isMutable)
        assertFalse(vs.isStale)

        assertEquals(va.versionSerial, vs.versionSerial)
        assertEquals(va.get(), vs.get())

        v2.set(22)
        va = v2
        vs.get()
        assertTrue(vs.isMutable)
        assertFalse(vs.isStale)
        //        println("$v1, $v2, $v3, $vs")
        assertEquals(va.versionSerial, vs.versionSerial)
        assertEquals(va.get(), vs.get())

        v3.set(33)
        va = v3
        vs.get()
        assertTrue(vs.isMutable)
        assertFalse(vs.isStale)
        //        println("$v1, $v2, $v3, $vs")
        assertEquals(va.versionSerial, vs.versionSerial)
        assertEquals(va.get(), vs.get())
    }

    @Test
    fun test_Fun() {
        val v1 = SmartVolatileData(10.0)
        val v2 = SmartVolatileData(5.0)
        val v3 = SmartVolatileData(3.0)
        val vd = SmartVectorData(listOf(v1, v2, v3)) {
            val prod = it.fold(1.0) { a, b -> a * b }
            println(it.fold("product of ") { a, b -> "$a $b" } + " = $prod")
            prod
        }

        var t = vd.get()
        v2.set(2.0)
        t = vd.get()
        v1.set(7.0)
        t = vd.get()
        v1.set(7.0)
        v2.set(7.0)
        v3.set(7.0)
        t = vd.get()
    }

    @Test
    fun test_Recursive() {
        val v1 = SmartVolatileData(10.0)
        val v2 = SmartVolatileData(5.0)
        val v3 = SmartVolatileData(3.0)
        val vd = SmartVectorData<Double>(listOf(v1, v2, v3)) {
            val prod = it.fold(1.0) { a, b -> a * b }
            println(it.fold("product of ") { a, b -> "$a $b" } + " = $prod")
            prod
        }

        val vd2 = SmartVectorData<Double>(listOf(v1, v2, v3, vd)) {
            val sumOf = it.sum()
            println(it.fold("sum of ") { a, b -> "$a $b" } + " = $sumOf")
            v3.set(sumOf)
            sumOf
        }

        var t = vd.get()
        var t2 = vd2.get()
        v2.set(2.0)
        t = vd.get()
        t2 = vd2.get()
        v1.set(7.0)
        t = vd.get()
        t2 = vd2.get()
        v1.set(7.0)
        v2.set(7.0)
        v3.set(7.0)
        t = vd.get()
        t2 = vd2.get()
    }

    @Test
    fun test_aliased() {
        val v1 = SmartImmutableData(1)
        val v2 = SmartVolatileData(5)
        val v3 = SmartVolatileData(3)
        val ad = SmartVersionedDataAlias(v1)
        var va: SmartVersionedDataHolder<Int> = v1
        val vd = SmartVectorData<Int>(listOf(v1, v2, v3, ad)) {
            val prod = it.fold(1) { a, b -> a * b }
            println(it.fold("product of ") { a, b -> "$a $b" } + " = $prod")
            prod
        }

        assertTrue(vd.isMutable)

        va = v1
        assertFalse(vd.isStale)
        assertEquals(va.get(), ad.get())
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.alias = v2
        va = v2
        vd.get()
        assertFalse(vd.isStale)
        assertEquals(va.get(), ad.get())
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.alias = v3
        va = v3
        vd.get()
        assertFalse(vd.isStale)
        assertEquals(va.get(), ad.get())
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.alias = vd
        va = vd
        vd.get()
        assertFalse(vd.isStale)
        assertEquals(va.get(), ad.get())
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.touchVersionSerial()
        vd.get()
        assertFalse(vd.isStale)
        assertEquals(va.get(), ad.get())
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)
    }

    @Test
    fun test_property() {
        val v1 = SmartVolatileData("v1", 1)
        val v2 = SmartVolatileData("v2", 20)
        val va = SmartVersionedDataAlias("va", v1)
        val vp = SmartVersionedProperty("vp", 0)

        assertTrue(vp.isMutable)
        assertEquals(0, vp.get())

        va.set(2)
        vp.connect(va)
        assertTrue(vp.isStale)
        assertEquals(2, vp.get())

        vp.set(2)
        assertTrue(vp.isStale)
        assertEquals(2, vp.get())

        vp.disconnect()
        assertTrue(vp.isStale)
        assertEquals(2, vp.get())

        vp.set(5)
        assertTrue(vp.isStale)
        assertEquals(5, vp.get())

        vp.connect(va)
        assertTrue(vp.isStale)
        assertEquals(2, vp.get())

        va.alias = v2
        assertEquals(20, va.get())
        assertTrue(vp.isStale)
        assertEquals(20, vp.get())

        println(vp)
        vp.connectionFinalized()
        println(vp)
        assertFalse(vp.isStale)
        assertEquals(20, vp.get())

        va.alias = v1
        v1.set(100)
        assertFalse(vp.isStale)
        assertEquals(20, vp.get())

        v2.set(200)
        assertTrue(vp.isStale)
        assertEquals(200, vp.get())
    }

    @Test
    fun test_propertyArrayDistribute() {
        val v1 = SmartVolatileData(1)
        val v2 = SmartVolatileData(20)
        val v3 = SmartVolatileData(20)
        val va1 = SmartVersionedDataAlias(v1)
        val va2 = SmartVersionedDataAlias(v2)
        val va3 = SmartVersionedDataAlias(v3)
        var called = 0
        var inAggr = false

        val pa = SmartVersionedPropertyArray<Int>("pa", 3, 0, DataValueComputable {
            assertFalse(inAggr)
            try {
                inAggr = true
//                print("aggregating: ")
//                for (n in it) {
//                    print("$n ")
//                }
//                println()
                called++
                it.sum()
            } finally {
                inAggr = false
            }
        }, DataValueComputable
        {
            assertFalse(inAggr)
            try {
                inAggr = true
//                println("distributing")
                called++
                DistributingIterator(3, it)
            } finally {
                inAggr = false
            }
        })

        // test as distributing
        pa.get()
        assertEquals(0, pa.get())
        println("called: $called")
        //        println(pa)

        for (i in 0..10) {
            pa.set(i)
            assertEquals(i, pa.get())
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].get())
            }
            //            println(pa)
            assertEquals(i, pa.get())
            //            println(pa)
            println("called: $called")
        }

        // test connected distribution
        pa.connect(v1)
        for (i in 0..10) {
            v1.set(i)
            assertEquals(i, pa.get())
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].get())
            }
            //            println(pa)
            assertEquals(i, pa.get())
            //            println(pa)
            println("called: $called")
        }
    }

    @Test
    fun test_propertyArrayAggregate() {
        val v1 = SmartVolatileData(1)
        val v2 = SmartVolatileData(20)
        val v3 = SmartVolatileData(3)
        val va = SmartVersionedDataAlias(v1)
        var called = 0
        var inAggr = false

        val pa = SmartVersionedPropertyArray<Int>("pa", 3, 0, DataValueComputable {
            assertFalse(inAggr)
            try {
                inAggr = true
//                print("aggregating: ")
//                for (n in it) {
//                    print("$n ")
//                }
//                println()
                called++
                it.sum()
            } finally {
                inAggr = false
            }
        }, DataValueComputable
        {
            assertFalse(inAggr)
            try {
                inAggr = true
//                println("distributing")
                called++
                DistributingIterator(3, it)
            } finally {
                inAggr = false
            }
        })

        // test as aggregating
        pa.get()
        assertEquals(0, pa.get())
        println("called: $called")

        pa[0].set(1)
        pa.get()
        println(pa)
        assertEquals(1, pa.get())
        println("called: $called")

        pa[1].set(2)
        assertEquals(3, pa.get())
        println("called: $called")

        pa[2].set(3)
        assertEquals(6, pa.get())
        println("called: $called")

        for (a in 1..6) {
            pa[0].set(a)
            for (b in 1..6) {
                pa[1].set(b)
                for (c in 1..6) {
                    pa[2].set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }

        // test a connected array property
        pa[0].connect(v1)
        pa.get()
        println(pa)

        v1.set(10)
        pa[1].set(20)
        pa[2].set(30)
        println("about to value")
        pa.get()
        println(pa)
        assertEquals(60, pa.get())


        for (a in 1..6) {
            v1.set(a)
            for (b in 1..6) {
                pa[1].set(b)
                for (c in 1..6) {
                    pa[2].set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }

        pa[1].connect(v2)
        for (a in 1..6) {
            v1.set(a)
            for (b in 1..6) {
                v2.set(b)
                for (c in 1..6) {
                    pa[2].set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }

        pa[2].connect(v3)
        for (a in 1..6) {
            v1.set(a)
            for (b in 1..6) {
                v2.set(b)
                for (c in 1..6) {
                    v3.set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }
        println("called: $called")
    }

    @Test
    fun test_propertyArrayDistributor() {
        val v1 = SmartVolatileData(1)
        val v2 = SmartVolatileData(20)
        val v3 = SmartVolatileData(20)
        val va1 = SmartVersionedDataAlias(v1)
        val va2 = SmartVersionedDataAlias(v2)
        val va3 = SmartVersionedDataAlias(v3)

        val pa = SmartVersionedIntAggregatorDistributor("pa", 3, 0)

        // test as distributing
        pa.get()
        assertEquals(0, pa.get())
        //        println(pa)

        for (i in 0..10) {
            pa.set(i)
            assertEquals(i, pa.get())
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].get())
            }
            //            println(pa)
            assertEquals(i, pa.get())
            //            println(pa)
        }

        // test connected distribution
        pa.connect(v1)
        for (i in 0..10) {
            v1.set(i)
            assertEquals(i, pa.get())
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].get())
            }
            //            println(pa)
            assertEquals(i, pa.get())
            //            println(pa)
        }
    }

    @Test
    fun test_propertyArrayAggregator() {
        val v1 = SmartVolatileData(1)
        val v2 = SmartVolatileData(20)
        val v3 = SmartVolatileData(3)
        val va = SmartVersionedDataAlias(v1)

        val pa = SmartVersionedIntAggregatorDistributor("pa", 3, 0)

        // test as aggregating
        pa.get()
        assertEquals(0, pa.get())

        pa[0].set(1)
        pa.get()
        println(pa)
        assertEquals(1, pa.get())

        pa[1].set(2)
        assertEquals(3, pa.get())

        pa[2].set(3)
        assertEquals(6, pa.get())

        for (a in 1..6) {
            pa[0].set(a)
            for (b in 1..6) {
                pa[1].set(b)
                for (c in 1..6) {
                    pa[2].set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }

        // test a connected array property
        pa[0].connect(v1)
        pa.get()
        println(pa)

        v1.set(10)
        pa[1].set(20)
        pa[2].set(30)
        println("about to value")
        pa.get()
        println(pa)
        assertEquals(60, pa.get())


        for (a in 1..6) {
            v1.set(a)
            for (b in 1..6) {
                pa[1].set(b)
                for (c in 1..6) {
                    pa[2].set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }

        pa[1].connect(v2)
        for (a in 1..6) {
            v1.set(a)
            for (b in 1..6) {
                v2.set(b)
                for (c in 1..6) {
                    pa[2].set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }

        pa[2].connect(v3)
        for (a in 1..6) {
            v1.set(a)
            for (b in 1..6) {
                v2.set(b)
                for (c in 1..6) {
                    v3.set(c)
                    assertEquals(a + b + c, pa.get())
                }
            }
        }
    }
}
