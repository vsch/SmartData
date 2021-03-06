/*
 * Copyright (c) 2015-2020 Vladimir Schneider <vladimir.schneider@gmail.com>
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

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * Helper Classes
 */
public class Range {
    static final public Range EMPTY = new Range(0, 0);
    static final public Range NULL = new Range(Integer.MAX_VALUE, Integer.MIN_VALUE);

    private final int myStart;
    private final int myEnd;

    public int getStart() { return myStart; }

    public int getEnd() { return myEnd; }

    public int component1() { return myStart; }

    public int component2() { return myEnd; }

    public TextRange asTextRange() {
        return new TextRange(myStart, myEnd);
    }

    public Range(int start, int end) {
        myStart = start;
        myEnd = end;
    }

    public Range(Range other) {
        myStart = other.myStart;
        myEnd = other.myEnd;
    }

    @NotNull
    public Range withStart(int start) {
        return start == myStart ? this : new Range(start, myEnd);
    }

    @NotNull
    public Range withEnd(int end) {
        return end == myEnd ? this : new Range(myStart, end);
    }

    @NotNull
    public Range withRange(int start, int end) {
        return start == myStart && end == myEnd ? this : new Range(start, end);
    }

    public boolean doesNotOverlap(Range other) { return other.myEnd <= myStart || other.myStart >= myEnd; }

    public boolean doesOverlap(Range other) { return !(other.myEnd <= myStart || other.myStart >= myEnd); }

    public boolean isEqual(Range other) { return myEnd == other.myEnd && myStart == other.myStart; }

    public boolean doesContain(Range other) { return myEnd >= other.myEnd && myStart <= other.myStart; }

    public boolean doesProperlyContain(Range other) { return myEnd > other.myEnd && myStart < other.myStart; }

    public boolean isEmpty() { return myStart >= myEnd; }

    public boolean isNotEmpty() { return myStart < myEnd; }

    public boolean isContainedBy(int start, int end) { return end >= myEnd && start <= myStart; }

    public boolean isProperlyContainedBy(int start, int end) { return end > myEnd && start < myStart; }

    public boolean isContainedBy(Range other) { return other.myEnd >= myEnd && other.myStart <= myStart; }

    public boolean isProperlyContainedBy(Range other) { return other.myEnd > myEnd && other.myStart < myStart; }

    public boolean doesContain(int index) {
        return index >= myStart && index < myEnd;
    }

    public boolean isAdjacent(int index) {
        return index == myStart - 1 || index == myEnd;
    }

    public boolean isStart(int index) {
        return index == myStart;
    }

    public boolean isEnd(int index) {
        return index == myEnd;
    }

    public boolean isLast(int index) {
        return index >= myStart && index == myEnd - 1;
    }

    public boolean isAdjacentBefore(int index) {
        return myEnd == index;
    }

    public boolean isAdjacentAfter(int index) {
        return myStart - 1 == index;
    }

    @NotNull
    public Range intersect(Range other) {
        int thisStart = myStart;
        if (thisStart < other.myStart) thisStart = other.myStart;
        int thisEnd = myEnd;
        if (thisEnd > other.myEnd) thisEnd = other.myEnd;

        if (thisStart >= thisEnd) thisStart = thisEnd = 0;
        return withRange(thisStart, thisEnd);
    }

    @NotNull
    public Range exclude(Range other) {
        int thisStart = myStart;
        if (thisStart >= other.myStart && thisStart < other.myEnd) thisStart = other.myEnd;

        int thisEnd = myEnd;
        if (thisEnd <= other.myEnd && thisEnd > other.myStart) thisEnd = other.myStart;

        if (thisStart >= thisEnd) thisStart = thisEnd = 0;
        return withRange(thisStart, thisEnd);
    }

    public int compare(Range other) {
        if (myStart < other.myStart) {
            return -1;
        } else if (myStart > other.myStart) {
            return 1;
        } else if (myEnd > other.myEnd) {
            return -1;
        } else if (myEnd < other.myEnd) {
            return 1;
        }
        return 0;
    }

    public int getSpan() {
        return myEnd - myStart;
    }

    public boolean isNull() { return this == NULL /*myStart > myEnd*/; }

    public boolean isNotNull() { return this != NULL/*myStart <= myEnd*/; }

    @Override
    public String toString() {
        return "[" + myStart + ", " + myEnd + ")";
    }

    public boolean isAdjacent(Range other) {
        return myStart == other.myEnd || myEnd == other.myStart;
    }

    public boolean isAdjacentBefore(Range other) {
        return myEnd == other.myStart;
    }

    public boolean isAdjacentAfter(Range other) {
        return myStart == other.myEnd;
    }

    @NotNull
    public Range include(@NotNull Range other) {
        return other.isNull() ? (this.isNull() ? NULL : this) : expandToInclude(other);
    }

    @NotNull
    public Range include(int pos) {
        return include(pos, pos);
    }

    @NotNull
    public Range include(int start, int end) {
        return this.isNull() ? new Range(start, end) : expandToInclude(start, end);
    }

    @NotNull
    public Range expandToInclude(@NotNull Range other) {
        return expandToInclude(other.myStart, other.myEnd);
    }

    @NotNull
    public Range expandToInclude(int start, int end) {
        return withRange(Math.min(myStart, start), Math.max(myEnd, end));
    }

    public boolean equals(TextRange o) {
        return myStart == o.getStartOffset() && myEnd == o.getEndOffset();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof TextRange) {
            return myStart == ((TextRange) o).getStartOffset() && myEnd == ((TextRange) o).getEndOffset();
        }

        if (!(o instanceof Range)) return false;

        Range range = (Range) o;

        if (myStart != range.myStart) return false;
        return myEnd == range.myEnd;
    }

    @Override
    public int hashCode() {
        int result = myStart;
        result = 31 * result + myEnd;
        return result;
    }
}
