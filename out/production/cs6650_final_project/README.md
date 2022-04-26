# cs6650_final_project
Final Project

Client 
    - sends 1 wiki link to the coordinator/dispatcher

Dispatcher (RMIDispatcher interface for this)
    - Inputs: 1 link from client; many links from all spiders 
    - Outputs: update the queue of links to crawl
    - System to keep track of spider count and always ensure that there are 5 active spiders (fault tolerance)
    - Spawns 5 spider server threads --> 1 will pick up the link from the front of the queue; others will wait
    - Spider duties will be carried out 
    - Receives links from all spiders and handles visited/normalized/blacklist (limit to wikipedia)
    - Maintain a storage of urls that lead to a bad response code/spider failure (if the url leads to 2 failures, blacklist it)

Spider (libraries: HttpClient, JSoup)
    - Handle different response codes 
    Inputs: 1 normalized link from front of dispatcher queue --> crawl the webpage
    Outputs:
    - 1. Link that was crawled + HTML text --> Index
    - 2. Outlinks --> back to the dispatcher

Index (RMIIndex interface)
    - {link1: html1, link2: html2, ..., linkx: htmlx} 
    - Inputs: link, html text and update the index

Weaknesses: 1 point of failure in the dispatcher --> will lose 
    - if we lose dispatcher, also lose data