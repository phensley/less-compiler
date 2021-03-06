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

package com.squarespace.less.exec;

import static com.squarespace.less.model.CombinatorType.DESC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.squarespace.less.core.Buffer;
import com.squarespace.less.core.CartesianProduct;
import com.squarespace.less.model.Combinator;
import com.squarespace.less.model.ExtendList;
import com.squarespace.less.model.Mixin;
import com.squarespace.less.model.Selector;
import com.squarespace.less.model.SelectorPart;
import com.squarespace.less.model.Selectors;
import com.squarespace.less.model.TextElement;
import com.squarespace.less.model.ValueElement;
import com.squarespace.less.model.WildcardElement;


/**
 * Utility methods for manipulating {@link Selectors}.
 */
public class SelectorUtils {

  private SelectorUtils() {
  }

  /**
   * Given a list of ancestor selectors and a list of current selectors, combine them into a
   * single merged selector list.  The merge strategy is based on the way LESS language does
   * nested selector combination:
   *
   * 1. If a selector contains no wildcards, it is appended to each of the ancestors.
   * 2. Otherwise, we need to replace each wildcard element in the selector with
   *    the list of ancestors, and then return the cartesian product.
   */
  public static Selectors combine(Selectors ancestors, Selectors current) {
    if (ancestors.isEmpty()) {
      return filter(current);
    }

    if (current.isEmpty()) {
      return filter(ancestors);
    }

    Selectors result = new Selectors();
    for (Selector selector : current.selectors()) {
      ExtendList extendList = selector.extendList();

      // When no wildcard is present, the selector is prepended to the ancestors.
      if (!selector.hasWildcard()) {
        List<List<Selector>> inputs = new ArrayList<>(2);

        Selector child = new Selector();
        child.add(new Combinator(DESC));
        for (SelectorPart part : selector.parts()) {
          child.add(part);
        }

        inputs.add(ancestors.selectors());
        inputs.add(Arrays.asList(child));
        flatten(inputs, result, extendList);
        continue;
      }

      // Otherwise, substitute the ancestors after each wildcard element found.
      List<List<Selector>> inputs = new ArrayList<>();
      Selector temp = new Selector();
      for (SelectorPart elem : selector.parts()) {

        // Replace each instance of the wildcard with the ancestor
        // selectors.
        if (elem instanceof WildcardElement) {
          inputs.add(Arrays.asList(temp));
          inputs.add(ancestors.selectors());
          temp = new Selector();

        } else {
          temp.add(elem);
        }

      }

      if (!temp.isEmpty()) {
        inputs.add(Arrays.asList(temp));
      }

      flatten(inputs, result, extendList);
    }
    return result;
  }

  /**
   * Generates a cartesian product from {@code selectors} and appends the flattened
   * selectors {@code result}.
   */
  public static void flatten(List<List<Selector>> selectors, Selectors result, ExtendList extendList) {
    CartesianProduct<Selector> product = new CartesianProduct<>(selectors);
    while (product.hasNext()) {
      Selector flat = new Selector();
      for (Selector tmp : product.next()) {
        for (SelectorPart elem : tmp.parts()) {
          flat.add(elem);
        }
      }
      if (extendList != null) {
        flat.extendList(extendList);
      }
      result.add(filter(flat));
    }
  }

  /**
   * Remove unnecessary and redundant parts from each selector in the group.
   */
  public static Selectors filter(Selectors selectors) {
    Selectors result = new Selectors();
    for (Selector selector : selectors.selectors()) {
      result.add(filter(selector));
    }
    return result;
  }

  /**
   * Remove unnecessary and redundant parts from a selector.
   * Wildcard elements and trailing combinators are stripped.
   */
  public static Selector filter(Selector selector) {
    Selector result = new Selector();
    Combinator combinator = null;
    for (SelectorPart part : selector.parts()) {
      if (part instanceof Combinator) {
        combinator = (Combinator)part;
      } else {
        if (part instanceof WildcardElement) {
          combinator = null;
          continue;

        } else if (combinator != null) {
          result.add(combinator);
        }
        result.add(part);
        combinator = null;
      }
    }
    ExtendList extendList = selector.extendList();
    if (extendList != null) {
      result.extendList(extendList);
    }
    return result;
  }

  /**
   * Constructs string representation of a selector, to enable faster {@link Mixin} matching.
   * This only renders selectors which require no evaluation, since no context / shared
   * buffer exists at the point the selector is being rendered.
   *
   * Note: this is a rendering method but exists here, since it renders intermediate
   * representation of the selector for mixin matching purposes, and may in fact render
   * nothing if the selector is incompatible with mixin matching.
   */
  public static String renderSelector(Selector selector) {
    if (selector.needsEval()) {
      return null;
    }

    List<SelectorPart> elements = selector.parts();
    if (elements.isEmpty()) {
      return null;
    }

    Buffer buf = new Buffer(0);
    int size = elements.size();
    for (int i = 0; i < size; i++) {
      SelectorPart elem = elements.get(i);
      if (elem instanceof WildcardElement) {
        if (i == 0) {
          continue;
        }
        return null;
      }

      if (elem instanceof Combinator) {
        continue;
      }

      // Ignore all non-text elements, since at the time this selector is being
      // expanded, only text elements can be used for mixin matching.
      if (elem instanceof TextElement) {
        buf.append(((TextElement)elem).name());

      } else {
        return null;
      }

    }
    return buf.toString();
  }

  /**
   * Constructs string representation from a selector, to enable faster {@link Mixin} matching.
   * This version able to render non-text elements.
   *
   * Note: this is a rendering method but exists here, since it renders intermediate
   * representation of the selector for mixin matching purposes, and may in fact render
   * nothing if the selector is incompatible with mixin matching.
   */
  public static boolean renderCompositeSelector(Selector selector, Buffer buffer) {
    if (selector == null) {
      return false;
    }
    List<SelectorPart> elements = selector.parts();
    if (elements.isEmpty()) {
      return false;
    }

    int size = elements.size();
    for (int i = 0; i < size; i++) {
      SelectorPart elem = elements.get(i);
      if (elem instanceof WildcardElement) {
        if (i == 0) {
          continue;
        }
        return false;
      }

      // Value elements have been evaluated so now these composite nodes can be rendered.
      // For example, a selector with a variable reference, like ".@{foo}" may have
      // been evaluated into ".123" or [TEXT, DIMENSION(123)].
      boolean valid = (elem instanceof TextElement) || (elem instanceof ValueElement);
      if (!valid) {
        return false;
      }

      if (elem instanceof TextElement) {
        buffer.append(((TextElement)elem).name());

      } else if (elem instanceof ValueElement) {
        NodeRenderer.render(buffer, ((ValueElement)elem).value());
      }

    }
    return true;
  }

}
