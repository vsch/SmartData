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

import java.util.*

abstract class SmartDataProviderHolder {
    val aggregatorMap = HashMap<SmartValueKey<*>, SmartValueAggregator>()
    val providerMap = HashMap<SmartValueKey<*>, ArrayList<SmartValueProvider>>()
    val dependentKeys = HashMap<SmartValueKey<*>, HashSet<SmartValueKey<*>>>()

    fun registerProvider(vararg providers: SmartValueProvider) {
        for (provider in providers) {
            for (resultKey in provider.resultKeys) {
                if (!providerMap.containsKey(resultKey)) {
                    providerMap.put(resultKey, arrayListOf(provider))
                    createKeyDependencies(resultKey, provider.sourceKeys)
                } else {
                    providerMap[resultKey]?.add(provider)
                }
            }
        }
    }

    fun registerAggregator(vararg aggregators: SmartValueAggregator) {
        for (aggregator in aggregators) {
            for (resultKey in aggregator.resultKeys) {
                if (!aggregatorMap.containsKey(resultKey)) {
                    aggregatorMap.put(resultKey, aggregator)
                } else {
                    if (aggregatorMap[resultKey] !== aggregator) {
                        throw IllegalArgumentException("duplicate aggregator for key " + resultKey.id + " existing " + aggregatorMap[resultKey] + ", duplicate " + aggregator)
                    }
                }
            }
        }
    }

    fun createKeyDependencies(resultKey: SmartValueKey<*>, sourceKeys: Array<SmartValueKey<*>>) {
        val resultDeps: Set<SmartValueKey<*>> = dependentKeys[resultKey] ?: setOf()
        for (sourceKey in sourceKeys) {
            dependentKeys.putIfAbsent(sourceKey, HashSet())
            dependentKeys[sourceKey]?.add(resultKey)

            if (resultDeps.contains(sourceKey)) {
                throw IllegalArgumentException("sourceKey $sourceKey has resultKey $resultKey as dependency")
            }
        }
    }
}

class SmartDataManager() : SmartDataProviderHolder() {
    fun compute(key: SmartValueKey<*>, cache: SmartData) {
        // println("enter compute ${cache.name} for ${key.id}")
        SmartVersionManager.groupedUpdate(Runnable() {
            val aggregator = cache.aggregatorMap[key] ?: aggregatorMap[key]
            if (aggregator != null) {
                // TODO: add the aggregator's resultKeys to the allowed to access key list for child caches
                if (cache.inAggregatorCompute.contains(aggregator)) {
                    throw IllegalStateException("aggregator $aggregator in ${cache.name} is already in compute() function, recursive compute")
                }

                //println("start aggregator compute ${cache.name} for ${key.id}")
                try {
                    cache.inAggregatorCompute.add(aggregator)
                    for (type in SmartAggregate.values()) {
                        if (aggregator.type.contains(type)) {
                            aggregator.provider.aggregate(type, cache, when (type) {
                                SmartAggregate.CHILDREN -> cache.children
                                SmartAggregate.PARENT -> if (cache.parent != null) listOf(cache.parent) else listOf()
                                else -> listOf()
                            })
                        }
                    }
                } finally {
                    cache.inAggregatorCompute.remove(aggregator)
                }
                //println("end aggregator compute ${cache.name} for ${key.id}")
            } else {
                // TODO: add the provider's resultKeys to the allowed to access key list for this cache
                invokeProviders(cache, providerMap[key])
                invokeProviders(cache, cache.providerMap[key])
            }
            //        println("exit compute ${cache.name} for ${key.id}")
        })
    }

    private fun invokeProviders(cache: SmartData, providers: ArrayList<SmartValueProvider>?) {
        if (providers != null) {
            for (provider in providers) {
                if (cache.inProviderCompute.contains(provider)) {
                    throw IllegalStateException("provider $provider in ${cache.name} is already in compute() function, recursive compute")
                }

                try {
                    cache.inProviderCompute.add(provider)
                    //                    println("start provider compute ${cache.name} for ${key.id}")
                    provider.provider.compute(cache)
                    //                    println("end provider compute ${cache.name} for ${key.id}")
                } finally {
                    cache.inProviderCompute.remove(provider)
                }
            }
        }
    }

    fun invalidateKey(cache: SmartData, key: SmartValueKey<*>) {
        // this cache only invalidates dependent keys
        // parent caches invalidate both this key and dependent keys if there is a child aggregator registered for it
        // child caches invalidate both this key and dependent keys if there is a parent aggregator registered for it
        val aggregator = aggregatorMap[key]

        val keys = dependentKeys[key]
        if (keys != null) cache.invalidateKeys(keys)

        if (aggregator != null) {
            if (aggregator.type.contains(SmartAggregate.SELF)) {
                if (cache.parent != null) cache.invalidateKey(key)
            }
            if (aggregator.type.contains(SmartAggregate.CHILDREN)) {
                if (cache.parent != null) invalidateParentKeys(cache.parent, key, keys)
            }
            if (aggregator.type.contains(SmartAggregate.PARENT)) {
                for (child in cache.children) {
                    invalidateChildKeys(child, key, keys)
                }
            }
        }

    }

    private fun invalidateParentKeys(cache: SmartData, key: SmartValueKey<*>?, keys: Collection<SmartValueKey<*>>?) {
        if (cache.parent != null) {
            if (key != null) cache.parent.invalidateKey(key)
            if (keys != null) cache.parent.invalidateKeys(keys)
            invalidateParentKeys(cache.parent, key, keys)
        }
    }

    private fun invalidateChildKeys(cache: SmartData, key: SmartValueKey<*>?, keys: Collection<SmartValueKey<*>>?) {
        for (child in cache.children) {
            if (key != null) child.invalidateKey(key)
            if (keys != null) child.invalidateKeys(keys)
            invalidateChildKeys(child, key, keys)
        }
    }

    fun createData(name: String): SmartData {
        return SmartData(name, this, null)
    }

}

class SmartValueListenerCaster<V : Any> : SmartValueListener<V> {
    private val myIndexListeners = HashMap<Int, HashSet<SmartIndexedValueListener<V>>>()

    fun addIndexListener(index: Int, listener: SmartIndexedValueListener<V>) {
        myIndexListeners.putIfAbsent(index, HashSet<SmartIndexedValueListener<V>>())
        myIndexListeners[index]?.add(listener)
    }

    override fun update(values: MutableMap<Int, V>) {
        for (pair in values) {
            val listeners = myIndexListeners[pair.key] ?: continue
            for (listener in listeners) {
                listener.update(pair.value)
            }
        }
    }
}


class SmartData(val name: String, var manager: SmartDataManager, val parent: SmartData?) : SmartDataProviderHolder() {
    val values = HashMap<SmartValueKey<*>, Any>()
    val children = HashSet<SmartData>()
    val versions = HashMap<SmartValueKey<*>, Int>()
    val listeners = HashMap<SmartValueKey<*>, ArrayList<SmartValueListener<*>>>()
    val indexListeners = HashMap<SmartValueKey<*>, SmartValueListenerCaster<*>>()
    val announced = HashMap<SmartValueKey<*>, Int>()
    val inAggregatorCompute = HashSet<SmartValueAggregator>()
    val inProviderCompute = HashSet<SmartValueProvider>()

    init {
        if (parent != null) {
            assert(parent.manager === manager, { "Parent cache must have the same manager as child cache" })
            parent.children.add(this)
        }
    }

    fun createData(name: String): SmartData {
        return SmartData(name, manager, this)
    }

    fun <V : Any> registerListener(key: SmartValueKey<V>, listener: SmartValueListener<V>) {
        listeners.putIfAbsent(key, ArrayList<SmartValueListener<*>>())
        listeners[key]?.add(listener)
    }

    fun <V : Any> registerListener(key: SmartValueKey<V>, index: Int, listener: SmartIndexedValueListener<V>) {
        var indexListener = indexListeners[key]
        if (indexListener == null) {
            indexListener = SmartValueListenerCaster<V>()
            indexListeners[key] = indexListener
            registerListener(key, indexListener)
        }

        key.addIndexListener(indexListener, index, listener)
    }

    fun notifyListeners() {
        //        println("enter ${name}::informListeners()")
        for (pair in listeners) {
            //            println("${name}::informListeners(${pair.key.id})")
            val version = announced[pair.key]
            val values = getAnyValues(pair.key)

            if (version == null || version < SmartVersionManager.currentVersion) {
                for (listener in pair.value) {
                    pair.key.informListener(listener, values)
                }

                announced[pair.key] = SmartVersionManager.currentVersion
            }
        }

        for (child in children) {
            child.notifyListeners()
        }
        //        println("exit ${name}::informListeners()")
    }

    inline fun <reified V : Any> getValues(key: SmartValueKey<V>): Map<Int, V> {
        return getAnyValues(key) as Map<Int, V>
    }

    //    operator inline fun <reified V : Any> get(key: SmartValueKey<V>): Map<Int, V> {
    //        return getAnyValues(key) as Map<Int, V>
    //    }

    operator inline fun <reified V : Any> set(key: SmartValueKey<V>, value: V) {
        setValue(key, value)
    }

    operator inline fun <reified V : Any> set(key: SmartValueKey<V>, values: Map<Int, V>) {
        setValue(key, values)
    }

    operator inline fun <reified V : Any> get(key: SmartValueKey<V>): SmartVariableVector<V> {
        return SmartVariableVector(key, this)
    }

    operator fun get(index: Int): SmartVariableIndex {
        return SmartVariableIndex(this, 0)
    }

    fun getAny(key: SmartValueKey<*>): SmartVariableVector<*> {
        return SmartVariableVector(key, this)
    }

    fun getAnyValues(key: SmartValueKey<*>): Map<Int, Any> {
        ensureValues(key)
        val value = values[key]
        return value as Map<Int, Any>? ?: mapOf<Int, Any>()
    }

    inline fun <reified V : Any> getValue(key: SmartValueKey<V>, index: Int? = null): V? {
        ensureValues(key)
        val value = values[key] as HashMap<Int, V>
        if (index == null) {
            for (pair in value) {
                return pair.value
            }
            return null
        }
        return value[index]
    }

    fun ensureValues(key: SmartValueKey<*>) {
        val version = versions[key]
        if (version == null || version < SmartVersionManager.currentVersion) {
            // compute the required data point by aggregation or computation
            manager.compute(key, this)
        }
    }

    fun <V : Any> updateValues(key: SmartValueKey<V>, update: (key: Int, other: V) -> V) {
        val keyValues = values[key]
        if (keyValues != null) {
            val list = (keyValues as HashMap<Int, V>).clone() as HashMap<Int, V>

            for (pair in list) {
                keyValues.put(pair.key, update(pair.key, pair.value))
            }
        }
    }

    fun <V : Any> computeValues(key: SmartValueKey<V>, inMap: Map<Int, V>, compute: (key: Int, other: V) -> V) {
        val computed = HashMap<Int, V>()
        for (pair in inMap) {
            computed.put(pair.key, compute(pair.key, pair.value))
        }

        setValue(key, computed)
    }

    fun <V : Any> updateValue(key: SmartValueKey<V>, values: HashMap<Int, V>) {
        touchKey(key)
        setValue(key, values)
    }

    fun <V : Any> updateValue(key: SmartValueKey<V>, value: V, index: Int? = null) {
        touchKey(key)
        setValue(key, value, index)
    }

    fun <V : Any> touchKey(key: SmartValueKey<V>) {
        // don't allow invalidation during update, everything should have been invalidated that needed to be
        // before the computation
        val keyVersion = versions[key]

        if (keyVersion != null && !SmartVersionManager.isGrouped) {
            if (keyVersion != SmartVersionManager.currentVersion) {
                manager.invalidateKey(this, key)
            }
        }
    }

    // this one changes the value but does not invalidate dependents
    fun <V : Any> setValue(key: SmartValueKey<V>, newValues: Map<Int, V>) {
        var valList = values[key] as HashMap<Int, V>?
        if (valList == null) {
            val cloned = HashMap<Int, V>()
            cloned.putAll(newValues)
            values.put(key, cloned)
        } else {
            for (pair in newValues) {
                valList.put(pair.key, pair.value)
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    fun <V : Any> setValue(key: SmartValueKey<V>, value: V, index: Int? = null) {
        var valList = values[key] as HashMap<Int, V>?
        if (valList == null) {
            val index = index ?: 0
            valList = hashMapOf(Pair(index, value))
            values.put(key, valList)
        } else {
            val index = index ?: valList.size
            valList.put(index, value)
        }
    }

    fun invalidateKey(key: SmartValueKey<*>) {
        values.remove(key)
        versions.remove(key)
    }

    fun invalidateKeys(keys: Collection<SmartValueKey<*>>) {
        for (key in keys) {
            values.remove(key)
            versions.remove(key)
        }
    }
}

open class SmartValueKey<V : Any> {
    val id: String
    val valueClass: Class<V>
    val nullValue: V

    constructor(nullValue: V, id: String, valueClass: Class<V>) {
        this.id = id
        this.valueClass = valueClass
        this.nullValue = nullValue
    }

    fun informListener(listener: SmartValueListener<*>, values: Any) {
        (listener as SmartValueListener<V>).update(values as Map<Int, V>)
    }

    fun values(values: Any): Map<Int, V> {
        return values as Map<Int, V>
    }

    fun values(data: SmartData): Map<Int, V> {
        return data.getAnyValues(this) as Map<Int, V>
    }

    fun vector(data: SmartData): SmartVariableVector<V> {
        return data.getAny(this) as SmartVariableVector<V>
    }

    fun value(cache: SmartData, index: Int): V {
        return cache.getAnyValues(this)[index] as V? ?: nullValue
    }

    fun addIndexListener(indexListener: SmartValueListenerCaster<*>, index: Int, listener: SmartIndexedValueListener<V>) {
        (indexListener as SmartValueListenerCaster<V>).addIndexListener(index, listener)
    }
}

fun <V1 : Any, R : Any> vectorMap(vector1: SmartVariableVector<V1>, combiner: (V1) -> R): Map<Int, R> {
    val values = HashMap<Int, R>()
    for (key in vector1.keys) {
        values.put(key, combiner(vector1[key]))
    }
    return values
}

fun <V1 : Any, V2 : Any, R : Any> vectorMap(vector1: SmartVariableVector<V1>, vector2: SmartVariableVector<V2>, combiner: (V1, V2) -> R): Map<Int, R> {
    val values = HashMap<Int, R>()
    for (key in vector1.keys.union(vector2.keys)) {
        values.put(key, combiner(vector1[key], vector2[key]))
    }
    return values
}

fun <V1 : Any, V2 : Any, V3 : Any, R : Any> vectorMap(vector1: SmartVariableVector<V1>, vector2: SmartVariableVector<V2>, vector3: SmartVariableVector<V3>, combiner: (V1, V2, V3) -> R): Map<Int, R> {
    val values = HashMap<Int, R>()
    for (key in vector1.keys.union(vector2.keys.union(vector3.keys))) {
        values.put(key, combiner(vector1[key], vector2[key], vector3[key]))
    }
    return values
}

fun <V1 : Any, V2 : Any, V3 : Any, V4 : Any, R : Any> vectorMap(vector1: SmartVariableVector<V1>, vector2: SmartVariableVector<V2>, vector3: SmartVariableVector<V3>, vector4: SmartVariableVector<V4>, combiner: (V1, V2, V3, V4) -> R): Map<Int, R> {
    val values = HashMap<Int, R>()
    for (key in vector1.keys.union(vector2.keys.union(vector3.keys.union(vector4.keys)))) {
        values.put(key, combiner(vector1[key], vector2[key], vector3[key], vector4[key]))
    }
    return values
}

fun <V : Any> vectorUnion(key: SmartValueKey<V>, datum: Collection<SmartData>): HashMap<Int, V> {
    var values = HashMap<Int, V>()
    for (data in datum) {
        values.plusAssign(key.vector(data))
    }

    return values
}

fun <V : Any> vectorFold(key: SmartValueKey<V>, datum: Collection<SmartData>, fold: (V, V) -> V): HashMap<Int, V> {
    var values = HashMap<Int, V>()
    for (data in datum) {
        val vector = key.vector(data)
        for (index in vector.keys) {
            val value = values[index]
            if (value != null) {
                values.put(index, fold(value, vector[index]))
            } else {
                values.put(index, vector[index])
            }
        }
    }

    return values
}

open class SmartVariableVector<V : Any>(key: SmartValueKey<V>, data: SmartData) : Iterable<SmartVariable<V>>, Map<Int, V> {
    protected val myData = data
    protected val myKey = key

    val key: SmartValueKey<V> get() = myKey
    val data: SmartData get() = myData

    operator fun invoke(index: Int): SmartVariable<V> {
        return SmartVariable(this, index)
    }

    operator fun invoke(): SmartVariable<V> {
        return SmartVariable(this, 0)
    }

    override operator fun get(key: Int): V {
        return myKey.value(myData, key)
    }

    operator fun set(key: Int, value: V) {
        myData.updateValue(myKey, value, key)
    }

    // operate on one beyond the end, nullValue on get, append on set
    var tail: V
        get() = myKey.nullValue
        set(value: V) {
            myData.setValue(myKey, value)
        }

    operator fun plusAssign(value: V) {
        myData.setValue(myKey, value)
    }

    operator fun plusAssign(map: Map<Int, V>) {
        myData.setValue(myKey, map)
    }

    class VariableIterator<V : Any>(private val vector: SmartVariableVector<V>, private val keys: Iterator<Int>) : Iterator<SmartVariable<V>> {
        override fun hasNext(): Boolean {
            return keys.hasNext()
        }

        override fun next(): SmartVariable<V> {
            return SmartVariable(vector, keys.next())
        }
    }

    override fun iterator(): Iterator<SmartVariable<V>> {
        return VariableIterator(this, myKey.values(myData).keys.iterator())
    }

    override val entries: Set<Map.Entry<Int, V>>
        get() = myKey.values(myData).entries
    override val keys: Set<Int>
        get() = myKey.values(myData).keys
    override val size: Int
        get() = myKey.values(myData).size
    override val values: Collection<V>
        get() = myKey.values(myData).values

    override fun containsKey(key: Int): Boolean {
        return myKey.values(myData).containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        return myKey.values(myData).containsValue(value)
    }

    override fun isEmpty(): Boolean {
        return myKey.values(myData).isEmpty()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb += "${myData.name}[${myKey.id}] "
        for (pair in entries) {
            sb += "(${pair.key}:${pair.value}) "
        }
        return sb.toString()
    }
}

open class SmartVariable<V : Any>(vector: SmartVariableVector<V>, index: Int) {
    protected val myVector = vector
    protected var myIndex = index

    val key: SmartValueKey<V> get() = myVector.key
    val data: SmartData get() = myVector.data

    var value: V
        get() = myVector[myIndex]
        set(value: V) {
            myVector[myIndex] = value
        }

    var index: Int get() = myIndex
        set(value) {
            myIndex = value
        }

    operator fun inc(): SmartVariable<V> = SmartVariable(myVector, myIndex + 1)

    operator fun dec(): SmartVariable<V> = SmartVariable(myVector, myIndex - 1)

    operator fun <R : Any> get(key: SmartValueKey<R>): SmartVariable<R> {
        return SmartVariable(key.vector(data), index)
    }

    operator fun <R : Any> set(key: SmartValueKey<R>, value: R) {
        return data.setValue(key, value, index)
    }
}

open class SmartVariableIndex(data: SmartData, index: Int) {
    protected val myData = data
    protected var myIndex = index

    val data: SmartData get() = myData
    var index: Int get() = myIndex
        set(value) {
            myIndex = value
        }

    operator fun inc(): SmartVariableIndex = SmartVariableIndex(myData, myIndex + 1)
    operator fun dec(): SmartVariableIndex = SmartVariableIndex(myData, myIndex - 1)

    operator fun <R : Any> get(key: SmartValueKey<R>): SmartVariable<R> {
        return SmartVariable(key.vector(data), index)
    }

    operator fun plusAssign(delta:Int) {
        myIndex += delta
    }

    operator fun minusAssign(delta:Int) {
        myIndex -= delta
    }

    operator fun <R : Any> set(key: SmartValueKey<R>, value: R) {
        return data.setValue(key, value, index)
    }

    fun equals(int: Int): Boolean {
        return index == int
    }

    operator fun compareTo(int: Int): Int{
        return index - int
    }
}

data class SmartValueIndex<V : Any>(val value: V, val index: Int)

fun <V : Any> HashMap<Int, V>.computeValue(inMap: Map<Int, V>, compute: (self: V?, key: Int, other: V) -> V) {
    for (pair in inMap) {
        val value = this[pair.key]
        this.put(pair.key, compute(value, pair.key, pair.value))
    }
}

fun <V : Any> aggregate(includeSelf: Boolean, resultCache: SmartData, sourceCaches: Collection<SmartData>, updateKey: SmartValueKey<V>, vararg keys: SmartValueKey<V>, compute: (self: V?, key: Int, other: V) -> V) {
    var values: HashMap<Int, V> = hashMapOf()

    for (cache in sourceCaches) {
        values.computeValue(cache.getAnyValues(updateKey) as Map<Int, V>, compute)
        for (key in keys) {
            values.computeValue(cache.getAnyValues(key) as Map<Int, V>, compute)
        }
    }

    if (includeSelf) {
        for (key in keys) {
            values.computeValue(resultCache.getAnyValues(key) as Map<Int, V>, compute)
        }
    }

    resultCache.updateValue(updateKey, values)
}

fun <V : Any> update(resultCache: SmartData, sourceCaches: Collection<SmartData>, updateKey: SmartValueKey<V>, compute: (key: Int, other: V) -> V) {
    var values: HashMap<Int, V> = hashMapOf()
    val aggregate: (V?, Int, V) -> V = { self, key, other -> compute(key, other) }

    for (cache in sourceCaches) {
        values.computeValue(cache.getAnyValues(updateKey) as Map<Int, V>, aggregate)
    }
    resultCache.updateValue(updateKey, values)
}

fun <V : Any> HashMap<Int, V>.computeValue(inMap: Map<Int, V>, compute: (key: Int, other: V) -> V) {
    for (pair in inMap) {
        this.put(pair.key, compute(pair.key, pair.value))
    }
}

fun <V : Any> Map<Int, V>.forEachValue(update: (key: Int, other: V) -> Unit) {
    for (pair in this) {
        update(pair.key, pair.value)
    }
}

open class SmartValueProvider(val sourceKeys: Array<SmartValueKey<*>>, val resultKeys: Array<SmartValueKey<*>>, val provider: ValueProvider)

enum class SmartAggregate(val flags: Int) {
    SELF(1),
    PARENT(2),
    ANCESTORS(4),
    CHILDREN(8),
    DESCENDANTS(16);

    companion object : BitSetEnum<SmartAggregate>(SmartAggregate::class.java, { it.flags })
}

open class SmartValueAggregator(val resultKeys: Array<SmartValueKey<*>>, val type: Set<SmartAggregate>, val provider: ValueAggregator)

