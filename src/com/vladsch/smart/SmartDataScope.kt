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

enum class SmartScopes(val flags: Int) {
    SELF(1),
    PARENT(2),
    ANCESTORS(4),
    CHILDREN(8),
    DESCENDANTS(16),
    INDICES(32);               // across indices within a scope, maybe?

    companion object : BitSetEnum<SmartScopes>(SmartScopes::class.java, { it.flags }) {

        fun isValidSet(scope: SmartScopes): Boolean = isValidSet(scope.flags)
        fun isValidSet(scopes: Set<SmartScopes>): Boolean = isValidSet(asFlags(scopes))
        fun isValidSet(flags: Int): Boolean = !(isAncestorsSet(flags) && isDescendantSet(flags)) && !isIndicesSet(flags)

        fun isIndicesSet(scope: SmartScopes): Boolean = isIndicesSet(scope.flags)
        fun isIndicesSet(scopes: Set<SmartScopes>): Boolean = isIndicesSet(asFlags(scopes))
        fun isIndicesSet(flags: Int): Boolean = flags and INDICES.flags > 0

        fun isAncestorsSet(scope: SmartScopes): Boolean = isAncestorsSet(scope.flags)
        fun isAncestorsSet(scopes: Set<SmartScopes>): Boolean = isAncestorsSet(asFlags(scopes))
        fun isAncestorsSet(flags: Int): Boolean = flags and (PARENT.flags or ANCESTORS.flags) > 0

        fun isDescendantSet(scope: SmartScopes): Boolean = isDescendantSet(scope.flags)
        fun isDescendantSet(scopes: Set<SmartScopes>): Boolean = isDescendantSet(asFlags(scopes))
        fun isDescendantSet(flags: Int): Boolean = flags and (CHILDREN.flags or DESCENDANTS.flags) > 0
    }
}

abstract class SmartDataKey<V : Any> {
    val id: String
    val nullValue: V
    val nullData: SmartImmutableData<V>
    val scopes: Int

    constructor(id: String, nullValue: V, scopes: Set<SmartScopes>) {
        this.id = id
        this.nullValue = nullValue
        this.nullData = SmartImmutableData(nullValue)
        this.scopes = SmartScopes.asFlags(scopes)
    }

    fun onInit() {
        SmartDataScopeManager.registerKey(this)
    }

    val isIndependent: Boolean get() = dependencies.isEmpty() || dependencies.size == 1 && dependencies.first() == this

    abstract val dependencies: List<SmartDataKey<*>>
    abstract fun createData(result: SmartDataScope, sources: Set<SmartDataScope>, indices: Set<Int>)

    fun values(data: SmartDataScope): HashMap<Int, SmartVersionedDataHolder<V>> {
        return data.getValues(this) as HashMap<Int, SmartVersionedDataHolder<V>>
    }

    fun value(cache: SmartDataScope, index: Int): SmartVersionedDataHolder<V>? {
        return cache.getValue(this, index) as SmartVersionedDataHolder<V>?
    }

    fun value(value: Any): V {
        return value as V
    }

    fun list(list: List<*>): List<V> {
        return list as List<V>
    }

    fun createValues(): HashMap<Int, SmartVersionedDataHolder<V>> {
        return HashMap()
    }

    fun createDataAlias(): SmartVersionedDataAlias<V> {
        return SmartVersionedDataAlias(nullData)
    }

    fun createDataAlias(data: SmartVersionedDataHolder<*>): SmartVersionedDataAlias<V> {
        return SmartVersionedDataAlias(data as SmartVersionedDataHolder<V>)
    }

    fun createDataAlias(scope: SmartDataScope, index: Int): SmartVersionedDataAlias<V> {
        val alias = SmartVersionedDataAlias(nullData)
        scope.setValue(this, index, alias)
        return alias
    }

    fun createData(): SmartVersionedDataHolder<V> {
        return SmartVolatileData(nullValue)
    }

    fun createList(): ArrayList<V> {
        return ArrayList()
    }

    override fun toString(): String {
        return super.toString() + " id: $id"
    }

    fun addItem(list: ArrayList<*>, item: Any) {
        (list as ArrayList<V>).add(item as V)
    }

    fun setAlias(item: SmartVersionedDataAlias<*>, value: SmartVersionedDataHolder<V>) {
        (item as SmartVersionedDataAlias<V>).alias = value
    }

    fun setAlias(item: SmartVersionedDataAlias<*>, scope: SmartDataScope, index: Int) {
        val value = scope.getValue(this, index) as SmartVersionedDataHolder<V>
        (item as SmartVersionedDataAlias<V>).alias = if (value is SmartVersionedDataAlias<V>) value.alias else value
    }

    fun setNullData(scope: SmartDataScope, index: Int) {
        scope.setValue(this, index, nullData)
    }
}

// values computed from corresponding parent values
class SmartVolatileDataKey<V : Any>(id: String, nullValue: V) : SmartDataKey<V>(id, nullValue, setOf(SmartScopes.SELF)) {
    override val dependencies: List<SmartDataKey<*>>
        get() = listOf()

    init {
        onInit()
    }

    override fun createData(result: SmartDataScope, sources: Set<SmartDataScope>, indices: Set<Int>) {
        // only first set will be used since there can only be one self
        for (source in sources) {
            for (index in indices) {
                val dependent = value(source, index) ?: throw IllegalStateException("Dependent data for dataKey $this for $source[$index] is missing")
                val resultItem = result.getValue(this, index)
                result.setValue(this, index, nullData)
            }

            break
        }
    }
}

class SmartParentComputedDataKey<V : Any>(id: String, nullValue: V, val computable: DataValueComputable<V, V>) : SmartDataKey<V>(id, nullValue, setOf(SmartScopes.PARENT)) {
    constructor(id: String, nullValue: V, computable: (V) -> V) : this(id, nullValue, DataValueComputable { computable(it) })

    val myComputable: IterableDataComputable<V> = IterableDataComputable { computable.compute(it.first()) }

    override val dependencies: List<SmartDataKey<*>>
        get() = listOf(this)

    init {
        onInit()
    }

    override fun createData(result: SmartDataScope, sources: Set<SmartDataScope>, indices: Set<Int>) {
        // only first set will be used since there can only be one parent
        for (source in sources) {
            for (index in indices) {
                val dependent = value(source, index) ?: throw IllegalStateException("Dependent data for dataKey $this for $source[$index] is missing")
                val resultItem = result.getValue(this, index)
                result.setValue(this, index, SmartIterableData(listOf(dependent), myComputable))
            }

            break
        }
    }
}

class SmartComputedDataKey<V : Any>(id: String, nullValue: V, scopes: Set<SmartScopes>, override val dependencies: List<SmartDataKey<*>>, val computable: DataValueComputable<HashMap<SmartDataKey<*>, List<*>>, V>) : SmartDataKey<V>(id, nullValue, scopes) {

    constructor(id: String, nullValue: V, scopes: Set<SmartScopes>, dependencies: List<SmartDataKey<*>>, computable: (dependencies: HashMap<SmartDataKey<*>, List<*>>) -> V) : this(id, nullValue, scopes, dependencies, DataValueComputable { computable(it) })

    val myComputable: IterableDataComputable<V> = IterableDataComputable {
        // here we create a hash map of by out dependent keys to lists of passed in source scopes
        val params = HashMap<SmartDataKey<*>, List<*>>()
        val iterator = it.iterator()

        do {
            for (depKey in dependencies) {
                val list = params[depKey] as ArrayList<*>? ?: depKey.createList()
                depKey.addItem(list, iterator.next())
            }
        } while (iterator.hasNext())

        computable.compute(params)
    }

    init {
        if (SmartScopes.isValidSet(this.scopes)) throw IllegalArgumentException("Scopes cannot contain both parent/ancestors and children/descendants")
        onInit()
    }

    override fun createData(result: SmartDataScope, sources: Set<SmartDataScope>, indices: Set<Int>) {
        for (index in indices) {
            var dependents = ArrayList<SmartVersionedDataHolder<V>>()

            for (dependencyKey in dependencies) {
                for (source in sources) {
                    val dependent = value(source, index) ?: throw IllegalStateException("Dependent data for dataKey $this for $source[$index] is missing")
                    dependents.add(dependent)
                }
            }

            result.setValue(this, index, SmartIterableData(dependents, myComputable))
        }
    }
}

class SmartTransformedDataKey<V : Any, R : Any>(id: String, nullValue: V, override val dependencies: List<SmartDataKey<*>>, scope: SmartScopes, val computable: DataValueComputable<HashMap<SmartDataKey<*>, *>, V>) : SmartDataKey<V>(id, nullValue, setOf(scope)) {

    constructor(id: String, nullValue: V, dependencies: List<SmartDataKey<*>>, scope: SmartScopes, computable: (dependencies: HashMap<SmartDataKey<*>, *>) -> V) : this(id, nullValue, dependencies, scope, DataValueComputable { computable(it) })

    constructor(id: String, nullValue: V, dependency: SmartDataKey<R>, scope: SmartScopes, computable: DataValueComputable<R, V>) : this(id, nullValue, listOf(dependency), scope, DataValueComputable { computable.compute(dependency.value(it[dependency]!!)) })

    constructor(id: String, nullValue: V, dependency: SmartDataKey<R>, scope: SmartScopes, computable: (R) -> V) : this(id, nullValue, listOf(dependency), scope, DataValueComputable { computable(dependency.value(it[dependency]!!)) })

    val myComputable: IterableDataComputable<V> = IterableDataComputable {
        // here we create a hash map of by out dependent keys to lists of passed in source scopes
        val params = HashMap<SmartDataKey<*>, Any>()
        val iterator = it.iterator()

        for (depKey in dependencies) {
            params[depKey] = iterator.next()
        }

        if (iterator.hasNext()) throw IllegalStateException("iterator hasNext() is true after all parameters have been used up")

        computable.compute(params)
    }

    init {
        if (scope != SmartScopes.SELF && scope != SmartScopes.PARENT) throw IllegalArgumentException("TransformedDataKey can only be applied to SELF or PARENT scope")
        onInit()
    }

    override fun createData(result: SmartDataScope, sources: Set<SmartDataScope>, indices: Set<Int>) {
        for (index in indices) {
            var dependents = ArrayList<SmartVersionedDataHolder<V>>()

            for (dependencyKey in dependencies) {
                for (source in sources) {
                    val dependent = value(source, index) ?: throw IllegalStateException("Dependent data for dataKey $this for $source[$index] is missing")
                    dependents.add(dependent)

                    break
                }
            }

            result.setValue(this, index, SmartIterableData(dependents, myComputable))
        }
    }
}

class SmartAggregatedDataKey<V : Any>(id: String, nullValue: V, val dataKey: SmartDataKey<*>, scopes: Set<SmartScopes>, val computable: IterableDataComputable<V>) : SmartDataKey<V>(id, nullValue, scopes) {
    constructor(id: String, nullValue: V, dataKey: SmartDataKey<*>, scopes: Set<SmartScopes>, computable: (Iterable<V>) -> V) : this(id, nullValue, dataKey, scopes, IterableDataComputable { computable(it) })

    override val dependencies: List<SmartDataKey<*>>
        get() = listOf(dataKey)

    val myComputable = computable

    init {
        onInit()
    }

    override fun createData(result: SmartDataScope, sources: Set<SmartDataScope>, indices: Set<Int>) {
        for (index in indices) {
            var dependents = ArrayList<SmartVersionedDataHolder<V>>()

            for (source in sources) {
                val dependent = value(source, index) ?: throw IllegalStateException("Dependent data for dataKey $this for $source[$index] is missing")
                dependents.add(dependent)
            }

            result.setValue(this, index, SmartIterableData(dependents, myComputable))
        }
    }
}

class SmartDataScopeManager {
    companion object {
        @JvmField val INSTANCE = SmartDataScopeManager()

        val dependentKeys: Map<SmartDataKey<*>, Set<SmartDataKey<*>>> get() = INSTANCE.dependentKeys
        val keyComputeLevel: Map<SmartDataKey<*>, Int> get() = INSTANCE.keyComputeLevel

        fun computeKeyOrder(keys: Set<SmartDataKey<*>>): List<List<SmartDataKey<*>>> = INSTANCE.computeKeyOrder(keys)
        fun registerKey(dataKey: SmartDataKey<*>) {
            INSTANCE.registerKey(dataKey)
        }

        fun resolveDependencies() {
            INSTANCE.resolveDependencies()
        }

        fun createDataScope(name: String): SmartDataScope = INSTANCE.createDataScope(name)
    }

    private val myKeys = HashSet<SmartDataKey<*>>()
    private val myIndependentKeys = HashSet<SmartDataKey<*>>()
    private val myDependentKeys = HashMap<SmartDataKey<*>, HashSet<SmartDataKey<*>>>()
    private var myDependenciesResolved = true
    //    private var myKeyDependencyMap = HashMap<SmartDataKey<*>, HashSet<SmartDataKey<*>>>()
    private var myComputeLevel = HashMap<SmartDataKey<*>, Int>()

    val dependentKeys: Map<SmartDataKey<*>, Set<SmartDataKey<*>>> get() {
        return myDependentKeys
    }

    //    val dependencyMap: Map<SmartDataKey<*>, Set<SmartDataKey<*>>> get() {
    //        if (!myDependenciesResolved) resolveDependencies()
    //        return myKeyDependencyMap
    //    }

    val keyComputeLevel: Map<SmartDataKey<*>, Int> get() {
        if (!myDependenciesResolved) resolveDependencies()
        return myComputeLevel
    }

    fun computeKeyOrder(keys: Set<SmartDataKey<*>>): List<List<SmartDataKey<*>>> {
        val needKeys = HashSet<SmartDataKey<*>>()

        for (key in keys) {
            needKeys.addAll(key.dependencies)
        }

        val orderedKeyList = HashMap<Int, ArrayList<SmartDataKey<*>>>()
        for (entry in keyComputeLevel) {
            if (needKeys.contains(entry.key)) {
                orderedKeyList.putIfAbsent(entry.value, arrayListOf())
                orderedKeyList[entry.value]!!.add(entry.key)
            }
        }

        val resultList = arrayListOf<List<SmartDataKey<*>>>()
        for (computeLevel in orderedKeyList.keys.sorted()) {
            val list = orderedKeyList[computeLevel] ?: continue
            resultList.add(list)
        }

        return resultList
    }

    fun registerKey(dataKey: SmartDataKey<*>) {
        myKeys.add(dataKey)

        myDependenciesResolved = false

        if (dataKey.isIndependent) {
            myIndependentKeys.add(dataKey)
        } else {
            val resultDeps: Set<SmartDataKey<*>> = myDependentKeys[dataKey] ?: setOf()

            for (sourceKey in dataKey.dependencies) {
                myDependentKeys.putIfAbsent(sourceKey, HashSet())
                myDependentKeys[sourceKey]!!.add(dataKey)

                if (resultDeps.contains(sourceKey)) {
                    throw IllegalArgumentException("sourceKey $sourceKey has resultKey $dataKey as dependency, circular dependency in $dataKey")
                }
            }
        }
    }

    fun createDataScope(name: String): SmartDataScope {
        return SmartDataScope(name, null)
    }

    fun resolveDependencies() {
        // compute dependency levels so that we can take a consumer key and get a list of keys that must be computed, sorted by order
        // of these computations
        val unresolvedKeys = myKeys.clone() as HashSet<SmartDataKey<*>>
        val resolvedKeys = myIndependentKeys.clone() as HashSet<SmartDataKey<*>>

        myComputeLevel = HashMap<SmartDataKey<*>, Int>()
        myComputeLevel.putAll(myIndependentKeys.map { Pair(it, 0) })

        var computeOrder = 1
        unresolvedKeys.removeAll(myIndependentKeys)

        while (!unresolvedKeys.isEmpty()) {
            // take out all the keys that no longer have any dependencies that are not computed
            val currentKeys = unresolvedKeys.filter { it.dependencies.intersect(unresolvedKeys).isEmpty() }
            if (currentKeys.isEmpty()) throw IllegalStateException("computation level has no keys, means remaining have circular dependencies")

            resolvedKeys.addAll(currentKeys)
            unresolvedKeys.removeAll(currentKeys)
            myComputeLevel.putAll(currentKeys.map { Pair(it, computeOrder) })
            computeOrder++
        }

        myDependenciesResolved = true
    }
}

// if the data point has a consumer then the corresponding entry will contain a SmartVersionedDataAlias, else it will contain null
// so when a version data point provider is computed and the data point has non-null non-alias then it is a conflict and exception time
//
// all points that are provided have to be in existence before the call to finalizeAllScopes(), at which point all computed data keys that provide
// consumed data will have their inputs computed, and the inputs of those computed points, until every consumer can be satisfied.
// if there is an input for which only a non-computed key exists then that data point will get the key's nullValue and be immutable data.
//
// data points for which no consumers exist will not be created, and will not cause intermediate data points be created nor computed
//
class SmartDataScope(val name: String, val parent: SmartDataScope?) {
    protected val myValues = HashMap<SmartDataKey<*>, HashMap<Int, SmartVersionedDataHolder<*>>>()
    protected val myChildren = HashSet<SmartDataScope>()    // children
    protected val myDescendants = HashSet<SmartDataScope>() // descendants
    protected val myAncestors = HashSet<SmartDataScope>()   // ancestors
    protected val myConsumers = HashMap<SmartDataKey<*>, ArrayList<Int>>()  // list of indices per key for which consumers are available for this iteration

    val children: Set<SmartDataScope> get() = myChildren
    val descendants: Set<SmartDataScope> get() = myDescendants
    val ancestors: Set<SmartDataScope> get() = myAncestors
    val consumers: Map<SmartDataKey<*>, List<Int>> get() = myConsumers
    val level: Int

    init {
        parent?.addChild(this)
        level = (parent?.level ?: -1) + 1
    }

    fun addChild(scope: SmartDataScope) {
        myChildren.add(scope)
        parent?.addDescendant(scope)
    }

    fun addDescendant(scope: SmartDataScope) {
        myDescendants.add(scope)
        scope.addAncestor(this)
        parent?.addDescendant(scope)
    }

    fun addAncestor(scope: SmartDataScope) {
        myAncestors.add(scope)
    }

    fun createDataScope(name: String): SmartDataScope {
        return SmartDataScope(name, this)
    }

    fun getValues(key: SmartDataKey<*>): Map<Int, SmartVersionedDataHolder<*>>? {
        return myValues[key]
    }

    fun getValue(key: SmartDataKey<*>, index: Int): SmartVersionedDataHolder<*>? {
        val value = getRawValue(key, index) ?: parent?.getValue(key, index)
        return if (value is SmartVersionedDataAlias<*>) value.alias else value
    }

    fun getRawValue(key: SmartDataKey<*>, index: Int): SmartVersionedDataHolder<*>? {
        val value = myValues[key] ?: return null
        return value[index]
    }

    fun <V : Any> setValue(key: SmartDataKey<V>, index: Int, value: SmartVersionedDataHolder<V>) {
        var valList = myValues[key]
        if (valList == null) {
            valList = hashMapOf(Pair(index, value))
            myValues.put(key, valList)
        } else {
            val item = valList[index]
            if (item != null) {
                if (item is SmartVersionedDataAlias<*>) {
                    if (value is SmartVersionedDataAlias<*>) throw IllegalStateException("data point in $name for $key already has an alias $item, second alias $value is in error")
                    key.setAlias(item, value)
                } else {
                    throw IllegalStateException("data point in $name for $key already has a data value $item, second value $value is in error")
                }
            } else {
                valList.put(index, value)
            }
        }
    }

    fun consumerDataPoint(key: SmartDataKey<*>, index: Int): SmartVersionedDataHolder<*> {
        var dataPoint = getRawValue(key, index)
        if (dataPoint == null) {
            // not yet computed
            dataPoint = key.createDataAlias(this, index)
            myConsumers.putIfAbsent(key, arrayListOf())
            myConsumers[key]!!.add(index)
        } else if (dataPoint !is SmartVersionedDataAlias<*>) {
            // wrap it in aliased as per convention
            dataPoint = key.createDataAlias(dataPoint)
            myValues[key]!![index] = dataPoint
        }
        return dataPoint
    }

    private fun addConsumedKeys(keys: HashMap<SmartDataKey<*>, HashSet<SmartDataScope>>) {
        addConsumedScopes(keys)

        for (scope in myChildren.union(myDescendants)) {
            scope.addConsumedScopes(keys)
        }
    }

    private fun addConsumedScopes(keys: HashMap<SmartDataKey<*>, HashSet<SmartDataScope>>) {
        for (entry in myConsumers) {
            keys.putIfAbsent(entry.key, HashSet())
            val scopesSet = keys[entry.key]!!

            // include self in computations if scopes were added on our behalf
            if (addKeyScopes(entry.key.scopes, scopesSet) > 0) scopesSet.add(this)
        }
    }

    private fun addKeyScopes(scopes: Int, scopesSet: HashSet<SmartDataScope>): Int {
        var count = 0
        if (scopes.and(SmartScopes.ANCESTORS.flags) > 0) {
            scopesSet.addAll(myAncestors)
            count += myAncestors.size
        }
        if (parent != null && scopes.and(SmartScopes.PARENT.flags) > 0) {
            scopesSet.add(parent)
            count++
        }
        if (scopes.and(SmartScopes.SELF.flags) > 0) {
            scopesSet.add(this)
            count++
        }
        if (scopes.and(SmartScopes.CHILDREN.flags) > 0) {
            scopesSet.addAll(myChildren)
            count += myChildren.size
        }
        if (scopes.and(SmartScopes.DESCENDANTS.flags) > 0) {
            scopesSet.addAll(myDescendants)
            count += myDescendants.size
        }
        return count
    }

    private fun addConsumedKeyIndices(dataKey: SmartDataKey<*>, scopesSet: Set<SmartDataScope>, indices: HashSet<Int>) {
        addKeyIndices(dataKey, indices)

        for (scope in scopesSet) {
            scope.addKeyIndices(dataKey, indices)
        }
    }

    private fun addKeyIndices(dataKey: SmartDataKey<*>, indicesSet: HashSet<Int>) {
        val indices = myConsumers[dataKey] ?: return

        for (index in indices) {
            val rawValue = getRawValue(dataKey, index)
            if (rawValue == null || (rawValue is SmartVersionedDataAlias<*> && rawValue.alias === dataKey.nullData)) {
                indicesSet.add(index)
            }
        }
    }

    /**
     * used to create smart data relationships
     *
     * can only be invoked from top level scope
     */
    fun finalizeAllScopes() {
        if (parent != null) throw IllegalStateException("finalizeScope can only be invoked on top level dataScope object")
        val consumedKeys = HashMap<SmartDataKey<*>, HashSet<SmartDataScope>>()
        addConsumedKeys(consumedKeys)

        val computeKeys = SmartDataScopeManager.computeKeyOrder(consumedKeys.keys)

        for (keyList in computeKeys) {
            for (key in keyList) {
                val scopeSet = consumedKeys[key] ?: continue
                finalizeKey(key, scopeSet)
            }
        }

        // copy parent values to child consumers that have defaults
        finalizeParentProvided()

        // TODO: validate that all have been computed before clearing consumers for possible next batch
        myConsumers.clear()

        for (scope in myChildren.union(myDescendants)) {
            scope.myConsumers.clear()
        }
    }

    private fun finalizeParentProvided() {
        if (parent != null) {
            for (entry in myConsumers) {
                for (index in entry.value) {
                    val value = getRawValue(entry.key, index)
                    if (value is SmartVersionedDataAlias<*>) {
                        if (value.alias === entry.key.nullData) {
                            // see if the parent has a value
                            entry.key.setAlias(value, parent, index)
                        }
                    }
                }
            }
        }

        for (scope in children) {
            scope.finalizeParentProvided()
        }
    }

    private fun finalizeKey(dataKey: SmartDataKey<*>, keyScopes: HashSet<SmartDataScope>) {
        if (parent != null) throw IllegalStateException("finalizeKey should only be called from top level scope")

        val indicesSet = HashSet<Int>()
        addConsumedKeyIndices(dataKey, myChildren.union(myDescendants), indicesSet)

        if (!indicesSet.isEmpty()) {
            // we add a value at the top level for all dependencies of this key if one does not exist, this will provide the missing default for all descendants
            for (key in dataKey.dependencies) {
                for (index in indicesSet) {
                    if (getValue(key, index) == null) {
                        key.setNullData(this, index)
                    }
                }
            }

            for (scope in keyScopes.sortedBy { it.level }) {
                scope.finalizeKeyScope(dataKey, this)
            }
        }
    }

    private fun finalizeKeyScope(dataKey: SmartDataKey<*>, topScope: SmartDataScope) {
        val scopesSet = HashSet<SmartDataScope>()
        addKeyScopes(dataKey.scopes, scopesSet)
        if (!scopesSet.isEmpty()) {
            val indicesSet = HashSet<Int>()

            addConsumedKeyIndices(dataKey, setOf(this), indicesSet)
            //println("finalizing $name[$dataKey] indices $indicesSet")
            if (!indicesSet.isEmpty()) {
                dataKey.createData(this, scopesSet, indicesSet)
            }
        }
    }
}
