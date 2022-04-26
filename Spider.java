import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.rmi.RemoteException;

public class Spider {

    private String result;
    private String currUrl;
    private String errorCode;

    public Spider() {
    }

    public String[] crawl(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        System.out.println(doc.text());
        return new String[] {this.currUrl, this.result};
    }

     public static void main(String[] args) throws IOException {
        Spider spider = new Spider();
        spider.crawl("https://en.wikipedia.org/wiki/Emmanuel_Macron");
        }
}
