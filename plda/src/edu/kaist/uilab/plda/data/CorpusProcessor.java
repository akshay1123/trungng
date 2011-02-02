package edu.kaist.uilab.plda.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.ModifyTokenTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Counter;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.Strings;

import edu.kaist.uilab.plda.file.DocumentReader;

/**
 * A class that prepares data for the model.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class CorpusProcessor {

  private String corpusDir;
  private DocumentReader reader;
  private SymbolTable symbolTable;
  private TokenizerFactory tokenizerFactory;
  private EntityParser entityParser;
  // to maintain the same order of documents in the corpus
  private ArrayList<String> docNames;
  private int minTokenCount;
  private int minEntityCount;
  private int topStopWords;
  private int maxEntitiesPerDoc;
  private int[][] documentTokens;
  private Entity[][] documentEntities;
  private CharSequence[] cSeqs;
  private Set<String> stopWords;

  /**
   * Constructor
   * 
   * @param corpusDir
   * @param reader
   * @param minTokenCount
   * @param minEntityCount
   * @param topStopWords
   * @param maxEntitiesPerDoc
   */
  public CorpusProcessor(String corpusDir, DocumentReader reader, int minTokenCount,
      int minEntityCount, int topStopWords, int maxEntitiesPerDoc, String[] stopwordList) {
    this.corpusDir = corpusDir;
    this.reader = reader;
    this.minTokenCount = minTokenCount;
    this.minEntityCount = minEntityCount;
    this.topStopWords = topStopWords;
    this.maxEntitiesPerDoc = maxEntitiesPerDoc;
    this.stopWords = new HashSet<String>(Arrays.asList(stopwordList));
  }

  /**
   * Processes data in this corpus.
   * 
   * <p>
   * After calling this method, all properties of the corpus can be queried
   * using the various getter methods.
   */
  public void process() throws IOException {
    File dir = new File(corpusDir);
    docNames = new ArrayList<String>();
    for (File file : dir.listFiles()) {
      if (file.isFile()) {
        docNames.add(file.getName());
      }
    }
    // TODO(trung): remove after testing
//    ArrayList<String> holder = new ArrayList<String>(3000);
//    for (int i = 0; i < 3000; i++) {
//      holder.add(docNames.get((int) (Math.random() * docNames.size())));
//    }
//    docNames = holder;
    
    //docNames = new ArrayList<String>(docNames.subList(0, 1000));

    System.out.println("\nParsing the corpus for entities...");
    entityParser = new EntityParser(corpusDir, reader, docNames,
        minEntityCount, maxEntitiesPerDoc);
    entityParser.setAcceptedEntityType(true, false, true);
    entityParser.parseCorpus();
    documentEntities = entityParser.getDocumentEntities();
    System.out.println("\nParsing entities done!");
    tokenizerFactory = customTokenizerFactory();
    symbolTable = new MapSymbolTable();

    System.out.println("Tokenizing the corpus to tokens...");
    cSeqs = readCorpus(docNames);
    documentTokens = tokenizeDocuments(cSeqs, tokenizerFactory, symbolTable,
        minTokenCount);
    System.out.println("Tokenizing done!");
  }

  /**
   * Returns the {@link CorpusEntitySet} underlying this corpus processor.
   * 
   * @return
   */
  public CorpusEntitySet getCorpusEntitySet() {
    return entityParser.getCorpusEntitySet();
  }
  
  /**
   * Returns the vocabulary size of this corpus.
   * 
   * @return
   */
  public int getVocabularySize() {
    return symbolTable.numSymbols();
  }

  /**
   * Returns the number of documents in this corpus.
   * 
   * @return
   */
  public int getNumDocuments() {
    return docNames.size();
  }

  /**
   * Returns the number of entities in this corpus.
   * 
   * @return
   */

  public int getNumEntities() {
    return entityParser.getNumEntities();
  }

  /**
   * Returns the list (array) of tokens for each document in the corpus.
   */
  public int[][] getDocumentTokens() throws IOException {
    return documentTokens;
  }

  /**
   * Returns the entities of all documents.
   * 
   * @return
   */
  public Entity[][] getDocumentEntities() {
    return documentEntities;
  }

  /**
   * Gets the symbol table of this corpus.
   * 
   * @return
   */
  public SymbolTable getSymbolTable() {
    return symbolTable;
  }

  /**
   * Reports statistics about the corpus.
   * 
   * @param corpusFile
   *          file to store corpus information
   * @param documentsFile
   *          file to store the names of documents
   * @param entitiesFile
   *          file to store all entities in the corpus
   * @param docEntitiesFile
   *          file to store entities of each document in the corpus
   * @param tokensFile
   *          file to store all tokens in the corpus
   */
  public void reportCorpus(String corpusFile, String documentsFile,
      String entitiesFile, String docEntitiesFile, String tokensFile)
      throws IOException {
    writeCorpus(corpusFile);
    writeDocumentNames(documentsFile);
    writeEntities(entitiesFile);
    writeDocEntities(docEntitiesFile);
    writeTokens(tokensFile);
  }

  private void writeCorpus(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    out.printf("Corpus directory: %s\n", corpusDir);
    ObjectToCounterMap<String> tokenCounter = new ObjectToCounterMap<String>();
    int numTokens = 0;
    for (CharSequence cSeq : cSeqs) {
      char[] cs = cSeq.toString().toCharArray();
      for (String token : tokenizerFactory.tokenizer(cs, 0, cs.length)) {
        tokenCounter.increment(token);
        numTokens++;
      }
    }
    out.printf("# tokens: %d\n", numTokens);
    out.printf("# unique tokens: %d (minTokenCount = %d)\n",
        getVocabularySize(), minTokenCount);
    out.printf("# entities: %d (minEntityCount = %d)\n", getNumEntities(),
        minEntityCount);
    out.println("TOKEN COUNTS");
    for (String token : tokenCounter.keysOrderedByCountList()) {
      out.printf("%9d %s\n", tokenCounter.getCount(token), token);
    }
    
    out.close();
  }

  private void writeDocumentNames(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (String doc : docNames) {
      out.println(doc);
    }
    out.close();
  }

  private void writeTokens(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int id = 0; id < symbolTable.numSymbols(); id++) {
      out.println(symbolTable.idToSymbol(id));
    }
    out.close();
  }

  private void writeEntities(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    ArrayList<Entity> entities = entityParser.getEntities();
    for (Entity entity : entities) {
      out.println(entity);
    }
    out.close();
  }

  private void writeDocEntities(String file) throws IOException {
    PrintWriter out = new PrintWriter(file);
    for (int docIdx = 0; docIdx < documentEntities.length; docIdx++) {
      for (int entityIdx = 0; entityIdx < documentEntities[docIdx].length; entityIdx++) {
        out.print(documentEntities[docIdx][entityIdx] + ",");
      }
      out.println();
    }
    out.close();
  }

  /**
   * Reads the entire corpus for training the model.
   * 
   * @param directory
   * @return
   * @throws IOException
   */
  private CharSequence[] readCorpus(ArrayList<String> docNames) {
    ArrayList<CharSequence> documents = new ArrayList<CharSequence>(
        docNames.size());
    try {
      for (String doc : docNames) {
        documents.add(reader.readDocument(corpusDir + "/" + doc));
      }
    } catch (IOException e) {
      // do nothing to continue reading other docs
    }

    return documents.<CharSequence> toArray(new CharSequence[documents.size()]);
  }

  /**
   * Tokenize an array of text documents represented as character sequences into
   * a form usable by LDA, using the specified tokenizer factory and symbol
   * table. The symbol table should be constructed fresh for this application,
   * but may be used after this method is called for further token to symbol
   * conversions. Only tokens whose count is equal to or larger the specified
   * minimum count are included. Only tokens whose count exceeds the minimum are
   * added to the symbol table, thus producing a compact set of symbol
   * assignments to tokens for downstream processing.
   * 
   * <p>
   * <i>Warning</i>: With some tokenizer factories and or minimum count
   * thresholds, there may be documents with no tokens in them.
   * 
   * @param texts
   *          The text corpus.
   * @param tokenizerFactory
   *          A tokenizer factory for tokenizing the texts.
   * @param symbolTable
   *          Symbol table used to convert tokens to identifiers.
   * @param minCount
   *          Minimum count for a token to be included in a document's
   *          representation.
   * @return The tokenized form of a document suitable for input to LDA.
   */
  private int[][] tokenizeDocuments(CharSequence[] texts,
      TokenizerFactory tokenizerFactory, SymbolTable symbolTable, int minCount) {
    ObjectToCounterMap<String> tokenCounter = new ObjectToCounterMap<String>();
    for (CharSequence text : texts) {
      char[] cs = Strings.toCharArray(text);
      Tokenizer tokenizer = tokenizerFactory.tokenizer(cs, 0, cs.length);
      for (String token : tokenizer)
        tokenCounter.increment(token);
    }
    tokenCounter.prune(minCount);
    pruneTopWords(tokenCounter, topStopWords);
    Set<String> tokenSet = tokenCounter.keySet();
    for (String token : tokenSet)
      symbolTable.getOrAddSymbol(token);

    int[][] docTokenId = new int[texts.length][];
    for (int i = 0; i < docTokenId.length; ++i) {
      docTokenId[i] = tokenizeDocument(texts[i], tokenizerFactory, symbolTable);
    }
    return docTokenId;
  }

  /**
   * Prunes the top {@code num} tokens from the vocabulary set.
   * 
   * @param tokenCounter
   * @param num
   */
  private void pruneTopWords(ObjectToCounterMap<String> tokenCounter, int num) {
    HashSet<String> topKeys = new HashSet<String>(
        tokenCounter.keysOrderedByCountList().subList(0, num));
    Iterator<Map.Entry<String, Counter>> it = tokenCounter.entrySet().iterator();
    while (it.hasNext()) {
      if (topKeys.contains(it.next().getKey())) {
        it.remove(); // remove this entry
      }   
    }
  }

  /**
   * Tokenizes the specified text document using the specified tokenizer factory
   * returning only tokens that exist in the symbol table. This method is useful
   * within a given LDA model for tokenizing new documents into lists of words.
   * 
   * @param text
   *          Character sequence to tokenize.
   * @param tokenizerFactory
   *          Tokenizer factory for tokenization.
   * @param symbolTable
   *          Symbol table to use for converting tokens to symbols.
   * @return The array of integer symbols for tokens that exist in the symbol
   *         table.
   */
  private int[] tokenizeDocument(CharSequence text,
      TokenizerFactory tokenizerFactory, SymbolTable symbolTable) {
    char[] cs = Strings.toCharArray(text);
    Tokenizer tokenizer = tokenizerFactory.tokenizer(cs, 0, cs.length);
    List<Integer> idList = new ArrayList<Integer>();
    for (String token : tokenizer) {
      int id = symbolTable.symbolToID(token);
      if (id >= 0)
        idList.add(id);
    }
    int[] tokenIds = new int[idList.size()];
    for (int i = 0; i < tokenIds.length; ++i)
      tokenIds[i] = idList.get(i);

    return tokenIds;
  }

  /**
   * Returns the custom tokenizer factory used for this corpus.
   * 
   * @return
   */
  private TokenizerFactory customTokenizerFactory() {
    TokenizerFactory factory = new RegExTokenizerFactory("[$a-zA-Z0-9]+");
    factory = new NonAlphaStopTokenizerFactory(factory);
    factory = new LowerCaseTokenizerFactory(factory);
    factory = new EnglishStopTokenizerFactory(factory);
    factory = new StopTokenizerFactory(factory, stopWords);

    return factory;
  }

  /**
   * Tokenizer that only accepts token that contains letters.
   */
  static final class NonAlphaStopTokenizerFactory extends
      ModifyTokenTokenizerFactory {
    static final long serialVersionUID = -3401639068551227864L;

    public NonAlphaStopTokenizerFactory(TokenizerFactory factory) {
      super(factory);
    }

    public String modifyToken(String token) {
      return stop(token) ? null : token;
    }

    public boolean stop(String token) {
      if (token.length() < 2)
        return true;
      for (int i = 0; i < token.length(); ++i)
        if (Character.isLetter(token.charAt(i)))
          return false;
      return true;
    }
  }
}
