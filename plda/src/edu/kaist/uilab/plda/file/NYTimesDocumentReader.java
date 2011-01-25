package edu.kaist.uilab.plda.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * An implementation of {@link DocumentReader} for the Newyork Times corpus.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class NYTimesDocumentReader implements DocumentReader {
  
  @Override
  public String readDocument(String filename) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    // ignore the first 3 lines
    for (int i = 0; i < 3; i++) {
      reader.readLine();
    }
    String content = reader.readLine();
    int pos = content.indexOf('-');
    // remove the reporting LOCATION at the beginning of an article (if exists)
    if (pos > 0 && pos < 30) {
      return content.substring(pos + 1);
    } else {
      return content;
    }
  }
}
