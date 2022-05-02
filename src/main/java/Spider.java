import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Spider is a class that takes a given URL, and  a http string, status code, outgoing URLS, and the incoming URL.
 */
public class Spider implements SpiderImpl, Runnable {
    public Boolean running;
    public Logger logger;
    public int identifier;
    public Spider(int id) {
        this.running = true;
        this.logger = new Logger();
        this.identifier = id;
    }

    /**
     * Crawls a given url. If the response is valid, it returns SpiderResponse with a 200 statusCode.
     * Otherwise it returns a response with the original url, empty fields, and the corresponding
     * statusCode
     */
    public SpiderResponse crawl(String incomingUrl) throws IOException {
        SpiderResponse resp = new SpiderResponse(incomingUrl);
        try {
            Document doc = Jsoup.connect(incomingUrl).get();
            // set up spider responses.
            resp.setResult(doc.html());
            ArrayList<String> tempUrls = new ArrayList<String>();

            Elements allLinks = doc.getElementsByTag("a");
            for (Element link : allLinks) {
                String absoluteUrl = link.attr("abs:href");
                if (absoluteUrl.equals("") | !absoluteUrl.contains("wikipedia")) {
                    continue;
                }
                //System.out.println("Processing absoluteurl: " + absoluteUrl);

                try {
                    URL normalize = new URL(absoluteUrl);
                    URI normalize2 = normalize.toURI().normalize();
                    absoluteUrl = normalize2.toString();
                    tempUrls.add(absoluteUrl);
                } catch (URISyntaxException | MalformedURLException ignored) {

                }

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

    /**
     * Runs in a thread-friendly fashion.
     * */
    public void run() {
        try {

            this.runSpider();
        } catch (RemoteException | NotBoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The actual body of the run method.
     * This will use the Dispatcher RMI to get urls to crawl, then return responses / failures.
     * */
    public void runSpider() throws RemoteException, NotBoundException, InterruptedException {
        try {
            Registry registry = LocateRegistry.getRegistry();
            RMIImpl stub = (RMIImpl) registry.lookup("RMIImpl");
            while (this.running) {
                String url = stub.getLinkToCrawl(this.identifier);
                try {
                    Logger.log(url);
                    SpiderResponse results = crawl(url);
                    if (results.getStatusCode() > 300 && results.getStatusCode() < 600) {
                        stub.writeToDispatcherLog("Error from " + results.getIncomingUrl() + ". StatusCode: " + results.getStatusCode() + "\n");
                        Logger.log("Error from " + results.getIncomingUrl() + ". StatusCode: " + results.getStatusCode() + "\n");
                    }
                    stub.processResponseCode(results.getIncomingUrl(), results.getStatusCode());

                    stub.processOutlinksFromSpider(results.getOutgoingUrls());
                    //List<String> a = results.getOutgoingUrls().subList(0, 5);
                    //stub.processOutlinksFromSpider((ArrayList<String>) a);
                } catch(IllegalArgumentException e){
                    stub.addToFailedLinks(url);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                TimeUnit.SECONDS.sleep(1); // Wikipedia doesn't like being DDOSed.
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /** Kills the spider, breaking it's while(this.running) run loop
     * */
    public void kill() {
        this.setRunning(false);
    }

    /**
     * Returns a boolean if the spider is running.
     * Also works as an awk, as to whether there is a response or not.
     * */
    public Boolean getRunning() {
        return running;
    }

    /**
     * Sets whether the spider value is running or not.
     * */
    public void setRunning(Boolean running) {
        this.running = running;
    }

    /**
     * This is how a dispatcher will ask if a spider is running.
     * */
    @Override
    public boolean spiderIsAlive() throws RemoteException {
        return this.getRunning();
    }


    /**
     * Main method for spider. Mainly for testing, and requires Dispatcher to be running.
     * Dispatcher requires Client to populate the starter urls.
     * So may as well just run dispatcher, which populates spiders, and client.
     * */
    public static void main(String[] args) throws IOException {
        Spider spider = new Spider(1);
//         TEST CRAWL FOR DEBUGGING
//          THIS WILL NOT WORK WITHOUT A RUNNING DISPATCHER.
        SpiderResponse spiderResponse = spider.crawl("https://en.wikipedia.org/wiki/Emmanuel_Macron");
        Logger.log(spiderResponse.getIncomingUrl());
        Logger.log(spiderResponse.getResult());
        Logger.log(Integer.toString(spiderResponse.getStatusCode()));
        for (int i = 0; i < spiderResponse.getOutgoingUrls().size(); i++) {
            Logger.log(spiderResponse.getOutgoingUrls().get(i) + " ");
        }

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
            Logger.log("RMIClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
