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

public interface SafeCharSequenceError {
    /**
     * @return current error count
     */
    int getSafeErrors();

    /**
     * reset error count to 0
     */
    void clearSafeErrors();

    /**
     * return true if any errors occurred after the last call to clearHadSafeErrors()
     */
    boolean getHadSafeErrors();

    /**
     * set a marker at the current error level
     */
    void clearHadSafeErrors();

    /**
     * @return true if errors occurred since the last call to clearHadSafeErrors() or to getHadSafeErrorsAndClear()
     */
    boolean getHadSafeErrorsAndClear();

    /**
     * @return the error count at last call to clearHadSafeErrors() or getHadSafeErrorsAndClear()
     */
    int getLastSafeErrors();

    /**
     * increment error and return value before it was incremented
     */
    int addSafeError();

    /**
     * @return a copy of the error object
     */
    @NotNull
    SafeCharSequenceError getSafeErrorSnapshot();

    /**
     * use a copy of the passed error object to copy its parameters to this error
     */
    void setSafeErrorSnapshot(@NotNull SafeCharSequenceError snapshot);
}
