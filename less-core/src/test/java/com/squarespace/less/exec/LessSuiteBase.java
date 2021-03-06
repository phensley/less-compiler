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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeSuite;

import com.squarespace.less.LessCompiler;
import com.squarespace.less.LessContext;
import com.squarespace.less.LessException;
import com.squarespace.less.LessOptions;
import com.squarespace.less.core.Buffer;
import com.squarespace.less.core.FlexList;
import com.squarespace.less.core.LessUtils;
import com.squarespace.less.model.Block;
import com.squarespace.less.model.Comment;
import com.squarespace.less.model.Node;
import com.squarespace.less.model.Stylesheet;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;


/**
 * Routines shared among multiple unit test classes.
 */
public class LessSuiteBase {

  protected static final String PROTOCOL_FILE = "file";

  protected static final String PROTOCOL_JAR = "jar";

  protected static final String GLOB_LESS = "glob:*.less";

  protected static final String TEST_SUITE_RESOURCE = "test-suite/";

  protected static Path testSuiteTempDir;

  /**
   * The test-suite is a collection of LESS files which can be compiled and compared
   * to the expected output. This resides in a directory, but when less-core is
   * assembled it lives inside the less-core jar. In order for command line tools
   * to be able to execute the test suite, we need to detect the jar and extract
   * it to a temporary directory.  We annotate it to only run once per suite.
   */
  @BeforeSuite()
  public void setUpTestSuite() {
    URL url = testSuiteUrl();
    String protocol = url.getProtocol();
    if (protocol.equals(PROTOCOL_FILE)) {
      // The test-suite is available in a local directory. Do nothing.

    } else if (protocol.equals(PROTOCOL_JAR)) {
      try {
        testSuiteTempDir = LessUtils.extractTempJarResource(url);

      } catch (IOException e) {
        throw new RuntimeException("Failed to extract test suite to temporary directory", e);
      }

    } else {
      throw new RuntimeException("cannot locate the test-suite using unknown protocol: " + protocol);
    }
  }

  public static Path testSuiteRoot() {
    if (testSuiteTempDir != null) {
      return testSuiteTempDir;
    }
    return Paths.get(testSuiteUrl().getPath());
  }

  private static URL testSuiteUrl() {
    URL url = LessSuiteBase.class.getClassLoader().getResource(TEST_SUITE_RESOURCE);
    if (url == null) {
      throw new RuntimeException("Cannot locate the test suite on the classpath!");
    }
    return url;
  }

  protected Stylesheet parse(String source, Path importRoot) throws LessException {
    LessOptions opts = new LessOptions();
    opts.addImportPath(importRoot.toString());
    LessContext ctx = new LessContext(opts);
    LessCompiler compiler = new LessCompiler();
    ctx.setFunctionTable(compiler.functionTable());
    return compiler.parse(source, ctx);
  }

  protected String compile(String source, Path importRoot) throws LessException {
    // Setup the compiler
    LessOptions opts = new LessOptions();
    opts.addImportPath(importRoot.toString());

    LessCompiler compiler = new LessCompiler();
    LessContext ctx = new LessContext(opts);
    ctx.setFunctionTable(compiler.functionTable());

    // First, parse the stylesheet and generate the parse tree and canonical representations,
    // in order to exercise more parts of the code.
    Buffer buf = new Buffer(2);
    Stylesheet sheet = compiler.parse(source, ctx);
    sheet.modelRepr(buf);
    sheet.repr(buf);

    // Hack to detect case-specific options enabled via comments.
    // Next version of the compiler will make this easier.
    Block block = sheet.block();
    FlexList<Node> rules = block.rules();
    int size = rules.size();
    for (int i = 0; i < size; i++) {
      Node rule = rules.get(i);
      if (rule instanceof Comment) {
        Comment comment = (Comment)rule;
        if (comment.body().trim().equals("strict=false")) {
          opts.strict(false);
        }
      }
    }

    // Finally, compile and execute the stylesheet.
    ctx = new LessContext(opts);
    ctx.setFunctionTable(compiler.functionTable());
    String result = compiler.compile(source, ctx);
    ctx.sanityCheck();
    return result;
  }

  /**
   * Create a diff between the expected and actual strings. If any
   * differences are found, format an error message.
   */
  protected String diff(String expected, String actual) {
    List<String> expList = Arrays.asList(expected.split("\n"));
    List<String> actList = Arrays.asList(actual.split("\n"));
    Patch<String> patch = DiffUtils.diff(expList, actList);
    List<Delta<String>> deltas = patch.getDeltas();
    if (deltas.size() == 0) {
      return null;
    }
    StringBuilder buf = new StringBuilder();
    for (Delta<String> delta : deltas) {
      Chunk<String> chunk1 = delta.getOriginal();
      int pos1 = chunk1.getPosition();
      List<String> lines1 = chunk1.getLines();

      Chunk<String> chunk2 = delta.getRevised();
      int pos2 = chunk2.getPosition();
      List<String> lines2 = chunk2.getLines();

      buf.append("@@ -" + pos1 + "," + lines1.size());
      buf.append(" +" + pos2 + "," + lines2.size()).append(" @@\n");
      for (String row : lines1) {
        buf.append("- ").append(row).append('\n');
      }
      for (String row : lines2) {
        buf.append("+ ").append(row).append('\n');
      }
    }
    return buf.toString();
  }

  protected void logFailure(String header, int index, Object ... arguments) {
    System.err.print(header + " failure " + index + ":");
    for (Object arg : arguments) {
      System.err.print(" " + arg.toString());
    }
    System.err.println();
  }


}
