package edu.kaist.uilab.plda.file;

import java.io.IOException;

/**
 * A basic implementation of {@link DocumentReader} which reads everything in
 * a file as the content of a document.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class DefaultDocumentReader implements DocumentReader {

  @Override
  public String readDocument(String filename) throws IOException {
    return TextFiles.readFile(filename);
  }
}
