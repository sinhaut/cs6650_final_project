import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;

/**
 * Spider is a class that takes a given URL, and returns that URL with either a string http response,
 * or a string error code. */
public class Spider {
    private String result;
    private String currUrl;

    public Spider() {
        // Seems a bit redundant if no data is saved between goes.
        // Maybe just have a main method and call from there?
    }


    public String request() {
        // Request a new URL from the dispatcher.
        // feed it into the crawl func.
        // probably have a loop in main.
        return "";
    }

    /**
     * Crawls a given url. If the response is valid, it returns an array
     * in the format of
     * [String url, String http].
     * If it's not valid, it returns
     * [String url, String error code int]
     */
    public String[] crawl(String url) throws IOException {
        try {
            Document doc = Jsoup.connect(url).get();
            System.out.println(doc.text());
            this.result = doc.html();
        } catch (HttpStatusException ex) {
            // return the url and the status code as a string.
            return new String[]{this.currUrl, Integer.toString(ex.getStatusCode())};
        }
        return new String[]{this.currUrl, this.result};
    }

    public boolean read_robots_txt(String url) {
        return true;
    }

    public static void main(String[] args) throws IOException {
        Spider spider = new Spider();
        spider.crawl("https://en.wikipedia.org/wiki/Emmanuel_Macron");
    }

}
