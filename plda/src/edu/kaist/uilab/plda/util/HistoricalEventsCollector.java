package edu.kaist.uilab.plda.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import edu.kaist.uilab.plda.file.TextFiles;

/**
 * Collects the articles about historical events.
 * 
 * @author trung nguyen
 */
public class HistoricalEventsCollector {
  private static String URL = "http://www.famoushistoricalevents.net/";
  
  public static void main(String args[]) throws Exception {
    HistoricalEventsCollector collector = new HistoricalEventsCollector();
    System.err.println("Getting links...");
    ArrayList<String> links = collector.collectLinks("historicalevents.txt");
    System.err.println("Getting articles...");
    collector.collectArticles(links, "C:/datasets/historicalevents");
  }

  /**
   * Collects all links at the front page.
   * 
   * @param filename
   * @return
   * @throws IOException
   * @throws ParserException
   */
  public ArrayList<String> collectLinks(String filename)
      throws IOException, ParserException {
    Parser parser = new Parser(URL);
    ArrayList<String> list = new ArrayList<String>();
    NodeList nodes = parser.extractAllNodesThatMatch(
        new HasAttributeFilter("id", "frontpage"));
    if (nodes.size() > 0) {
      Node node = nodes.elementAt(0);
      nodes.removeAll();
      node.collectInto(nodes, new TagNameFilter("a"));
      for (int i = 0; i < nodes.size(); i++) {
        CompositeTag tag = (CompositeTag) nodes.elementAt(i);
        list.add(tag.getAttribute("href"));
        System.out.println(tag.getAttribute("href"));
      }
    }
    
    // write links to file
    PrintWriter out = new PrintWriter(new FileWriter(filename));
    for (String s : list) {
      out.println(s);
    }
    out.close();
    
    return list;
  }
  
  /**
   * Collects articles and writes them to the directory {@code outDir}.
   */
  public void collectArticles(ArrayList<String> links, String outdir)
      throws IOException {
    for (int i = 0; i < links.size(); i++) {
      String content = getArticle(links.get(i));
      if (content.length() > 1700) {
        System.out.println(links.get(i));
        TextFiles.writeFile(outdir + "/" + i + ".txt" , content);
      }  
    }
  }
  
  /*
   * Gets the article.
   */
  private String getArticle(String link) {
    try {
      Parser parser = new Parser(link);
      NodeList list = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "class", "format_text"));
      if (list.size() > 0) {
        Node node = list.elementAt(0);
        list.removeAll();
        node.collectInto(list, new TagNameFilter("p"));
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
          String para = list.elementAt(i).toPlainTextString().replaceAll(
              "&#8217;", "'");
          if (para.length() > 40) {
            content.append(para).append("\n");
          }
        }
        
        return content.toString();
      }
    } catch (ParserException e) {
      e.printStackTrace();
    }
    
    return "";
  }
}
