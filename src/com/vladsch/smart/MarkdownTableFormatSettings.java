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

public class MarkdownTableFormatSettings {
    public static final int TABLE_CAPTION_AS_IS = 0;
    public static final int TABLE_CAPTION_ADD = 2;
    public static final int TABLE_CAPTION_REMOVE_EMPTY = 3;
    public static final int TABLE_CAPTION_REMOVE = 4;
    public static final int TABLE_CAPTION_SPACES_AS_IS = 0;
    public static final int TABLE_CAPTION_SPACES_REMOVE = -1;
    public static final int TABLE_CAPTION_SPACES_ADD = 1;

    public boolean TABLE_LEAD_TRAIL_PIPES = true;
    public boolean TABLE_SPACE_AROUND_PIPE = true;
    public boolean TABLE_ADJUST_COLUMN_WIDTH = true;
    public boolean TABLE_APPLY_COLUMN_ALIGNMENT = true;
    public boolean TABLE_FILL_MISSING_COLUMNS = true;
    public boolean TABLE_TRIM_CELLS = false;
    public int TABLE_LEFT_ALIGN_MARKER = 1;
    public int TABLE_CAPTION = 0;
    public int TABLE_CAPTION_SPACES = 0;

    public MarkdownTableFormatSettings() {
    }

    public MarkdownTableFormatSettings(boolean TABLE_LEAD_TRAIL_PIPES,
            boolean TABLE_SPACE_AROUND_PIPE,
            boolean TABLE_ADJUST_COLUMN_WIDTH,
            boolean TABLE_APPLY_COLUMN_ALIGNMENT,
            int TABLE_LEFT_ALIGN_MARKER,
            boolean TABLE_FILL_MISSING_COLUMNS,
            boolean TABLE_TRIM_CELLS,
            int TABLE_CAPTION,
            int TABLE_CAPTION_SPACES
    ) {
        this.TABLE_LEAD_TRAIL_PIPES = TABLE_LEAD_TRAIL_PIPES;
        this.TABLE_SPACE_AROUND_PIPE = TABLE_SPACE_AROUND_PIPE;
        this.TABLE_ADJUST_COLUMN_WIDTH = TABLE_ADJUST_COLUMN_WIDTH;
        this.TABLE_APPLY_COLUMN_ALIGNMENT = TABLE_APPLY_COLUMN_ALIGNMENT;
        this.TABLE_LEFT_ALIGN_MARKER = TABLE_LEFT_ALIGN_MARKER;
        this.TABLE_FILL_MISSING_COLUMNS = TABLE_FILL_MISSING_COLUMNS;
        this.TABLE_TRIM_CELLS = TABLE_TRIM_CELLS;
        this.TABLE_CAPTION = TABLE_CAPTION;
        this.TABLE_CAPTION_SPACES = TABLE_CAPTION_SPACES;
    }
}
