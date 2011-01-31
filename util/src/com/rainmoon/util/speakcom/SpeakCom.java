package com.rainmoon.util.speakcom;

import org.htmlparser.util.ParserException;

import com.rainmoon.util.common.TextFiles;

/**
 * Utility class for the SpeakCom app.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class SpeakCom {

  private static final String ENGLISH_SPEAK = "http://www.englishspeak.com/english-lesson.cfm?lessonID=";
  private static final String ENGLISH_USA = "http://www.eigozai.com/USA/";
  private static final int NUM_LESSONS = 100;

  public static void main(String args[]) throws Exception {
    doEnglishUSA();
  }
  
  @SuppressWarnings("unused")
  private static void doEnglishUSA() throws Exception {
    // L001~ L009, L010, L11 ~ L104
    EnglishUSA eu = new EnglishUSA();
    String url;
    for (int i = 1; i <= 9; i++) {
      url = ENGLISH_USA + "L00" + i;
      System.err.println("Executing " + url + "P1.htm");
      TextFiles.writeCollection(eu.getDialog(url + "P1.htm"),
          String.format("speakcom/eu%d_%d.txt", i, 1));
      System.err.println("Executing " + url + "P2.htm");
      TextFiles.writeCollection(eu.getDialog(url + "P2.htm"),
          String.format("speakcom/eu%d_%d.txt", i, 2));
    }
    url = ENGLISH_USA + "L0" + 10;
    System.err.println("Executing " + url + "P1.htm");
    TextFiles.writeCollection(eu.getDialog(url + "P1.htm"),
        String.format("speakcom/eu%d_%d.txt", 10, 1));
    System.err.println("Executing " + url + "P2.htm");
    TextFiles.writeCollection(eu.getDialog(url + "P2.htm"),
        String.format("speakcom/eu%d_%d.txt", 10, 2));
    for (int i = 11; i < 104; i++) {
      url = ENGLISH_USA + "L" + i;
      System.err.println("Executing " + url + "P1.htm");
      TextFiles.writeCollection(eu.getDialog(url + "P1.htm"),
          String.format("speakcom/eu%d_%d.txt", i, 1));
      System.err.println("Executing " + url + "P2.htm");
      TextFiles.writeCollection(eu.getDialog(url + "P2.htm"),
          String.format("speakcom/eu%d_%d.txt", i, 2));
    }
  }
  
  @SuppressWarnings("unused")
  private static void doEnglishSpeak() throws ParserException {
    EnglishSpeak es = new EnglishSpeak();
    for (int i = 1; i <= NUM_LESSONS; i++) {
      System.err.println("Dialog " + i);
      System.out.println(es.getDialog(ENGLISH_SPEAK + i));
    }
  }
}
