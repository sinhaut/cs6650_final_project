Final Project

How to run:
**all files can be compiled with javac

In another terminal window, run 'java Dispatcher <port#>' **port # is optional
In another terminal window, run 'java Client <host> <port#>' **port # is optional
**port # is optional and must match for coordinator and client if specified**
**default port is 1099, default host is localhost

Files included:
Client 
- Sends operations to Dispatcher via RMI connected via default port 1099
- Another port can be specified in args for coordinator and client
- Logs are held in clientlogs.txt - timestamped success/failure

Dispatcher - Communicates with client and spiders via RMI
- populateSpiders - Initializes the spiders, and serves urls to crawl via RMI
- processResponseCode - Handles self-stabilization by blacklisting faulty urls
- getLinkToCrawl - Synchronized blocking queue only gives spiders a url to crawl 1 at a time
- ensureSpidersAreAlive - respawns any dead spiders and kills unresponsive one
- processOutlinksFromSpider - only adds unvisited links
RMIInterface - This is the RMI interface for the Dispatcher

Spider - Crawls webpages and returns response to client + stores html in text format
- crawl - uses Jsoup to crawl and get information + normalizes urls that are on the page
- runSpider - communicates the response codes, outlinks back to the dispatcher
SpiderResponse - Stores information gathered from webpage
- writes text to the index folder

Jar files for Client and Dispatcher are included

Other files
preload.txt - Contains seed urls that Client sends to dispatcher in the beginning
coordinatorLogs.txt - Timestamp, Spider id, response code, and link that spider crawled
clientLogs.txt - Success/failure of links sent to dispatcher by the client