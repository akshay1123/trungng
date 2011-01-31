package com.rainmoon.util.contagts;

import java.io.PrintWriter;
import java.net.URLEncoder;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeList;

import com.rainmoon.util.common.Htmls;

public class ContagtsTesting {

  private static final String URL = "http://uicontacts.appspot.com/search?";
  private static final String SHARE_URL = "http://uicontacts.appspot.com/share?";
  private static final String GUGI_URL = "http://kr.sch.gugi.yahoo.com/search/gugi?searcher=Keyword";
  static String queries[] = { "음식점", "고기집", "부페", "분식집", "레스토랑 ", "일본음식",
      "중국음식", "전통음식", "한정식", "삼계탕", "카페", "호프집", "음식배달", "빵집", "영화관", "연극극장",
      "놀이방", "유치원", "박물관", "학원 서점", "펜션", "호텔", "콘도", "여행지", "휴양림", "노래방",
      "놀이동산", "비디오대여", "헬스클럽", "찜질방", "미용실", "철학관", "의류", "예식장", "병원", "약국",
      "한의원", "건강식품", "동물병원", "산후조리원" };

  static String cities[] = { "KR:Gyeonggi-do", "KR:Daegu", "KR:Gangwon-do",
      "KR:Seoul", "KR:Jeollanam-do", "KR:Incheon", "KR:Gwangju", "KR:Busan",
      "KR:Jeollabuk-do", "KR:Chungcheongnam-do", "KR:Chungcheongbuk-do",
      "KR:Gyeongsangbuk-do", "KR:Ulsan", "KR:Gyeongsangnam-do",
      "KR:Gyeongsangbuk-do", "KR:Daejeon", "KR:Jeju-do", };

  public static void main(String args[]) throws Exception {
    String deviceId = "354957033110530";
    String city = "PT:Lisbon";
    String url = setParam(SHARE_URL, "deviceid", deviceId);
    url = appendParam(url, "city", city);
    url = appendParam(url, "name", "DOMINOS PIZZA");
    url = appendParam(url, "number", "217112770");
    url = appendParam(url, "labels", "[dominos*pizza*]");
    url = appendParam(url, "address",
        "Estrada Benfica 466 LOJA A Lisboa, Lisboa");
    Htmls.sendRequest(url);

    // "213903153";
    // Mr Pizza
    // AVENIDA D. CARLOS I 44-A, LISBOA, LISBOA
    // 1200

    // DOMINOS PIZZA
    // 217 112 770
    // ESTRADA BENFICA 466 LOJA A LISBOA, LISBOA
  }

  // parse search result query and stores into a file
  private static void process(PrintWriter writer, String url, String query)
      throws Exception {
    Parser parser = Parser.createParser(Htmls.getResponse(url), "UTF-8");
    NodeList list = parser.extractAllNodesThatMatch(new HasAttributeFilter(
        "class", "sr"));
    for (int i = 0; i < list.size(); i++) {
      String line = parseNode(query, list.elementAt(i)).toString();
      writer.println(line);
      System.out.println(line);
    }
  }

  private static void getKoreanPlaces() throws Exception {
    // http://kr.sch.gugi.yahoo.com/search/gugi?searcher=Keyword&p=피자&offset=10
    // 1. get category, queries from yahoo local
    // 2. for each query, parse ~ 200 points of interest into db
    // 3. post to the server
    PrintWriter writer = new PrintWriter("places.txt", "UTF-8");
    for (int i = 0; i < queries.length; i++) {
      for (int j = 0; j < 20; j++) {
        String url = appendParam(GUGI_URL, "p", queries[i]);
        url = appendParam(url, "offset", String.valueOf(j * 10));
        process(writer, url, queries[i]);
      }
    }
    writer.close();
  }

  /**
   * Parses {@code node} and returns a SearchResult.
   * 
   * @param node
   * @return
   */
  private static SearchResult parseNode(String query, Node node) {
    Tag tag = (Tag) node;
    String name = tag.getAttribute("title");
    NodeList childNodes = tag.getFirstChild().getNextSibling().getChildren();

    // find address and number
    String number = null;
    String address = null;
    for (int i = 0; i < childNodes.size(); i++) {
      Node childNode = childNodes.elementAt(i);
      if (childNode instanceof TextNode)
        continue;
      Tag childTag = (Tag) childNode;
      // find the element <dd title="address"> or <dd>
      if (childTag.getRawTagName().equals("dd")) {
        if (childTag.getAttribute("title") != null) {
          address = childTag.getAttribute("title");
        }
        // find the element <span class="iphone">
        if (number == null) {
          try {
            number = getPhoneNode(childTag.getFirstChild());
          } catch (NullPointerException e) {
            number = null;
          }
        }
      }
    }

    SearchResult result = new SearchResult(name, number, address, query, null);
    return result;
  }

  private static String getPhoneNode(Node node) {
    if (node instanceof Tag) {
      Tag tag = (Tag) node;
      if (tag.getRawTagName().equals("span")
          && tag.getAttribute("class").equals("iphone")) {
        return tag.toPlainTextString();
      }
    }
    return null;
  }

  private static class SearchResult {
    private String mName;
    private String mNumber;
    private String mAddress;
    private String mLabels;
    private String mCity;

    public SearchResult() {
    }

    public SearchResult(String name, String number, String address,
        String labels, String city) {
      mName = name;
      mNumber = number;
      mAddress = address;
      mLabels = labels;
      mCity = city;
    }

    /**
     * Returns string for writing to a file.
     */
    @Override
    public String toString() {
      return mAddress + "," + mName + "," + mNumber + "," + mLabels;
    }
  }

  private static String setParam(String url, String param, String value)
      throws Exception {
    return url.concat(param).concat("=").concat(
        URLEncoder.encode(value, "utf-8"));
  }

  private static String appendParam(String url, String param, String value)
      throws Exception {
    return url.concat("&").concat(param).concat("=").concat(
        URLEncoder.encode(value, "utf-8"));
  }

  private static void setParam(StringBuilder builder, String param, String value)
      throws Exception {
    builder.append(param).append("=").append(URLEncoder.encode(value, "utf-8"));
  }

  private static void appendParam(StringBuilder builder, String param,
      String value) throws Exception {
    builder.append("&").append(param).append("=").append(
        URLEncoder.encode(value, "utf-8"));
  }
}
