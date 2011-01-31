package com.rainmoon.util.speakcom;

import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

/**
 * Class for getting dialogs from EnglishUSA.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class EnglishUSA {
  
  /**
   * Gets the dialog from the given {@code url}.
   * 
   * @param url
   * @return
   */
  public ArrayList<String> getDialog(String url) throws ParserException {
    ArrayList<String> res = new ArrayList<String>();
    Parser parser = new Parser(url);
    NodeList list = parser.extractAllNodesThatMatch(
        new HasAttributeFilter("style", "margin-top: 5; margin-bottom: 5"));
    int size = list.size();
    for (int i = 1; i < size; i++) {
      Node node = list.elementAt(i);
      String s = node.toPlainTextString();
      if (s.length() > 2) {
        s = s.replaceAll("[\\s]+", " ");
        s = s.trim();
        res.add(s);
        System.out.println(s);
      }
    }
    
    return res;
  }
}
