package edu.kaist.uilab.plda.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.ModifyTokenTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.ObjectToCounterMap;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.util.StringUtils;

/**
 * A class that prepares data for the model.
 * 
 * TODO(trung): write unit-test for this class.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class DataPreprocessor {
  
  private static final String[] STOPWORD_LIST = new String[] {};
  private static final Set<String> STOPWORD_SET = new HashSet<String>(Arrays
      .asList(STOPWORD_LIST));
  private static PrintWriter logger;
  
  private final int minTokenCount = 5;
  private TokenizerFactory tokenizerFactory;
  private String corpusFile;
  private SymbolTable symbolTable;

  /**
   * Constructor
   * 
   * @param corpusFile
   */
  public DataPreprocessor(String corpusFile, int numTopics) {
    tokenizerFactory = customTokenizerFactory();
    this.corpusFile = corpusFile;
    symbolTable = new MapSymbolTable();
  }

  public static void main(String args[]) throws IOException {
    logger = new PrintWriter("log.txt");
    //DataPreprocessor processor = new DataPreprocessor("", 5);
    
    String serializedClassifier = "classifiers/ner-eng-ie.crf-3-all2008-distsim.ser.gz";
    AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
    // file processing
    String fileContents = StringUtils.slurpFile("data/smalltest/1.txt");
    List<List<CoreLabel>> out = classifier.classify(fileContents);
    for (List<CoreLabel> sentence : out) {
      for (CoreLabel word : sentence) {
        logger.print(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
      }
      logger.println();
    }
    out = classifier.classifyFile("data/smalltest/2.txt");
    for (List<CoreLabel> sentence : out) {
      for (CoreLabel word : sentence) {
        logger.print(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
      }
      logger.println();
    }
    
    // text
    String s1 = "Good afternoon Rajat Raina, how are you today?";
    String s2 = "I go to school at Stanford University, which is located in California.";
    logger.println(classifier.classifyToString(s1));
    logger.println(classifier.classifyWithInlineXML(s2));
    logger.println(classifier.classifyToString(s2, "xml", true));
    
    logger.close();
  }
  
  /**
   * Returns the Gibbs sample for this model.
   * 
   * @return
   */
  public void tokenizeCorpus() throws IOException {
    // TODO(trung): this is the int[][] documents that we can use for our LDA model
    int[][] docTokens = LatentDirichletAllocation.tokenizeDocuments(
        readCorpus(new File(corpusFile)), tokenizerFactory, symbolTable,
        minTokenCount);

    int numTokens = 0;
    for (int[] tokens : docTokens) {
      numTokens += tokens.length;
    }
    logger.println("Tokenized.  #Tokens After Pruning=" + numTokens);
  }

  /**
   * Reports statistics about the corpus.
   */
  public void reportCorpusStatistics() throws IOException {
    CharSequence[] cSeqs = readCorpus(new File(corpusFile));
    ObjectToCounterMap<String> tokenCounter = new ObjectToCounterMap<String>();
    for (CharSequence cSeq : cSeqs) {
      char[] cs = cSeq.toString().toCharArray();
      for (String token : tokenizerFactory.tokenizer(cs, 0, cs.length))
        tokenCounter.increment(token);
    }

    logger.println("TOKEN COUNTS");
    for (String token : tokenCounter.keysOrderedByCountList())
      logger.printf("%9d %s\n", tokenCounter.getCount(token), token);

    logger.println("Number of unique words above count threshold="
        + symbolTable.numSymbols());
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
   * Reads the entire corpus for training the model.
   * 
   * TODO(trung): write unit-test for this method
   * 
   * @param directory
   * @return
   * @throws IOException
   */
  private CharSequence[] readCorpus(File corpus) throws IOException {
    ArrayList<CharSequence> documents = new ArrayList<CharSequence>(corpus.listFiles().length);
    BufferedReader reader;
    String content;
    int pos;
    for (File file : corpus.listFiles()) {
      reader = new BufferedReader(new FileReader(file));
      // ignore the first 3 lines
      for (int i = 0; i < 3; i++) {
        reader.readLine();
      }
      content = reader.readLine();
      pos = content.indexOf('-');
      // remove the reporting LOCATION at the beginning of an article (if exists)
      if (pos > 0 && pos < 30) {
        documents.add(content.substring(pos + 1));
      } else {
        documents.add(content);
      }  
      reader.close();
    }
    
    return documents.<CharSequence> toArray(new CharSequence[documents.size()]);
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
    factory = new StopTokenizerFactory(factory, STOPWORD_SET);

    return factory;
  }

  /**
   * Tokenizer that only accepts token that contains letters.
   */
  static class NonAlphaStopTokenizerFactory extends ModifyTokenTokenizerFactory {
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
