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
     * <p>
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

    // get a snapshot of location to source mapping appended to locations and source locations
    void getSourceLocations(@NotNull ArrayList<Object> sources, @NotNull ArrayList<Range> locations, @NotNull ArrayList<Range> sourceLocations);

    public static class Stats {
        public int segments = 0;
        public int nesting = 0;

        @Override
        public String toString() {
            return "{" +
                    "segments=" + segments +
                    ", nesting=" + nesting +
                    '}';
        }
    }

    // add some stats about content
    void addStats(@NotNull Stats stats);

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
    EditableCharSequence asEditable();
    @NotNull
    SmartSegmentedCharSequence segmented();
    @NotNull
    SmartVariableCharSequence variable();

    /**
     * Query functions
     */
    default int countLeading(CharSequence chars) {
        return countLeading(chars, 0, length());
    }

    default int countLeading(CharSequence chars, int startIndex) {
        return countLeading(chars, startIndex, length());
    }

    default int countLeading(CharSequence chars, int startIndex, int endIndex) {
        if (startIndex < 0) startIndex = 0;
        if (endIndex > length()) endIndex = length();
        if (startIndex >= endIndex) return 0;
        int i = startIndex;
        String charS = chars.toString();
        while (i < endIndex) {
            if (charS.indexOf(charAt(i)) == -1) break;
            i++;
        }
        return i - startIndex;
    }

    default int countTrailing(CharSequence chars) {
        return countTrailing(chars, 0, length());
    }

    default int countTrailing(CharSequence chars, int startIndex) {
        return countTrailing(chars, 0, startIndex);
    }

    default int countTrailing(CharSequence chars, int startIndex, int endIndex) {
        if (startIndex < 0) startIndex = 0;
        if (endIndex > length()) endIndex = length();
        if (startIndex >= endIndex) return 0;
        int i = endIndex;
        String charS = chars.toString();
        while (i-- > startIndex) {
            if (charS.indexOf(charAt(i)) == -1) break;
        }
        return endIndex - i - 1;
    }

    default SmartCharSequence replace(CharSequence find, CharSequence replace) {
        if (find.length() == 0) return this;

        int iMax = length();
        int findPos = 0;
        int findMax = find.length();

        ArrayList<SmartCharSequence> result = null;
        int lastPos = 0;

        for (int i = 0; i < iMax; i++) {
            char c = charAt(i);
            if (find.charAt(findPos) == c) {
                if (findPos + 1 == findMax) {
                    // match, replace
                    if (result == null) result = new ArrayList<>();

                    if (lastPos < i - findPos) result.add(subSequence(lastPos, i - findPos));
                    result.add(SmartCharSequenceWrapper.smart(replace));
                    lastPos = i + 1;
                    findPos = 0;
                } else {
                    findPos++;
                }
            } else {
                findPos = 0;
            }
        }

        if (result != null && lastPos < iMax) result.add(subSequence(lastPos, iMax));
        return result != null ? SmartSegmentedCharSequence.smart(result) : this;
    }
}
