package com.rainmoon.util.gre;
import java.io.PrintWriter;
import java.util.List;

import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.NodeList;

/**
 * Aggregates word usages from different sources.
 */
public class WordUsagesCollector {
	public static final String MERRIAM_URL = "http://www.merriam-webster.com/dictionary/";
	public static final String GOOGLE_URL = "http://www.google.com/dictionary?langpair=en|en&q=";
	
	/**
	 * Writes usages for each word in the list {@code words} to the file {@code file}.
	 *
	 * <p> Usages for each word is written on one line. If a word does not have
	 * usages, an empty string is written on that line.
	 * 
	 * @param file name of the file to write
	 * @param words
	 * @return
	 */
	public void writeWordUsages(String file, List<String> words) throws Exception {
		PrintWriter writer = new PrintWriter(file);
		String usages; 
		for (String word : words) {
			usages = getUsagesFromGoogle(word);
			writer.println(usages);
			if (usages.length() == 0) {
				System.out.println(word);
			}
		}
		writer.close();
	}

	/**
	 * Gets usages of {@code word} from the Google dictionary.
	 * 
	 * @param word
	 * @return
	 * @throws Exception
	 */
	private String getUsagesFromGoogle(String word) throws Exception {
		Parser parser = new Parser(GOOGLE_URL + word);
		NodeList nodes = parser.extractAllNodesThatMatch(new HasAttributeFilter(
				"class", "dct-ee"));
		int size = nodes.size();
		StringBuilder builder = new StringBuilder();
		String usage;
		for (int i = 0; i < size; i++) {
			usage = nodes.elementAt(i).toPlainTextString().trim();
			builder.append(usage + ".");
		}
		
		return builder.toString();
	}
	
	/**
	 * Gets usages of {@code word} from the Merriam-Webster dictionary.
	 * 
	 * @param word
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private String getUsagesFromMW(String word) throws Exception {
		Parser parser = new Parser(MERRIAM_URL + word);
		NodeList nodes = parser.extractAllNodesThatMatch(new OrFilter(
				new HasAttributeFilter("class", "vi-learners"),
				new HasAttributeFilter("class", "vi-cthes"))
		);
		
		if (nodes.size() > 0) {
			return formatString(nodes.asString());
		} else {
			return "";
		}
	}
	
	/**
	 * Formats the string {@code s}, specifically removes the &lt; and &gt;
	 * character if they exist.
	 * 
	 * @param s
	 * @return
	 */
	private String formatString(String s) {
		return s.replaceAll("&lt;", "").replaceAll("&gt;", ".");
	}
}
