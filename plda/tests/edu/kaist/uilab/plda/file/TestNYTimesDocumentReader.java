package edu.kaist.uilab.plda.file;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Tests for the class {@link NYTimesDocumentReader}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestNYTimesDocumentReader extends TestCase {
  
  public void testReadDocument() throws IOException {
    String expected = "Trung Nguyen lives in Daejeon. He studied his" +
    		" bachelor degree at the Korea Advanced University of Science and Technology.";
    NYTimesDocumentReader reader = new NYTimesDocumentReader();
    assertEquals(expected, reader.readDocument("data/tests/testNyTimes.txt"));
  }
}
