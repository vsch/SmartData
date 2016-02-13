SmartData Library
=================
       
A collection of smart data elements in Kotlin that are usable in java programs. This is a very early version in constant development. I am using it to implement Markdown formatting in my idea-multimarkdown plugin and make changes as the need arises.

The library consists of several inter-operating smart data classes:

1. SmartVersion: a set of classes implementing versions based on a single integer serial number. The serial number is globally valid across classes and threads. So a `anInstance.versionSerial` > `anotherClass.versionSerial` is more recent. Versions are considered stale if any of their dependents have a higher `versionSerial` number than they do. A version that has dependents will have its `versionSerial` = max(versionSerial of its dependents)

    All versioning is managed by a SmartVersionManager that provides currentSerial and nextSerial values. Additionally, it is possible to freeze the serial number for group updates. This freezing occurs on a per-thread basis and all calls to SmartVersionManager.nextSerial will return the same value.
    
2. SmartVersionedData: a set of classes that hold arbitrary non-null data and automatically track its version serial. All determination of when a dependent data variable needs to be updated are done on version serial numbers and not on actual data values. 

    These classes decouple data providers and data consumers since neither one has any effect on the other. Both simply work with variables that wrap values of interest to them at the time of their choosing.
    
    They also provide connecting properties so that the value of a property of a class can be controlled by another smart data value. Neither the controlled class nor the controller need to be aware of the connection between their properties.
    
3.  SmartCharSequences: a set of classes that implement CharSequence but have the following additional properties:
    - allow tracking of offset into original sequence or character array even when heavily manipulated though various editing calls 
    - allow concatenation, insertion and deletion of regions, splicing together of sequences, etc. all without performing copies of the data
    - some have variable content that allows their value to change depending on settings. For example:
        - `SmartVariableCharSequence` will allow you to left, right, center justify another sequence with optional prefix, suffix and left/right padding characters.
        -  `SmartTextReflowCharSequence` will allow you to reflow another sequence to margins with first line indent, left margin and right margin properties. Simply set these properties and read the result from the sequence.
        
    - all variable content sequences work with SmartVersions and SmartVersionedData 
    
4. SmartDataScopes: a set of classes that work like a switchboard for interconnecting data providers, data consumers with optional computations performed in between. These are used with versioned data and smart sequences to implement complex formatting without having to concern yourself with complexity of dynamically routing data, figuring out data dependencies or dynamic variable count. 

    For example, the meat of a markdown table formatting sequence is implemented in less than 75 lines which includes parsing of the original text, determining the column alignments and dynamic formatting options that can be changed to change the sequence content, after it has been created. 

    The result takes:          
    
    ```markdown
    Header 0|Header 1|Header 2|Header 3
     --------|:-------- |:--------:|-------:
    Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
    Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
    ```
    
    and returns a sequence whose content will be:
    
    ```markdown
    | Header 0                      | Header 1              |          Header 2           |              Header 3 |
    | :---------------------------- | :-------------------- | :-------------------------: | --------------------: |
    | Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
    | Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |
    ```
    
### Smart Versioned Data     

- `SmartImmutableData<V>`: immutable value of V, never changes, equivalent to a constant
- `SmartVolatileData<V>`:  volatile value of V, updated via `instance.value = aValue`
- `SmartSnapshotData<V>`: immutable copy of a changing value, can us `instance.isStale` to test whether the copy is out of date 
- `SmartComputedData<V>`: computed value, update via `instance.nextVersion()`
- `SmartUpdateDependentData<V>`: computed value that is based on 1..n other versioned data instances. Test via `instance.isStale` and use `instance.nextVersion()` to update
- `SmartDependentData<V>`: computed value same as SmartDependentData, except automatically updates on access to any of its properties if dependent data changed since last update, otherwise returns previously computed value 
- `SmartUpdateIterableData<V>`: computed value based on iteration over dependent data, Test via `instance.isStale` and use `instance.nextVersion()`
- `SmartIterableData<V>`: computed based on iteration over dependent data, except automatically updates on access to any of its properties if dependent data changed since last update, otherwise returns previously computed value
- `SmartLatestDependentData<V>`: computed value based on the value of the dependent that has the greatest `versionSerial`. In the event more than one dependent has the same serial which is greatest, then the value of the first one is returned. 
- `SmartVersionedDataAlias<V>`: a versioned value that holds another versioned value, returned versionSerial will be the greater of versionSerial of the contained value or the value returned by SmartVersionManager.nextSerial at the time when the alias was set. This allows versioned aliases to be switched causing any other versioned values that depend on their values to be updated, similarly if the contained value is updated it too will cause dependent updates.  
        
For example the following code will create a computed value that is always the sum of its dependents:
   
```kotlin
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
```

Will output the following. Note the first computation occurs when the `sum` variable is first created to compute its initial value and version. Thereafter it is only recomputed when the values it depends on have changed and only on access to the dependent value property.

```text
computed sum: 0
v1: 0, v2: 0, v3: 0
sum: 0
sum: 0
v1: 10, v2: 0, v3: 0
computed sum: 10
sum: 10
sum: 10
v1: 10, v2: 20, v3: 0
computed sum: 30
sum: 30
sum: 30
v1: 10, v2: 20, v3: 30
computed sum: 60
sum: 60
sum: 60
v1: 100, v2: 200, v3: 300
computed sum: 600
sum: 600
sum: 600
```

The following will create a computed value that is always a product of its dependents and outputs a string every time the product is recomputed:

```kotlin
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
```

Will output:

```text
Prod of 1 1 1 is 1
Prod of 10 1 1 is 10
Prod of 10 20 1 is 200
Prod of 10 20 30 is 6000
Prod of 100 200 300 is 6000000
```

### Smart Character Sequences

These are CharSequences that have extra characteristics like ability to keep track of offset into original CharSequence even after being chopped up with subSequence() and spliced with append()

Some are dynamic and will return a different result if their properties change. They also support SmartVersionedData and SmartVersionedProperties allowing their properties to be taken from data points "connected" to their properties. 

The example below shows this at work:

```kotlin
    fun test_VariableSequence() {
        val columns = SmartCharArraySequence("Column1|Column2|Column3".toCharArray())

        // split into pieces
        val splitColumns = columns.splitParts('|', includeDelimiter = false)
        val col1 = SmartVariableCharSequence(splitColumns[0])
        val col2 = SmartVariableCharSequence(splitColumns[1])
        val col3 = SmartVariableCharSequence(splitColumns[2])

        val delimiter = SmartCharArraySequence("|".toCharArray())

        // splice them into a single sequence
        val formattedColumns = SmartSegmentedCharSequence(delimiter, col1, delimiter, col2, delimiter, col3,delimiter)

        // connect width and alignment of column 2 and 3 to corresponding properties of column 1
        col2.widthDataPoint = col1.widthDataPoint
        col3.widthDataPoint = col1.widthDataPoint
        col2.alignmentDataPoint = col1.alignmentDataPoint
        col3.alignmentDataPoint = col1.alignmentDataPoint

        // output formatted sequence, all columns follow the setting of column 1 
        println("Unformatted Columns: $formattedColumns\n")
        col1.width = 15
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.CENTER
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.RIGHT
        println("Formatted Columns: $formattedColumns\n")
    }
```

Will output:

```text
Unformatted Columns: |Column1|Column2|Column3|

Formatted Columns: |Column1        |Column2        |Column3        |

Formatted Columns: |    Column1    |    Column2    |    Column3    |

Formatted Columns: |        Column1|        Column2|        Column3|

```

Note that it is the same sequence instance but its content is changed to reflect property changes of its contained sub-sequences. 

This allows the creation of self-formatting smart sequences what expose formatting options to vary the formatting style. To prevent the sequence from being modified simply get `cachedProxy` sequence from any of the `SmartCharSequence` implementations which will give you a SmartCharArraySequence that is immutable, but you can always test its version's isStale property to see if the original has changed.    

Here we change the printing lines of the code above to take snapshots of the sequence after each modification: 

```kotlin
    fun test_VariableSequenceSnapshot() {
        // ..... identical lines skipped 
        
        // output formatted sequence, all columns follow the setting of column 1
        println("Unformatted Columns: $formattedColumns\n")
        col1.width = 15
        val cashedProxyLeft15 = formattedColumns.cachedProxy
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.CENTER
        val cashedProxyCenter15 = formattedColumns.cachedProxy
        println("Formatted Columns: $formattedColumns\n")
        col1.alignment = TextAlignment.RIGHT
        val cashedProxyRight15 = formattedColumns.cachedProxy
        println("Formatted Columns: $formattedColumns\n")
        
        println("cachedProxyLeft15 isStale(${cashedProxyLeft15.version.isStale}): $cashedProxyLeft15\n")
        println("cachedProxyCenter15 isStale(${cashedProxyCenter15.version.isStale}): $cashedProxyCenter15\n")
        println("cachedProxyRight15 isStale(${cashedProxyRight15.version.isStale}): $cashedProxyRight15\n")
    }    
```

And the output shows that the cachedProxy is unchanged but the isStale property reflects that only the `cachedProxyRight15` instance is an accurate representation of the original dynamic sequence.

```text
Unformatted Columns: |Column1|Column2|Column3|

Formatted Columns: |Column1        |Column2        |Column3        |

Formatted Columns: |    Column1    |    Column2    |    Column3    |

Formatted Columns: |        Column1|        Column2|        Column3|

cachedProxyLeft15 isStale(true): |Column1        |Column2        |Column3        |

cachedProxyCenter15 isStale(true): |    Column1    |    Column2    |    Column3    |

cachedProxyRight15 isStale(false): |        Column1|        Column2|        Column3|

```

### Smart Data Scopes

A data scope is a SmartVersionedData container where each value is identified by a key: `SmartDataKey<V:Any>` which defines the data type, default value and optionally how the value is computed and over what related scopes: ANCESTORS, PARENT, SELF, CHILDREN, DESCENDANTS. Each data point represents N values indexed by an arbitrary integer.

This is easiest to show with an example:

```kotlin
    fun test_DataScopes() {
        val WIDTH = SmartVolatileDataKey("WIDTH", 0)
        val MAX_WIDTH = SmartAggregatedDataKey("MAX_WIDTH", 0, WIDTH, setOf(SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() })
        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)
        val COLUMN_ALIGNMENT = SmartDependentDataKey("COLUMN_ALIGNMENT", TextColumnAlignment.NULL_VALUE, listOf(ALIGNMENT, MAX_WIDTH), SmartScopes.SELF, { TextColumnAlignment(WIDTH.value(it), ALIGNMENT.value(it)) })

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
```

will output:

```text
MAX_WIDTH: 20
MAX_WIDTH: 17
MAX_WIDTH: 15
```

The real benefit of all of these smart classes is when you put them together to create a self formatting sequence. Of course this can be done without smart data or smart sequences but you have to keep track of a lot of plumbing, especially when the logic to generate the sequence and the logic to figure out the final format are widely separated in space and time. These classes handle the plumbing. Here is piece of code that will parse and format a Markdown table:

```kotlin
    fun formatTable() {
        // width of text in the column, without formatting
        val COLUMN_WIDTH = SmartVolatileDataKey("COLUMN_WIDTH", 0)
        
        // alignment of the column
        val ALIGNMENT = SmartVolatileDataKey("ALIGNMENT", TextAlignment.LEFT)

        // max column width across all rows in the table
        val MAX_COLUMN_WIDTH = SmartAggregatedDataKey("MAX_COLUMN_WIDTH", 0, COLUMN_WIDTH, setOf(SmartScopes.RESULT_TOP, SmartScopes.SELF, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS), IterableDataComputable { it.max() })
        
        // alignment of each in the top data scope, a copy of one provided by header row 
        val COLUMN_ALIGNMENT = SmartLatestDataKey("COLUMN_ALIGNMENT", TextAlignment.LEFT, ALIGNMENT, setOf(SmartScopes.RESULT_TOP, SmartScopes.CHILDREN, SmartScopes.DESCENDANTS))

        val tableDataScope = SmartDataScopeManager.createDataScope("tableDataScope")
        var formattedTable = EditableCharSequence()

        val table = SmartCharArraySequence("""Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data
""".toCharArray())

        val tableRows = table.splitPartsSegmented('\n', false)

        var row = 0
        for (line in tableRows.segments) {
            var formattedRow = EditableCharSequence()
            val rowDataScope = tableDataScope.createDataScope("row:$row")
            var col = 0
            for (column in line.splitPartsSegmented('|', false).segments) {
                val headerParts = column.extractGroupsSegmented("(\\s+)?(:)?(-{1,})(:)?(\\s+)?")
                val formattedCol = SmartVariableCharSequence(column, if (headerParts != null) EMPTY_SEQUENCE else column)
                
                // treatment of discretionary left align marker `:` in header. 1 to always add, 0 to leave as is, any other value to remove it 
                val discretionary = 1 

                if (headerParts != null) {
                    val haveLeft = headerParts.segments[2] != NULL_SEQUENCE
                    val haveRight = headerParts.segments[4] != NULL_SEQUENCE

                    formattedCol.leftPadChar = '-'
                    formattedCol.rightPadChar = '-'
                    when {
                        haveLeft && haveRight -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.CENTER
                            formattedCol.prefix = ":"
                            formattedCol.suffix = ":"
                        }
                        haveRight -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.RIGHT
                            formattedCol.suffix = ":"
                        }
                        else -> {
                            ALIGNMENT[rowDataScope, col] = TextAlignment.LEFT
                            if (discretionary == 1 || discretionary == 0 && haveLeft) formattedCol.prefix = ":"
                        }
                    }
                }

                if (col > 0) formattedRow.append(" | ")
                formattedRow.append(formattedCol)
                rowDataScope[COLUMN_WIDTH, col] = formattedCol.lengthDataPoint
                formattedCol.alignmentDataPoint = COLUMN_ALIGNMENT.dataPoint(tableDataScope, col)
                formattedCol.widthDataPoint = MAX_COLUMN_WIDTH.dataPoint(tableDataScope, col)
                col++
            }

            formattedTable.append("| ", formattedRow, " |\n")
            row++
        }

        tableDataScope.finalizeAllScopes()
        println("Unformatted Table\n$table\n")
        println("Formatted Table\n$formattedTable\n")
    }
```

The outputs shows the unformatted and formatted Markdown table:

```text
Unformatted Table
Header 0|Header 1|Header 2|Header 3
 --------|:-------- |:--------:|-------:
Row 1 Col 0 Data|Row 1 Col 1 Data|Row 1 Col 2 More Data|Row 1 Col 3 Much Data
Row 2 Col 0 Default Alignment|Row 2 Col 1 More Data|Row 2 Col 2 a lot more Data|Row 2 Col 3 Data


Formatted Table
| Header 0                      | Header 1              |          Header 2           |              Header 3 |
| :---------------------------- | :-------------------- | :-------------------------: | --------------------: |
| Row 1 Col 0 Data              | Row 1 Col 1 Data      |    Row 1 Col 2 More Data    | Row 1 Col 3 Much Data |
| Row 2 Col 0 Default Alignment | Row 2 Col 1 More Data | Row 2 Col 2 a lot more Data |      Row 2 Col 3 Data |

```
