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

package com.squarespace.less.model;

import static com.squarespace.less.core.LessUtils.safeEquals;

import java.util.List;

import com.squarespace.less.LessContext;
import com.squarespace.less.LessException;
import com.squarespace.less.core.Buffer;
import com.squarespace.less.core.LessUtils;
import com.squarespace.less.exec.ExecEnv;
import com.squarespace.less.exec.NodeRenderer;
import com.squarespace.less.exec.SelectorUtils;


/**
 * Represents one selector in a selector set.
 */
public class Selector extends BaseNode {

  private static final byte FLAG_EVALUATE = 0x01;

  private static final byte FLAG_HAS_WILDCARD = 0x02;

  private static final byte FLAG_MIXIN_PATH_BUILT = 0x04;

  private static final byte FLAG_HAS_EXTEND = 0x08;

  /**
   * Default capacity for the selector's parts list.
   */
  private static final int DEFAULT_CAPACITY = 6;

  /**
   * List of parts in this selector.
   */
  protected List<SelectorPart> parts;

  /**
   * Last part appended to this selector.
   */
  protected SelectorPart lastPart;

  /**
   * {@link ExtendList} attached to this selector.
   */
  protected ExtendList extendList;

  /**
   * {@link Mixin} path this selector maps to.
   */
  protected String mixinPath;

  /**
   * {@link Guard} condition protecting this selector. Note that this
   * attribute is only used to pass the guard from the parse up to the
   * parent {@link Selectors} node.
   */
  protected Guard guard;

  /**
   * Flags set on this selector.
   */
  protected byte flags;

  /**
   * Indicates whether the selector list contains a wildcard element.
   */
  public boolean hasWildcard() {
    return (flags & FLAG_HAS_WILDCARD) != 0;
  }

  /**
   * Indicates whether this selector has a least one {@link Extend} attached.
   */
  public boolean hasExtend() {
    return (flags & FLAG_HAS_EXTEND) != 0;
  }

  /**
   * Adds an part to the selector.
   */
  public void add(SelectorPart part) {
    parts = LessUtils.initList(parts, DEFAULT_CAPACITY);

    // Handle sequences of combinator characters. If last combinator
    // was DESC replace it with this part.
    if (lastPart instanceof Combinator && part instanceof Combinator) {
      Combinator lastCombinator = (Combinator)lastPart;
      if (lastCombinator.combinatorType() == CombinatorType.DESC) {
        parts.set(parts.size() - 1, part);
      }
    } else {
      parts.add(part);
    }

    if (part.needsEval()) {
      flags |= FLAG_EVALUATE;
    }
    if (part instanceof WildcardElement) {
      flags |= FLAG_HAS_WILDCARD;
    }
    lastPart = part;
  }

  public void extendList(ExtendList extendList) {
    this.extendList = extendList;
    flags |= FLAG_HAS_EXTEND;
    if (extendList.needsEval()) {
      flags |= FLAG_EVALUATE;
    }
  }

  /**
   * Returns the list of parts in this selector.
   */
  public List<SelectorPart> parts() {
    return LessUtils.safeList(parts);
  }

  /**
   * Returns the guard expression for this selector, if any.
   */
  public Guard guard() {
    return guard;
  }

  /**
   * Set the guard expression for this selector.
   */
  public void guard(Guard guard) {
    this.guard = guard;
  }

  /**
   * Returns the extend list attached to this selector, if any.
   */
  public ExtendList extendList() {
    return extendList;
  }

  /**
   * Indicates whether this selector has a guard expression.
   */
  public boolean hasGuard() {
    return guard != null;
  }

  /**
   * Indicates this selector has a "mixin-friendly" path.
   */
  public boolean hasMixinPath() {
    buildMixinPath();
    return mixinPath != null;
  }

  /**
   * Returns the segmented {@link Mixin} path corresponding to this selector.
   */
  public String mixinPath() {
    buildMixinPath();
    return mixinPath;
  }

  /**
   * Number of parts in the selector.
   */
  public int size() {
    return (parts == null) ? 0 : parts.size();
  }

  /**
   * Indicates whether the selector has no parts.
   */
  public boolean isEmpty() {
    return parts == null || parts.isEmpty();
  }

  /**
   * See {@link Node#needsEval()}
   */
  @Override
  public boolean needsEval() {
    return (flags & FLAG_EVALUATE) != 0;
  }

  /**
   * See {@link Node#eval(ExecEnv)}
   */
  @Override
  public Node eval(ExecEnv env) throws LessException {
    if (!needsEval()) {
      return this;
    }

    // Selectors requiring evaluation are now rendered and parsed
    // to produce the canonical form.

    // Render the parts to a temporary buffer.
    LessContext context = env.context();
    Buffer buf = context.acquireBuffer();
    int size = parts.size();
    int last = size - 1;
    for (int i = 0; i < size; i++) {
      SelectorPart part = (SelectorPart)parts.get(i).eval(env);
      NodeRenderer.renderSelectorPart(buf, part, i == 0, i == last);
    }

    // Parse the rendered representation to produce the canonical form of the
    // selector.  This is essential for later comparison.
    String source = buf.toString();
    Selector selector = env.context().selectorParser().parse(source);
    context.returnBuffer();

    if (selector == null) {
      // Selector failed to parse. Emit a warning and fall back to the evaluated
      // original.
      // TODO: perhaps a strict mode should raise an execution error
      env.addWarning("Failed to parse selector: " + source);

      selector = new Selector();
      for (SelectorPart fragment : parts) {
        selector.add((SelectorPart)fragment.eval(env));
      }
    }

    String path = context.renderMixinPath(selector);
    if (path != null) {
      selector.mixinPath = path;
    }
    selector.flags |= FLAG_MIXIN_PATH_BUILT;

    if (extendList != null) {
      // Extend list contains selectors which will be canonicalized when evaluated.
      selector.extendList((ExtendList)extendList.eval(env));
    }
    return selector;
  }

  /**
   * See {@link Node#type()}
   */
  @Override
  public NodeType type() {
    return NodeType.SELECTOR;
  }

  /**
   * See {@link Node#repr(Buffer)}
   */
  @Override
  public void repr(Buffer buf) {
    Selectors.reprSelector(buf, this);
  }

  /**
   * See {@link Node#modelRepr(Buffer)}
   */
  @Override
  public void modelRepr(Buffer buf) {
    typeRepr(buf);
    posRepr(buf);
    buf.incrIndent();
    if (parts != null && !parts.isEmpty()) {
      buf.append('\n');
      ReprUtils.modelRepr(buf, "\n", true, parts);
    }
    if (extendList != null) {
      buf.append('\n');
      buf.indent();
      extendList.modelRepr(buf);
    }
    if (guard != null) {
      buf.indent();
      guard.modelRepr(buf);
    }
    buf.decrIndent();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof Selector) ? safeEquals(parts, ((Selector)obj).parts) : false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Builds the mixin path on demand.
   */
  private void buildMixinPath() {
    if ((flags & FLAG_MIXIN_PATH_BUILT) == 0) {
      this.mixinPath = SelectorUtils.renderSelector(this);
      flags |= FLAG_MIXIN_PATH_BUILT;
    }
  }

}
