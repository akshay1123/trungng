package edu.kaist.uilab.plda;

import java.io.IOException;
import java.io.Serializable;

import edu.kaist.uilab.plda.file.TextFiles;

/**
 * A sample usable in Gibbs sampling.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Sample implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public static void main(String args[]) throws IOException {
    String dir = "/home/trung/elda/data/smalltest";
    int id = 34;
    for (int doc = 4; doc <= 17; doc++) {
      String content = TextFiles.readFile(dir + "/" + doc + ".txt");
      int portion = content.length() / 5;
      for (int i = 0; i < 5; i++) {
        TextFiles.writeFile(dir + "/" + id++ + ".txt", content.substring(i, (i + 1) * portion));
      }
    }
  }
}
