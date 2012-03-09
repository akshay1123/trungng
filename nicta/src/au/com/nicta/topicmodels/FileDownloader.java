package au.com.nicta.topicmodels;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;

import edu.kaist.uilab.asc.util.TextFiles;

/**
 * Downloads file from a resource on the Internet.
 * 
 * <p>
 * This downloader downloads
 * 
 * @author trung (trung.ngvan@gmail.com)
 * 
 */
public class FileDownloader {

    private ArrayList<String> links;
    private String outputDir;

    /**
     * Constructor
     * 
     * @param file
     *            a file which contains the list of links to all files to
     *            download. Each line of the file is read as a link.
     * @param outputDir
     *            the output directory where all files are stored
     * @throws IOException
     */
    public FileDownloader(String linksFile, String outputDir)
            throws IOException {
        this.links = (ArrayList<String>) TextFiles.readLines(linksFile);
        this.outputDir = outputDir;
    }

    /**
     * Constructor
     * 
     * @param links
     *            links of all files to download
     * @param outputDir
     *            the output directory where all files are stored
     */
    public FileDownloader(ArrayList<String> links, String outputDir) {
        this.links = links;
        this.outputDir = outputDir;
    }

    public static void main(String args[]) throws IOException {
        String linksFile = "/home/trung/output/topicmodels/pdfLinks.txt";
        String linksFile2 = "/home/trung/output/topicmodels/pdfLinks2.txt";
        String outputDir = "/home/trung/output/topicmodels/pdffiles";
        Collection<String> collection = TextFiles.readUniqueLines(linksFile);
        collection.addAll(TextFiles.readUniqueLines(linksFile2));
        FileDownloader downloader = new FileDownloader(
                Lists.newArrayList(collection), outputDir);
        // FileDownloader downloader = new FileDownloader(linksFile, outputDir);
        int numThreads = 50;
        downloader.download(numThreads);
    }

    /**
     * Starts the downloading process.
     * 
     * @param numThreads
     *            number of threads to use for downloading
     */
    public void download(int numThreads) {
        int batchSize = links.size() / numThreads; // per thread
        for (int threadIdx = 0; threadIdx < numThreads; threadIdx++) {
            int offset = threadIdx * batchSize;
            int lastSublistIdx = (threadIdx + 1) * batchSize < links.size() ? (threadIdx + 1)
                    * batchSize
                    : links.size();
            FilesDownloaderThread thread = new FilesDownloaderThread(
                    links.subList(offset, lastSublistIdx), offset, outputDir);
            thread.start();
        }
    }

    /**
     * A helper thread for downloading files from a batch of links.
     */
    static class FilesDownloaderThread extends Thread {
        List<String> links;
        String outputDir;
        int offset;

        /**
         * Constructor
         * 
         * @param links
         *            all links to be downloaded by this thread
         * @param offset
         *            the position of this sublist with respect to the global
         *            list of links
         */
        public FilesDownloaderThread(List<String> links, int offset,
                String outputDir) {
            this.links = links;
            this.outputDir = outputDir;
            this.offset = offset;
        }

        @Override
        public void run() {
            System.out.printf("\nDownloading links from %d to %d", offset,
                    offset + links.size());
            for (int idx = 0; idx < links.size(); idx++) {
                try {
                    FileUtils.copyURLToFile(
                            new URL(links.get(idx)),
                            new File(String.format("%s/%d.pdf", outputDir,
                                    offset + idx)), 5 * 60 * 1000,
                            1 * 60 * 1000);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
