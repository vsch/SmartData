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

class SmartVersionedDataTest {
    @Test
    fun test_immutable() {
        for (i in -10..10) {
            val v1 = SmartImmutableData(i)

            assertFalse(v1.isStale)
            assertFalse(v1.isMutable)
            assertEquals(i, v1.value)
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
        v1.value = 5
        assertFalse(v1.versionSerial < v2.versionSerial)
        assertEquals(5, v1.value)
        val v1Serial = v1.versionSerial
        v1.value = 5
        assertEquals(v1Serial, v1.versionSerial)

        SmartVersionManager.groupedUpdate(Runnable {
            v1.value = 10
            v2.value = 11
        })

        assertEquals(v1.versionSerial, v2.versionSerial)
        assertEquals(10, v1.value)
        assertEquals(11, v2.value)

        val v1Version = v1.versionSerial
        v1.nextVersion()
        assertEquals(v1Version, v1.versionSerial)
        val v2Version = v2.versionSerial
        v2.nextVersion()
        assertEquals(v2Version, v2.versionSerial)

        v1.value = 2
        v2.value = 3
        assertTrue(v1.versionSerial < v2.versionSerial)

        SmartVersionManager.groupedUpdate(Runnable {
            v1.value = 4
            v2.value = 5
        })

        assertEquals(v1.versionSerial, v2.versionSerial)

        v1.value = 6
        v2.value = 7

        SmartVersionManager.groupedCompute({
            v1.value = 8
            v2.value = 9
        })

        assertEquals(v1.versionSerial, v2.versionSerial)
    }

    @Test
    fun test_updateDependent() {
        var v1 = SmartVolatileData(1)
        var v2 = SmartVolatileData(2)
        var v3 = SmartVolatileData(3)
        var vc = SmartDependentData(v3, { v3.value })
        val sum: () -> Int = { v1.value + v2.value + vc.value }
        val sumEq = { v1.value + v2.value + v3.value }
        var dv = SmartUpdateDependentData(listOf(v1, v2, vc), sum)
        var dv2 = SmartUpdateDependentData(listOf(vc), sum)
        var vi = SmartImmutableData(10)
        var dvn = SmartLatestDependentData(listOf(vi))

        assertTrue(dv.isMutable)
        assertFalse(dv.isStale)
        v2.value = 21
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(sumEq(), dv.value)

        assertFalse(dvn.isMutable)
        assertFalse(dvn.isStale)
        assertEquals(10, dvn.value)

        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)

        v3.value = 31
        vc.value
        assertEquals(v3.versionSerial, vc.versionSerial)
        assertEquals(v3.value, vc.value)

        //        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, v3: ${v3.versionSerial}, vc: ${vc.versionSerial} : dv: ${dv.versionSerial} ${dv.isStale}")
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)

        v3.value = 32
        assertTrue(dv.isStale)
        dv.nextVersion()
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)
    }

    @Test
    fun test_dependent() {
        var v1 = SmartVolatileData(1)
        var v2 = SmartVolatileData(2)
        var v3 = SmartVolatileData(3)
        var vc = SmartDependentData(v3, { v3.value })
        val sum = { v1.value + v2.value + vc.value }
        val sumEq = { v1.value + v2.value + v3.value }
        var dv = SmartDependentData(listOf(v1, v2, vc), sum)
        var vi = SmartImmutableData(10)
        var dvn = SmartIterableData(listOf(vi), { it -> vi.value })

        assertTrue(dv.isMutable)
        assertFalse(dv.isStale)
        v2.value = 21
        dv.value
        assertFalse(dv.isStale)
        assertEquals(sumEq(), dv.value)

        assertFalse(dvn.isMutable)
        assertFalse(dvn.isStale)
        assertEquals(10, dvn.value)

        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)

        v3.value = 31
        dv.value
        assertEquals(v3.versionSerial, vc.versionSerial)
        assertEquals(v3.value, vc.value)

        //        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, v3: ${v3.versionSerial}, vc: ${vc.versionSerial} : dv: ${dv.versionSerial} ${dv.isStale}")
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)

        v3.value = 32
        dv.value
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)
    }

    @Test
    fun test_dependentIterable() {
        var v1 = SmartVolatileData(1)
        var v2 = SmartVolatileData(2)
        var v3 = SmartVolatileData(3)
        var vc = SmartDependentData(v3, { v3.value })
        val sum = IterableDataComputable<Int> { it.sumBy { it } }
        val sumEq = { v1.value + v2.value + v3.value }
        var dv = SmartVectorData(listOf(v1, v2, vc), sum)
        var vi = SmartImmutableData(10)
        var dvn = SmartIterableData(listOf(vi), { it -> vi.value })

        assertTrue(dv.isMutable)
        assertFalse(dv.isStale)
        v2.value = 21
        dv.value
        assertFalse(dv.isStale)
        assertEquals(sumEq(), dv.value)

        assertFalse(dvn.isMutable)
        assertFalse(dvn.isStale)
        assertEquals(10, dvn.value)

        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)

        v3.value = 31
        dv.value
        assertEquals(v3.versionSerial, vc.versionSerial)
        assertEquals(v3.value, vc.value)

        //        println("v1: ${v1.versionSerial}, v2: ${v2.versionSerial}, v3: ${v3.versionSerial}, vc: ${vc.versionSerial} : dv: ${dv.versionSerial} ${dv.isStale}")
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)

        v3.value = 32
        dv.value
        assertFalse(dv.isStale)
        assertEquals(v1.versionSerial.max(v2.versionSerial, v3.versionSerial), dv.versionSerial)
        assertEquals(sumEq(), dv.value)
    }

    @Test
    fun test_snapshot() {
        var v1 = SmartImmutableData("v1", 1)
        var v2 = SmartVolatileData("v2", 2)
        var vs1 = SmartCachedData("vs1", v1)
        var vs2 = SmartCachedData("vs2", v2)

        println("$v1, $v2, $vs1, $vs2")

        assertFalse(vs1.isMutable)

        assertFalse(vs1.isStale)
        assertEquals(v1.versionSerial, vs1.versionSerial)
        assertEquals(v1.value, vs1.value)

        assertFalse(vs2.isMutable)
        assertFalse(vs2.isStale)
        assertEquals(v2.versionSerial, vs2.versionSerial)
        assertEquals(v2.value, vs2.value)

        v2.value = 22

        assertFalse(vs2.isMutable)
        assertTrue(vs2.isStale)
        assertNotEquals(v2.versionSerial, vs2.versionSerial)
        assertNotEquals(v2.value, vs2.value)
    }

    @Test
    fun test_latest() {
        var v1 = SmartImmutableData("v1", 1)
        var v2 = SmartVolatileData("v2", 2)
        var v3 = SmartVolatileData("v3", 3)
        var vs: SmartVersionedDataHolder<Int> = v1
        vs = SmartLatestDependentData("vs2", listOf(v1, v2, v3)) { println("$v1, $v2, $v3, $vs") }
        var va: SmartVersionedDataHolder<Int> = v1

        va = v3
        assertTrue(vs.isMutable)
        assertFalse(vs.isStale)

        assertEquals(va.versionSerial, vs.versionSerial)
        assertEquals(va.value, vs.value)

        v2.value = 22
        va = v2
        vs.value
        assertTrue(vs.isMutable)
        assertFalse(vs.isStale)
        //        println("$v1, $v2, $v3, $vs")
        assertEquals(va.versionSerial, vs.versionSerial)
        assertEquals(va.value, vs.value)

        v3.value = 33
        va = v3
        vs.value
        assertTrue(vs.isMutable)
        assertFalse(vs.isStale)
        //        println("$v1, $v2, $v3, $vs")
        assertEquals(va.versionSerial, vs.versionSerial)
        assertEquals(va.value, vs.value)
    }

    @Test
    fun test_Fun() {
        var v1 = SmartVolatileData(10.0)
        var v2 = SmartVolatileData(5.0)
        var v3 = SmartVolatileData(3.0)
        var vd = SmartVectorData(listOf(v1, v2, v3), {
            val prod = it.fold(1.0) { a, b -> a * b }
            println(it.fold("product of ") { a, b -> "$a $b" } + " = $prod")
            prod
        })

        var t = vd.value
        v2.value = 2.0
        t = vd.value
        v1.value = 7.0
        t = vd.value
        v1.value = 7.0
        v2.value = 7.0
        v3.value = 7.0
        t = vd.value
    }

    @Test
    fun test_Recursive() {
        var v1 = SmartVolatileData(10.0)
        var v2 = SmartVolatileData(5.0)
        var v3 = SmartVolatileData(3.0)
        var vd = SmartVectorData<Double>(listOf(v1, v2, v3), {
            val prod = it.fold(1.0) { a, b -> a * b }
            println(it.fold("product of ") { a, b -> "$a $b" } + " = $prod")
            prod
        })

        var vd2 = SmartVectorData<Double>(listOf(v1, v2, v3, vd), {
            val sumOf = it.sum()
            println(it.fold("sum of ") { a, b -> "$a $b" } + " = $sumOf")
            v3.value = sumOf
            sumOf
        })

        var t = vd.value
        var t2 = vd2.value
        v2.value = 2.0
        t = vd.value
        t2 = vd2.value
        v1.value = 7.0
        t = vd.value
        t2 = vd2.value
        v1.value = 7.0
        v2.value = 7.0
        v3.value = 7.0
        t = vd.value
        t2 = vd2.value
    }

    @Test
    fun test_aliased() {
        var v1 = SmartImmutableData(1)
        var v2 = SmartVolatileData(5)
        var v3 = SmartVolatileData(3)
        var ad = SmartVersionedDataAlias(v1)
        var va: SmartVersionedDataHolder<Int> = v1
        var vd = SmartVectorData<Int>(listOf(v1, v2, v3, ad), {
            val prod = it.fold(1) { a, b -> a * b }
            println(it.fold("product of ") { a, b -> "$a $b" } + " = $prod")
            prod
        })

        assertTrue(vd.isMutable)

        va = v1
        assertFalse(vd.isStale)
        assertEquals(va.value, ad.value)
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.alias = v2
        va = v2
        vd.value
        assertFalse(vd.isStale)
        assertEquals(va.value, ad.value)
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.alias = v3
        va = v3
        vd.value
        assertFalse(vd.isStale)
        assertEquals(va.value, ad.value)
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.alias = vd
        va = vd
        vd.value
        assertFalse(vd.isStale)
        assertEquals(va.value, ad.value)
        assertTrue(ad.isMutable)
        assertTrue(va.versionSerial <= ad.versionSerial)

        ad.touchVersionSerial()
        vd.value
        assertFalse(vd.isStale)
        assertEquals(va.value, ad.value)
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
        assertEquals(0, vp.value)

        va.value = 2
        vp.connect(va)
        assertTrue(vp.isStale)
        assertEquals(2, vp.value)

        vp.value = 2
        assertTrue(vp.isStale)
        assertEquals(2, vp.value)

        vp.disconnect()
        assertTrue(vp.isStale)
        assertEquals(2, vp.value)

        vp.value = 5
        assertTrue(vp.isStale)
        assertEquals(5, vp.value)

        vp.connect(va)
        assertTrue(vp.isStale)
        assertEquals(2, vp.value)

        va.alias = v2
        assertEquals(20, va.value)
        assertTrue(vp.isStale)
        assertEquals(20, vp.value)

        println(vp)
        vp.connectionFinalized()
        println(vp)
        assertFalse(vp.isStale)
        assertEquals(20, vp.value)

        va.alias = v1
        v1.value = 100
        assertFalse(vp.isStale)
        assertEquals(20, vp.value)

        v2.value = 200
        assertTrue(vp.isStale)
        assertEquals(200, vp.value)
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
        pa.value
        assertEquals(0, pa.value)
        println("called: $called")
        //        println(pa)

        for (i in 0..10) {
            pa.value = i
            assertEquals(i, pa.value)
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].value)
            }
            //            println(pa)
            assertEquals(i, pa.value)
            //            println(pa)
            println("called: $called")
        }

        // test connected distribution
        pa.connect(v1)
        for (i in 0..10) {
            v1.value = i
            assertEquals(i, pa.value)
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].value)
            }
            //            println(pa)
            assertEquals(i, pa.value)
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
        pa.value
        assertEquals(0, pa.value)
        println("called: $called")

        pa[0].value = 1
        pa.value
        println(pa)
        assertEquals(1, pa.value)
        println("called: $called")

        pa[1].value = 2
        assertEquals(3, pa.value)
        println("called: $called")

        pa[2].value = 3
        assertEquals(6, pa.value)
        println("called: $called")

        for (a in 1..6) {
            pa[0].value = a
            for (b in 1..6) {
                pa[1].value = b
                for (c in 1..6) {
                    pa[2].value = c
                    assertEquals(a + b + c, pa.value)
                }
            }
        }

        // test a connected array property
        pa[0].connect(v1)
        pa.value
        println(pa)

        v1.value = 10
        pa[1].value = 20
        pa[2].value = 30
        println("about to value")
        pa.value
        println(pa)
        assertEquals(60, pa.value)


        for (a in 1..6) {
            v1.value = a
            for (b in 1..6) {
                pa[1].value = b
                for (c in 1..6) {
                    pa[2].value = c
                    assertEquals(a + b + c, pa.value)
                }
            }
        }

        pa[1].connect(v2)
        for (a in 1..6) {
            v1.value = a
            for (b in 1..6) {
                v2.value = b
                for (c in 1..6) {
                    pa[2].value = c
                    assertEquals(a + b + c, pa.value)
                }
            }
        }

        pa[2].connect(v3)
        for (a in 1..6) {
            v1.value = a
            for (b in 1..6) {
                v2.value = b
                for (c in 1..6) {
                    v3.value = c
                    assertEquals(a + b + c, pa.value)
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
        pa.value
        assertEquals(0, pa.value)
        //        println(pa)

        for (i in 0..10) {
            pa.value = i
            assertEquals(i, pa.value)
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].value)
            }
            //            println(pa)
            assertEquals(i, pa.value)
            //            println(pa)
        }

        // test connected distribution
        pa.connect(v1)
        for (i in 0..10) {
            v1.value = i
            assertEquals(i, pa.value)
//            print("i:$i ")
//            for (c in 0..2) {
//                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
//                print("[$c] -> $col ")
//            }
//            println()

            for (c in 0..2) {
                val col = (i / 3) + if (c < (i - (i / 3) * 3)) 1 else 0
                assertEquals(col, pa[c].value)
            }
            //            println(pa)
            assertEquals(i, pa.value)
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
        pa.value
        assertEquals(0, pa.value)

        pa[0].value = 1
        pa.value
        println(pa)
        assertEquals(1, pa.value)

        pa[1].value = 2
        assertEquals(3, pa.value)

        pa[2].value = 3
        assertEquals(6, pa.value)

        for (a in 1..6) {
            pa[0].value = a
            for (b in 1..6) {
                pa[1].value = b
                for (c in 1..6) {
                    pa[2].value = c
                    assertEquals(a + b + c, pa.value)
                }
            }
        }

        // test a connected array property
        pa[0].connect(v1)
        pa.value
        println(pa)

        v1.value = 10
        pa[1].value = 20
        pa[2].value = 30
        println("about to value")
        pa.value
        println(pa)
        assertEquals(60, pa.value)


        for (a in 1..6) {
            v1.value = a
            for (b in 1..6) {
                pa[1].value = b
                for (c in 1..6) {
                    pa[2].value = c
                    assertEquals(a + b + c, pa.value)
                }
            }
        }

        pa[1].connect(v2)
        for (a in 1..6) {
            v1.value = a
            for (b in 1..6) {
                v2.value = b
                for (c in 1..6) {
                    pa[2].value = c
                    assertEquals(a + b + c, pa.value)
                }
            }
        }

        pa[2].connect(v3)
        for (a in 1..6) {
            v1.value = a
            for (b in 1..6) {
                v2.value = b
                for (c in 1..6) {
                    v3.value = c
                    assertEquals(a + b + c, pa.value)
                }
            }
        }
    }
}
