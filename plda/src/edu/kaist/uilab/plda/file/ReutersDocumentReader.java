package edu.kaist.uilab.plda.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * An implementation of {@link DocumentReader} for the Reuters corpus.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class ReutersDocumentReader implements DocumentReader {

  @Override
  public String readDocument(String filename) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    reader.readLine();
    String content = reader.readLine();
    reader.close();
    
    return content;
  }
}
