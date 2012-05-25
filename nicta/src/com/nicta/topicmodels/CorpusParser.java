package com.nicta.topicmodels;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.ModifyTokenTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.Counter;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.Strings;

import edu.kaist.uilab.stemmers.EnglishStemmer;

/**
 * Parser that prepares data for the Gibbs sampler.
 * 
 * @author trung (trung.ngvan@gmail.com)
 */
public class CorpusParser {

    int mMinTokenCount;
    int mTopStopWords;
    int mTopDocumentTokens;
    String mCorpusDir;
    TokenizerFactory mTokenizerFactory;
    SymbolTable mSymbolTable;
    ObjectToCounterMap<String> mWordCnt;
    ArrayList<ArrayList<Integer>> mDocuments;
    ArticleContentExtractor mExtractor;
    boolean mUseAbstract;

    /**
     * Constructor
     * 
     * @param corpusDir
     *            the directory that contains documents of the corpus (each
     *            document is a file)
     * @param minTokenCount
     *            the minimum count of a token to be retained as one word in the
     *            vocabulary
     * @param topStopWords
     *            the number of words which has highest frequency to be removed
     * @param stopStems
     *            the list of stop words (in addition to the standard stop words
     *            used in lingpipe)
     * @param useAbstract
     *            <code>true</code> to use the abstracts of articles,
     *            <code>false</code> to use full article content
     */
    public CorpusParser(String corpusDir, int minTokenCount, int topStopWords,
            List<String> stopStems, boolean useAbstract) throws IOException {
        mCorpusDir = corpusDir;
        mMinTokenCount = minTokenCount;
        mTopStopWords = topStopWords;
        mTokenizerFactory = StemmingTokenizerFactory.getInstance(stopStems);
        mSymbolTable = new MapSymbolTable();
        mWordCnt = new ObjectToCounterMap<String>();
        mExtractor = new ArticleContentExtractor();
        mDocuments = new ArrayList<ArrayList<Integer>>();
        mUseAbstract = useAbstract;
    }

    /**
     * Parses all text files in this corpus.
     * 
     * <p>
     * After calling this method, all properties of the corpus can be queried
     * using the various getter methods.
     */
    public void parse() throws IOException {
        ArrayList<String> documents = constructSymbolTable();
        for (String document : documents) {
            mDocuments.add(tokenizeDocument(document));
        }
    }

    /**
     * Reports statistics about the corpus.
     * 
     * @param file
     *            file to store corpus information
     */
    public void reportCorpus(String file) throws IOException {
        System.out.println("\nReporting corpus...");
        writeReport(file);
    }

    private void writeReport(String file) throws IOException {
        PrintWriter out = new PrintWriter(file);
        out.printf("Number of documents: %d\n", mDocuments.size());
        out.printf("Number of tokens: %d\n", mSymbolTable.numSymbols());
        out.println("--------------------------");
        for (String token : mWordCnt.keySet()) {
            out.printf("%s\t\t%d\n", token, mWordCnt.getCount(token));
        }
        out.close();
    }

    /**
     * Returns the vocabulary size of this corpus.
     * 
     * @return
     */
    public int getVocabularySize() {
        return mSymbolTable.numSymbols();
    }

    /**
     * Returns the number of documents in this corpus.
     */
    public int getNumDocuments() {
        return mDocuments.size();
    }

    /**
     * Returns the list documents in the corpus.
     */
    public ArrayList<ArrayList<Integer>> getDocuments() {
        return mDocuments;
    }

    /**
     * Returns the document tokens as 2-D array.
     * 
     * @return
     */
    public int[][] getDocumentsAsArray() {
        int[][] res = new int[mDocuments.size()][];
        for (int i = 0; i < res.length; i++) {
            ArrayList<Integer> doc = mDocuments.get(i);
            res[i] = new int[doc.size()];
            for (int j = 0; j < doc.size(); j++) {
                res[i][j] = doc.get(j);
            }
        }

        return res;
    }

    /**
     * Returns the symbol table of words in this corpus.
     * 
     * @return
     */
    public SymbolTable getSymbolTable() {
        return mSymbolTable;
    }

    /**
     * Constructs the symbol table which keeps all valid tokens in the corpus.
     * 
     * <p>
     * This method reads all documents in the corpus, constructing a symbol
     * table in the process. Top words and not frequent words are pruned from
     * the set of vocabulary.
     * 
     * @return the document contents read from the corpus directory
     * @throws IOException
     */
    private ArrayList<String> constructSymbolTable() throws IOException {
        ArrayList<String> documents = new ArrayList<String>();
        File directory = new File(mCorpusDir);
        String[] articles = directory.list();
        for (String article : articles) {
            try {
                String content = "";
                if (mUseAbstract) {
                    content = mExtractor.getAbstract(directory.getPath() + "/"
                            + article);
                } else {
                    content = mExtractor.getFullText(directory.getPath() + "/"
                            + article);
                }
                documents.add(content);
                char[] articleContent = Strings.toCharArray(content);
                Tokenizer tokenizer = mTokenizerFactory.tokenizer(
                        articleContent, 0, articleContent.length);
                for (String token : tokenizer) {
                    mWordCnt.increment(token);
                }
            } catch (IOException e) {
                // some file cannot be read, just ignore
                System.err.printf("Cannot read file %s\n", article);
            }

        }
        mWordCnt.prune(mMinTokenCount);
        pruneTopTokens(mWordCnt, mTopStopWords);
        for (String token : mWordCnt.keySet()) {
            mSymbolTable.getOrAddSymbol(token);
        }

        return documents;
    }

    /**
     * Prunes the top {@code num} tokens from the vocabulary set.
     * 
     * @param tokenCounter
     * @param num
     */
    private void pruneTopTokens(ObjectToCounterMap<String> tokenCounter, int num) {
        System.out.printf("Pruning top %d words in the corpus\n", num);
        HashSet<String> topKeys = new HashSet<String>(tokenCounter
                .keysOrderedByCountList().subList(0, num));
        Iterator<Map.Entry<String, Counter>> iter = tokenCounter.entrySet()
                .iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Counter> entry = iter.next();
            if (topKeys.contains(entry.getKey())) {
                System.out.print(entry.getKey() + " ");
                iter.remove();
            }
        }
    }

    /**
     * Tokenizes the specified text document returning only tokens that exist in
     * the symbol table constructed in previous step. This method is useful
     * within a given LDA model for tokenizing new documents into lists of
     * words.
     * 
     * @param content
     *            a document content
     * @return the document, null if the document does not contain any word
     */
    private ArrayList<Integer> tokenizeDocument(String content) {
        ArrayList<Integer> document = new ArrayList<Integer>();
        char[] cs = Strings.toCharArray(content);
        Tokenizer tokenizer = mTokenizerFactory.tokenizer(cs, 0, cs.length);
        for (String token : tokenizer) {
            int id = mSymbolTable.symbolToID(token);
            if (id >= 0) {
                document.add(id);
            }
        }

        return document;
    }

    /**
     * Tokenizer factory.
     * <p>
     * This tokenizer stems every word using Porter stemmer.
     */
    static final class StemmingTokenizerFactory extends
            ModifyTokenTokenizerFactory {
        static final long serialVersionUID = -3401639068551227864L;
        static final EnglishStemmer stemmer = new EnglishStemmer();
        static final int MIN_WORD_LENGTH = 2;
        static final int MAX_WORD_LENGTH = 40;

        static TokenizerFactory instance = new StemmingTokenizerFactory();

        private StemmingTokenizerFactory() {
            super(new LowerCaseTokenizerFactory(new RegExTokenizerFactory(
                    "[$a-zA-Z_]+")));
        }

        @Override
        public String modifyToken(String token) {
            token = stemmer.getStem(token);
            return stop(token) ? null : token;
        }

        boolean stop(String token) {
            if (token.length() <= MIN_WORD_LENGTH
                    || token.length() >= MAX_WORD_LENGTH)
                return true;
            // contains at least one letter
            for (int i = 0; i < token.length(); ++i)
                if (Character.isLetter(token.charAt(i)))
                    return false;
            return true;
        }

        static TokenizerFactory getInstance(List<String> stopStems) {
            HashSet<String> stems = new HashSet<String>();
            stems.addAll(stopStems);
            return new StopTokenizerFactory(instance, stems);
        }
    }
}
