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

public interface SafeCharSequence extends CharSequence, SafeCharSequenceError {
    @Override
    @NotNull
    SafeCharSequence subSequence(int start, int end);

    /**
     * @return the underlying non-safe character sequence
     */
    @NotNull
    CharSequence getCharSequence();

    /**
     * @return first/last characters of the sequence or the beforeStartNonChar/afterEndNonChar if the sequence is empty
     */
    char getFirstChar();
    char getLastChar();

    /**
     * @return true if the corresponding sequence is empty/blank correspondingly.
     */
    boolean isEmpty();
    boolean isBlank();

    /**
     * @return character values used when index is before the 0 index or >= length of the sequence.
     *
     * Default value for both of these properties is '\u0000' you can set them to any character value and
     * any access to charAt(index) with index outside the valid range will return the corresponding character and increment
     * the safeErrors property.
     *
     */
    char getBeforeStartNonChar();
    void setBeforeStartNonChar(char value);
    char getAfterEndNonChar();
    void setAfterEndNonChar(char value);

    /**
     * @param index
     * @return a valid index 0..length range, if passed parameter is outside this range then safeErrors will be incremented
     */
    int safeIndex(int index);

    /**
     * @param index
     * @return a valid index 0..length-1 range, if passed parameter is outside this range then safeErrors will be incremented
     */
    int safeInclusiveIndex(int index);

    /**
     * @param startIndex    start index of the range 0..length
     * @param endIndex      end index of the range 0..length
     * @return              a valid range (even if it has to be empty) whose properties maintain 0 <= start <= end <= length relationship
     *                      if passed in parameters do meet these conditions safeErrors is incremented and a valid range is returned.
     */
    @NotNull
    Range safeRange(int startIndex, int endIndex);

}
