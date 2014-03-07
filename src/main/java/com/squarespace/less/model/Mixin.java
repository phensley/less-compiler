package com.squarespace.less.model;

import static com.squarespace.less.core.LessUtils.safeEquals;

import com.squarespace.less.core.Buffer;
import com.squarespace.less.exec.ExecEnv;


/**
 * A Mixin is a macro that has a few function-like properties.  When a mixin
 * is called, the mixin is "unrolled" into the calling scope, similar
 * to the way a macro would be expanded.
 *
 * A mixin is not a proper function, as the mixin body's local scope is
 * intermingled with the caller's.  It can see variables in the caller's scope.
 * The values of local variables depend heavily on the order of evaluation
 * inside the mixin body and can lead to confusing outcomes.
 *
 * A spec is being written which will contain illustrations of how mixin
 * evaluation works in less.js 1.3.3 - phensley.
 */
public class Mixin extends BlockNode {

  private final String name;

  private final MixinParams params;

  private final Guard guard;

  private ExecEnv closure;

  private int entryCount;

  public Mixin(String name, MixinParams params, Guard guard) {
    this(name, params, guard, new Block());
  }

  public Mixin(String name, MixinParams params, Guard guard, Block block) {
    super(block);
    this.name = name;
    this.params = params;
    this.guard = guard;
  }

  public Mixin copy() {
    Mixin result = new Mixin(name, params, guard, block.copy());
    result.closure = closure;
    if (originalBlockNode != null) {
      result.originalBlockNode = originalBlockNode;
    }
    return result;
  }

  public String name() {
    return name;
  }

  public MixinParams params() {
    return params;
  }

  public Guard guard() {
    return guard;
  }

  public int entryCount() {
    return entryCount;
  }

  public void enter() {
    entryCount++;
  }

  public void exit() {
    entryCount--;
  }

  public ExecEnv closure() {
    return closure;
  }

  public void closure(ExecEnv env) {
    this.closure = env.copy();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Mixin) {
      Mixin other = (Mixin)obj;
      return safeEquals(name, other.name)
          && safeEquals(params, other.params)
          && safeEquals(guard, other.guard);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public NodeType type() {
    return NodeType.MIXIN;
  }

  @Override
  public void repr(Buffer buf) {
    buf.append(name).append('(');
    if (params != null) {
      params.repr(buf);
    }
    buf.append(')');
    if (guard != null) {
      buf.append(" when ");
      guard.repr(buf);
    }
    buf.append(" {\n");
    buf.incrIndent();
    block.repr(buf);
    buf.decrIndent();
    buf.indent().append("}\n");
  }

  @Override
  public void modelRepr(Buffer buf) {
    typeRepr(buf);
    buf.append(' ').append(name).append('\n');
    buf.incrIndent();

    if (params != null) {
      buf.indent();
      params.modelRepr(buf);
      buf.append('\n');
    }

    if (guard != null) {
      buf.indent();
      guard.modelRepr(buf);
      buf.append('\n');
    }

    buf.indent();
    super.modelRepr(buf);
    buf.decrIndent().append('\n');
  }

}