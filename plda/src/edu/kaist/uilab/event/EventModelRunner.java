package edu.kaist.uilab.event;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import edu.kaist.uilab.plda.file.DefaultDocumentReader;

public class EventModelRunner {
  static String[] stopword = new String[] {
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
    "monday",
    "tuesday",
    "wednesday",
    "thursday",
    "friday",
    "saturday",
    "sunday",
    "mon",
    "tue",
    "wed",
    "thu",
    "fri",
    "sat",
    "sun",
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
    "what",
    "although",
    "though",
    "too",
    "just",
    "where",
    "while",
    "then",
    "since",
    "could",
    "should",
    "throughout",
    "still",
    "several",
    "day",
    "days",
    "our",
    "go",
    "these",
    "very",
    "next",
    "me",
    "early",
    "until",
    "ago",
    "now",
    "today",
    "yesterday",
    "tomorrow",
    "week",
    "month",
    "year",
    "years",
    "weeks",
    "months",
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december",
    "continued",
//    "born",
//    "died",
    "get",
    "send",
    "sent",
    "got",
    "move",
    "moved",
    "become",
    "became",
    "took",
    "spent",
    "went",
    "going",
    "doing",
    "being",
//    "meet",
//    "met",
    "did",
    "made",
    "make",
    "do",
    "said",
    "does",
    "old",
    "through",
    "soon",
    "back",
    "never",
    "name",
    "another",
//    "named",
    "able",
    "how",
    "few",
    "those",
    "them",
    "herself",
    "himself",
    "told",
    "new", // for new york times
    "york",
    "times",
  };
  
  public static void main(String args[]) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader("eventruns.txt"));
    String line;
    int minTokenCount = 5;
    int minEntityCount = 3;
    int topStopWords = 70;
    int maxDocumentCount = 70; // maybe 50 (50 gives 0)?

    int numEvents = 100;
    double alpha = 0.1;
    double beta = 0.01;
    double gamma = 0.01;
    CorpusProcessor corpus = new CorpusProcessor("C:/datasets/bbchistory",
        new DefaultDocumentReader(), minTokenCount, minEntityCount,
        topStopWords, stopword, maxDocumentCount);
    //  corpus = new CorpusProcessor("/home/trung/elda/data/bbchistory",
    //  new DefaultDocumentReader(), minTokenCount, minEntityCount,
    //  topStopWords, maxEntitiesPerDoc, stopword);
    //corpus = new CorpusProcessor("/home/trung/workspace/util/nytimes/general",
    //  new NYTimesDocumentReader(), minTokenCount, minEntityCount,
    //  topStopWords, maxEntitiesPerDoc, stopword);
    corpus.process();

    while ((line = in.readLine()) != null) {
      if (line.charAt(0) != '#' ) {
        // parse the parameters
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        numEvents = Integer.parseInt(tokenizer.nextToken());
        alpha = Double.parseDouble(tokenizer.nextToken());
        beta = Double.parseDouble(tokenizer.nextToken());
        gamma = Double.parseDouble(tokenizer.nextToken());
        // use 50/T instead which seems to give better result
        // alpha = 50 / numEvents;
        String outputDir = String.format("C:/events/bbchistory%d_a%.2f_b%.2f_g%.2f",
            numEvents, alpha, beta, gamma);
        System.out.println(outputDir);
        EventGibbsSampler sampler;
        (new File(outputDir)).mkdir();
        corpus.reportCorpus(outputDir + "/corpus.txt",
            outputDir + "/docNames.txt",
            outputDir + "/entity.txt",
            outputDir + "/docEntity.txt",
            outputDir + "/token.txt");
        
        sampler = new EventGibbsSampler(numEvents,
            corpus.getVocabularySize(),
            corpus.getNumEntities(),
            corpus.getDocumentTokens(),
            corpus.getDocumentEntities());
        sampler.setPriors(alpha, beta, gamma);
        sampler.setSamplerParameters(5000, 300, 20, 10);
        sampler.setOutputParameters(corpus.getSymbolTable(),
            corpus.getEntityTable(), outputDir, 30, 10, 10);
        System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
        sampler.doGibbsSampling(false);
      }
    }
    in.close();
  }
}
