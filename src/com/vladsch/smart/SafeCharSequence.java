/*
 * Copyright (c) 2015-2016 Vladimir Schneider <vladimir.schneider@gmail.com>, all rights reserved.
 *
 * This code is private property of the copyright holder and cannot be used without
 * having obtained a license or prior written permission of the of the copyright holder.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package com.vladsch.smart;

import org.jetbrains.annotations.NotNull;

public interface SafeCharSequence extends CharSequence, SafeCharSequenceError {
    @Override
    @NotNull
    SafeCharSequence subSequence(int start, int end);

    @NotNull
    CharSequence getCharSequence();

    char getBeforeStartNonChar();
    void setBeforeStartNonChar(char value);
    char getAfterEndNonChar();
    void setAfterEndNonChar(char value);

    int safeIndex(int index);
    int safeInclusiveIndex(int index);

    @NotNull
    Range safeRange(int startIndex, int endIndex);
    char getFirstChar();
    char getLastChar();
    boolean isEmpty();
    boolean isBlank();
}
