package com.squarespace.less;

import static org.testng.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.squarespace.less.core.LessTestBase;


public class LessImporterTest extends LessTestBase {

  private static final LessCompiler COMPILER = new LessCompiler();

  @Test
  public void testImporter() throws LessException {
    LessLoader loader = new HashMapLessLoader(buildMap());
    LessOptions opts = buildOptions();
    LessContext ctx = new LessContext(opts, loader);
    ctx.setCompiler(COMPILER);
    String source = "@import 'base.less'; .ruleset { color: @color; font-size: @size; }";
    String result = COMPILER.compile(source, ctx, Paths.get("."), null);

    assertEquals(result, ".child{font-size:12px;}\n.ruleset{color:#abc;font-size:12px;}\n");
  }

  private static Path path(String path) {
    return Paths.get(path).toAbsolutePath().normalize();
  }

  private static Map<Path, String> buildMap() {
    Map<Path, String> map = new HashMap<>();
    map.put(path("base.less"), "@color: #abc; @import 'child.less';");
    map.put(path("child.less"), ".child { font-size: 12px; }\n@size: 12px;");
    return map;
  }

  private static LessOptions buildOptions() {
    LessOptions opts = new LessOptions();
    opts.compress(true);
    opts.tracing(false);
    opts.indent(4);
    opts.importOnce(true);
    opts.strict(false);
    opts.hideWarnings(false);
    return opts;
  }

}