package edu.kaist.uilab.asc.data;

import java.io.BufferedReader;
import java.io.IOException;

import edu.kaist.uilab.bs.DocumentUtils;

/**
 * Reader that can read reviews.
 * 
 * @author trung
 */
public class ReviewReader {

  /**
   * Reads a review from the given reader <code>reader</code>.
   * 
   * @param reader
   * @param negateContent
   *          true to negate the content of the returned review
   * @return
   * @throws IOException
   */
  public Review readReview(BufferedReader reader, boolean negateContent)
      throws IOException {
    String line;
    Double rating;
    Review review = null;
    if ((line = reader.readLine()) != null) {
      String[] ids = line.split(" ");
      try {
        rating = Double.parseDouble(reader.readLine());
      } catch (NumberFormatException e) {
        rating = -1.0;
      }
      String content = reader.readLine();
      if (negateContent) {
        review = new Review(ids[0], ids[1], rating,
            DocumentUtils.negate(content));
      } else {
        review = new Review(ids[0], ids[1], rating, content);
      }
    }

    return review;
  }
}
