package com.rainmoon.util.speakcom;

import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.rainmoon.util.common.Htmls;

/**
 * Class to get dialogs from EnglishSpeak.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class EnglishSpeak {
  
  /**
   * Gets the dialog from at the given {@code url}.
   * 
   * @param url
   * @return
   */
  public ArrayList<String> getDialog(String url) throws ParserException {
    ArrayList<String> res = new ArrayList<String>();
    String html = Htmls.getResponse(url).replace("<!-- Table 3 --->", " ");
    NodeList list = getNodeList(html);
    int size = list.size();
    for (int i = 0; i < size; i++) {
      Node node = list.elementAt(i);
      res.add(getDialogLine(node));
    }
    
    return res;
  }
  
  /**
   * Returns the list of nodes whose element contains one dialog line.
   * 
   * @param content the html content to be parsed
   */
  private NodeList getNodeList(String content) throws ParserException {
    Parser parser = Parser.createParser(content, "utf-8");
    NodeFilter filter = new OrFilter(new HasAttributeFilter("class", "DataB"),
        new HasAttributeFilter("class", "DataA"));
    return parser.extractAllNodesThatMatch(filter);
  }

  /**
   * Returns the dialog line contained in a node.
   * 
   * @param node
   * @return
   */
  private String getDialogLine(Node node) {
    /*
     * <tr class="DataA"> (or DataB)
     * <tr...>
     * <td width="70%"> <span class="textSentences">word</span>
     */
    NodeList list = new NodeList();
    node.collectInto(list, new HasAttributeFilter("class", "textSentences"));
    StringBuilder sentence = new StringBuilder();
    int size = list.size();
    for (int i = 1; i < size; i++) {
      sentence.append(list.elementAt(i).getFirstChild().getText() + " ");
    }
    sentence.deleteCharAt(sentence.length() - 1);
    
    return sentence.toString();
  }
}
