package edu.kaist.uilab.plda.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import edu.kaist.uilab.plda.file.TextFiles;

public class EssortmenCollector {
  private static String URL = "http://www.essortment.com/in/History.People/";
  
  public static void main(String args[]) throws Exception {
    EssortmenCollector collector = new EssortmenCollector();
    System.err.println("Getting links...");
    ArrayList<String> links = collector.collectLinks("essortmen.txt");
    System.err.println("Getting articles...");
    collector.collectArticles(links, "C:/datasets/bbchistory/");
  }
  
  public ArrayList<String> collectLinks(String filename)
      throws IOException, ParserException {
    Parser parser = new Parser(URL);
    ArrayList<String> list = new ArrayList<String>();
    NodeList nodes = parser.extractAllNodesThatMatch(new TagNameFilter("li"));
    for (int i = 0; i < nodes.size(); i++) {
      CompositeTag tag = (CompositeTag) nodes.elementAt(i).getChildren().elementAt(0);
      list.add(tag.getAttribute("href"));
      System.out.println(tag.getAttribute("href"));
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
      if (content.length() > 1700) {
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
      NodeList list = parser.extractAllNodesThatMatch(new HasAttributeFilter(
          "class", "txt"));
      if (list.size() > 0) {
        String content = list.elementAt(0).toPlainTextString();
        int pos = content.indexOf("<!--");
        String s1 = content.substring(0, pos);
        pos = content.indexOf("//-->") + "//-->".length();
        String s2 = content.substring(pos);
        return s1 + s2;
      }
    } catch (ParserException e) {
      e.printStackTrace();
    }
    
    return "";
  }
}
