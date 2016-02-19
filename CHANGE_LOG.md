### 0.0.8 - Optimization

- add optimization of SmartSegmentedCharSequence for sequential access of indices either forward or backward
- change tableFormatter now returns a cachedProxy SmartCharArrayCharSequence not the SmartSegmentedCharSequence
- fix source tracking location not being properly adjusted by wrapper and a few other classes
- add firstWidthOffset to SmartParagraphCharSequence to provide line width delta for the first line of the paragraph. Now can control how the paragraph is reformatted to fit its destination without postprocessing when the paragraph starts from a lead in prefix that is not part of the reformat operation. ie. Markdown list item, footnote definition, etc. 

### 0.0.7 - Bug Fixes

- add `MarkdownTableFormatter` and `MarkdownTableFormatSettings`

### 0.0.6 - Bug Fixes

- add `SmartTableColumnBalancer` which handles optimizing column widths with or without spanning columns present in the table.

### 0.0.5 - Bug Fixes

- change respectHardBreaks to keepMarkdownHardBreaks.
- add keepLineBreaks property to keep line breaks of original text.
- add nextVersion() call to connect() and disconnect() of SmartVersionedPropertyArray, otherwise no distribution will be performed since version after connection is not updated until value is accessed and accessing distributed values relies on the main value version.

### 0.0.4 - Bug Fixes

- remove grouping of versions when computing new values. Prevents complex relationships from being properly recognized as stale
- add smart paragraph sequence that will reflow text to margins: has first line indent, indent, width and respectHardBreaks to respect markdown two spaces followed by end of line as a hard break.

### 0.0.3 - Bug Fixes

- fix recursion in SmartCharSequenceWrapper on getCharsImpl(), add test for this
- add SafeCharSequence and related classes for working with char sequences without exceptions with extra functionality to process text lines based around an index, with dynamic sub sequencing via SafeCharSequenceRange 

### 0.0.2 - SmartVersion/SmartVersionedData Optimizations

- change smart versions never self update, all require nextVersion() call to update
- data updates on value getter only, version data is not modified until getValue() is called, gives more control and no unnecessary updates on upstream tests for version and stale condition
- added isStale stickiness so that once a version is determined to be stale based on dependencies, it will remain so until nextVersion() is used to update it. Eliminates unnecessary scanning of dependencies after the first determination of staleness.
- add latest version/data optimization to take value from VersionSnapshot. Eliminates double scanning of dependencies.
- add `SmartVersionedProperty<V>` which behaves like `SmartVolatileData<V>` but is connectible to another `SmartVersionedDataHolder<V>` via the connect(aliased) method. The getValue() returns the latest of setValue() and version provided connector. Used to have properties that can be programmatically set and externally provided, whichever is fresher. 
- add `SmartVersionedPropertyArray<V>` which is a property and an array of properties. 
    - read of the main property returns either the last written value or the value aggregated via the user defined aggregate function of the properties in the array, depending on which is fresher.
    - read of a property in the array returns either the last written value or the main value distributed via the user defined distribute function, depending on which is fresher.    

### 0.0.1 - Initial Commit
