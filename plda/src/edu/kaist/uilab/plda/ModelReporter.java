package edu.kaist.uilab.plda;

import java.io.File;
import java.io.IOException;

import edu.kaist.uilab.plda.data.CorpusProcessor;
import edu.kaist.uilab.plda.file.DefaultDocumentReader;
import edu.kaist.uilab.plda.file.NYTimesDocumentReader;
import edu.kaist.uilab.plda.file.ReutersDocumentReader;

/**
 * TODO(trung):
 * 1. Try beta instead of uniform
 * 
 * 6. The UNIFORM assumption for entities might not be a good one (depends on
 * the corpus). Discrete (~approximate with the frequency of entities)
 * 
 * @author trung nguyen
 */
public class ModelReporter {
  
  public static void main(String args[]) throws IOException {
    double alpha = 0.1;
    double beta = 0.01;
    double gamma = 0.1;
    int numTopics = 20;
    int minTokenCount = 3;
    int minEntityCount = 15;
    int topStopWords = 40;
    int maxEntitiesPerDoc = 5;
    String outputDir = "/home/trung/elda/nytimesgeneral20_ent15_iter100_maxent5";
    CorpusProcessor corpus;
    EntityLdaGibbsSampler sampler;
    
    (new File(outputDir)).mkdir();
//    corpus = new CorpusProcessor("data/reuterstest", new DefaultDocumentReader(),
//        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc);
    String[] stopword = new String[] {
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "now",
        "today",
        "week",
        "month",
        "year",
        "years",
        "weeks",
        "months",
        "&lt;",
        "mln",
        "cts",
        "dlrs",
        "qtr",
        "pct",
        "vs",
        "lt",
        "reuter",
        "shr",
        "said",
        "what",
        "you",
        "since",
        "off",
        "still",
        "do",
        "several",
        "does",
        "day",
        "days",
        "our",
        "go",
        "these",
        "while",
        "then",
        "next",
        "me",
        "ago",
        "little",
        "too",
        "early",
        "already",
        "every",
        "where",
        "few",
        "each",
        "yesterday",
        "monday",
        "tuesday",
        "wednesday",
        "thursday",
        "friday",
        "saturday",
        "sunday",
        "my",
        "here",
        "how",
    };
//    corpus = new CorpusProcessor("/home/trung/elda/data/nytimes/general", new DefaultDocumentReader(),
//        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc, stopword);
    corpus = new CorpusProcessor("/home/trung/elda/data/nytimes/general", new NYTimesDocumentReader(),
        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc, stopword);
    corpus.process();
    corpus.reportCorpus(outputDir + "/corpus.txt",
        outputDir + "/docNames.txt",
        outputDir + "/entity.txt",
        outputDir + "/docEntity.txt",
        outputDir + "/token.txt");
    
    sampler = new EntityLdaGibbsSampler(numTopics,
        corpus.getVocabularySize(),
        corpus.getNumEntities(),
        corpus.getDocumentTokens(),
        corpus.getDocumentEntities(),
        corpus.getCorpusEntitySet(),
        alpha,
        beta,
        gamma);
    sampler.setSamplerParameters(3000, 100, 20, 2);
    sampler.setOutputParameters(corpus.getSymbolTable(), outputDir, 30, 10, 10);
    System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
    sampler.doGibbsSampling();
  }
}
