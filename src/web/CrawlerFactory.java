package web;

import java.util.*;
import java.util.concurrent.*;

public class CrawlerFactory {

    public final static int BFS = 0;
    public final static int DFS = 1;
    public final static int MULTI = 2;

    private String seedURL;
    private int mode;
    private boolean isSwitchOn;
    private String downloadPath;
    private Map<String,String> visitedURLsAndPaths; //url,path
    private Deque<String[]> unvisitedURLsAndPaths;
    private ExecutorService executorService;

    public CrawlerFactory(String seedURL,String downloadPath, ExecutorService executorService) {
        this(seedURL, downloadPath, BFS, executorService);
    }

    public CrawlerFactory(String seedURL,String downloadPath, int mode, ExecutorService executorService) {
        this.seedURL = seedURL;
        this.downloadPath = downloadPath;
        this.isSwitchOn = false;
        this.executorService = executorService;
        switch(this.mode = mode) {
            case BFS: //first in first out
                this.visitedURLsAndPaths = new HashMap();
                this.unvisitedURLsAndPaths = new ArrayDeque<>(); //use as queue
                this.unvisitedURLsAndPaths.offerLast(new String[]{seedURL, this.downloadPath});
                break;
            case DFS: //first in last out
                this.visitedURLsAndPaths = new HashMap();
                this.unvisitedURLsAndPaths = new ArrayDeque<>(); //use as stack
                this.unvisitedURLsAndPaths.offerLast(new String[]{seedURL, this.downloadPath});
                break;
            case MULTI:
                //not implemented yet
                /*
                this.visitedURLsAndPaths = new ConcurrentHashMap<>();
                this.unvisitedURLsAndPaths = new ConcurrentLinkedDeque<>();
                this.unvisitedURLsAndPaths.offerLast(new String[]{seedURL, ""});
                break;
                */
        }
    }

    public void work() {
        this.switchOn();
        String[] urlAndPath;
        switch(this.mode) {
            case BFS://first in first out
                while (this.getIsSwitchOn()&&(urlAndPath=this.unvisitedURLsAndPaths.pollFirst())!=null) {
                    this.visitedURLsAndPaths.put(urlAndPath[0],urlAndPath[1]);
                    Crawler crawler = new Crawler(urlAndPath[0], urlAndPath[1], this);
                    List<String> foundURLs = crawler.work();
                    foundURLs.parallelStream().
                            filter(u->!urlIsVisited(u)).
                            forEach(u->this.unvisitedURLsAndPaths.offerLast(new String[]{u, crawler.getPath()}));

                }
                break;

            case DFS://first in last out
                while (this.getIsSwitchOn()&&(urlAndPath=this.unvisitedURLsAndPaths.pollLast())!=null) {
                    this.visitedURLsAndPaths.put(urlAndPath[0],urlAndPath[1]);
                    Crawler crawler = new Crawler(urlAndPath[0], urlAndPath[1], this);
                    List<String> foundURLs = crawler.work();
                    foundURLs.parallelStream().
                            filter(u->!urlIsVisited(u)).
                            forEach(u->this.unvisitedURLsAndPaths.offerLast(new String[]{u, crawler.getPath()}));
                }
                break;

            case MULTI:
                //not implemented yet
                /*
                this.unvisitedURLsAndPaths = new ConcurrentLinkedDeque<>();
                //if we can poll something
                while ((urlAndPath=this.unvisitedURLsAndPaths.pollLast())!=null) {
                    if (urlAndPath!=null){
                        this.visitedURLsAndPaths.put(urlAndPath[0],urlAndPath[1]);
                        Crawler crawler = new Crawler(urlAndPath[0], urlAndPath[1], this);

                        //crawlers do the job and they would offer the result to the queue
                        CompletableFuture.supplyAsync(()->crawler.work());

                    }
                }
                */

                break;
        }
        this.switchOff();
    }

    public String getSeedURL() {
        return this.seedURL;
    }

    public void switchOn() {this.isSwitchOn=true;}

    public void switchOff() {this.isSwitchOn=false;}

    public boolean getIsSwitchOn() {return this.isSwitchOn;}

    public String getDownloadPath() {return this.downloadPath;}

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public boolean urlIsVisited(String url) {
        return this.visitedURLsAndPaths.containsKey(url);
    }

    public void setMode(int mode) {
        if (mode==BFS||mode==DFS) {
            this.mode=mode;
        }
    }
}
