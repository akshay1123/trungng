package edu.kaist.uilab.plda.lingpipe;

import java.io.PrintWriter;
import java.util.List;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.cluster.LatentDirichletAllocation.GibbsSample;
import com.aliasi.corpus.ObjectHandler;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.util.ObjectToCounterMap;

/**
 * Handler to report the lda samples.
 */
public class LdaReportingHandler implements
    ObjectHandler<LatentDirichletAllocation.GibbsSample> {

  private final SymbolTable mSymbolTable;
  @SuppressWarnings("unused")
  private final long mStartTime;

  LdaReportingHandler(SymbolTable symbolTable) {
    mSymbolTable = symbolTable;
    mStartTime = System.currentTimeMillis();
  }

  void fullReport(LatentDirichletAllocation.GibbsSample sample, String file,
      int maxWordsPerTopic, int maxTopicsPerDoc, boolean reportTokens) {
    try {
      PrintWriter writer = new PrintWriter(file);
      writer.println("\nFull Report");
      int numTopics = sample.numTopics();
      int numWords = sample.numWords();
      int numDocs = sample.numDocuments();
      int numTokens = sample.numTokens();

      writer.println("epoch=" + sample.epoch());
      writer.println("numDocs=" + numDocs);
      writer.println("numTokens=" + numTokens);
      writer.println("numWords=" + numWords);
      writer.println("numTopics=" + numTopics);

      for (int topic = 0; topic < numTopics; ++topic) {
        int topicCount = sample.topicCount(topic);
        ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
        for (int word = 0; word < numWords; ++word)
          counter.set(Integer.valueOf(word), sample.topicWordCount(topic, word));
        List<Integer> topWords = counter.keysOrderedByCountList();
        writer.println("\nTOPIC " + topic + "  (total count=" + topicCount
            + ")");
        writer.println("SYMBOL             WORD    COUNT   PROB          Z");
        writer.println("--------------------------------------------------");
        for (int rank = 0; rank < maxWordsPerTopic && rank < topWords.size(); ++rank) {
          int wordId = topWords.get(rank);
          String word = mSymbolTable.idToSymbol(wordId);
          int wordCount = sample.wordCount(wordId);
          int topicWordCount = sample.topicWordCount(topic, wordId);
          double topicWordProb = sample.topicWordProb(topic, wordId);
          double z = binomialZ(topicWordCount, topicCount, wordCount, numTokens);
          writer.printf("%6d  %15s  %7d   %4.3f  %8.1f\n", wordId, word,
              topicWordCount, topicWordProb, z);
        }
      }

      for (int doc = 0; doc < numDocs; ++doc) {
        int docCount = 0;
        for (int topic = 0; topic < numTopics; ++topic)
          docCount += sample.documentTopicCount(doc, topic);
        ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
        for (int topic = 0; topic < numTopics; ++topic)
          counter.set(Integer.valueOf(topic), sample.documentTopicCount(doc,
              topic));
        List<Integer> topTopics = counter.keysOrderedByCountList();
        writer.println("\nDOC " + doc);
        writer.println("TOPIC    COUNT    PROB");
        writer.println("----------------------");
        for (int rank = 0; rank < topTopics.size() && rank < maxTopicsPerDoc; ++rank) {
          int topic = topTopics.get(rank);
          int docTopicCount = sample.documentTopicCount(doc, topic);
          double docTopicPrior = sample.documentTopicPrior();
          double docTopicProb = (sample.documentTopicCount(doc, topic) + docTopicPrior)
              / (docCount + numTopics * docTopicPrior);
          writer.printf("%5d  %7d   %4.3f\n", topic, docTopicCount,
              docTopicProb);
        }
        writer.println();
        if (!reportTokens)
          continue;
        int numDocTokens = sample.documentLength(doc);
        for (int tok = 0; tok < numDocTokens; ++tok) {
          int symbol = sample.word(doc, tok);
          short topic = sample.topicSample(doc, tok);
          String word = mSymbolTable.idToSymbol(symbol);
          writer.print(word + "(" + topic + ") ");
        }
        writer.println();
      }
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static double binomialZ(double wordCountInDoc, double wordsInDoc,
      double wordCountinCorpus, double wordsInCorpus) {
    double pCorpus = wordCountinCorpus / wordsInCorpus;
    double var = wordsInCorpus * pCorpus * (1 - pCorpus);
    double dev = Math.sqrt(var);
    double expected = wordsInDoc * pCorpus;
    double z = (wordCountInDoc - expected) / dev;
    return z;
  }

  @Override
  public void handle(GibbsSample e) {
    // TODO Auto-generated method stub
    
  }

}