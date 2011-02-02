package edu.kaist.uilab.plda.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.aliasi.util.ObjectToCounterMap;

import edu.kaist.uilab.plda.file.DocumentReader;
import edu.kaist.uilab.plda.file.NYTimesDocumentReader;
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
  private CorpusEntitySet corpusEntities;
  private Entity[][] documentEntities;
  private int minEntityCount;
  private int maxEntitiesPerDoc;
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
      ArrayList<String> documentNames, int minEntityCount, int maxEntitiesPerDoc) {
    this.corpusDir = corpusDir;
    this.reader = reader;
    this.documentNames = documentNames;
    this.minEntityCount = minEntityCount;
    this.maxEntitiesPerDoc = maxEntitiesPerDoc;
    corpusEntities = new CorpusEntitySet();
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
   * Returns the {@link CorpusEntitySet} underlying this parser.
   * 
   * @return
   */
  public CorpusEntitySet getCorpusEntitySet() {
    return corpusEntities;
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
   * Returns the total number of entities in the corpus being parsed.
   * 
   * @return
   */
  public int getNumEntities() {
    return corpusEntities.getNumEntities();
  }

  /**
   * Returns the list of entities in the corpus.
   * 
   * @return
   */
  public ArrayList<Entity> getEntities() {
    return corpusEntities.getEntities();
  }
  
  /**
   * Returns the array (list) of entities for each document in the corpus.
   * 
   * @return
   */
  public Entity[][] getDocumentEntities() {
    return documentEntities;
  }

  /**
   * Parses the corpus to get entities for each document.
   */
  public void parseCorpus() {
    ArrayList<ArrayList<Entity>> docEntities = new ArrayList<ArrayList<Entity>>(
        documentNames.size());
    ArrayList<Entity> entities, copy;
    try {
      for (String doc : documentNames) {
        String content = reader.readDocument(corpusDir + "/" + doc);
        entities = classifyDocument(content);
        // maintain 2 different list of entities
        copy = new ArrayList<Entity>(entities.size());
        for (Entity entity : entities) {
          copy.add(entity.clone());
        }
        docEntities.add(copy);
        corpusEntities.add(entities);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // convert data to desirable form by LDA model
    corpusEntities.setMinEntityCount(minEntityCount);
    setCorpusData(corpusEntities, docEntities);
  }

  /**
   * Sets the corpus data that can be returned to other methods.
   */
  void setCorpusData(CorpusEntitySet corpusEntities,
      ArrayList<ArrayList<Entity>> docEntities) {
    int numDocuments = documentNames.size();
    documentEntities = new Entity[numDocuments][];
    ArrayList<Entity> entities;
    for (int i = 0; i < numDocuments; i++) {
      entities = docEntities.get(i);
      ObjectToCounterMap<Entity> counter = new ObjectToCounterMap<Entity>();
      // get entities that are in corpus (have count > mincount)
      for (Entity entity : entities) {
        if (corpusEntities.toId(entity) > -1) {
          counter.set(entity, entity.count);
        }
      }

      // number of unique entities to take
      int size = counter.size() > maxEntitiesPerDoc ? maxEntitiesPerDoc : counter.size();
      List<Entity> holder = counter.keysOrderedByCountList().subList(0, size);
      documentEntities[i] = new Entity[size];
      for (int k = 0; k < size; k++) {
        documentEntities[i][k] = holder.get(k);
      }
    }
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
  ArrayList<Entity> getDistinctEntities(ArrayList<Entity> entities) {
    HashSet<Entity> set = new HashSet<Entity>();
    for (int i = 0; i < entities.size(); i++) {
      // find the longest representation for each entity
      Entity longest = entities.get(i), temp;
      for (int j = 0; j < entities.size(); j++) {
        temp = Entity.mergeRepresentation(longest, entities.get(j));
        if (temp != null) {
          longest = temp;
        }
      }
      set.add(longest);
    }
    
    return new ArrayList<Entity>(set);
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
    String corpusDir = "/home/trung/workspace/util/nytimes/general";
    File dir = new File(corpusDir);
    ArrayList<String> docNames = new ArrayList<String>();
    for (File file : dir.listFiles()) {
      if (file.isFile()) {
        docNames.add(file.getName());
      }
    }
    // TODO(trung): remove after testing
    ArrayList<String> holder = new ArrayList<String>(1000);
    for (int i = 0; i < 1000; i++) {
      holder.add(docNames.get((int) (Math.random() * docNames.size())));
    }
    docNames = holder;
//    EntityParser parser = new EntityParser("data/smalltest",
//        new DefaultDocumentReader(), docNames, 10, 10);
    EntityParser parser = new EntityParser(corpusDir, new NYTimesDocumentReader(),
        docNames, 4, 3);
    parser.setAcceptedEntityType(true, false, true);
    parser.parseCorpus();
    System.out.println("Number of entities: " + parser.getNumEntities());
    // print out the entities
    PrintWriter out = new PrintWriter("entity.txt");
    ArrayList<Entity> entities = parser.getEntities();
    for (Entity entity : entities) {
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
