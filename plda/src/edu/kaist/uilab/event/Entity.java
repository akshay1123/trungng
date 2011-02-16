package edu.kaist.uilab.event;

import java.util.StringTokenizer;

/**
 * An entity in the corpus.
 * <p>
 * An entity is considered unique in a corpus.
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
  int id;

  /**
   * Constructs a new entity.
   * 
   * @param value
   *          the string value of this entity
   * @param type
   *          the type of this entity
   */
  public Entity(String value, int type) {
    this.value = value;
    this.type = (type >= 0 && type <= 3) ? type : 0;
  }

  public void setId(int id) {
    this.id = id;
  }
  
  public int getId() {
    return id;
  }
  
  public String getValue() {
    return value;
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
    return String.format("<%s, %s>", value, getType());
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
