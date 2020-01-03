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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class TrackedLocation {
    static final ArrayList<TrackedLocation> EMPTY_LIST = new ArrayList<TrackedLocation>();
    public final int index;
    public final int prevIndex;   // if offset is not exact then this will be the prevClosest exact offset, if available
    public final int nextIndex;   // if offset is not exact then this will be the nextClosest exact offset, if available
    public final int offset;
    public final int prevOffset; // if offset is not exact then this will be the previous closest source offset corresponding to prevClosest
    public final int nextOffset; // if offset is not exact then this will be the next closest source offset corresponding to nextClosest
    @NotNull public final Object source;
    @NotNull public final Object prevSource;
    @NotNull public final Object nextSource;

    public TrackedLocation(int index, int prevIndex, int nextIndex, int offset, int prevOffset, int nextOffset, @NotNull Object source, @NotNull Object prevSource, @NotNull Object nextSource) {
        this.index = index;
        this.prevIndex = prevIndex;
        this.nextIndex = nextIndex;
        this.offset = offset;
        this.prevOffset = prevOffset;
        this.nextOffset = nextOffset;
        this.source = source;
        this.prevSource = prevSource;
        this.nextSource = nextSource;
    }

    public boolean isExact() {
        return index == prevIndex && index == nextIndex && source == prevSource && source == nextSource;
    }

    public TrackedLocation(int index, int offset, @NotNull Object source) {
        this(index, index, index, offset, offset, offset, source, source, source);
    }

    @NotNull
    public TrackedLocation withPrevClosest(int prevIndex) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withPrevClosest(int prevIndex, int prevOffset) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withPrevClosest(int prevIndex, int prevOffset, @NotNull Object prevSource) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withNextClosest(int nextIndex) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withNextClosest(int nextIndex, int nextOffset) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withNextClosest(int nextIndex, int nextOffset, @NotNull Object nextSource) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withIndex(int index) {
        return new TrackedLocation(index, prevIndex, nextIndex, this.offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withIndex(int index, int offset) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }

    @NotNull
    public TrackedLocation withIndex(int index, int offset, @NotNull Object source) {
        return new TrackedLocation(index, prevIndex, nextIndex, offset, prevOffset, nextOffset, source, prevSource, nextSource);
    }
}
