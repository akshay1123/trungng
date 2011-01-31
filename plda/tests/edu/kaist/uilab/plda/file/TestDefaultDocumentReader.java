package edu.kaist.uilab.plda.file;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Tests for the class {@link DefaultDocumentReader}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestDefaultDocumentReader extends TestCase {
  
  public void testReadDocument() throws IOException {
    DefaultDocumentReader reader = new DefaultDocumentReader();
    String expected = "Trung Nguyen lives in Daejeon.";
    assertEquals(expected, reader.readDocument("data/tests/testDefault.txt"));
  }
}
