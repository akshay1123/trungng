package com.rainmoon.util.elda;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;

import com.rainmoon.util.common.TextFiles;

/**
 * Collects news from Reuters.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ReutersCollector {
  // get 100 pages from the search result
  static final String BUSINESS_SEARCH_URL = "http://query.nytimes.com/search/query?n=30&daterange=period&year1=1999&mon1=01&day1=01&year2=2004&mon2=12&day2=30&d=nytdsection%2b&o=e%2b&v=Business%2b&c=a%2b";
  static final String TECHNOLOGY_SEARCH_URL = "http://query.nytimes.com/search/query?n=30&daterange=period&year1=1999&mon1=01&day1=01&year2=2004&mon2=12&day2=30&d=nytdsection%2b&o=e%2b&v=Technology%2b&c=a%2b";
  static final LinksCollection businessLinks = new LinksCollection();
  static final LinksCollection technologyLinks = new LinksCollection();

  public static void main(String args[]) throws Exception {
    List<String> queries = TextFiles.readLines("nytimes/query.txt");
    ReutersCollector collector = new ReutersCollector();
    for (String q : queries) {
      collector.collectBusinessSectionLinks(q);
      collector.collectTechnologySectionLinks(q);
    }
    
    // write links to file
    TextFiles.writeIterator(businessLinks.getLinks(), "nytimes/business.txt");
    TextFiles.writeIterator(technologyLinks.getLinks(), "nytimes/technology.txt");
  }

  public void collectBusinessSectionLinks(String query) {
    System.err.print("Getting links for the query " + query + " in Business section");
    collectLinks(BUSINESS_SEARCH_URL, businessLinks, query);
  }
  
  public void collectTechnologySectionLinks(String query) {
    System.err.print("Getting links for the query " + query + " in Technology section");    
    collectLinks(TECHNOLOGY_SEARCH_URL, technologyLinks, query);
  }
  
  /**
   * Searches the New York Times with the given {@code query} and adds article links
   * to the collection {@code col}.
   */
  private void collectLinks(String searchUrl, LinksCollection col, String query) {
    try {
      NodeList linksList;
      // get 500 * 30 = 15000 links (if available)
      for (int i = 0; i < 500; i++) {
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
            System.out.println(href);
          }
        }
      }
    } catch (Exception e) {
      // do nothing
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
