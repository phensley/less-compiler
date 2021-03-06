/**
 * Copyright (c) 2014 SQUARESPACE, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squarespace.less.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Given a List<List<T>> input, each iteration returns a List<T> containing
 * one permutation of the elements from each of the input lists.  Iterating
 * to the end produces the full cartesian product of the input.
 *
 * input:    [ [a, b], [1, 2] ]
 * results:  [a, 1]  [a, 2]  [b, 1]  [b, 2]
 *
 * If one or more of the input lists are empty, that position will be missing
 * from the output.  For example, the input "[ [a, b], [], [1, 2] ]" will produce
 * the same results sequence shown above.
 */
public class CartesianProduct<T> implements Iterator<List<T>> {

  /**
   * List of lists, used to produce the permutations.
   */
  private final List<List<T>> lists;

  /**
   * Tracks the current index in each list.
   */
  private final int[] indices;

  /**
   * Length of each list.
   */
  private final int[] lengths;

  /**
   * Iterator has at least one more element to produce.
   */
  private boolean hasNext;

  /**
   * Constructs an iterator that will produce the cartesian product of the
   * elements in the given lists.
   */
  public CartesianProduct(List<List<T>> lists) {
    this.lists = lists;
    if (lists != null && !lists.isEmpty()) {
      int size = lists.size();
      lengths = new int[size];
      indices = new int[size];
      for (int i = 0; i < size; i++) {
        lengths[i] = lists.get(i).size();
        if (lengths[i] > 0) {
          hasNext = true;
        }
      }

    } else {
      // All input lists are of length zero
      lengths = null;
      indices = null;
      hasNext = false;
    }
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public List<T> next() {
    if (indices == null) {
      throw new IllegalStateException("No input to iterate over.");
    }
    if (!hasNext) {
      throw new NoSuchElementException("Iterator is exhausted.");
    }

    int size = indices.length;
    List<T> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      if (lengths[i] == 0) {
        continue;
      }
      result.add(lists.get(i).get(indices[i]));
    }
    incrIndices();
    return result;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove() is not a supported method");
  }

  /**
   * Move the indices to the next position, set the internal iteration state.
   */
  private void incrIndices() {
    for (int i = indices.length - 1; i >= 0; i--) {
      if (lengths[i] == 0) {
        indices[i] = -1;
      }
      if (indices[i] == lengths[i] - 1) {
        indices[i] = 0;
        if (i == 0) {
          hasNext = false;
        }

      } else {
        indices[i]++;
        break;
      }
    }
  }

}
