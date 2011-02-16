package edu.kaist.uilab.event;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.plda.file.DefaultDocumentReader;
import edu.kaist.uilab.plda.file.DocumentReader;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;

/**
 * Parse the entities of documents.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class EntityParser {
  
  private static final String serializedClassifier = "classifiers/ner-eng-ie.crf-3-all2008-distsim.ser.gz";
  private static final AbstractSequenceClassifier classifier = CRFClassifier
      .getClassifierNoExceptions(serializedClassifier);
  private static final String PERSON = "PERSON";
  private static final String LOCATION = "LOCATION";
  private static final String ORGANIZATION = "ORGANIZATION";
  
  private String corpusDir;
  private DocumentReader reader;
  private ArrayList<String> documentNames;
  // serves as the id to symbol (entity) table
  private ArrayList<Entity> entityList;
  private Entity[][] documentEntities;
  private int minEntityCount;
  private boolean acceptPerson = true;
  private boolean acceptLocation = true;
  private boolean acceptOrganization = true;

  /**
   * Initializes an entity parser for all documents of a corpus.
   * 
   * @param corpusDir
   *       the directory which contains these documents
   * @param reader      
   * @param documentNames
   *       list of document names in the corpus
   * @param minEntityCount
   * @param maxEntitiesPerDoc
   */
  public EntityParser(String corpusDir, DocumentReader reader,
      ArrayList<String> documentNames, int minEntityCount) {
    this.corpusDir = corpusDir;
    this.reader = reader;
    this.documentNames = documentNames;
    this.minEntityCount = minEntityCount;
  }

  /**
   * Sets the entity type to be accepted.
   * 
   * @param person
   * @param location
   * @param organization
   */
  public void setAcceptedEntityType(boolean person, boolean location,
      boolean organization) {
    acceptPerson = person;
    acceptLocation = location;
    acceptOrganization = organization;
  }
  
  /**
   * Returns the directory where the corpus resides.
   * 
   * @return
   */
  public String getCorpusDir() {
    return corpusDir;
  }
  
  /**
   * Returns the list of document names in this corpus.
   * 
   * @return
   */
  public ArrayList<String> getDocumentNames() {
    return documentNames;
  }

  /**
   * Returns the array of entities for each document in the corpus.
   * 
   * @return
   */
  public Entity[][] getDocumentEntities() {
    return documentEntities;
  }

  /**
   * Gets the list of entities of this corpus (similar to an entity table).
   * 
   * @return
   */
  public ArrayList<Entity> getEntityList() {
    return entityList;
  }
  
  public int getNumEntities() {
    return entityList.size();
  }
  
  /**
   * Parses the corpus to get entities for each document.
   */
  public void parseCorpus() {
    ArrayList<ArrayList<Entity>> docEntity = new ArrayList<ArrayList<Entity>>(
        documentNames.size());
    ObjectToCounterMap<Entity> counter = new ObjectToCounterMap<Entity>();
    try {
      ArrayList<Entity> list;
      for (String doc : documentNames) {
        String content = reader.readDocument(corpusDir + "/" + doc);
        list = classifyDocument(content);
        docEntity.add(list);
        HashSet<Entity> countedSet = new HashSet<Entity>();
        for (Entity entity : list) {
          if (countedSet.add(entity)) {
            counter.increment(entity);
          }  
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // convert data to desirable form by LDA model
    // TODO(trung): this prunes entity based on count of the entire corpus
    // perhaps it is better to prune based on count of entity in documents
    counter.prune(minEntityCount);
    entityList = new ArrayList<Entity>(counter.keySet());
    HashMap<Entity, Integer> map = new HashMap<Entity, Integer>();
    for (int idx = 0; idx < entityList.size(); idx++) {
      map.put(entityList.get(idx), idx);
    }
    documentEntities = pruneEntityFromDocument(counter, map, docEntity);
  }

  /**
   * Removes the entities that were pruned by the minimum entity count. Also
   * sets the id of each entity.
   */
  Entity[][] pruneEntityFromDocument(ObjectToCounterMap<Entity> counter,
      HashMap<Entity, Integer> map, ArrayList<ArrayList<Entity>> docEntity) {
    int numDocuments = documentNames.size();
    Entity[][] res = new Entity[numDocuments][];
    ArrayList<Entity> list;
    for (int doc = 0; doc < numDocuments; doc++) {
      list = docEntity.get(doc);
      ArrayList<Entity> holder = new ArrayList<Entity>();
      for (Entity entity : list) {
        if (counter.containsKey(entity)) {
          entity.setId(map.get(entity));
          holder.add(entity);
        }
      }
      res[doc] = new Entity[holder.size()];
      holder.toArray(res[doc]);
    }
    
    return res;
  }
  
  /**
   * Classifies the content of a document.
   * 
   * @param content
   * @return
   *       the list of {@link Entity}s in this document 
   */
  public ArrayList<Entity> classifyDocument(String content) {
    ArrayList<Entity> entities = new ArrayList<Entity>();
    List<List<CoreLabel>> out = classifier.classify(content);
    String prevAnnotation = "", annotation;
    // get all entities for document (without considering variance)
    for (List<CoreLabel> sentence : out) {
      if (sentence.size() > 0) {
        StringBuilder currentWord = null;
        CoreLabel label;
        int currentEntityType = -1;
        int idx = 0;
        do {
          label = sentence.get(idx);
          annotation = label.get(AnswerAnnotation.class);
          if (getEntityType(annotation) > -1) {
            String word = label.word();
            if(!annotation.equals(prevAnnotation)) {
              // start of a new entity
              currentWord = new StringBuilder(word);
              currentEntityType = getEntityType(annotation);
            } else {
              // or the entity continues
              currentWord.append(" ").append(word);
            }
          } else {
            // an entity (if exists) finishes
            if (currentWord != null) {
              entities.add(new Entity(currentWord.toString(), currentEntityType));
              currentWord = null;
            }
          }
          prevAnnotation = annotation;
          idx++;
        } while (idx < sentence.size());
      }
    }
    
    return getDistinctEntities(entities);
  }

  // make the entities of a document distinctive
  ArrayList<Entity> getDistinctEntities(ArrayList<Entity> entity) {
    HashMap<Integer, Entity> map = new HashMap<Integer, Entity>();
    for (int i = 0; i < entity.size(); i++) {
      // find the longest representation for each entity
      int longest = i;
      for (int j = 0; j < entity.size(); j++) {
        String s1 = entity.get(longest).value;
        String s2 = entity.get(j).value;
        if (Entity.encloses(s2, s1)) {
          longest = j;
        }
      }
      map.put(i, entity.get(longest));
    }
    
    // copy the list to return, replacing each entity with its longest form
    ArrayList<Entity> res = new ArrayList<Entity>(entity.size());
    for (int i = 0; i < entity.size(); i++) {
      res.add(map.get(i));
    }
    
    return res;
  }
  
  /**
   * Converts an annotation {@code s} to an entity type.
   * 
   * @param s
   * 
   * @return
   *       -1 if s is not an entity
   */
  int getEntityType(String s) {
    if (s.equalsIgnoreCase(PERSON) && acceptPerson) {
      return Entity.PERSON;
    } else if (s.equalsIgnoreCase(LOCATION) && acceptLocation) {
      return Entity.LOCATION;
    } else if (s.equalsIgnoreCase(ORGANIZATION) && acceptOrganization) {
      return Entity.ORGANIZATION;
    }
    
    return -1;
  }
  
  public static void main(String[] args) throws Exception {
    String corpusDir = "C:/datasets/bbchistory";
    File dir = new File(corpusDir);
    ArrayList<String> docNames = new ArrayList<String>();
    for (File file : dir.listFiles()) {
      if (file.isFile()) {
        docNames.add(file.getName());
      }
    }
//    EntityParser parser = new EntityParser("data/smalltest",
//        new DefaultDocumentReader(), docNames, 10, 10);
    EntityParser parser = new EntityParser(corpusDir, new DefaultDocumentReader(),
        docNames, 3);
    parser.setAcceptedEntityType(true, false, true);
    parser.parseCorpus();
    List<Entity> list = parser.getEntityList();
    System.out.println("Number of entities: " + list.size());
    PrintWriter out = new PrintWriter("entity.txt");
    for (Entity entity : list) {
      out.println(entity);
    }
    out.close();
    
    out = new PrintWriter("doc_entity.txt");
    Entity[][] docEntities = parser.getDocumentEntities();
    for (int docIdx = 0; docIdx < docEntities.length; docIdx++) {
      for (int entityIdx = 0; entityIdx < docEntities[docIdx].length; entityIdx++) {
        out.print(docEntities[docIdx][entityIdx] + " ");
      }
      out.println();
    }
    out.close();
  }
}
