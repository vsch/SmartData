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

class SmartSamples {
    @Test
    fun test_Sum() {
        val v1 = SmartVolatileData("v1", 0)
        val v2 = SmartVolatileData("v2", 0)
        val v3 = SmartVolatileData("v3", 0)
        val sum = SmartVectorData("sum", listOf(v1, v2, v3)) { val sum = it.sumBy { it }; println("computed sum: $sum"); sum }

        println("v1: ${v1.value}, v2: ${v2.value}, v3: ${v3.value}")
        println("sum: ${sum.value}")
        println("sum: ${sum.value}")
        v1.value = 10
        println("v1: ${v1.value}, v2: ${v2.value}, v3: ${v3.value}")
        println("sum: ${sum.value}")
        println("sum: ${sum.value}")
        v2.value = 20
        println("v1: ${v1.value}, v2: ${v2.value}, v3: ${v3.value}")
        println("sum: ${sum.value}")
        println("sum: ${sum.value}")
        v3.value = 30
        println("v1: ${v1.value}, v2: ${v2.value}, v3: ${v3.value}")
        println("sum: ${sum.value}")
        println("sum: ${sum.value}")

        v1.value = 100
        v2.value = 200
        v3.value = 300
        println("v1: ${v1.value}, v2: ${v2.value}, v3: ${v3.value}")
        println("sum: ${sum.value}")
        println("sum: ${sum.value}")
    }

    @Test
    fun test_Prod() {
        val v1 = SmartVolatileData("v1", 1)
        val v2 = SmartVolatileData("v2", 1)
        val v3 = SmartVolatileData("v3", 1)
        val prod = SmartVectorData("prod", listOf(v1, v2, v3)) {
            val iterator = it.iterator()
            var prod = iterator.next()
            print("Prod of $prod ")
            for (value in iterator) {
                print("$value ")
                prod *= value
            }
            println("is $prod")
            prod
        }

        var t = prod.value
        v1.value = 10
        t = prod.value
        v2.value = 20
        t = prod.value
        v3.value = 30
        t = prod.value

        v1.value = 100
        v2.value = 200
        v3.value = 300
        t = prod.value
    }

    @Test
    fun test_DataScopes() {
        val WIDTH = SmartVolatileDataKey("WIDTH", 0)
        val MAX_WIDTH = SmartAggregatedDataKey("MAX_WIDTH", 0, WIDTH, setOf(SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() } )
        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)
        val COLUMN_ALIGNMENT = SmartDependentDataKey("COLUMN_ALIGNMENT", TextColumnAlignment.NULL_VALUE, listOf(ALIGNMENT, MAX_WIDTH), SmartScopes.SELF, { TextColumnAlignment(WIDTH.value(it), ALIGNMENT.value(it) ) })

        val topScope = SmartDataScopeManager.createDataScope("top")
        val child1 = topScope.createDataScope("child1")
        val child2 = topScope.createDataScope("child2")
        val grandChild11 = child1.createDataScope("grandChild11")
        val grandChild21 = child2.createDataScope("grandChild21")

        WIDTH[child1, 0] = 10
        WIDTH[child2, 0] = 15
        WIDTH[grandChild11, 0] = 8
        WIDTH[grandChild21, 0] = 20

        val maxWidth = topScope[MAX_WIDTH, 0]

        topScope.finalizeAllScopes()

        println("MAX_WIDTH: ${maxWidth.value}")

        WIDTH[grandChild11, 0] = 12
        WIDTH[grandChild21, 0] = 17

        println("MAX_WIDTH: ${maxWidth.value}")

        WIDTH[grandChild21, 0] = 10

        println("MAX_WIDTH: ${maxWidth.value}")
    }

}


