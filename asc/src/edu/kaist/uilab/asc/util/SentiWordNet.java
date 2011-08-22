package edu.kaist.uilab.asc.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.kaist.uilab.bs.Model;
import edu.kaist.uilab.stemmers.EnglishStemmer;

public class SentiWordNet {
  private static final String UTF8 = "utf-8";
  private String pathToSWN = "SentiWordNet_3.0.0.txt";
  private HashMap<String, Double> dict;
  private static EnglishStemmer stemmer;

  public SentiWordNet() {
    dict = new HashMap<String, Double>();
    stemmer = new EnglishStemmer();
    HashMap<String, Vector<Double>> map = new HashMap<String, Vector<Double>>();
    try {
      BufferedReader csv = new BufferedReader(new FileReader(pathToSWN));
      String line = "";
      while ((line = csv.readLine()) != null) {
        String[] data = line.split("\t");
        // data[2] = pos, data[3] = neg, data[4] = words (separated by space)
        Double score = Double.parseDouble(data[2])
            - Double.parseDouble(data[3]);
        String[] words = data[4].split(" ");
        for (String w : words) {
          String[] w_n = w.split("#");
          // key = stem#part-of-speech (n, v, a)
          String key = stemmer.getStem(w_n[0]) + "#" + data[0];
          // sense index
          int index = Integer.parseInt(w_n[1]) - 1;
          // v = vector of polarity of each sense
          if (map.containsKey(key)) {
            Vector<Double> v = map.get(key);
            if (index > v.size())
              for (int i = v.size(); i < index; i++)
                v.add(0.0);
            v.add(index, score);
            map.put(key, v);
          } else {
            Vector<Double> v = new Vector<Double>();
            for (int i = 0; i < index; i++)
              v.add(0.0);
            v.add(index, score);
            map.put(key, v);
          }
        }
      }
      Set<String> temp = map.keySet();
      for (Iterator<String> iterator = temp.iterator(); iterator.hasNext();) {
        String word = (String) iterator.next();
        Vector<Double> v = map.get(word);
        double score = 0.0;
        double sum = 0.0;
        for (int i = 0; i < v.size(); i++)
          score += ((double) 1 / (double) (i + 1)) * v.get(i);
        for (int i = 1; i <= v.size(); i++)
          sum += (double) 1 / (double) i;
        score /= sum;
        dict.put(word, score);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Double getPolarity(String word, String partOfSpeech) {
    return dict.get(word + "#" + partOfSpeech);
  }

  public String getLabel(double score) {
    if (score >= 0.75)
      return "strong_positive";
    if (score > 0.25 && score <= 0.5)
      return "positive";
    if (score > 0 && score >= 0.25)
      return "weak_positive";
    if (score < 0 && score >= -0.25)
      return "weak_negative";
    if (score < -0.25 && score >= -0.5)
      return "negative";
    if (score <= -0.75)
      return "strong_negative";
    return "neutral";
  }

  public static void main(String args[]) throws IOException {
//    classifyWordSentiment();
    classifyPhraseSentiment();
  }

  static void classifyPhraseSentiment() throws IOException {
    String dir = "C:/datasets/bs";
    HashSet<String> posPhrases = (HashSet<String>) TextFiles
        .readUniqueLinesAsLowerCase(dir + "/pos.data");
    HashSet<String> negPhrases = (HashSet<String>) TextFiles
        .readUniqueLinesAsLowerCase(dir + "/neg.data");
    HashSet<String> allPhrases = new HashSet<String>(posPhrases);
    allPhrases.addAll(negPhrases);
    ArrayList<String> words;
    SentiWordNet wn = new SentiWordNet();
    Model model = Model.loadModel(dir
        + "/restaurants/T50-A0.1-B0.0010-G1.00,0.10-I1000()--84/1000/model.gz");
    String[][] w = model.getTopAspectWords(50);
    HashSet<String> aspectWords = new HashSet<String>();
    for (String[] topicWords : w) {
      for (String topicWord : topicWords) {
        aspectWords.add(topicWord);
      }
    }
    int numNotClassified = 0, numCorrect = 0;
    for (String phrase : allPhrases) {
      words = stemPhrase(phrase);
      Double score = null;
      boolean hasAspectWord = false;
      for (String word : words) {
        Double wordScore = wn.getPolarity(word, "a");
        if (wordScore != null) {
          if (score == null) {
            score = 0.0;
          }
          score += wordScore;
        }
        if (aspectWords.contains(word)) {
          hasAspectWord = true;
        }
      }
      if (score != null && hasAspectWord) {
        int annotatedSenti = posPhrases.contains(phrase) ? 0 : 1;
        int classifiedSenti = score > 0 ? 0 : 1;
        if (score == 0) {
          classifiedSenti = -1;
        } 
        if (classifiedSenti == annotatedSenti) {
          numCorrect++;
        }
      } else {
        System.err.println("not classified: " + phrase);
        numNotClassified++;
      }
    }
    System.out.println("#positive phrases: " + posPhrases.size());
    System.out.println("#negative phrases: " + negPhrases.size());
    System.out.println("#testing phrases: " + allPhrases.size());
    System.out.println("#phrases not classified: " + numNotClassified);
    System.out.printf("Accuracy: %.3f\n",
        (1.0 * numCorrect) / (allPhrases.size() - numNotClassified));    
  }
  
  /**
   * Returns the list of stemmed words from the given phrase <code>s</code>.
   * 
   * @param s
   * @return
   */
  static ArrayList<String> stemPhrase(String s) {
    StringTokenizer tokenizer = new StringTokenizer(s);
    ArrayList<String> list = new ArrayList<String>();
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      list.add(stemmer.getStem(token));
    }

    return list;
  }

  static void classifyWordSentiment() throws IOException {
    SentiWordNet wn = new SentiWordNet();
    String dir = "C:/datasets/bs";
    HashSet<String> positiveAdjs = new HashSet<String>();
    HashSet<String> negativeAdjs = new HashSet<String>();
    for (String word : TextFiles.readUniqueLines(dir + "/pos.txt", UTF8)) {
      positiveAdjs.add(stemmer.getStem(word));
    }
    for (String word : TextFiles.readUniqueLines(dir + "/neg.txt", UTF8)) {
      negativeAdjs.add(stemmer.getStem(word));
    }
    HashSet<String> adjs = new HashSet<String>(positiveAdjs);
    adjs.addAll(negativeAdjs);
    int numCorrect = 0, numNotClassified = 0, numPolyByNP = 0;
    // measure the accuracy
    for (String word : adjs) {
      // 0: pos, 1: neg, -1: not classified
      int classified = -1;
      Double polarityScore = wn.getPolarity(word, "a");
      if (polarityScore != null) {
        if (polarityScore > 0) {
          classified = 0;
        } else if (polarityScore < 0) {
          classified = 1;
        }
      }
      int annotated = -1;
      if (positiveAdjs.contains(word) && negativeAdjs.contains(word)) {
        numPolyByNP++;
      } else if (positiveAdjs.contains(word)) {
        annotated = 0;
      } else {
        annotated = 1;
      }
      if (classified == -1 || annotated == -1) {
        numNotClassified++;
      } else if (classified == annotated) {
        numCorrect++;
      }
    }
    System.out.printf("#words in NP: %d (pos = %d, neg = %d)\n",
        adjs.size(), positiveAdjs.size(), negativeAdjs.size());
    System.out.printf("#annotated as both positive and negative: %d\n",
        numPolyByNP);
    System.out.printf("#not classified by WN: %d\n", numNotClassified);
    System.out.printf("Classification accuracy: %.2f\n", ((double) numCorrect)
        / (adjs.size() - numNotClassified));
  }
}
