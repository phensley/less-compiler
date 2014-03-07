package com.squarespace.less;

import static com.squarespace.less.model.Operator.DIVIDE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import org.testng.annotations.Test;

import com.squarespace.less.core.LessHarness;
import com.squarespace.less.core.LessTestBase;
import com.squarespace.less.model.Argument;
import com.squarespace.less.model.Unit;
import com.squarespace.less.parse.Parselets;


public class ArgumentsTest extends LessTestBase {

  @Test
  public void testEquals() {
    Argument argXY = arg("x", anon("y"));
    Argument argXZ = arg("x", anon("z"));

    assertEquals(args(';'), args(';'));
    assertEquals(args(';', argXY), args(';', argXY));
    assertEquals(args(',', argXY, argXZ), args(',', argXY, argXZ));

    assertNotEquals(args(','), null);
    assertNotEquals(args(','), args(';'));
    assertNotEquals(args(','), args(';', argXZ));
    assertNotEquals(args(',', argXY), args(';', argXZ));
  }

  @Test
  public void testModelReprSafety() {
    arg(null, anon("z")).toString();
    arg("y", anon("z")).toString();
  }

  @Test
  public void testArguments() throws LessException {
    LessHarness h = new LessHarness(Parselets.MIXIN_CALL_ARGS);

    h.parseEquals("()",
        args(','));

    h.parseEquals("(@b)",
        args(',', arg(null, var("@b"))));

    h.parseEquals("(@b: 12px)",
        args(',', arg("@b", dim(12, Unit.PX))));

    h.parseEquals("('@{x} y @{z}')",
        args(',', arg(null, quoted('\'', false, var("@x", true), anon(" y "), var("@z", true)))));

    h.parseEquals("(@a @b, @c)",
        args(',', arg(null, expn(var("@a"), var("@b"))), arg(null, var("@c"))));

    h.parseEquals("(@a: 1,2; @b: 2)",
        args(';', arg("@a", expnlist(dim(1), dim(2))), arg("@b", dim(2))));

    h.parseEquals("(1,2; @b)",
        args(';', arg(null, expnlist(dim(1), dim(2))), arg(null, var("@b"))));

    h.parseEquals("(1; 2/16;)",
        args(';', arg(null, dim(1)), arg(null, oper(DIVIDE, dim(2), dim(16)))));
  }

}