package edu.kaist.uilab.plda.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * The set of entities in a corpus.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class CorpusEntitySet {
  private static final int NOT_AN_ENTITY = -1;
  String[] names = {
      "United States",
      "Georgia",
      "Germany",
      "United Kingdom",
      "Britain",
      "Poland",
      "Nikita Khrushchev",
      "France",
      "Washington",
      "Texas",
      "American",
      "China",
      "U.S.",
      "America",
      "REUTER",
  };
  int[] types = {
    Entity.LOCATION, 
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.LOCATION,
    Entity.PERSON,
  };

  HashMap<Entity, Pair> map;
  
  static final class Pair {
    int count;
    int id;
    
    Pair(int count, int id) {
      this.count = count;
      this.id = id;
    }
  }
  
  CorpusEntitySet() {
    map = new HashMap<Entity, Pair>();
  }
  
  /**
   * Adds a list of {@link Entity}s from a same document to the corpus' set
   * of entities.
   * 
   * @param entities
   *       the list of {@link Entity}s which comes from a same document
   */
  void add(ArrayList<Entity> entities) {
    /**
     * Our assumptions and implementations are:
     * (1) If several strings refer to a same entity in a document, the longest
     * string is used to represent that entity.
     * (2) The list of entities given as parameter to this method contains
     * distinctive elements.
     * (3) If 2 documents in the same corpus refer to a same entity, it is highly
     * likely that the longest form (representation) of that entity appears in both
     * document. Therefore, the equality of two entities can be tested by comparing
     * their longest representations in two documents.
     */
    for (Entity entity : entities) {
      add(entity);
    }
  }
  
  void add(Entity entity) {
    if (!map.containsKey(entity)) {
      map.put(entity, new Pair(1, -1));
    } else {
      // increases count for the entity
      Pair pair = map.get(entity);
      pair.count += 1; // only count each entity (of a document) once
    }
  }

  int getNumEntities() {
    return map.size();
  }

  /**
   * Converts the given {@code entity} to its id in the corpus' set
   * of entities.
   * 
   * @param entity the entity
   * @return
   *       the id of {@code entity}, -1 if the entity does not belong to the set
   */
  public int toId(Entity entity) {
    return map.containsKey(entity) ? map.get(entity).id : NOT_AN_ENTITY;
  }
  
  /**
   * Returns the list of entities in the corpus.
   * 
   * <p>The id of an entity equals the index that can be used to address
   * that entity in the returned list. In other words,
   * {@code toId(returnedList.get(idx)) = idx}
   * 
   * <p> Since the returned list can be used to address entities and their ids,
   * this method must be called after {@link #setMinEntityCount(int)} had already
   * been called (if you plan to use min count).
   *  
   * @return
   */
  ArrayList<Entity> getEntities() {
    ArrayList<Entity> list = new ArrayList<Entity>(map.size());
    for (int i = 0; i < map.size(); i++) {
      list.add(null);
    }
    Iterator<Entity> iter = map.keySet().iterator();
    Entity entity;
    Pair pair;
    while (iter.hasNext()) {
      entity = iter.next();
      pair = map.get(entity);
      list.set(pair.id, entity);
    }
    
    return list;
  }
  
  /**
   * Sets stop entities.
   * 
   * @param name an array of entity names
   * @param type an array of entity type
   */
  public void setStopEntities(String[] name, int[] type) {
    for (int i = 0; i < name.length; i++) {
      map.remove(new Entity(name[i], type[i]));
    }
  }
  
  /**
   * Sets the minimum count of entities in the corpus. 
   * 
   * <p> This method will likely change the underlying entities maintained
   * in the corpus. Therefore it should only be invoked at specific time, such
   * as when finish reading and adding new entities to the set. And it should
   * only be called once.
   * 
   * @param minCount
   */
  void setMinEntityCount(int minCount) {
    // remove entity with count <= minCount
    Iterator<Entry<Entity, Pair>> iter = map.entrySet().iterator();
    Entry<Entity, Pair> entry;
    while (iter.hasNext()) {
      entry = iter.next();
      if (((Pair) entry.getValue()).count < minCount) {
        iter.remove();
      }
    }
    
    // remove stop entities
    setStopEntities(names, types);
    
    // index the entities
    iter = map.entrySet().iterator();
    int idx = 0;
    Pair pair;
    while (iter.hasNext()) {
      entry = iter.next();
      pair = entry.getValue();
      entry.getKey().count = pair.count;
      pair.id = idx++;
    }
  }
}
