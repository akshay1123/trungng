package com.rainmoon.util.elda;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.rainmoon.util.common.TextFiles;

/**
 * Collects articles from <a href="http://www.bbc.co.uk/history/historic_figures/">
 * BBC Historical figure</a>
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class BbcCollector {
  private static final String URL = "http://www.bbc.co.uk/history/historic_figures/";
  private static final String BBC_UK = "http://www.bbc.co.uk";
  
  public ArrayList<String> collectLinks(String filename)
      throws ParserException, IOException {
    Parser parser;
    ArrayList<String> list = new ArrayList<String>();
    for (char c = 'a'; c <= 'z'; c++) {
       parser = new Parser(URL + c + ".shtml");
       NodeList nodes = parser.extractAllNodesThatMatch(
           new HasAttributeFilter("class", "a_z_content"));
       Node node = nodes.elementAt(0);
       nodes.removeAll();
       node.collectInto(nodes, new HasAttributeFilter("href"));
       for (int i = 0; i < nodes.size(); i++) {
         CompositeTag tag = (CompositeTag) nodes.elementAt(i);
         list.add(BBC_UK + tag.getAttribute("href"));
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
    for (String link : links) {
      String filename = link.substring(link.lastIndexOf("/") + 1);
      String content = getArticle(link);
      if (content.length() > 1000) {
        System.out.println(link);
        TextFiles.writeFile(outdir + "/" + filename, content);
      }  
    }
  }
  
  /*
   * Gets the article.
   */
  private String getArticle(String link) {
    try {
      Parser parser = new Parser(link);
      NodeList nodes = parser.extractAllNodesThatMatch(
          new HasAttributeFilter("class", "a_z_content"));
      if (nodes.size() > 0) {
        return nodes.elementAt(0).toPlainTextString();
      }
    } catch (ParserException e) {
      e.printStackTrace();
    }
    
    return "";
  }
  
  public static void main(String args[]) throws Exception {
    BbcCollector collector = new BbcCollector();
    System.err.println("Getting links...");
    ArrayList<String> links = collector.collectLinks("bbc.txt");
    System.err.println("Getting articles...");
    collector.collectArticles(links, "C:/datasets/bbchistory/");
  }
}
