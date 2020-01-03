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
import org.jetbrains.annotations.Nullable;

public interface SmartSourceLocationTracker {
    /**
     * trackedSourceLocation
     *
     * @param index offset into the sequence
     * @return TrackedLocation data
     */
    @NotNull
    TrackedLocation trackedSourceLocation(int index);
    /**
     * get Source location information for a given source data and originalOffset in it
     *
     * @param source source data object or null if any will do
     * @param offset original offset in that source data object
     * @return TrackedLocation for this offset or null if none found for this originalOffset
     */
    @Nullable
    TrackedLocation trackedLocation(@Nullable Object source, int offset);
}
