package edu.kaist.uilab.plda.data;

import junit.framework.TestCase;

/**
 * Tests for the class {@link Entity}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestEntity extends TestCase {
  
  public void testLongestRepresentation() {
    // same entity type
    Entity e1 = new Entity("Trung Nguyen Van", Entity.PERSON);
    Entity e2 = new Entity("Trung Nguyen", Entity.PERSON);
    assertEquals(e1, Entity.mergeRepresentation(e1, e2));
    // different entity type
    e2 = new Entity("Trung Nguyen", Entity.LOCATION);
    assertNull(Entity.mergeRepresentation(e1, e2));
  }
  
  public void testEncloses() {
    String s1 = "Trung Nguyen Van";
    String s2 = "Trung Nguyen";
    // 2 consecutive tokens
    assertTrue(Entity.encloses(s1, s2));
    s2 = "Trung Van";
    // 2 separate tokens
    assertTrue(Entity.encloses(s1, s2));
    s2 = "trung";
    // case sensitive
    assertFalse(Entity.encloses(s1, s2));
  }
}
