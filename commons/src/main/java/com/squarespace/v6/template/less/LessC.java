package com.squarespace.v6.template.less;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.squarespace.v6.template.less.core.Buffer;
import com.squarespace.v6.template.less.core.LessUtils;
import com.squarespace.v6.template.less.model.Stylesheet;


public class LessC {

  private static final String PROGRAM_NAME = "sqs_lessc";
  
  private static final String IMPLNAME = "(LESS Compiler) [Java, Squarespace]";
  
  private static final String VERSION = "1.3.3";
  
  @Parameter
  private List<String> args = new ArrayList<String>();

  @Parameter(names = { "-D" }, description = "Debug mode (canonical, parse, expand)", converter = DebugModeConverter.class)
  private DebugMode debugMode;
  
  @Parameter(names = { "-R" }, description = "Recursion limit")
  private int recursionLimit = Options.DEFAULT_RECURSION_LIMIT;
  
  @Parameter(names = { "-h", "-help" }, description = "Show usage", help = true)
  private boolean help;

  @Parameter(names = { "-i", "-indent" }, description = "Indent size")
  private int indent = Options.DEFAULT_INDENT;

  @Parameter(names = { "-x", "-compress" }, description = "Compress mode" )
  private boolean compress = false;
  
  @Parameter(names = { "-v", "-version" }, description = "Show version")
  private boolean version = false;

  @Parameter(names = { "-stats" }, description = "Output statistics")
  private boolean stats = false;
  
  @Parameter(names = {"-W", "-wait" }, description = "Wait before executing / exiting.")
  private boolean waitForUser = false;
 
// TBD:
//  @Parameter(names = "-include-path", description = "Include path" )
//  public String includePath;

  private Options options = new Options();
  
  private void buildOptions() {
    options.compress(compress);
    options.indent(indent);
    options.recursionLimit(recursionLimit);
    // options.trace(true); // TBD
  }
  
  public static String version() {
    return PROGRAM_NAME + " " + VERSION + " " + IMPLNAME;
  }
  
  public static void main(String[] args) {
    LessC lessc = new LessC();
    JCommander cmd = new JCommander(lessc);
    cmd.setProgramName(PROGRAM_NAME);
    try {
      cmd.parse(args);
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    if (lessc.help) { 
      cmd.usage();
      System.exit(0);
    }
    lessc.main();
  }
  
  private void main() {
    if (version) {
      System.out.println(version());
      System.exit(0);
    }
    buildOptions();
    if (waitForUser) {
      waitForUser();
    }
    execute();
    if (waitForUser) {
      waitForUser();
    }
    System.exit(0);
  }
  
  /**
   * Wait for a newline before executing / exiting.
   */
  private void waitForUser() {
    BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
    try {
      buffer.readLine();
    } catch (IOException e) {
      System.exit(1);
    }
  }
  
  private void execute() {
    if (args == null || args.isEmpty()) {
      System.err.println("you must provide a .less file.");
      System.exit(1);
    }
    Path path = Paths.get(args.get(0));
    Path rootPath = path.getParent();
    LessCompiler compiler = new LessCompiler();
    String source = null;
    try {
      source = LessUtils.readFile(path);
    } catch (IOException e) {
      
    }
    Context ctx = new Context(options);
    ctx.setCompiler(compiler);
    try {
      if (debugMode == DebugMode.CANONICAL) {
        Stylesheet stylesheet = (Stylesheet) compiler.parse(source, ctx, rootPath);
        System.out.println(canonicalize(stylesheet));
      
      } else if (debugMode == DebugMode.PARSE) {
        Stylesheet stylesheet = (Stylesheet) compiler.parse(source, ctx, rootPath);
        System.out.println(parseTree(stylesheet));
      
      } else if (debugMode == DebugMode.EXPAND) {
        // NOTE: This mode doesn't fully work yet.
        Stylesheet stylesheet = (Stylesheet) compiler.parse(source, ctx, rootPath);
        stylesheet = compiler.expand(stylesheet, ctx);
        System.out.println(canonicalize(stylesheet));

      } else {
        String result = compiler.compile(source, ctx, rootPath);
        System.out.print(result);
      }
      
    } catch (LessException e) {
      System.out.println(e.errorInfo().getMessage());
      System.exit(1);
    }
    if (stats) {
      System.err.println(formatStats(ctx.stats()));
    }
  }

  private String canonicalize(Stylesheet stylesheet) {
    Buffer buf = new Buffer(options.indent());
    stylesheet.repr(buf);
    return buf.toString();
  }

  private String parseTree(Stylesheet stylesheet) {
    Buffer buf = new Buffer(options.indent());
    stylesheet.modelRepr(buf);
    return buf.toString();
  }

  private String formatStats(LessStats stats) {
    StringBuilder buf = new StringBuilder();
    buf.append("--------------------------------------------------------\n");
    buf.append(PROGRAM_NAME).append(" statistics:\n");
    buf.append("    parse time: ").append(stats.parseTimeMs()).append("ms\n");
    buf.append("  compile time: ").append(stats.compileTimeMs()).append("ms\n");
    buf.append("disk wait time: ").append(stats.diskWaitTimeMs()).append("ms\n");
    buf.append("  import count: ").append(stats.importCount()).append('\n');
    return buf.toString();
  }
  
  static enum DebugMode {
    
    CANONICAL,
    PARSE,
    EXPAND
    ;
    
    public static DebugMode fromString(String str) {
      switch (str) {
        case "canonical":
          return CANONICAL;
        case "parse":
          return PARSE;
        case "expand":
          return EXPAND;
        default:
          return null;
      }
    }
    
    public static String modes() {
      StringBuilder buf = new StringBuilder();
      DebugMode[] modes = values();
      int size = modes.length;
      for (int i = 0; i < size; i++) {
        if (i > 0) {
          buf.append(", ");
        }
        buf.append(modes[i].name().toLowerCase());
      }
      return buf.toString();
    }

  }
  
  public static class DebugModeConverter implements IStringConverter<DebugMode> {
    @Override
    public DebugMode convert(String value) {
      DebugMode mode = DebugMode.fromString(value);
      if (value == null) {
        throw new ParameterException("Unknown debug mode '" + value + "'. "
            + "Available values are: " + DebugMode.modes());
      }
      return mode;
    }
  }
    
}
