package edu.kaist.uilab.plda.util;

import java.io.IOException;

import edu.kaist.uilab.plda.file.TextFiles;

/**
 * Reader to read the Reuters corpus.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class ReutersCorpusReader {
  
  private static final int MIN_LENGTH = 150;
  private String mCorpusFile;
  
  /**
   * Constructor
   * 
   * @param corpusFile the .sgm file that contains Reuters articles
   */
  public ReutersCorpusReader(String corpusFile) {
    mCorpusFile = corpusFile;
  }
  
  /**
   * Reads the documents in the corpus file (passed to the constructor) and writes
   * them to separate files.
   * 
   * @param outdir output dir to write files
   * @param docId the document id to start
   */
  public void corpusToDocuments(String outdir, int docId) throws IOException {
    String text = TextFiles.readFile(mCorpusFile);
    String title, body;
    int start, end, pos;
    do {
      // get the title
      start = text.indexOf("<TITLE>");
      if (start < 0) {
        System.out.println(docId);
        return;
      }  
      end = text.indexOf("</TITLE>");
      title = "Title: " + text.substring(start + "<TITLE>".length(), end);
      // get the body
      start = text.indexOf("<BODY>");
      end = text.indexOf("</BODY>");
      body = text.substring(start + "<BODY>".length(), end);
      // remove the "Reuter" word at the end of the body
      pos = body.indexOf("Reuter");
      if (pos > 0) {
        body = body.substring(0, pos);
      }
      if (body.length() > MIN_LENGTH) {
        TextFiles.writeFile(outdir + "/" + docId++ + ".txt",
            title.toLowerCase() + "\n" + body);
      }  
      // ignore the part just read
      text = text.substring(end + "</BODY>".length());
    } while (text.length() > 0);
  }
  
  public static void main(String args[]) throws IOException {
    // note: run this program as is (reut01-05 have been read)
    ReutersCorpusReader reader = new ReutersCorpusReader("data/reut2-000.sgm");
    reader.corpusToDocuments("data/reuters", 0);
  }
}
