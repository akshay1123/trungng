package edu.kaist.uilab.plda;

import java.io.File;
import java.io.IOException;

import edu.kaist.uilab.plda.data.CorpusProcessor;
import edu.kaist.uilab.plda.file.DefaultDocumentReader;

/**
 * TODO(trung):
 * 1. Try beta instead of uniform
 * 2. High probability for entities with higher frequency (not uniform)
 * [harder for inference?]
 * 3. Limit the number of entities for a document (top 10?, top 60%)
 * 4. Varies min token, min entity (of course)
 * 5. Try NYTimes corpus (of course)
 * 
 * 6. The UNIFORM assumption for entities might not be a good one (depends on
 * the corpus). Discrete (~approximate with the frequency of entities)
 * (This is similar to 2)
 * 
 * @author trung nguyen
 */
public class ModelReporter {
  final double alpha = 0.1;
  final double beta = 0.01;
  final double gamma = 0.1;
  final int numTopics = 100;
  final int minTokenCount = 5;
  final int minEntityCount = 10;
  final int topStopWords = 40;
  final int maxEntitiesPerDoc = 10;
  private String outputDir;
  private CorpusProcessor corpus;
  private EntityLdaGibbsSampler sampler;
  
  public ModelReporter(String outputDir) throws IOException {
    this.outputDir = outputDir;
    (new File(outputDir)).mkdir();
    corpus = new CorpusProcessor("data/smalltest", new DefaultDocumentReader(),
        minTokenCount, minEntityCount, topStopWords, maxEntitiesPerDoc);
    corpus.process();
    report();
    sampler = new EntityLdaGibbsSampler(numTopics,
        corpus.getVocabularySize(),
        corpus.getNumEntities(),
        corpus.getDocumentTokens(),
        corpus.getDocumentEntities(),
        corpus.getCorpusEntitySet(),
        alpha,
        beta,
        gamma);
    sampler.setSamplerParameters(1000, 1000, 1, 5);
    sampler.setOutputParameters(corpus.getSymbolTable(), outputDir, 30, 10, 10);
    System.out.println("Latent Dirichlet Allocation using Gibbs Sampling.");
    sampler.doGibbsSampling();
  }

  public static void main(String args[]) throws IOException {
//    new ModelReporter(
//        "C:/elda/smalltest10_tok5_ent7_stop40_maxent60p");
    new ModelReporter("/home/trung/elda/smalltest100_ent7_iter1000_maxent10");
  }
  
  public void report() throws IOException {
    corpus.reportCorpus(outputDir + "/corpus.txt",
        outputDir + "/docNames.txt",
        outputDir + "/entity.txt",
        outputDir + "/docEntity.txt",
        outputDir + "/token.txt");
  }
}
