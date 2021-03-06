/*
 * Copyright (c) 2017 NCIC, Institute of Computing Technology, Chinese Academy of Sciences
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.ncic.bioinfo.sparkseq.algorithms.data.basic;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Author: wbc
 */
public class CountSet implements Cloneable, Set<Integer> {

    /**
     * The size of the set.
     */
    private int size;

    /**
     * Holds the element of the set within the subrange <code>[0 .. size - 1]</code> in ascending order.
     */
    private int[] elements;

    /**
     * Creates a copy of an existing int-set.
     * @param template the intset to copy values from.
     */
    public CountSet(final CountSet template) {
        elements = template.elements.clone();
        size = template.size;
    }

    /**
     * Creates a new set indicating the expected maximum number of elements it will contain.
     * @param initialCapacity the desired initial capacity of the set.
     * @throws IllegalArgumentException if <code>initialCapacity</code> is negative.
     */
    public CountSet(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        elements = new int[initialCapacity];
        size = 0;
    }

    /**
     * Set the set contents to a single integer value.
     * @param value the integer value to set the set to.
     */
    public void setTo(int value) {
        ensureCapacity(1);
        size = 1;
        elements[0] = value;
    }

    /**
     * Set the content of this set to a collection of integers.
     * @param values the new values to be included in the set.
     * @throws NullPointerException if <code>value</code> is <code>null</code>.
     */
    public void setTo(int ... values) {
        ensureCapacity(values.length);
        size = values.length;
        System.arraycopy(values, 0, elements, 0, size);
        Arrays.sort(elements,0,size);
    }

    /**
     * Increase (or decrease) all elements in the set by a number.
     * @param delta the number of add (or substract if negative) to all elements.
     *
     * @return <code>true</code> if the set changed as a result of this invocation, <code>false</code> otherwise.
     */
    public boolean incAll(final int delta) {
        if (size == 0 || delta == 0)
            return false;
        for (int i = 0; i < size; i++)
            elements[i] += delta;
        return true;
    }

    /**
     * Returns the smallest integer value in the set.
     *
     * @throws NoSuchElementException if the set is empty (thus there is no minimum).
     * @return the smallest integer value in the set.
     */
    public int min() {
        if (size == 0)
            throw new NoSuchElementException("cannot have a min from an empty set");
        return elements[0];
    }

    /**
     * Returns the largest integer value in the set.
     *
     * @throws NoSuchElementException if the set is empty (thus there is no maximum).
     * @return the largest integer value in the set.
     */
    public int max() {
        if (size == 0)
            throw new NoSuchElementException("cannot have a max from an empty set");
        return elements[size - 1];
    }

    /**
     * Adds a range of integer values to the collection.
     *
     * This method avoid the need to explicity indicate all values in that range. Notice that the range is fully inclusive.
     * You can indicate a decrease range (fromValue > toValue).
     *
     * @param fromValue the first value to add in the set (inclusive).
     * @param toValue the last value to add to the set (inclusive).
     * @return <code>true</code> if the set changed as a result of this invocation, <code>false</code> otherwise.
     */
    public boolean addRange(final int fromValue, final int toValue) {
        final int lowEnd;
        final int highEnd;

        if (fromValue <= toValue) {
            lowEnd = fromValue; highEnd = toValue;
        } else {
            highEnd = fromValue; lowEnd = toValue;
        }

        //TODO to be optimized to add missing sub-ranges in one go:
        boolean result = false;
        for (int i = lowEnd; i <= highEnd; i++)
            result = add(i) | result;
        return result;
    }

    /**
     * Add an integer value to the set.
     * @param value to add to the set.
     * @return <code>true</code> if the set changed as a result of this invocation, <code>false</code> otherwise.
     */
    public boolean add(final int value) {
        int pos = Arrays.binarySearch(elements,0,size,value);
        if (pos >= 0) return false;
        int insertPos = - pos - 1;
        ensureCapacity(size + 1);
        System.arraycopy(elements, insertPos, elements, insertPos + 1, size - insertPos);
        elements[insertPos] = value;
        size++;
        return true;
    }

    /**
     * Add a arbitrary number of integers to the set.
     *
     * @param values integer to add to the set.
     * @return <code>true</code> if the set changed as a result of this invocation, <code>false</code> otherwise.
     */
    public boolean addAll(final int ... values) {
        ensureCapacity(size + values.length);
        boolean result = false;
        for (final int v : values)
            result = add(v) | result;
        return result;
    }

    @Override
    public boolean addAll(final Collection<? extends Integer> numbers) {
        ensureCapacity(size + numbers.size());
        boolean result = false;
        for (final Number n : numbers)
            result = add(n.intValue()) | result;
        return result;
    }

    /**
     * Add all values within a range in an integer array.
     *
     * @param source array where the values to add are found.
     * @param fromIndex first position from <code>source</code> to add (inclusive).
     * @param toIndex index after the last position in <code>source</code> to add (thus exclusive).
     * @throws NullPointerException if <code>source</code> is <code>null</code>.
     * @throws NegativeArraySizeException if <code>fromIndex</code> or <code>toIndex</code> are negative.
     * @throws ArrayIndexOutOfBoundsException if <code>fromIndex</code> or <code>toIndex</code> are beyond bounds
     *    allowed <code>[0 .. source.length]</code>.
     * @return <code>true</code> if the set changed as a result of this invocation, <code>false</code> otherwise.
     */
    public boolean addAll(final int[] source, final int fromIndex, final int toIndex) {
        ensureCapacity(size + source.length);
        boolean result = false;
        for (int i = fromIndex; i < toIndex; i++)
            result = add(source[i]) | result;
        return result;
    }


    /**
     * Add all elements present in a int-set.
     *
     * @param other the other inset.
     *
     * @throws NullPointerException if <code>other</code> is <code>null</code>.
     * @return <code>true</code> if this set changed due to this operation, <code>false</code> otherwise.
     */
    public boolean addAll(final CountSet other) {
        return addAll(other.elements,0,other.size);
    }

    /**
     * Checks whether a integer value is included in the set.
     * @param value the value to check.
     * @return <code>true</code> if <code>value</code> is inside the set, <code>false</code> otherwise.
     */
    public boolean contains(final int value) {
        return Arrays.binarySearch(elements, 0, size, value) >= 0;
    }

    /**
     * Make sure that this int-set has capacity to handle a number of elements.
     * <p/>
     * If the set has already that or greater capacity nothing would be changed.
     *
     * @param capacity the requested capacity.
     */
    private void ensureCapacity(final int capacity) {
        if (elements.length >= capacity) return;
        int newLength = Math.max(elements.length << 1, capacity);
        elements = Arrays.copyOf(elements,newLength);
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(final Object o) {
        if (o instanceof Integer) {
            final int i = (Integer)o;
            return contains(i);
        } else
            return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Iterator<Integer> iterator() {
        return new MyIterator();
    }

    @Override
    public Object[] toArray() {
        final Integer[] result = new Integer[size];
        for (int i = 0; i < size; i++)
            result[i] = elements[i];
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(final T[] a) {
        if (a == null)
            throw new NullPointerException();

        @SuppressWarnings("unchecked")
        final Class<T> componentClass = (Class) a.getClass().getComponentType();
        if (!componentClass.isAssignableFrom(Integer.class))
            throw new ArrayStoreException();

        @SuppressWarnings("unchecked")
        final T[] dest = (a.length < size) ? (T[]) Array.newInstance(componentClass, size) : a;

        for (int i = 0; i < size; i++)
            dest[i] = (T) (Integer) elements[i];
        return dest;
    }

    /**
     * Copies the content of the set into an integer array. The result can be freely modified by the invoker.
     * @return never <code>null</code> but a zero-length array if the set is empty.
     */
    public int[] toIntArray() {
        return Arrays.copyOfRange(elements,0,size);
    }

    /**
     * Copy the content of the set into an array.
     * @param dest the destination array.
     * @param offset where to store the first element of the set.
     * @throws NullPointerException if <code>dest</code> is <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException if <code>offset</code> is out of range of there is not enough
     * space after <code>offset</code> in the destination array to hold all values in the set.
     */
    public void copyTo(final int[] dest, int offset) {
        if (dest == null)
            throw new NullPointerException();
        if (dest.length < (size + offset))
            throw new ArrayIndexOutOfBoundsException("destination is to short");
        System.arraycopy(elements,0,dest,offset,size);
    }

    /**
     * Copy the content of the set into an array.
     * @param dest the destination array.
     * @throws NullPointerException if <code>dest</code> is <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException if there is not enough
     * space after <code>offset</code> in the destination array to hold all values in the set.
     */
    public void copyTo(final int[] dest) {
        copyTo(dest,0);
    }


    @Override
    public boolean add(final Integer integer) {
        return add((int) integer);
    }

    @Override
    public boolean remove(final Object o) {
        return o instanceof Integer && remove((int)o);
    }

    /**
     * Removes a single integer value for the set.
     * @param i the value to remove.
     * @return <code>true</code> if the set has changed as a result of this invocation, <code>false</code> otherwise.
     */
    public boolean remove(final int i) {
        final int pos = Arrays.binarySearch(elements,0,size,i);
        if (pos < 0)
            return false;
        else {
            removeIndex(pos);
            return true;
        }
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c)
            if (!contains(o))
                return false;
        return true;
    }


    @Override
    public boolean retainAll(final Collection<?> c) {
        if (size == 0)
            return false;
        @SuppressWarnings("all")
        final CountSet retainIndices = new CountSet(c.size() + 2);
        retainIndices.add(-1);
        retainIndices.add(size);
        for (final Object o : c) {
            if (!(o instanceof Integer))
                continue;
            final int pos = Arrays.binarySearch(elements,0,size,(int) o);
            if (pos < 0)
                continue;
            retainIndices.add(pos);
        }
        if (retainIndices.size == 2) {
            size = 0;
            return true;
        } else if (retainIndices.size == size + 2) {
            return false;
        } else {
            for (int idx = retainIndices.size - 1; idx > 0; idx--) {
                final int toIdx = retainIndices.elements[idx];
                final int fromIdx = retainIndices.elements[idx - 1] + 1;
                removeIndices(toIdx,fromIdx);
            }
            return true;
        }
    }

    /**
     * Removes the values found in a range of indexes in {@link #elements}.
     * @param fromIdx first index to remove (inclusive).
     * @param toIdx right after last index to remove (exclusive).
     */
    private void removeIndices(final int fromIdx, final int toIdx) {
        System.arraycopy(elements,toIdx,elements,fromIdx,size - toIdx);
        size -= toIdx - fromIdx;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean result = false;
        for (final Object o : c)
            result = remove(o) | result;
        return result;
    }

    private void removeIndex(int idx) {
        System.arraycopy(elements,idx+1,elements,idx,size - idx - 1);
    }

    @Override
    public void clear() {
        size = 0;
    }

    /**
     * Returns a copy of this set which can be changed without modifying the original one.
     * @return never {@code null}.
     */
    @SuppressWarnings("all")
    public CountSet clone() {
        return new CountSet(this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(2 + size() * 10);
        sb.append('{');
        for (int i = 0; i < size; i++)
            sb.append(elements[i]).append(',');
        sb.replace(sb.length()-1,sb.length(),"}");
        return sb.toString();

    }


    /**
     * Custom iterator class for {@link CountSet IntSets}
     */
    private class MyIterator implements Iterator<Integer> {
        /** What position I am in. */
        private int next = 0;

        @Override
        public boolean hasNext() {
            return next < size;
        }

        @Override
        public Integer next() {
            if (next >= size)
                throw new NoSuchElementException();
            return elements[next];
        }

        @Override
        public void remove() {
            if (next == 0)
                throw new IllegalStateException();
            if (next >= size)
                throw new NoSuchElementException();
            removeIndex(next - 1);
        }
    }
}
