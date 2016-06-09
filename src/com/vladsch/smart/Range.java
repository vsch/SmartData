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

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * Helper Classes
 */
public class Range {
    static final public Range EMPTY = new Range(0, 0);

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

    public Range(Range that) {
        myStart = that.myStart;
        myEnd = that.myEnd;
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

    public boolean doesNotOverlap(Range that) { return that.myEnd <= myStart || that.myStart >= myEnd; }

    public boolean doesOverlap(Range that) { return !(that.myEnd <= myStart || that.myStart >= myEnd); }

    public boolean isEqual(Range that) { return myEnd == that.myEnd && myStart == that.myStart; }

    public boolean doesContain(Range that) { return myEnd >= that.myEnd && myStart <= that.myStart; }

    public boolean doesProperlyContain(Range that) { return myEnd > that.myEnd && myStart < that.myStart; }

    public boolean isEmpty() { return myStart >= myEnd; }

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
    public Range intersect(Range that) {
        int thisStart = myStart;
        if (thisStart < that.myStart) thisStart = that.myStart;
        int thisEnd = myEnd;
        if (thisEnd > that.myEnd) thisEnd = that.myEnd;

        if (thisStart >= thisEnd) thisStart = thisEnd = 0;
        return withRange(thisStart, thisEnd);
    }

    @NotNull
    public Range exclude(Range that) {
        int thisStart = myStart;
        if (thisStart >= that.myStart && thisStart < that.myEnd) thisStart = that.myEnd;

        int thisEnd = myEnd;
        if (thisEnd <= that.myEnd && thisEnd > that.myStart) thisEnd = that.myStart;

        if (thisStart >= thisEnd) thisStart = thisEnd = 0;
        return withRange(thisStart, thisEnd);
    }

    public int compare(Range that) {
        if (myStart < that.myStart) {
            return -1;
        } else if (myStart > that.myStart) {
            return 1;
        } else if (myEnd > that.myEnd) {
            return -1;
        } else if (myEnd < that.myEnd) {
            return 1;
        }
        return 0;
    }

    public int getSpan() {
        return myEnd - myStart;
    }

    @Override
    public String toString() {
        return "[" + myStart + ", " + myEnd + ")";
    }

    public boolean isAdjacent(Range that) {
        return myStart == that.myEnd || myEnd == that.myStart;
    }

    public boolean isAdjacentBefore(Range that) {
        return myEnd == that.myStart;
    }

    public boolean isAdjacentAfter(Range that) {
        return myStart == that.myEnd;
    }

    @NotNull
    public Range expandToInclude(Range that) {
        return withRange(myStart > that.myStart ? that.myStart : myStart, myEnd < that.myEnd ? that.myEnd : myEnd);
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
