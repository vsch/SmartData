### 0.0.3 - Bug Fixes

- fix recursion in SmartCharSequenceWrapper on getCharsImpl(), add test for this
- add SafeCharSequence class for working with char sequences without exceptions and with extra information

### 0.0.2 - SmartVersion/SmartVersionedData Optimizations

- change smart versions never self update, all require nextVersion() call to update
- data updates on value getter only, version data is not modified until getValue() is called, gives more control and no unnecessary updates on upstream tests for version and stale condition
- added isStale stickiness so that once a version is determined to be stale based on dependencies, it will remain so until nextVersion() is used to update it. Eliminates unnecessary scanning of dependencies after the first determination of staleness.
- add latest version/data optimization to take value from VersionSnapshot. Eliminates double scanning of dependencies.
- add `SmartVersionedProperty<V>` which behaves like `SmartVolatileData<V>` but is connectable to another `SmartVersionedDataHolder<V>` via the connect(aliased) method. The getValue() returns the latest of setValue() and version provided connector. Used to have properties that can be programmatically set and externally provided, whichever is fresher. 
- add `SmartVersionedPropertyArray<V>` which is a property and an array of properties. 
    - read of the main property returns either the last written value or the value aggregated via the user defined aggregate function of the properties in the array, depending on which is fresher.
    - read of a property in the array returns either the last written value or the main value distributed via the user defined distribute function, depending on which is fresher.    

### 0.0.1 - Initial Commit
