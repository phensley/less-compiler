package com.squarespace.less.exec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.squarespace.less.core.CartesianProduct;
import com.squarespace.less.core.LessUtils;
import com.squarespace.less.model.Element;
import com.squarespace.less.model.Selector;
import com.squarespace.less.model.Selectors;
import com.squarespace.less.model.TextElement;


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
    Selectors result = new Selectors();
    for (Selector selector : current.selectors()) {

      // When no wildcard is present, the selector is prepended to the ancestors.
      if (!selector.hasWildcard()) {
        List<List<Selector>> inputs = new ArrayList<>(2);
        inputs.add(ancestors.selectors());
        inputs.add(Arrays.asList(selector));
        SelectorUtils.flatten(inputs, result);
        continue;
      }

      // Otherwise, substitute the ancestors after each wildcard element found.
      List<List<Selector>> inputs = new ArrayList<>();
      Selector temp = new Selector();
      for (Element elem : selector.elements()) {
        temp.add(elem);

        if (elem.isWildcard()) {
          inputs.add(Arrays.asList(temp));
          inputs.add(ancestors.selectors());
          temp = new Selector();
        }
      }

      if (!temp.isEmpty()) {
        inputs.add(Arrays.asList(temp));
      }

      SelectorUtils.flatten(inputs, result);
    }
    return result;
  }

  /**
   * Generate a cartesian product from 'selectors' and append the flattened selectors
   * to 'result'.
   */
  public static void flatten(List<List<Selector>> selectors, Selectors result) {
    CartesianProduct<Selector> product = new CartesianProduct<>(selectors);
    while (product.hasNext()) {
      Selector flat = new Selector();
      for (Selector tmp : product.next()) {
        for (Element elem : tmp.elements()) {
          flat.add(elem);
        }
      }
      result.add(flat);
    }
  }

  /**
   * Constructs a list of strings from a selector, to enable simpler mixin matching.
   */
  public static List<String> renderMixinSelector(Selector selector) {
    List<Element> elements = selector.elements();
    if (elements.isEmpty()) {
      return null;
    }

    List<String> result = null;
    int size = elements.size();
    for (int i = 0; i < size; i++) {
      Element elem = elements.get(i);
      if (elem.isWildcard()) {
        if (i == 0) {
          continue;
        }
        return null;
      }
      if (!(elem instanceof TextElement)) {
        return null;
      }
      TextElement text = (TextElement)elem;
      if (result == null) {
        result = LessUtils.initList(result, size);
      }
      result.add(text.name());
    }
    return result;
  }

}