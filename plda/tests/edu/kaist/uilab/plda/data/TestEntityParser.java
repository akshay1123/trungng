package edu.kaist.uilab.plda.data;

import java.util.ArrayList;

import edu.kaist.uilab.plda.file.DefaultDocumentReader;

import junit.framework.TestCase;

/**
 * Tests for the {@link EntityParser} class.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestEntityParser extends TestCase {
  EntityParser parser;
  String testContent = "Trung Nguyen, a.k.a, Trung Van Nguyen, was born in Bac"
    + " Ninh, a small city 40km to the south of Hanoi,"
    + " the capital of Vietnam. Nguyen had"
    + " lived there his entire childhood before moving to Hanoi. There, he met"
    + " his future wife, Mrs. Hang Vu. Later on he worked for Google.";
  ArrayList<Entity> entities;
  
  public void setUp() {
    parser = new EntityParser("", new DefaultDocumentReader(),
        new ArrayList<String>(), 5, 3);
    entities = new ArrayList<Entity>();
    entities.add(new Entity("Trung Nguyen", Entity.PERSON));
    entities.add(new Entity("Trung Van Nguyen", Entity.PERSON));
    entities.add(new Entity("Bac Ninh", Entity.LOCATION));
    entities.add(new Entity("Hanoi", Entity.LOCATION));
    entities.add(new Entity("Vietnam", Entity.LOCATION));
    entities.add(new Entity("Nguyen", Entity.PERSON));
    entities.add(new Entity("Hanoi", Entity.LOCATION));
    entities.add(new Entity("Mrs. Hang Vu", Entity.PERSON));
    entities.add(new Entity("Google", Entity.ORGANIZATION));
  }
  
  public void testGetEntityType() {
    String s = "PERSON";
    assertEquals(parser.getEntityType(s), Entity.PERSON);
    s = "person";
    assertEquals(parser.getEntityType(s), Entity.PERSON);
    s = "LOCATION";
    assertEquals(parser.getEntityType(s), Entity.LOCATION);
    s = "location";
    assertEquals(parser.getEntityType(s), Entity.LOCATION);
    s = "ORGANIZATION";
    assertEquals(parser.getEntityType(s), Entity.ORGANIZATION);
    s = "organization";
    assertEquals(parser.getEntityType(s), Entity.ORGANIZATION);
    s = "xyz";
    assertEquals(parser.getEntityType(s), -1);
  }
  
  
  public void testClassifyDocument() {
    if (!TestUtils.listEqualsRandomOrder(TestUtils.createDistinctEntities(),
        parser.classifyDocument(testContent))) {
      fail("testClassifyDocument() failed...");
    }
  }
  
  public void testGetDistinctEntities() {
    ArrayList<Entity> expected = TestUtils.createDistinctEntities();
    ArrayList<Entity> result = parser.getDistinctEntities(entities);
    if (!TestUtils.listEqualsRandomOrder(expected, result)) {
      fail("testGetDistinctEntities() failed...");
    }
  }
}
