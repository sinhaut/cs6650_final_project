import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Spider is a class that takes a given URL, and returns that URL with either a string http response,
 * or a string error code.
 */
public class Spider implements SpiderImpl, Runnable {
    Boolean running;
    String host;
    int port;


    public Spider() {
        this.running = true;
    }

    /**
     * Crawls a given url. If the response is valid, it returns SpiderResponse with a 200 statusCode.
     * Otherwise it returns a response with the original url, empty fields, and the corresponding
     * statusCode
     */
    public SpiderResponse crawl(String incomingUrl) throws IOException {
        SpiderResponse resp = new SpiderResponse(incomingUrl);
        try {
            System.out.println(incomingUrl);
            Document doc = Jsoup.connect(incomingUrl).get();
            // set up spider responses.
            resp.setResult(doc.html());
            ArrayList<String> tempUrls = new ArrayList<String>();

            Elements allLinks = doc.getElementsByTag("a");
            for (Element link : allLinks) {
                String absoluteUrl = link.attr("abs:href");
                tempUrls.add(absoluteUrl);
            }
            resp.setOutgoingUrls(tempUrls);
            resp.setStatusCode(200); // if this try didn't fail, status code succeeded.
            resp.setIncomingUrl(incomingUrl);

        } catch (HttpStatusException ex) {
            resp.setStatusCode(ex.getStatusCode());
            // return the url and the status code as a string.
            return resp;
        }
        return resp;
    }

    public void run() {
        try {
            this.runSpider();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void runSpider() throws RemoteException, NotBoundException, InterruptedException {
        try {
            Registry registry = LocateRegistry.getRegistry();
            System.out.println("hello" + registry);
            RMIImpl stub = (RMIImpl) registry.lookup("RMIImpl");
            System.out.println(stub);
            while (this.running) {
                // get url
                try {
                    String url = stub.getLinkToCrawl();
                    System.out.println(url);
                    SpiderResponse results = crawl(url);
                    System.out.println(results);
                    if (results.getStatusCode() > 300 && results.getStatusCode() < 600) {
                        stub.writeToDispatcherLog("Error from " + results.getIncomingUrl() + ". StatusCode: " + results.getStatusCode() + "\n");
                    }
                    stub.processResponseCode(results.getIncomingUrl(), results.getStatusCode());
                    stub.processOutlinksFromSpider(results.getOutgoingUrls());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void kill() {
        this.running = false;
    }

    public Boolean getRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }

    @Override
    public boolean spiderIsAlive() throws RemoteException {
        return this.getRunning();
    }


    public static void main(String[] args) throws IOException {
        Spider spider = new Spider();
        System.out.println("Alive");
//         TEST CRAWL FOR DEBUGGING
//        SpiderResponse spiderResponse = spider.crawl("https://en.wikipedia.org/wiki/Emmanuel_Macron");
//        System.out.println(spiderResponse.getIncomingUrl());
//        System.out.println(spiderResponse.getResult());
//        System.out.println(spiderResponse.getStatusCode());
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
            spider.run();
        } catch (Exception e) {
            System.out.println("RMIClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
