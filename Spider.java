import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Array;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spider is a class that takes a given URL, and returns that URL with either a string http response,
 * or a string error code. */
public class Spider {

    // have an initializer with a running boolean value to kill the spider?
    Boolean running;

    public Spider() {
        this.running = true;
    }

    /**
     * Crawls a given url. If the response is valid, it returns an array
     * in the format of
     * [String url, String http].
     * If it's not valid, it returns
     * [String url, String error code int]
     */
    public SpiderResponse crawl(String incomingUrl) throws IOException {
        SpiderResponse resp = new SpiderResponse(incomingUrl);
        try {
            Document doc = Jsoup.connect(incomingUrl).get();
//            System.out.println(doc.text());
            // set up spider responses.
            resp.setResult(doc.html());
             ArrayList<String> tempUrls = new ArrayList<String>();

            Elements allLinks = doc.getElementsByTag("a");
            for(Element link: allLinks) {
                String absoluteUrl = link.attr("abs:href");
                tempUrls.add(absoluteUrl);
            }
            resp.setOutgoingUrls(tempUrls);
            resp.setStatusCode(200); // if this didn't fail, status code succeeded.
            resp.setIncomingUrl(incomingUrl);
        } catch (HttpStatusException ex) {
            resp.setStatusCode(ex.getStatusCode());
            // return the url and the status code as a string.
            return resp;
        }
        return resp;
    }



    public static void main(String[] args) throws IOException {
        Spider spider = new Spider();
        SpiderResponse spiderResponse = spider.crawl("https://en.wikipedia.org/wiki/Emmanuel_Macron");
        System.out.println(spiderResponse.getIncomingUrl());
        System.out.println(spiderResponse.getResult());
        System.out.println(spiderResponse.getStatusCode());
//        for (int i = 0; i < spiderResponse.getOutgoingUrls().size(); i++) {
//            System.out.println(spiderResponse.getOutgoingUrls().get(i) + " ");
//        }

        // Establish a server connection given a host and port
        //int port = Integer.parseInt(args[1]);
        String host = "localhost";
        int port = 1099;
        if (args.length > 1) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        try {
//            Registry registry = LocateRegistry.getRegistry(host, port);
//            RMIImpl stub = (RMIImpl) registry.lookup("RMIImpl");
//            TODO: do work on spider things using the stub
//            while (spider.running){
//                // get url
//                String url  = stub.getLinkToCrawl();
//                try{
//                    String[] results = spider.crawl(url);
//                    if (results[1].length() > 4){
//                        stub.processResponseCode();
//                    }
//                }
//                catch(Exception e){
//                    e.printStackTrace();
//                }
//            }
        }
        catch (Exception e) {
            System.out.println("RMIClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
