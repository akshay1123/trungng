package edu.kaist.uilab.plda.data;

import java.util.StringTokenizer;


/**
 * An entity in the corpus.
 * 
 * <p> An entity is considered unique in a corpus.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Entity {
  public static final int OTHER = 0;
  public static final int PERSON = 1;
  public static final int LOCATION = 2;
  public static final int ORGANIZATION = 3;
  
  String value;
  int type;
  int count;
  
  /**
   * Constructs a new entity.
   * 
   * @param value the string value of this entity
   * @param type the type of this entity
   */
  public Entity(String value, int type) {
    this.value = value;
    this.type = (type >= 0 && type <= 3) ? type : 0;
    this.count = 1;
  }

  /**
   * Returns a deep copy of this entity (all member variables are copied by value).
   * 
   * @param e
   */
  public Entity clone() {
    Entity e = new Entity(value, type);
    e.count = count;
    
    return e;
  }
  
  /**
   * Increases the count of this entity (could be count of the entity in a
   * document or the entire corpus).
   * 
   * @param amount
   */
  public void increaseCount(int amount) {
    count += amount;
  }
  
  /**
   * Returns the count of this entity.
   * 
   * @return
   */
  public int getCount() {
    return count;
  }
  
  /**
   * Merges two representations of the two entities {@code e1} and {@code e2}
   * if they are the same entity.
   * 
   * <p> This method does not modifies {@code e1} or {@code e2}; it returns a
   * copy of the longer representation among {@code e1} and {@code e2} with the
   * count being sum of {@code e1.count + e2.count}.
   * 
   * @param e1
   * @param e2
   * 
   * @return
   *       null if {@code e1} and {@e2} does not refer to the same entity
   */
  public static Entity mergeRepresentation(Entity e1, Entity e2) {
    Entity res;
    if (e1.type == e2.type) {
      if (e1.value.length() > e2.value.length()) {
        if (encloses(e1.value, e2.value)) {
          res = e1.clone();
          res.increaseCount(e2.count);
          return res;
        }
      } else {
        if (encloses(e2.value, e1.value)) {
          res = e2.clone();
          res.increaseCount(e1.count);
          return res;
        }
      }
    }
    
    return null;
  }
  
  /*
   * Returns true if s1 encloses s2 (s1 has more characters than s2).
   */
  static boolean encloses(String s1, String s2) {
    StringTokenizer tokenizer = new StringTokenizer(s2, " ");
    String token;
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      if (s1.indexOf(token + " ") != 0 && s1.indexOf(" " + token + " ") <= 0
          && s1.indexOf(" " + token) <= 0) {
        return false;
      }
    }
    
    return true;
  }
  
  @Override
  public int hashCode() {
    return value.hashCode() * type;
  }
  
  @Override
  public boolean equals(Object o) {
    Entity e;
    if ((o instanceof Entity)) {
      e = (Entity) o;
      return e.value.equals(this.value) && e.type == this.type;
    }
    
    return false;
  }
  
  @Override
  public String toString() {
    return String.format("<%s, %s, %d>", value, getType(), count);
  }

  private String getType() {
    switch (type) {
      case PERSON:
        return "PERSON";
      case ORGANIZATION:
        return "ORGANIZATION";
      case LOCATION:
        return "LOCATION";
      default:
        return "UNKNOWN";
    }
  }
}
