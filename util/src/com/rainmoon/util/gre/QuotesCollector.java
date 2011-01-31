package com.rainmoon.util.gre;
import java.io.PrintWriter;
import java.util.List;

/**
 * Collects quotes for words.
 */
public class QuotesCollector {
	public static final String QUOTES_URL = "http://quotes.dictionary.com/search/obscure";
	
	/**
	 * Writes quotes for each word in a separate line to the file named
	 *  {@code fileName}.
	 *  
	 * <p> If a word does not have any quotes, its corresponding line contains
	 * the empty string.
	 * 
	 * @param fileName
	 * @param words
	 */
	public void writeQuotes(String fileName, List<String> words) throws Exception {
		PrintWriter writer = new PrintWriter(fileName);
		for (String word : words) {
			writer.println(getQuotesForWord(word));
		}
		writer.close();
	}

	/**
	 * Gets quotes for {@code word}.
	 * 
	 * @param word
	 * @return
	 */
	private String getQuotesForWord(String word) throws Exception {
		throw new Exception("Not implemented");
	}
}
