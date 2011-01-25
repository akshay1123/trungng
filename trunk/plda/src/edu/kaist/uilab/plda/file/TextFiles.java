package edu.kaist.uilab.plda.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to deal with text files.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TextFiles {
  
  /**
   * Returns the text content of a file.
   *  
   * @return
   * @throws IOException
   */
  public static String readFile(String file) throws IOException {
    StringBuilder builder = new StringBuilder();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line).append(" ");
    }
    reader.close();
    builder.deleteCharAt(builder.length() - 1);
    return builder.toString();
  }
}
