package edu.kaist.uilab.plda.data;

import java.util.ArrayList;

/**
 * Utility class for test classes.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class TestUtils {
  /**
   * Returns true if two lists contain the same set of elements, i.e., a list
   * is a permutation of the other list.
   * 
   * <p> This method assumes that both lists already contain distinctive elements.
   * It might not work correctly if this assumption does not hold.
   * 
   * @param <T>
   * @param list1
   * @param list2
   * 
   * @return
   */
  public static <T> boolean listEqualsRandomOrder(ArrayList<T> list1,
      ArrayList<T> list2) {
    if (list1.size() == list2.size()) {
      for (int i = 0; i < list1.size(); i++) {
        // test if each element of list1 is in list2
        boolean inOtherList = false;
        for (int j = 0; j < list2.size(); j++) {
          if (list1.get(i).equals(list2.get(j))) {
            inOtherList = true;
            break;
          }
        }
        if (!inOtherList) {
          return false;
        }
      }
    } else {
      return false;
    }
    
    return true;
  }
  
  /**
   * Creates a list of distinctive entities for testing.
   * 
   * @return
   */
  public static ArrayList<Entity> createDistinctEntities() {
    ArrayList<Entity> list = new ArrayList<Entity>();
    list.add(new Entity("Trung Van Nguyen", Entity.PERSON));
    list.add(new Entity("Bac Ninh", Entity.LOCATION));
    list.add(new Entity("Vietnam", Entity.LOCATION));
    list.add(new Entity("Hanoi", Entity.LOCATION));
    list.add(new Entity("Mrs. Hang Vu", Entity.PERSON));
    list.add(new Entity("Google", Entity.ORGANIZATION));
    
    return list;
  }
}
