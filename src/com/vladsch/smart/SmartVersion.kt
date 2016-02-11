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

object SmartVersionManager {
    private var mySerial = Integer.MIN_VALUE + 1

    class GroupedUpdate : ThreadLocal<GroupedUpdate>() {
        var myInGroup = 0
        var myFrozenSerial = 0
    }

    private var groupedUpdate = GroupedUpdate()

    val isGrouped: Boolean get() = groupedUpdate.myInGroup > 0

    private val currentVersionRaw: Int
        get() {
            synchronized (this) {
                return mySerial
            }
        }

    val currentVersion: Int
        get() {
            return if (groupedUpdate.myInGroup > 0) groupedUpdate.myFrozenSerial
            else currentVersionRaw
        }

    val nextVersion: Int
        get() {
            if (groupedUpdate.myInGroup > 0) return groupedUpdate.myFrozenSerial
            return nextVersionRaw
        }

    private val nextVersionRaw: Int
        get() {
            synchronized (this) {
                if (mySerial == MAX_SERIAL) {
                    // roll over, should not happen under normal conditions
                    mySerial = MIN_SERIAL
                    return mySerial
                } else {
                    return ++mySerial
                }
            }
        }

    fun groupedUpdate(runnable: () -> Unit) {
        enterGroupedUpdate()
        try {
            runnable()
        } finally {
            leaveGroupedUpdate()
        }
    }

    fun groupedUpdate(runnable: Runnable) {
        enterGroupedUpdate()
        try {
            runnable.run()
        } finally {
            leaveGroupedUpdate()
        }
    }

    private fun leaveGroupedUpdate() {
        groupedUpdate.myInGroup--
    }

    private fun enterGroupedUpdate() {
        var wasGroupedUpdate = groupedUpdate.myInGroup++ > 0
        if (!wasGroupedUpdate) {
            // that way all updated data will be the latest version
            groupedUpdate.myFrozenSerial = nextVersionRaw
        }
    }

    fun <V> groupedCompute(dataComputable: () -> V): V {
        enterGroupedUpdate()
        try {
            return dataComputable()
        } finally {
            leaveGroupedUpdate()
        }
    }

    fun <V> groupedCompute(dataComputable: DataComputable<V>): V {
        enterGroupedUpdate()
        try {
            return dataComputable.compute()
        } finally {
            leaveGroupedUpdate()
        }
    }

    fun isStale(dependencies: Iterable<SmartVersion>, snapshotSerial: Int, dependenciesSerial: Int): Boolean {
        // see if last evaluation is current
        return groupedCompute(DataComputable {
            if (snapshotSerial >= currentVersion) return@DataComputable false

            // if last snapshot is less than the version serial then snapshot needs to be computed
            if (snapshotSerial < dependenciesSerial) return@DataComputable true

            // version snapshot < dependencies snapshot, see if any of the dependencies changed their version
            for (dependency in dependencies) {
                // if dependency is stale, then this snapshot is stale
                if (dependency.isStale) return@DataComputable true
                if (snapshotSerial < dependency.versionSerial) return@DataComputable true
            }

            false
        })
    }

    fun computeVersionSnapshot(dependencies: Iterable<SmartVersion>): VersionSnapshot {
        return groupedCompute(DataComputable {
            var dependenciesSerial = NULL_SERIAL

            for (dependency in dependencies) {
                if (dependency.isStale) dependency.nextVersion()
                if (dependenciesSerial < dependency.versionSerial) dependenciesSerial = dependency.versionSerial
            }

            VersionSnapshot(currentVersion, dependenciesSerial)
        })
    }

    fun isMutable(dependencies: Iterable<SmartVersion>): Boolean {
        for (dependency in dependencies) {
            if (dependency.isMutable) {
                return true
            }
        }
        return false
    }

    internal fun freshenSnapshot(snapshotHolder: SnapshotHolder<VersionSnapshot>, dependencies: Iterable<SmartVersion>): Boolean {
        return freshenSnapshot(snapshotHolder, computeVersionSnapshot(dependencies))
    }

    fun <V> freshenSnapshot(snapshotHolder: DataSnapshotHolder<V>, snapshot: DataSnapshot<V>): Boolean {
        synchronized(snapshotHolder) {
            @Suppress("SENSELESS_COMPARISON")
            if (snapshotHolder.dataSnapshot == null || snapshotHolder.dataSnapshot.serial < snapshot.serial) {
                snapshotHolder.dataSnapshot = snapshot
                return true
            }
        }
        return false
    }

    internal fun <V : SerialHolder> freshenSnapshot(snapshotHolder: SnapshotHolder<V>, snapshot: V): Boolean {
        synchronized(snapshotHolder) {
            @Suppress("SENSELESS_COMPARISON")
            if (snapshotHolder.snapshot == null || snapshotHolder.snapshot.serial < snapshot.serial) {
                snapshotHolder.snapshot = snapshot
                return true
            }
        }
        return false
    }
}

const val NULL_SERIAL = Integer.MIN_VALUE
const val STALE_SERIAL = Integer.MIN_VALUE + 1
const val MIN_SERIAL = Integer.MIN_VALUE + 2
const val MAX_SERIAL = Integer.MAX_VALUE - 1
const val FRESH_SERIAL = Integer.MAX_VALUE

val IMMUTABLE_VERSION_SNAPSHOT = VersionSnapshot(FRESH_SERIAL, NULL_SERIAL)
val STALE_VERSION_SNAPSHOT = VersionSnapshot(NULL_SERIAL, STALE_SERIAL)
val STALE_COMPUTED_VERSION_SNAPSHOT = STALE_VERSION_SNAPSHOT
val EMPTY_DEPENDENCIES = listOf<SmartVersion>()

interface SerialHolder {
    val serial: Int
}

internal interface SnapshotHolder<V : SerialHolder> {
    var snapshot: V
}

data class VersionSnapshot(val snapshotSerial: Int, val dependenciesSerial: Int) : SerialHolder {
    override val serial: Int = snapshotSerial
}

interface SmartVersion {
    val versionSerial: Int
    val isStale: Boolean                // return true if version needs to be refreshed
    val isMutable: Boolean              // return true if version is mutable
    val dependencies: Iterable<SmartVersion>
    fun nextVersion()                   // cause version to be updated
}

object SmartImmutableVersion : SmartVersion {
    final override val versionSerial: Int get() = NULL_SERIAL
    final override val isStale: Boolean get() = false
    final override val isMutable: Boolean get() = false
    override val dependencies: Iterable<SmartVersion> get() = EMPTY_DEPENDENCIES
    final override fun nextVersion() {
    }
}

fun SmartVersionedDataIterableAdapter(iterable: Iterable<SmartVersionedData>): Iterable<SmartVersion> = IterableAdapter(iterable, DataValueComputable<SmartVersionedData, SmartVersion> { it.version })

fun <V> IterableValueDependenciesAdapter(iterable: Iterable<SmartVersionedDataHolder<V>>): Iterable<V> = IterableAdapter(iterable, DataValueComputable<SmartVersionedDataHolder<V>, V> { it.value })

open class SmartVolatileVersion() : SmartVersion {
    protected var myVersion = SmartVersionManager.nextVersion
        private set

    override val versionSerial: Int
        get() = myVersion

    override val isStale: Boolean get() = false
    override val isMutable: Boolean get() = true
    override fun nextVersion() {
        myVersion = SmartVersionManager.nextVersion
    }

    override val dependencies: Iterable<SmartVersion> = EMPTY_DEPENDENCIES
}

open class SmartDependentVersion(dependencies: Iterable<SmartVersion>) : SmartVersion, SnapshotHolder<VersionSnapshot> {
    constructor(dependency: SmartVersion) : this(listOf(dependency))

    protected val myDependencies = dependencies
    protected var mySnapshot = STALE_COMPUTED_VERSION_SNAPSHOT
    protected var myMutable: Boolean? = null

    init {
        onInit()
    }

    final protected val isStaleRaw: Boolean get() = SmartVersionManager.isStale(myDependencies, mySnapshot.snapshotSerial, mySnapshot.dependenciesSerial)

    override val versionSerial: Int get() = mySnapshot.dependenciesSerial
    override val isStale: Boolean get() = isStaleRaw
    override val isMutable: Boolean
        get() {
            var mutable = myMutable
            if (mutable == null) {
                mutable = SmartVersionManager.isMutable(myDependencies)
                myMutable = mutable
            }
            return mutable
        }

    override val dependencies: Iterable<SmartVersion> get() = myDependencies
    override fun nextVersion() {
        if (isStaleRaw) {
            onNextVersion()
        }
    }

    protected open fun onInit() {
        onSuperInit()
    }

    protected open fun onSuperInit() {
        onNextVersion()
    }

    override var snapshot: VersionSnapshot
        get() = mySnapshot
        set(value) {
            mySnapshot = value
        }

    protected open fun onNextVersion() {
        SmartVersionManager.freshenSnapshot(this, dependencies)
    }
}

open class SmartDependentRunnableVersion(dependencies: Iterable<SmartVersion>, runnable: Runnable) : SmartDependentVersion(dependencies) {
    constructor(dependency: SmartVersion, runnable: Runnable) : this(listOf(dependency), runnable)

    protected var myRunnable = runnable

    init {
        onSuperInit()
    }

    override fun onInit() {
    }

    override fun onNextVersion() {
        SmartVersionManager.groupedUpdate(Runnable {
            super.onNextVersion()
            myRunnable.run()
        })
    }
}

/**
 *  will reflect the frozen version as it was at time of instance creation
 */
open class SmartSnapshotVersion(dependencies: Iterable<SmartVersion>) : SmartDependentVersion(dependencies) {
    constructor(dependency: SmartVersion) : this(listOf(dependency))

    init {
        onSuperInitNext()
    }

    open fun onSuperInitNext() {
        super.onNextVersion()
    }

    override fun onSuperInit() {
    }

    // we never update, we are a snapshot
    override fun onNextVersion() {
    }

    override fun nextVersion() {
    }
}

/**
 *  will auto update its version on isStale, versionSerial or snapshot access as needed
 */
open class SmartUpdatingVersion(dependencies: Iterable<SmartVersion>) : SmartDependentVersion(dependencies) {
    constructor(dependency: SmartVersion) : this(listOf(dependency))

    val freshSnapshot: VersionSnapshot get() {
        if (isStaleRaw) {
            onNextVersion()
        }
        return mySnapshot
    }

    override val versionSerial: Int get() = freshSnapshot.dependenciesSerial

    override val isStale: Boolean get() {
        freshSnapshot
        return false
    }

}

/**
 *  will auto update its version on isStale, versionSerial or snapshot access and invoke runnable.run() when an update was necessary
 */
open class SmartUpdatingRunnableVersion(dependencies: Iterable<SmartVersion>, runnable: Runnable) : SmartUpdatingVersion(dependencies) {
    constructor(dependency: SmartVersion, runnable: Runnable) : this(listOf(dependency), runnable)

    protected var myRunnable = runnable

    init {
        super.onSuperInit()
    }

    override fun onInit() {
    }

    override fun onNextVersion() {
        SmartVersionManager.groupedUpdate(Runnable {
            super.onNextVersion()
            myRunnable.run()
        })
    }
}

open class SmartDependentVersionHolder(dependencies: Iterable<SmartVersionedData>) : SmartDependentVersion(SmartVersionedDataIterableAdapter(dependencies)) {
    constructor(dependency: SmartVersionedData) : this(listOf(dependency))
}

open class SmartDependentRunnableVersionHolder(dependencies: Iterable<SmartVersionedData>, runnable: Runnable) : SmartDependentRunnableVersion(SmartVersionedDataIterableAdapter(dependencies), runnable) {
    constructor(dependency: SmartVersionedData, runnable: Runnable) : this(listOf(dependency), runnable)
}

open class SmartSnapshotVersionHolder(dependencies: Iterable<SmartVersionedData>) : SmartSnapshotVersion(SmartVersionedDataIterableAdapter(dependencies)) {
    constructor(dependency: SmartVersionedData) : this(listOf(dependency))
}

open class SmartUpdatingVersionHolder(dependencies: Iterable<SmartVersionedData>) : SmartUpdatingVersion(SmartVersionedDataIterableAdapter(dependencies)) {
    constructor(dependency: SmartVersionedData) : this(listOf(dependency))
}

open class SmartUpdatingRunnableVersionHolder(dependencies: Iterable<SmartVersionedData>, runnable: Runnable) : SmartUpdatingRunnableVersion(SmartVersionedDataIterableAdapter(dependencies), runnable) {
    constructor(dependency: SmartVersionedData, runnable: Runnable) : this(listOf(dependency), runnable)
}

/**
 * Versioned Data Classes
 */

data class DataSnapshot<V>(override val serial: Int, val value: V) : SerialHolder

interface DataSnapshotHolder<V> {
    var dataSnapshot: DataSnapshot<V>
}

open class SmartImmutableData<V>(name: String, value: V) : SmartVersionedDataHolder<V> {
    constructor(value: V) : this("<unnamed>", value)

    final override val versionSerial: Int get() = NULL_SERIAL
    final override val isStale: Boolean get() = false
    final override val isMutable: Boolean get() = false
    override val dependencies: Iterable<SmartVersion> get() = EMPTY_DEPENDENCIES

    final override fun nextVersion() {
    }

    val myName = name

    protected val myValue = value
    override fun getValue(): V = myValue
    override var dataSnapshot: DataSnapshot<V>
        get() = DataSnapshot(NULL_SERIAL, myValue)
        set(value) {
            throw UnsupportedOperationException()
        }

    override fun toString(): String {
        return "$myName:(version: ${NULL_SERIAL}, value: $value)"
    }
}

open class SmartVolatileData<V>(name: String, value: V) : SmartVolatileVersion(), SmartVersionedVolatileDataHolder<V> {
    constructor(value: V) : this("<unnamed>", value)

    val myName = name
    protected var myValue: DataSnapshot<V> = DataSnapshot(myVersion, value)

    override fun getValue(): V = myValue.value

    override fun setValue(value: V) {
        if (myValue.value != value) {
            nextVersion()
            SmartVersionManager.freshenSnapshot(this, DataSnapshot(versionSerial, value))
        }
    }

    override var dataSnapshot: DataSnapshot<V>
        get() = myValue
        set(value) {
            myValue = value
        }

    override fun toString(): String {
        return "$myName:(version: $myVersion, value: $value)"
    }
}

open class SmartSnapshotData<V>(name: String, dependency: SmartVersionedDataHolder<V>) : SmartSnapshotVersion(dependency), SmartVersionedDataHolder<V> {
    constructor(dependency: SmartVersionedDataHolder<V>) : this("<unnamed>", dependency)

    val myName = name
    protected val myDependency = dependency
    protected val myValue = myDependency.value

    override fun onInit() {
    }

    init {
        onVolatileInit()
    }

    override var dataSnapshot: DataSnapshot<V>
        get() = DataSnapshot(mySnapshot.dependenciesSerial, myValue)
        set(value) {
            throw UnsupportedOperationException()
        }

    protected fun onVolatileInit() {
        super.onSuperInitNext()
    }

    override fun getValue(): V = myValue

    override fun toString(): String {
        return "$myName:(version: ${mySnapshot.dependenciesSerial}, value: $value)"
    }
}

open class SmartComputedData<V>(name: String, computable: DataComputable<V>) : SmartVolatileVersion(), SmartVersionedDataHolder<V> {
    constructor(name: String, computable: () -> V) : this(name, DataComputable { computable() })

    constructor(computable: DataComputable<V>) : this("<unnamed>", computable)

    constructor(computable: () -> V) : this(DataComputable { computable() })

    val myName = name
    protected var myComputable = computable
    protected var myValue = DataSnapshot(myVersion, onCompute())

    open protected fun onCompute(): V {
        return SmartVersionManager.groupedCompute(DataComputable {
            nextVersion()
            SmartVersionManager.freshenSnapshot(this, DataSnapshot(myVersion, myComputable.compute()))
            myValue.value
        })
    }

    override var dataSnapshot: DataSnapshot<V>
        get() = myValue
        set(value) {
            myValue = value
        }

    override fun getValue(): V {
        return myValue.value
    }

    open fun updateValue() {
        onCompute()
    }

    override fun toString(): String {
        return "$myName:(version: $myVersion, value: $value)"
    }
}

open class SmartDependentData<V>(name: String, dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: DataComputable<V>) :
        SmartDependentVersion(dependencies), SmartVersionedDataHolder<V> {
    constructor(dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: DataComputable<V>) : this("<unnamed>", dependencies, computable)

    constructor(name: String, dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: () -> V) : this(name, dependencies, DataComputable { computable() })

    constructor(dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: () -> V) : this(dependencies, DataComputable { computable() })

    constructor(name: String, dependency: SmartVersionedDataHolder<*>, computable: DataComputable<V>) : this(name, listOf(dependency), computable)

    constructor(name: String, dependency: SmartVersionedDataHolder<V>) : this(name, listOf(dependency), DataComputable { dependency.value })

    constructor(dependency: SmartVersionedDataHolder<*>, computable: DataComputable<V>) : this(listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<V>) : this(listOf(dependency), DataComputable { dependency.value })

    constructor(name: String, dependency: SmartVersionedDataHolder<*>, computable: () -> V) : this(name, listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<*>, computable: () -> V) : this(listOf(dependency), computable)

    val myName = name
    protected var myComputable: DataComputable<V>? = computable
    protected var myValue: DataSnapshot<V> = onCompute()

    override fun onNextVersion() {
        SmartVersionManager.groupedUpdate(Runnable {
            val computable = myComputable
            if (computable != null) {
                super.onNextVersion()
                SmartVersionManager.freshenSnapshot(this, DataSnapshot(mySnapshot.dependenciesSerial, computable.compute()))
            }
        })
    }

    override var dataSnapshot: DataSnapshot<V>
        get() = myValue
        set(value) {
            myValue = value
        }

    open protected fun onCompute(): DataSnapshot<V> {
        return SmartVersionManager.groupedCompute(DataComputable {
            onNextVersion()
            myValue
        })
    }

    override fun getValue(): V {
        return myValue.value
    }


    open fun updateValue() {
        nextVersion()
    }

    override fun toString(): String {
        return "$myName:(version: ${mySnapshot.dependenciesSerial}, value: $value)"
    }
}

open class SmartUpdatingData<V>(name: String, dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: DataComputable<V>) :
        SmartDependentData<V>(name, dependencies, computable), SmartVersionedDataHolder<V> {
    constructor(dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: DataComputable<V>) : this("<unnamed>", dependencies, computable)

    constructor(name: String, dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: () -> V) : this(name, dependencies, DataComputable { computable() })

    constructor(dependencies: Iterable<SmartVersionedDataHolder<*>>, computable: () -> V) : this(dependencies, DataComputable { computable() })

    constructor(name: String, dependency: SmartVersionedDataHolder<*>, computable: DataComputable<V>) : this(name, listOf(dependency), computable)

    constructor(name: String, dependency: SmartVersionedDataHolder<V>) : this(name, listOf(dependency), DataComputable { dependency.value })

    constructor(dependency: SmartVersionedDataHolder<*>, computable: DataComputable<V>) : this(listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<V>) : this(listOf(dependency), DataComputable { dependency.value })

    constructor(name: String, dependency: SmartVersionedDataHolder<*>, computable: () -> V) : this(name, listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<*>, computable: () -> V) : this(listOf(dependency), computable)

    val freshSnapshot: VersionSnapshot get() {
        if (isStaleRaw) {
            onNextVersion()
        }
        return mySnapshot
    }

    override val versionSerial: Int get() = freshSnapshot.dependenciesSerial

    override val isStale: Boolean get() {
        freshSnapshot
        return false
    }

    override fun getValue(): V {
        nextVersion()
        return myValue.value
    }
}

open class SmartUpdateIterableData<V>(name: String, dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: IterableDataComputable<V>) :
        SmartDependentVersion(dependencies),
        SmartVersionedDataHolder<V> {
    constructor(dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: IterableDataComputable<V>) : this("<unnamed>", dependencies, computable)

    constructor(name: String, dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: (Iterable<V>) -> V) : this(name, dependencies, IterableDataComputable { computable(it) })

    constructor(dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: (Iterable<V>) -> V) : this(dependencies, IterableDataComputable { computable(it) })

    constructor(name: String, dependency: SmartVersionedDataHolder<V>, computable: IterableDataComputable<V>) : this(name, listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<V>, computable: IterableDataComputable<V>) : this(listOf(dependency), computable)

    constructor(name: String, dependency: SmartVersionedDataHolder<V>, computable: (Iterable<V>) -> V) : this(name, listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<V>, computable: (Iterable<V>) -> V) : this(listOf(dependency), computable)

    val myName = name
    protected var myComputable: IterableDataComputable<V>? = computable
    protected var myValue = onCompute()

    override fun onNextVersion() {
        SmartVersionManager.groupedUpdate(Runnable {
            val computable = myComputable
            if (computable != null) {
                super.onNextVersion()
                SmartVersionManager.freshenSnapshot(this, DataSnapshot(mySnapshot.dependenciesSerial, computable.compute(valueDependencies)))
            }
        })
    }

    val valueDependencies: Iterable<V>
        get() = IterableValueDependenciesAdapter(super.dependencies as Iterable<SmartVersionedDataHolder<V>>)

    open protected fun onCompute(): DataSnapshot<V> {
        return SmartVersionManager.groupedCompute(DataComputable {
            onNextVersion()
            myValue
        })
    }

    override var dataSnapshot: DataSnapshot<V>
        get() = myValue
        set(value) {
            myValue = value
        }

    override fun getValue(): V {
        return myValue.value
    }

    open fun updateValue() {
        nextVersion()
    }

    override fun toString(): String {
        return "$myName:(version: ${mySnapshot.dependenciesSerial}, value: $value)"
    }
}

open class SmartIterableData<V>(name: String, dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: IterableDataComputable<V>) :
        SmartUpdateIterableData<V>(name, dependencies, computable), SmartVersionedDataHolder<V> {
    constructor(dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: IterableDataComputable<V>) : this("<unnamed>", dependencies, computable)

    constructor(name: String, dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: (Iterable<V>) -> V) : this(name, dependencies, IterableDataComputable { computable(it) })

    constructor(dependencies: Iterable<SmartVersionedDataHolder<V>>, computable: (Iterable<V>) -> V) : this(dependencies, IterableDataComputable { computable(it) })

    constructor(name: String, dependency: SmartVersionedDataHolder<V>, computable: IterableDataComputable<V>) : this(name, listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<V>, computable: IterableDataComputable<V>) : this(listOf(dependency), computable)

    constructor(name: String, dependency: SmartVersionedDataHolder<V>, computable: (Iterable<V>) -> V) : this(name, listOf(dependency), computable)

    constructor(dependency: SmartVersionedDataHolder<V>, computable: (Iterable<V>) -> V) : this(listOf(dependency), computable)

    val freshSnapshot: VersionSnapshot get() {
        if (isStaleRaw) {
            onNextVersion()
        }
        return mySnapshot
    }

    override val versionSerial: Int get() = freshSnapshot.dependenciesSerial

    override val isStale: Boolean get() {
        freshSnapshot
        return false
    }

    override fun getValue(): V {
        nextVersion()
        return myValue.value
    }
}

class LatestIterableDataComputable<V>(val dependencies: Iterable<SmartVersionedDataHolder<V>>) : DataComputable<DataSnapshot<V>> {
    override fun compute(): DataSnapshot<V> {
        val iterator = dependencies.iterator()
        var latestDataSnapshot: DataSnapshot<V> = iterator.next().dataSnapshot
        for (data in iterator) {
            if (latestDataSnapshot.serial < data.dataSnapshot.serial) {
                latestDataSnapshot = data.dataSnapshot
            }
        }
        return latestDataSnapshot
    }
}

open class SmartLatestDependentData<V>(name: String, dependencies: Iterable<SmartVersionedDataHolder<V>>, runnable: Runnable?) : SmartUpdatingVersion(dependencies), SmartVersionedDataHolder<V> {
    constructor(name: String, dependencies: Iterable<SmartVersionedDataHolder<V>>) : this(name, dependencies, null)

    constructor(dependencies: Iterable<SmartVersionedDataHolder<V>>, runnable: Runnable) : this("<unnamed>", dependencies, runnable)

    constructor(dependencies: Iterable<SmartVersionedDataHolder<V>>) : this("<unnamed>", dependencies, null)

    constructor(name: String, dependencies: Iterable<SmartVersionedDataHolder<V>>, runnable: () -> Unit) : this(name, dependencies, Runnable { runnable() })

    constructor(dependencies: Iterable<SmartVersionedDataHolder<V>>, runnable: () -> Unit) : this("<unnamed>", dependencies, Runnable { runnable() })

    val myName = name
    protected val myRunnable = runnable
    protected val myComputable = LatestIterableDataComputable(dependencies)
    protected var myValue = onCompute()

    override fun onNextVersion() {
        SmartVersionManager.groupedUpdate(Runnable {
            val computable = myComputable
            @Suppress("SENSELESS_COMPARISON")
            if (computable != null) {
                super.onNextVersion()
                val runnable = myRunnable
                if (SmartVersionManager.freshenSnapshot(this, computable.compute()) && runnable != null) {
                    runnable.run()
                }
            }
        })
    }

    open protected fun onCompute(): DataSnapshot<V> {
        return SmartVersionManager.groupedCompute(DataComputable {
            onNextVersion()
            myValue
        })
    }

    override var dataSnapshot: DataSnapshot<V>
        get() = myValue
        set(value) {
            myValue = value
        }

    override fun getValue(): V {
        return myValue.value
    }

    open fun updateValue() {
        nextVersion()
    }

    override fun toString(): String {
        return "$myName:(version: ${mySnapshot.dependenciesSerial}, value: $value)"
    }
}

open class SmartVersionedDataAlias<V>(aliased: SmartVersionedDataHolder<V>) : SmartVersionedDataHolder<V>, SmartVersionedVolatileDataHolder<V> {
    protected var myVersion: Int = SmartVersionManager.nextVersion

    // unwrap nested alias references
    protected var myAliased = if (aliased is SmartVersionedDataAlias<V>) aliased.alias else aliased
    protected var myInRecursion = false
    protected var myLastDataSnapshot: DataSnapshot<V> = myAliased.dataSnapshot

    protected fun updateDataSnapshot() {
        myLastDataSnapshot = myAliased.dataSnapshot
    }

    var alias: SmartVersionedDataHolder<V>
        get() = myAliased
        set(value) {
            myAliased = value
            updateDataSnapshot()
            touchVersionSerial()
        }

    fun touchVersionSerial() {
        myVersion = SmartVersionManager.nextVersion
    }

    override val versionSerial: Int
        get() {
            if (myInRecursion) return myVersion.max(myLastDataSnapshot.serial)
            else {
                myInRecursion = true
                try {
                    val serial = myAliased.versionSerial
                    updateDataSnapshot()
                    return myVersion.max(serial)
                } finally {
                    myInRecursion = false
                }
            }
        }

    override val isStale: Boolean
        get() {
            if (myInRecursion) return false
            else {
                myInRecursion = true
                try {
                    val stale = myAliased.isStale
                    updateDataSnapshot()
                    return stale
                } finally {
                    myInRecursion = false
                }
            }
        }

    override val isMutable: Boolean
        get() = true

    override val dependencies: Iterable<SmartVersion>
        get() = myAliased.dependencies

    override fun nextVersion() {
        if (!myInRecursion) {
            myInRecursion = true
            try {
                myAliased.nextVersion()
                updateDataSnapshot()
            } finally {
                myInRecursion = false
            }
        }
    }

    override var dataSnapshot: DataSnapshot<V>
        get() {
            updateDataSnapshot()
            return myLastDataSnapshot
        }
        set(value) {
            throw UnsupportedOperationException()
        }

    override fun getValue(): V {
        if (myInRecursion) return myLastDataSnapshot.value
        else {
            myInRecursion = true
            try {
                updateDataSnapshot()
                return myAliased.value
            } finally {
                myInRecursion = false
            }
        }
    }

    override fun setValue(value: V) {
        val versionedDataHolder = myAliased
        if (versionedDataHolder is SmartVersionedVolatileDataHolder<V>) {
            versionedDataHolder.value = value
            updateDataSnapshot()
        } else {
            throw UnsupportedOperationException()
        }
    }
}
