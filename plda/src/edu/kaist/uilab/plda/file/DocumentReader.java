package edu.kaist.uilab.plda.file;

import java.io.IOException;

import edu.kaist.uilab.plda.data.EntityParser;

/**
 * An interface for a document reader.
 * 
 * <p> Each different corpus (that has different input for a document) must
 * implement this method to be used with an {@link EntityParser}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public interface DocumentReader {

  /**
   * Reads the content of a document from the given file.
   * 
   * @param filename path to the file
   * @return
   * @throws IOException if an error occurs when reading the document content
   */
  public String readDocument(String filename) throws IOException;
}
