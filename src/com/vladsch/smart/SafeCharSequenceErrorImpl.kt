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

package com.vladsch.smart

open class SafeCharSequenceErrorImpl(errors: Int, lastCheckedErrors: Int) : SafeCharSequenceError {
    constructor():this(0,0)

    protected var myErrors: Int = errors
    protected var myLastSafeErrors: Int = lastCheckedErrors

    override fun getSafeErrors(): Int {
        return myErrors
    }

    override fun addSafeError():Int {
        return myErrors++
    }

    override fun clearSafeErrors() {
        myErrors = 0
    }

    override fun getLastSafeErrors(): Int {
        return myLastSafeErrors
    }

    override fun getSafeErrorSnapshot(): SafeCharSequenceError {
        return SafeCharSequenceErrorImpl(myErrors, myLastSafeErrors)
    }

    override fun setSafeErrorSnapshot(snapshot: SafeCharSequenceError) {
        myErrors = snapshot.safeErrors
        myLastSafeErrors = snapshot.lastSafeErrors
    }

    override fun getHadSafeErrors(): Boolean {
        if (myLastSafeErrors > myErrors) myLastSafeErrors = myErrors
        return myLastSafeErrors < myErrors
    }

    override fun clearHadSafeErrors() {
        myLastSafeErrors = myErrors
    }

    override fun getHadSafeErrorsAndClear(): Boolean {
        if (myLastSafeErrors > myErrors) myLastSafeErrors = myErrors
        val result = myLastSafeErrors < myErrors
        myLastSafeErrors = myErrors
        return result
    }
}
