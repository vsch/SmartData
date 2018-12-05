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
import org.jetbrains.annotations.Nullable;

public interface CharWidthProvider {
    int getCharWidth(@NotNull Character c);
    default int getStringWidth(@NotNull CharSequence charSequence) {
        return getStringWidth(charSequence, null);
    }
    
    int getStringWidth(@NotNull CharSequence charSequence, @Nullable CharSequence zeroWidth);
    int getSpaceWidth();

    public static CharWidthProvider UNITY_PROVIDER = new CharWidthProvider() {
        @Override
        public int getSpaceWidth() {
            return 1;
        }

        @Override
        public int getCharWidth(@NotNull Character c) {
            return 1;
        }

        @Override
        public int getStringWidth(@NotNull CharSequence charSequence, @Nullable CharSequence zeroWidth) {
            return charSequence.length();
        }

        @Nullable
        @Override
        public CharSequence lineChars(int line) {
            return EMPTY_SEQUENCE.INSTANCE;
        }

        @Nullable
        @Override
        public Integer lineStart(int line) {
            return null;
        }

        @Nullable
        @Override
        public Integer lineEnd(int line) {
            return null;
        }

        @Nullable
        @Override
        public Integer offsetLineStart(int offset) {
            return null;
        }

        @Nullable
        @Override
        public Integer offsetLineEnd(int offset) {
            return null;
        }

        @Nullable
        @Override
        public Integer offsetLineNumber(int offset) {
            return null;
        }

        @Override
        public void initCharWidths(int startOffset, int endOffset) {

        }
    };

    @Nullable
    CharSequence lineChars(int line);
    @Nullable
    Integer lineStart(int line);
    @Nullable
    Integer lineEnd(int line);
    @Nullable
    Integer offsetLineStart(int offset);
    @Nullable
    Integer offsetLineEnd(int offset);
    @Nullable
    Integer offsetLineNumber(int offset);
    void initCharWidths(int startOffset, int endOffset);
}
