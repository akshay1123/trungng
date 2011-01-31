package com.rainmoon.util.elda;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;

import com.rainmoon.util.common.TextFiles;

/**
 * Collects news from New York Times.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class NYTimesCollector {
  static final String PARAM_QUERY = "query";
  static final String PARAM_FROM = "from";
  // query period: Jan 1, 1999 ~ Dec 30th, 2004
  // get 30 links as the search result
  static final String BUSINESS_SEARCH_URL = "http://query.nytimes.com/search/query?n=30&daterange=period&year1=1999&mon1=01&day1=01&year2=2004&mon2=12&day2=30&d=nytdsection%2b&o=e%2b&v=Business%2b&c=a%2b";
  static final String TECHNOLOGY_SEARCH_URL = "http://query.nytimes.com/search/query?n=30&daterange=period&year1=1999&mon1=01&day1=01&year2=2004&mon2=12&day2=30&d=nytdsection%2b&o=e%2b&v=Technology%2b&c=a%2b";
  static final String GENERAL_SEARCH_URL = "http://query.nytimes.com/search/query?n=30&daterange=period&year1=1999&mon1=01&day1=01&year2=2004&mon2=12&day2=30";
  
  final LinksCollection businessLinks = new LinksCollection();
  final LinksCollection technologyLinks = new LinksCollection();
  final LinksCollection generalLinks = new LinksCollection();
  static HashSet<Integer> businessCollected;// = getCollected("nytimes/business");
  static HashSet<Integer> techCollected;// = getCollected("nytimes/technology");
  static HashSet<Integer> generalCollected = getCollected("nytimes/general");
  

  static HashSet<Integer> getCollected(String directory) {
    HashSet<Integer> set = new HashSet<Integer>();
    final File dir = new File(directory);
    int pos;
    for (String f : dir.list()) {
      pos = f.indexOf(".txt");
      set.add(Integer.parseInt(f.substring(0, pos)));
    }
    
    return set;
  }
  
  public static void main(String args[]) throws Exception {
    NYTimesCollector collector = new NYTimesCollector();
    collector.collectLinks("nytimes/query.txt");
//    List<String> links = TextFiles.readLines("nytimes/business.txt");
//    int block = 10000; // each thread collects 10000 articles
//    for (int i = 0; i < 8; i++) {
//      int toIndex = (i + 1) * block - 1 > links.size() ? links.size() : (i + 1) * block - 1;
//      new SmallThread(links.subList(i * block, toIndex), i * block,
//          "nytimes/business").start();
//    }
  }

  static final class SmallThread extends Thread {
    NYTimesCollector collector;
    int offset;
    String outputDir;
    List<String> links;
    
    public SmallThread(List<String> links, int offset, String outputDir) {
      super();
      collector = new NYTimesCollector();
      this.links = links;
      this.offset = offset;
      this.outputDir = outputDir;
    }
    
    @Override
    public void run() {
      try {
        System.out.println("Getting document from offset " + offset);
        collector.collectArticles(offset, links, outputDir);
      } catch (Exception e) {
        // do nothing
      }
    }
  }
  
  /**
   * Collects links for Business and Technology section
   * 
   * @throws Exception
   */
  public void collectLinks(String queryFile) throws Exception {
    // queryFile = "nytimes/query.txt";
    List<String> queries = TextFiles.readLines(queryFile);
    for (String q : queries) {
//      collectBusinessSectionLinks(q);
//      collectTechnologySectionLinks(q);
      collectGeneralLinks(q);
    }
    
    // write links to file
//    TextFiles.writeIterator(businessLinks.getLinks(), "nytimes/business.txt");
//    TextFiles.writeIterator(technologyLinks.getLinks(), "nytimes/technology.txt");
    TextFiles.writeIterator(generalLinks.getLinks(), "nytimes/general.txt");
  }

  public void collectBusinessSectionLinks(String query) {
    System.err.print("Getting links for the query " + query + " in Business section");
    collectLinks(BUSINESS_SEARCH_URL, businessLinks, query);
  }
  
  public void collectTechnologySectionLinks(String query) {
    System.err.print("Getting links for the query " + query + " in Technology section");    
    collectLinks(TECHNOLOGY_SEARCH_URL, technologyLinks, query);
  }
  
  public void collectGeneralLinks(String query) {
    System.err.print("Query: " + query + " in all sections");    
    collectLinks(GENERAL_SEARCH_URL, generalLinks, query);
  }
  
  /**
   * Collects articles from all links in the given list and writes them to the
   * directory {@code outputDir}.
   */
  public void collectArticles(int offset, List<String> links, String outputDir) throws Exception {
    Parser parser;
    NodeList nodes;
    String link;
    boolean abort;
    for (int i = 0; i < links.size(); i++) {
      if (outputDir.equals("nytimes/business")) {
        abort = businessCollected.contains(offset + i);
      } else if (outputDir.equals("nytimes/technology")) {
        abort = techCollected.contains(offset + i);
      } else {
        abort = generalCollected.contains(offset + i);
      }
      
      if (!abort) {
        link = links.get(i);
        parser = new Parser(link);
        System.out.println(link);
        // get the element which contains main article 
        nodes = parser.extractAllNodesThatMatch(new HasAttributeFilter("class", "columnGroup first"));
        if (nodes.size() > 0) {
          handleFirstDocumentType(link, outputDir + "/" + (offset + i) + ".txt", nodes.elementAt(0));
        } else {
          // get the content again because it was discarded by parser before
          parser = new Parser(link);
          nodes = parser.extractAllNodesThatMatch(new HasAttributeFilter("id", "area-main-center-w-left"));
          if (nodes.size() > 0) {
            handleSecondDocumentType(link, outputDir + "/" + (offset + i) + ".txt", nodes.elementAt(0));
          }
        }
      }
    }
  }

  /**
   * Handles the first document structure type.
   * 
   * <p> An example link to the structure of this document type can be seen
   * <a href="http://www.nytimes.com/2003/11/04/business/us-subsidizes-companies-to-buy-subsidized-cotton.html?scp=2&sq=buy&st=nyt">here</a>
   * 
   * @param link
   * @param node
   */
  private void handleFirstDocumentType(String link, String fileName, Node node) {
    String articleHeadline, date, content;
    try {
      // get article headline
      NodeList list = new NodeList();
      node.collectInto(list, new HasAttributeFilter("class", "articleHeadline"));
      articleHeadline = list.elementAt(0).toPlainTextString();
      System.out.println(articleHeadline);
      
      // get published date
      list.removeAll();
      node.collectInto(list, new HasAttributeFilter("class", "dateline"));
      date = list.elementAt(0).toPlainTextString();
      System.out.println(date);
      
      // get document content
      list.removeAll();
      node.collectInto(list, new HasAttributeFilter("class", "articleBody"));
      content = list.elementAt(0).toPlainTextString().replace('\t', ' ').replace('\n', ' ');
      System.out.println(content);
      
      if (content.length() > 800) {
        writeArticle(fileName, link, articleHeadline, date, content);
      }
    } catch (Exception e) {
      // do nothing (to let the collection of other articles continues)
    }
  }
  
  /**
   * Handles the second document structure type.
   * 
   * <p> An example link to the structure of this document type can be seen 
   * <a href="http://www.nytimes.com/2003/05/21/business/the-media-business-practice-returning-but-minus-some-stars.html?src=pm">here</a>
   *
   * @param link  
   * @param node
   */
  private void handleSecondDocumentType(String link, String fileName, Node node) {
    String articleHeadline = "", date = "";
    StringBuilder content = new StringBuilder();
    try {
      // get article header
      NodeList list = new NodeList();
      node.collectInto(list, new HasAttributeFilter("id", "mod-article-header"));
      articleHeadline = list.elementAt(0).toPlainTextString();
      
      // get date
      list.removeAll();
      node.collectInto(list, new HasAttributeFilter("class", "pubdate"));
      date = list.elementAt(0).toPlainTextString();
      
      // get body content
      list.removeAll();
      node.collectInto(list, new OrFilter(new HasAttributeFilter("id", "mod-a-body-first-para"),
          new HasAttributeFilter("id", "mod-a-body-after-first-para")));
      content.append(list.elementAt(0).toPlainTextString()).append(" ");
      content.append(list.elementAt(1).toPlainTextString());

      if (content.length() > 800) {
        writeArticle(fileName, link, articleHeadline, date, content.toString());
      }
    } catch (Exception e) {
      // do nothing (to let the collection of other articles continues)
    }
  }
  
  /**
   * Writes an article to a file.
   * 
   * <p> The structure of the file is as followed.
   * <br />url
   * <br />articleHeadline
   * <br />date
   * <br />content
   * 
   * @param fileName
   * @param url
   * @param articleHeadline
   * @param date
   * @param content
   * 
   * @throws IOException
   */
  private void writeArticle(String fileName, String url, String articleHeadline,
      String date, String content) {
    try {
      PrintWriter out = new PrintWriter(fileName);
      out.println(url);
      out.println(articleHeadline);
      out.println(date);
      out.println(content);
      out.close();
    } catch (IOException e) {
      // do nothing
    }
  }
  
  /**
   * Searches the New York Times with the given {@code query} and adds article links
   * to the collection {@code col}.
   */
  private void collectLinks(String searchUrl, LinksCollection col, String query) {
    try {
      NodeList linksList;
      // get 700 * 30 = 21000 links (if available)
      // TODO(trung): change to 700 to get many links
      for (int i = 0; i < 700; i++) {
        // + 1 to ignore the first query result (which is often not an article)
        System.out.printf("\nGetting links (%d..%d)", i * 30 + 1, i * 30 + 30);
        Parser parser = new Parser(searchUrl + "&query=" + query + "&frow=" + (i * 30 + 1));
        NodeList list = parser.extractAllNodesThatMatch(
            new AndFilter(new HasAttributeFilter("class", "srchSearchResult"),
                new HasAttributeFilter("start")));
        if (list.size() > 0) {
          NodeList childNodes = list.elementAt(0).getChildren();
          linksList = childNodes.extractAllNodesThatMatch(new TagNameFilter("a"), true);
          for (int j = 0; j < linksList.size(); j++) {
            String href = ((CompositeTag) linksList.elementAt(j)).getAttribute("href");
            col.add(href);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * A collection of links from the search result.
   * 
   * <p> The collection ensures that the elements are unique and valid links to
   * New York Times articles.
   */
  static final class LinksCollection {
    private HashSet<String> set = new HashSet<String>();
    
    /**
     * Adds a new link to this collection.
     * 
     * <p> This method takes care of duplicated links and links that appear not valid
     * NYTimes articles.
     * 
     * @param newLink
     */
    public void add(String newLink) {
      if (newLink.indexOf("http://www.nytimes.com/") == 0) {
        set.add(newLink);
        System.out.println(newLink);
      }
    }
    
    /**
     * Returns the links as an {@link Iterator}.
     * 
     * @return
     */
    public Iterator<String> getLinks() {
      return set.iterator();
    }
  }
}
