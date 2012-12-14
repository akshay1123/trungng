package com.rainmoon.util.podcast;

import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.rainmoon.util.common.TextFiles;

/**
 * Utility class that sort feeds based on their names alphabetically.
 * 
 * @author trung
 * 
 */
public final class SortFeeds {

	public static void main(String args[]) throws Exception {
		sortFeed("/Users/trung/Documents/workspace/cnpodcast/res/raw/feeds.txt");
	}
	
	/**
	 * Prints the sorted feed names and url for the feeds in given file.
	 * 
	 * @param file
	 */
	public static void sortFeed(String file) throws Exception {
		List<String> lines = TextFiles.readLines(file);
		SortedMap<String, String> sortedMap = new TreeMap<String, String>();
		for (int i = 0; i < lines.size() - 1; i = i + 2) {
			sortedMap.put(lines.get(i + 1), lines.get(i));
		}
		for (Entry<String, String> entry : sortedMap.entrySet()) {
			System.out.println(entry.getValue());
			System.out.println(entry.getKey());
		}
	}
}
