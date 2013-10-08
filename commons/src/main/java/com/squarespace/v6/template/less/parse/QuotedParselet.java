package com.squarespace.v6.template.less.parse;

import static com.squarespace.v6.template.less.core.SyntaxErrorMaker.quotedBareLF;
import static com.squarespace.v6.template.less.parse.Parselets.VARIABLE_CURLY;

import java.util.ArrayList;
import java.util.List;

import com.squarespace.v6.template.less.LessException;
import com.squarespace.v6.template.less.core.Chars;
import com.squarespace.v6.template.less.model.Anonymous;
import com.squarespace.v6.template.less.model.Node;
import com.squarespace.v6.template.less.model.Quoted;


public class QuotedParselet implements Parselet {

  @Override
  public Node parse(LessStream stm) throws LessException {
    stm.skipWs();
    int offset = 0;
    boolean escaped = false;
    char ch = stm.peek();
    if (ch == Chars.TILDE) {
      escaped = true;
      offset++;
    }
    
    // Locate the string delimiter.
    char delim = stm.peek(offset);
    if (delim != Chars.APOSTROPHE && delim != Chars.QUOTATION_MARK) {
      return null;
    }
    
    // We have the beginning of a string.
    stm.seek(offset + 1);
    List<Node> parts = new ArrayList<>();
    StringBuilder buf = new StringBuilder();

    while (stm.index < stm.length) {
      ch = stm.peek();
      
      // If we see an @ symbol embedded into a quoted string, we have to check if it is a
      // valid variable reference.  We save the current position within the stream and 
      // attempt to parse out the reference.  If it fails, we restore the position and 
      // continue processing.
      if (ch == Chars.AT_SIGN) {
        Node ref = stm.parse(VARIABLE_CURLY);
        if (ref != null) {
          if (buf.length() > 0) {
            parts.add(new Anonymous(buf.toString()));
            buf = new StringBuilder();
          }
          parts.add(ref);
          continue;

        }
      }
      stm.seek1();

      // We've located end of string.
      if (ch == delim || ch == Chars.EOF) {
        break;
      }
      
      if (ch == Chars.LINE_FEED) {
        throw stm.parseError(new LessException(quotedBareLF()));
      }
      
      // We care about backslash only to avoid prematurely terminating the string. All
      // backslash-escaped sequences are left intact.

      if (ch != '\\') {
        buf.append(ch);
        continue;
      }

      // Collect the entire \" or \' sequence.
      buf.append(ch);
      ch = stm.peek();
      if (ch != Chars.EOF) {
        buf.append(ch);
        stm.seek1();
      }
    }
    
    if (buf.length() > 0) {
      parts.add(new Anonymous(buf.toString()));
    }
    return new Quoted(delim, escaped, parts);
  }
}
