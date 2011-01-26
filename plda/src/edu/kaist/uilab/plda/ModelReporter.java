package edu.kaist.uilab.plda;

import java.io.File;
import java.io.IOException;

import edu.kaist.uilab.plda.data.CorpusProcessor;
import edu.kaist.uilab.plda.file.ReutersDocumentReader;

/**
 * TODO(trung):
 * 1. Try beta instead of uniform
 * 3. Limit the number of entities for a document (top 10?, top 60%)
 * 4. Varies min token, min entity (of course)
 * 
 * 6. The UNIFORM assumption for entities might not be a good one (depends on
 * the corpus). Discrete (~approximate with the frequency of entities)
 * (This is similar to 2)
 * 
 * @author trung nguyen
 */
public class ModelReporter {
  
  public static void main(String args[]) throws IOException {
    double alpha = 0.1;
    double beta = 0.01;
    double gamma = 0.1;
    int numTopics = 200;
    int minTokenCount = 5;
    int minEntityCount = 10;
    int topStopWords = 40;
    int maxEntitiesPerDoc = 5;
    String outputDir = "/home/trung/elda/reuterstest200_ent10_iter100_maxent5";
    CorpusProcessor corpus;
    EntityLdaGibbsSampler sampler;
    
    (new File(outputDir)).mkdir();
//    corpus = new CorpusProcessor("data/reuterstest", new DefaultDocumentReader(),
//        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc);
    corpus = new CorpusProcessor("data/reuterstest", new ReutersDocumentReader(),
        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc);
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
    sampler.setSamplerParameters(2000, 100, 20, 10);
    sampler.setOutputParameters(corpus.getSymbolTable(), outputDir, 30, 10, 10);
    System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
    sampler.doGibbsSampling();
  }
}
