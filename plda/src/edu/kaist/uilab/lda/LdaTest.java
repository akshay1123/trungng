package edu.kaist.uilab.lda;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Counter;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.Strings;

import edu.kaist.uilab.plda.util.TextFiles;

public class LdaTest {
  static String[] stopWords = {
    "i",
    "me",
    "you",
    "us",
    "them",
    "they",
    "he",
    "she",
    "him",
    "her",
    "it",
    "your",
    "our",
    "their",
    "her",
    "his",
    "what",
    "every",
    "how",
    "will",
    "shall",
    "did",
    "myself",
    "too",
    "oh",
    "no",
    "yes",
    "let",
    "yet",
    "even",
  };
  
  private static int topStopWords = 100;
  
  public static void main(String args[]) throws IOException {
    LdaTest test = new LdaTest();
    // corpus, output, numTopics
//    test.run("novels/pride.txt", "novels/pride5.txt", (short) 5);
//    test.run("novels/pride.txt", "novels/pride7.txt", (short) 7);
//    test.run("novels/pride.txt", "novels/pride10.txt", (short) 10);
//    test.run("novels/pride.txt", "novels/pride15.txt", (short) 15);
    test.run("novels/quixote.txt", "novels/quixote5.txt", (short) 5);
    test.run("novels/quixote.txt", "novels/quixote7.txt", (short) 7);
    test.run("novels/quixote.txt", "novels/quixote10.txt", (short) 10);
    test.run("novels/quixote.txt", "novels/quixote15.txt", (short) 15);
    test.run("novels/alice.txt", "novels/alice5.txt", (short) 5);
    test.run("novels/alice.txt", "novels/alice7.txt", (short) 7);
    test.run("novels/alice.txt", "novels/alice10.txt", (short) 10);
    test.run("novels/alice.txt", "novels/alice15.txt", (short) 15);
    test.run("novels/holmes.txt", "novels/holmes5.txt", (short) 5);
    test.run("novels/holmes.txt", "novels/holmes7.txt", (short) 7);
    test.run("novels/holmes.txt", "novels/holmes10.txt", (short) 10);
    test.run("novels/holmes.txt", "novels/holmes15.txt", (short) 15);
  }
  
  public void run(String corpusFile, String outputFile, short numTopics)
      throws IOException {
    System.err.printf("Running test on %s with %d topics\n", corpusFile, numTopics);
    final double docTopicPrior = 0.1; // alpha
    final double topicWordPrior = 0.01; // beta
    final int burninEpochs = 1000; // burnin
    final int sampleLag = 100;
    final int numSamples = 10; // runs 10 and takes the last
    final int minCount = 3;
    final SymbolTable symbolTable = new MapSymbolTable();
    final Random random = new Random(2321);
    // parse the corpus
    final int[][] docWords = tokenizeDocuments(parseCorpus(corpusFile),
        customTokenizerFactory(), symbolTable, minCount);
    System.out.println("Documents tokenized.");
    final LdaReportingHandler handler = new LdaReportingHandler(symbolTable);
    final LatentDirichletAllocation.GibbsSample sample = LatentDirichletAllocation
        .gibbsSampler(docWords, numTopics, docTopicPrior, topicWordPrior,
            burninEpochs, sampleLag, numSamples, random, handler);
    handler.fullReport(sample, outputFile, 30, 10, true);
    System.out.println("Reported to file " + outputFile);
  }
  
  /**
   * Returns the custom tokenizer factory used for this corpus.
   * 
   * @return
   */
  TokenizerFactory customTokenizerFactory() {
    TokenizerFactory factory = new RegExTokenizerFactory("[$a-zA-Z0-9]+");
//    factory = new NonAlphaStopTokenizerFactory(factory);
    factory = new LowerCaseTokenizerFactory(factory);
    factory = new EnglishStopTokenizerFactory(factory);
    factory = new StopTokenizerFactory(factory,
        new HashSet<String>(Arrays.asList(stopWords)));

    return factory;
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
    // count #(a token appears in a document)
    ObjectToCounterMap<String> tokDocumentCounter = new ObjectToCounterMap<String>();
    for (CharSequence text : texts) {
      HashSet<String> uniqueDocTokens = new HashSet<String>();
      char[] cs = Strings.toCharArray(text);
      Tokenizer tokenizer = tokenizerFactory.tokenizer(cs, 0, cs.length);
      for (String token : tokenizer) {
        tokenCounter.increment(token);
        if (uniqueDocTokens.add(token)) {
          tokDocumentCounter.increment(token);
        }
      }  
    }
    tokenCounter.prune(minCount);
    pruneTopWords(tokenCounter, topStopWords);
    Set<String> tokenSet = tokenCounter.keySet();
    for (String token : tokenSet)
      symbolTable.getOrAddSymbol(token);

    int[][] docTokenId = new int[texts.length][];
    for (int i = 0; i < docTokenId.length; ++i) {
      docTokenId[i] = LatentDirichletAllocation.tokenizeDocument(
          texts[i], tokenizerFactory, symbolTable);
    }
    
    return docTokenId;
  }
  
  /**
   * Prunes the top {@code num} tokens from the vocabulary set.
   * 
   * @param tokenCounter
   * @param num
   */
  static void pruneTopWords(ObjectToCounterMap<String> tokenCounter, int num) {
    HashSet<String> topKeys = new HashSet<String>(
        tokenCounter.keysOrderedByCountList().subList(0, num));
    Iterator<Map.Entry<String, Counter>> iter = tokenCounter.entrySet().iterator();
    int count = 0;
    while (iter.hasNext()) {
      if (topKeys.contains(iter.next().getKey())) {
        iter.remove();
        count++;
      }
    }
    System.err.printf("%d stop words pruned.\n", count);
  }
  
  /**
   * Parses the corpus and returns each document as a charsequence.
   * 
   * <p> For novels from Gutenberg, each paragraph is separated by the "{"
   * character.
   * 
   * @param file
   * @return
   */
  CharSequence[] parseCorpus(String file) throws IOException {
    StringTokenizer tokenizer = new StringTokenizer(TextFiles.readFile(file), "{");
    CharSequence[] cSeqs = new CharSequence[tokenizer.countTokens()];
    int idx = 0;
    while (tokenizer.hasMoreTokens()) {
      cSeqs[idx++] = tokenizer.nextToken();
    }
    
    return cSeqs;
  }
}
