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

package com.vladsch.smart;

import org.jetbrains.annotations.NotNull;

public interface SafeCharSequenceRanger extends SafeCharSequence {
    @NotNull
    SafeCharSequenceError getCharSequenceError();

    int getStartIndex();
    void setStartIndex(int startIndex);

    int getEndIndex();
    void setEndIndex(int endIndex);

    /**
     * access the startIndex/endIndex limited sequence
     * @param start start index 0..length, the actual access to the original sequence will be offset by the current startIndex property value
     * @param end   end index 0..length, the actual access to the original sequence will be offset by the current startIndex property value
     * @return sub-sequence represented by start/end or empty. If the indices go outside the allowed range or are reversed then safeErrors property will be
     * incremented.
     */
    @NotNull
    @Override
    SafeCharSequenceRanger subSequence(int start, int end);

    /**
     * get the current sub sequence delimited by startIndex/endIndex properties
     * @return
     */
    @NotNull
    SafeCharSequenceRanger getSubSequence();

    /**
     * get the part of the main char sequence from 0..startIndex
     * @return
     */
    @NotNull
    SafeCharSequenceRanger getBeforeStart();

    /**
     * get the part of the main char sequence from endIndex..rawLength
     * @return
     */
    @NotNull
    SafeCharSequenceRanger getAfterEnd();

    /**
     * @param start  start index 0..rawLength
     * @param end    end index 0..rawLength
     * @return subSequence that can go outside the current startIndex/endIndex property settings
     */
    @NotNull
    SafeCharSequenceRanger rawSubSequence(int start, int end);

    /**
     * @return length of the underlying main sequence, 0..rawLength coordinates are used to limit the exposed
     * part of it by setting startIndex/endIndex properties.
     */
    int getRawLength();

    /*
     * safe raw index manipulation returned values will always be in the 0..rawLength range but if passed in value(s)
     * goes outside this range then safeErrors property will be incremented.
     */
    int safeRawIndex(int index);
    int safeRawInclusiveIndex(int index);
    @NotNull Range safeRawRange(int startIndex, int endIndex);
}
