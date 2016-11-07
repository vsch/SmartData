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

package com.intellij.openapi.util;

public class TextRange {
    //private static final Logger LOG = Logger.getInstance(TextRange.class);
    //private static final long serialVersionUID = -670091356599757430L;
    //public static final TextRange EMPTY_RANGE = new TextRange(0,0);
    private final int myStartOffset;
    private final int myEndOffset;

    public TextRange(int startOffset, int endOffset) {
        myStartOffset = startOffset;
        myEndOffset = endOffset;
    }

    public int getStartOffset() {
        return myStartOffset;
    }

    public int getEndOffset() {
        return myEndOffset;
    }
}

