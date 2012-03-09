package au.com.nicta.topicmodels;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 * Extract the content of pdf files into text files.
 * 
 * @author trung
 * 
 */
public class ArticleContentExtractor {

    private static final String INTRODUCTION = "Introduction";
    private static final String ACKNOWLEDGEMENTS = "Acknowledgements";
    private static final String REFERENCES = "References";

    PDFTextStripper mStripper;

    /**
     * Constructor
     * 
     * @throws IOException
     */
    public ArticleContentExtractor() throws IOException {
        mStripper = new PDFTextStripper();
    }

    public static void main(String args[]) throws IOException {
        ArticleContentExtractor extractor = new ArticleContentExtractor();
        System.out.println(extractor
                .getFullText("/home/trung/output/topicmodels/pdffiles/18.pdf"));
    }

    /**
     * Gets the abstract of the article in the given pdf file.
     * 
     * <p>
     * This method takes all of the text before Introduction as the abstract.
     * Note that this might include extra unnecessary information such as author
     * names and copyright statements. On the other hand, other information such
     * as the title, key words, and categories are also indicative of the
     * content of a article.
     * 
     * @param file
     *            path to the pdf file
     */
    public String getAbstract(String file) throws IOException {
        mStripper.setStartPage(1);
        mStripper.setEndPage(1);
        String firstPage = mStripper.getText(PDDocument.load(file));
        System.out.println(firstPage);
        int pos = getPosition(firstPage, INTRODUCTION);
        if (pos < 0)
            return firstPage;
        else
            return firstPage.substring(0, pos);
    }

    /**
     * Returns the position of <code>title</code> in the string
     * <code>source</code>. The title which is in upper-case will take
     * precedence over the captilalized title, as this is the standard format in
     * articles.
     * 
     * @param title
     * @return
     */
    private int getPosition(String source, String title) {
        String uppercase = title.toUpperCase();
        int pos = source.indexOf(uppercase);
        if (pos < 0) {
            pos = source.indexOf(title);
        }

        return pos;
    }

    /**
     * Gets the full text of the article from <code>file</code>.
     * 
     * <p>
     * This method return the entire content of the article except the
     * Acknowledgement and/or Reference section.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public String getFullText(String file) throws IOException {
        mStripper.setStartPage(1);
        mStripper.setEndPage(20);
        String content = mStripper.getText(PDDocument.load(file));
        int pos = getPosition(content, ACKNOWLEDGEMENTS);
        if (pos < 0)
            pos = getPosition(content, REFERENCES);
        if (pos > 0)
            return content.substring(0, pos);
        else
            return content;
    }
}
