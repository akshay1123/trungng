package edu.kaist.uilab.plda.data;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests for the class {@link CorpusEntitySet}.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestCorpusEntitySet extends TestCase {
  private CorpusEntitySet corpus;
  private Entity testEntity = new Entity("Test Location", Entity.LOCATION);
  private Entity testEntity2 = new Entity("Test Person", Entity.PERSON);
  
  @Override
  public void setUp() {
    corpus = new CorpusEntitySet();
  }
  
  public void testAddEntity() {
    corpus.add(testEntity);
    if (!corpus.map.containsKey(testEntity)) {
      fail("Entity not inserted");
    }
    // test no duplication of insert
    corpus.add(testEntity);
    if (corpus.getNumEntities() != 1) {
      fail("Duplication of entities were added");
    }
    // test entity count gets update
    Entity sameEntity = new Entity("Test Location", Entity.LOCATION);
    sameEntity.increaseCount(10);
    corpus.add(sameEntity);
    System.out.println(corpus.getEntities());
    System.out.println(corpus.getEntities().get(0).getCount());
    if (corpus.getEntities().get(0).getCount() != 10) {
      fail("Entity count not updated properly");
    }
  }
  
  public void testGetNumEntities() {
    corpus.add(testEntity);
    if (corpus.getNumEntities() != 1) {
      fail("Number of entities should be 1");
    }
  }
  
  public void testGetEntities() {
    ArrayList<Entity> expected = new ArrayList<Entity>();
    expected.add(testEntity2);
    expected.add(testEntity);
    corpus.add(testEntity);
    corpus.add(testEntity2);
    if (!TestUtils.listEqualsRandomOrder(expected, corpus.getEntities())) {
      fail("testGetEntities() failed");
    }
  }
  
  public void testAddEntities() {
    ArrayList<Entity> expected = new ArrayList<Entity>();
    expected.add(testEntity);
    expected.add(testEntity2);
    corpus.add(expected);
    if (!TestUtils.listEqualsRandomOrder(expected, corpus.getEntities())) {
      fail("testAddEntities() failed");
    }
  }
  
  public void testSetMinEntityCount() {
    ArrayList<Entity> list = new ArrayList<Entity>();
    Entity testEntity = new Entity("Test Location", Entity.LOCATION);
    Entity testEntity2 = new Entity("Test Person", Entity.PERSON);
    testEntity.increaseCount(5);
    testEntity2.increaseCount(2);
    list.add(testEntity);
    list.add(testEntity2);
    corpus.add(list);
    ArrayList<Entity> expected = new ArrayList<Entity>();
    expected.add(testEntity);
    corpus.setMinEntityCount(4);
    System.out.println(corpus.getEntities());
    if (!TestUtils.listEqualsRandomOrder(expected, corpus.getEntities())) {
      fail("testSetMinEntityCount() failed");
    }
  }
}
