package com.rainmoon.util.gre;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;

public class GRESupremeUtil {
	
	public static void main(String args[]) throws Exception {
//		new WordUsagesCollector().writeWordUsages("usages2.txt",
//				TextFileUtils.readLines("nousages.txt"));
	}
	
	// Meaning - real meaning
	static String getMeaning(String s) {
		int pos = s.indexOf("-") + 1;
		return s.substring(pos);
	}
	
	/**
	 * Loads all words from the file with {@code fileName}.
	 * 
	 * <p> The file contains a word with its definition on each line.
	 * 
	 * @param fileName
	 * @return
	 */
	public static HashSet<String> loadWords(String fileName) throws Exception {
		HashSet<String> words = new HashSet<String>();
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line;
		while ((line = reader.readLine()) != null) {
			int pos = line.indexOf(" ");
			words.add(line.substring(0, pos).toLowerCase());
		}
		reader.close();
		
		return words;
	}
	
	public static void readTests() throws Exception {
		// Fill in the blank: 1. -> 7.
		// Analogy: 8. -> 16.
		// Antonym: 28. -> 38.
		//"TEST 01-1.txt";
		PrintWriter blankWriter = new PrintWriter("completion.txt");
		PrintWriter antWriter = new PrintWriter("ant.txt");
		PrintWriter analogyWriter = new PrintWriter("analogy.txt");
		PrintWriter writer;
		for (int i = 1; i < 20; i++) {
			for (int j = 1; j < 3; j++) {
				if (i != 12 || j != 2) {
					String fileName = "res/TEST " + i + "-" + j + ".txt";
					System.err.println("file: " + fileName);
					StringBuilder builder = new StringBuilder();
					BufferedReader reader = new BufferedReader(new FileReader(fileName));
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}
					reader.close();
					String question;
					String idx[] = {
						"2.", "3.", "4.", "5.", "6.", "7.", 
						"8.", "9.", "10.", "11.", "12.", "13.", "14.", "15.", "16.",
						 "28.", "29.", "30.", "31.", "32.", "33.", "34.", "35.", "36.", "37.", "38.",
					};
					writer = blankWriter;
					for (int index = 0; index < idx.length; index++) {
						if (idx[index].equals("9.")) {
							writer = analogyWriter;
						}
						if (idx[index].equals("29.")) {
							writer = antWriter;
						}
						int pos = builder.indexOf(idx[index]);
						question = builder.substring(0, pos);
						builder.delete(0, pos);
						
						System.out.println(question);
						writer.println(question);
					}
					System.out.println(builder.toString());
					writer.println(builder.toString());
				}
			}
		}
		blankWriter.close();
		analogyWriter.close();
		antWriter.close();
	}
}
