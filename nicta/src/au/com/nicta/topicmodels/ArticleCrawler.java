package au.com.nicta.topicmodels;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import edu.kaist.uilab.asc.util.TextFiles;
import edu.kaist.uilab.crawler.Htmls;

/**
 * Crawler for articles from Google Scholar.
 * 
 * @author trung (trung.ngvan@gmail.com)
 * 
 */
public class ArticleCrawler {

    public static final String GOOGLE_SCHOLAR = "http://scholar.google.com.au/scholar?hl=en";
    public static final String UTF8 = "utf-8";

    private String outputFile;
    private int maxLinks;
    private String query;

    /**
     * Constructor
     * 
     * @param query
     *            the query string used for search
     * @param maxLinks
     *            the maximum number of links to crawl from the search result
     * @param outputFile
     *            the output file to store crawled links
     */
    public ArticleCrawler(String query, int maxLinks, String outputFile) {
        this.outputFile = outputFile;
        this.maxLinks = maxLinks;
        this.query = query;
    }

    public static void main(String args[]) throws Exception {
        final String output = "/home/trung/output/topicmodels/pdfLinks2.txt";
        final String query = "\"topic model\"";
        final int maxLinks = 1000;
        ArticleCrawler crawler = new ArticleCrawler(query, maxLinks, output);
        crawler.crawl();
    }

    /**
     * Crawls a page and writes all links that point to a pdf file into the
     * given file.
     * 
     * @param page
     * @param file
     */
    public void crawl() {
        ArrayList<String> links = new ArrayList<String>();
        for (int start = 0; start <= maxLinks; start = start + 20) {
            try {
                String page = String.format("%s&q=%s&start=%d", GOOGLE_SCHOLAR,
                        URLEncoder.encode(query, UTF8), start);
                getPdfLinks(links, Htmls.getResponse(page));
            } catch (UnsupportedEncodingException e) {
                System.err.println(e.getMessage());
                // do nothing
            }
        }

        try {
            TextFiles.writeCollection(links, outputFile, null);
        } catch (IOException e) {
            // do nothing
            e.printStackTrace();
        }
    }

    /**
     * Collects all pdf links from the given page content and add them to the
     * existing list of links <code>links</code>.
     * 
     * @param content
     *            content of a file (presumably in html format)
     */
    private void getPdfLinks(ArrayList<String> links, String content) {
        int openDiv = 0, endDiv = 0;
        while (true) {
            openDiv = content.indexOf("<div class=\"gs_ggs gs_fl\"", endDiv);
            int hrefStart = content.indexOf("href=\"", openDiv);
            if (openDiv < 0 || hrefStart < 0)
                break;
            int httpStart = hrefStart + "href\"".length() + 1;
            int hrefEnd = content.indexOf("\"", httpStart);
            String link = content.substring(httpStart, hrefEnd);
            System.out.println(link);
            links.add(link);
            endDiv = hrefEnd + 1;
        }
    }
}
