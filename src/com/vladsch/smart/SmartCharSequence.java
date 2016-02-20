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

import com.intellij.util.text.CharSequenceBackedByArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface allowing:
 * <p>
 * 1. two way tracking between sequence offset to original source offset
 * <p>
 * 2. merging of neighbouring sub-sequences into one sequence over the same source data
 */
public interface SmartCharSequence extends CharSequence, TrackingCharSequenceMarkerHolder, CharSequenceBackedByArray, SmartVersionedData, SmartSourceLocationTracker, SmartCharSequenceContainer {
    @NotNull
    @Override
    SmartCharSequence subSequence(int start, int end);

    /**
     * used for linear copy of contents for fast access to sequences that are pieced together from other segments. it is versioned data so you need to check its getVersion().isStale() method to know if you need to obtain a new copy via getCachedProxy()
     *
     * Proxy should return a fresh copy
     */
    @NotNull
    SmartCharSequence getCachedProxy();

    /**
     * @return Proxy should return the original it is proxying for, all others should return whatever will represent them best for location tracking purposes
     */
    @NotNull
    SmartCharSequence getOriginal();

    /**
     * spliceWith    append a sequence to the end of this one if they both have the same underlying sourceData and the given sequence is immediately following this sequence
     *
     * @param other other sequence which to splice to the end of this one
     * @return resulting joined sequenced from the same source data or null if cannot splice
     */
    @Nullable
    SmartCharSequence splicedWith(@Nullable CharSequence other);

    /**
     * Add contained sequences to the list
     *
     * @param sequences array list to which contained sequences are to be added
     */
    void flattened(@NotNull ArrayList<SmartCharSequence> sequences);

    /**
     * compare char sequence content for equivalence
     *
     * @param other sequence whose content to compare
     * @return true if the sequences are identical in content
     */
    boolean equivalent(@NotNull CharSequence other);

    // NOT guaranteed to return the array of the length of the original charSequence.length() - may be more for performance reasons.
    @NotNull
    char[] getChars();

    void getChars(@NotNull char[] dst, int dstOffset);
    /**
     * Editing functions
     */
    @NotNull
    SmartCharSequence mapped(@NotNull CharSequenceMapper mapper);
    @NotNull
    SmartCharSequence insert(@NotNull CharSequence charSequence, int startIndex);
    @NotNull
    SmartCharSequence delete(int startIndex, int endIndex);
    @NotNull
    SmartCharSequence replace(@NotNull CharSequence charSequence, int startIndex, int endIndex);
    @NotNull
    SmartCharSequence append(@NotNull CharSequence... others);
    @NotNull
    SmartCharSequence appendOptimized(@NotNull CharSequence... others);
    @NotNull
    List<SmartCharSequence> splitParts(char delimiter, boolean includeDelimiter);
    @NotNull
    SmartSegmentedCharSequence splitPartsSegmented(char delimiter, boolean includeDelimiter);
    @Nullable
    List<SmartCharSequence> extractGroups(@NotNull String regex);
    @Nullable
    SmartSegmentedCharSequence extractGroupsSegmented(@NotNull String regex);
    @NotNull
    SmartCharSequence wrapParts(char delimiter, boolean includeDelimiter, @NotNull CharSequence prefix, @NotNull CharSequence suffix);
    @NotNull
    SmartCharSequence expandTabs(int tabSize);
    @NotNull
    SmartCharSequence lowercase();
    @NotNull
    SmartCharSequence uppercase();
    @NotNull
    SmartCharSequence reversed();
    @NotNull
    EditableCharSequence editable();
    @NotNull
    SmartSegmentedCharSequence segmented();
    @NotNull
    SmartVariableCharSequence variable();
}
