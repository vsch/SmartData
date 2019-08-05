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

package com.vladsch.smart;

import org.jetbrains.annotations.NotNull;

public interface SafeCharSequenceIndexer extends SafeCharSequenceError {
    int getIndex();
    void setIndex(int index);

    char getChar();
    int getStartOfLine();
    int getEndOfLine();
    int getLastNonBlank();
    int getFirstNonBlank();
    boolean isBlankLine();
    boolean isEmptyLine();
    int getIndent();
    int getColumn();
    int columnOf(int index);
    int tabExpandedColumnOf(int index, int tabSize);
    int getEndOfPreviousLine();
    int getStartOfNextLine();
    int endOfPreviousSkipLines(int lines);
    int startOfNextSkipLines(int lines);

    @NotNull
    SafeCharSequence getBeforeIndexChars();
    @NotNull
    SafeCharSequence getAfterIndexChars();
    @NotNull
    SafeCharSequence getLineChars();
    @NotNull
    SafeCharSequence getStartOfLineToIndexChars();
    @NotNull
    SafeCharSequence getIndexToEndOfLineChars();
    @NotNull
    SafeCharSequence getFirstNonBlankToIndexChars();
    @NotNull
    SafeCharSequence getAfterIndexToLastNonBlankChars();
    int getAfterLastNonBlank();
    @NotNull
    SafeCharSequence getFirstToLastNonBlankLineChars();
    @NotNull
    SafeCharSequence getFirstNonBlankToEndOfLineChars();
    @NotNull
    SafeCharSequence getStartOfLineToLastNonBlankChars();
}
