package com.rainmoon.util.elda;

import java.util.List;

import com.rainmoon.util.common.TextFiles;
import com.rainmoon.util.elda.NYTimesCollector.SmallThread;

public class Temp {
  
  public static void main(String args[]) throws Exception {
    List<String> links = TextFiles.readLines("nytimes/general.txt");
    int offset = 200000;
    int block = 400000 / 50; // each thread collects 8000 articles
    for (int i = 0; i < 50; i++) {
      int toIndex = (i + 1) * block - 1 > links.size() ? links.size() : (i + 1) * block - 1;
      new SmallThread(links.subList(i * block, toIndex), offset + i * block,
          "nytimes/general").start();
    }
  }

}
