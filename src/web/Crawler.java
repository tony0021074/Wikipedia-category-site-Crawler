package web;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Crawler {

    private String url;
    private String path;
    private CrawlerFactory crawlerfactory;

    protected Crawler(String url, String path, CrawlerFactory crawlerfactory) {
        this.url = url;
        this.path = path;
        this.crawlerfactory = crawlerfactory;
    }

    protected List<String> work() {
        //get whole HTML by url
        String currentHTML = getHTML(this.url);

        //get pages' URLs
        CompletableFuture<List<String>> pageURLs = CompletableFuture.
                supplyAsync(()->getWikiHTMLPageURLs(currentHTML),this.crawlerfactory.getExecutorService());

        //get HTML title
        CompletableFuture<String> title = CompletableFuture.supplyAsync(()->getHTMLTitle(currentHTML),
                this.crawlerfactory.getExecutorService());
        //calculate the current path
        CompletableFuture<String> path = title.thenApply(t->this.path=this.path+t+File.separator);
        //create a folder for the current path
        CompletableFuture<?> saveAllHPages = path.thenApply(p-> {new File(p).mkdirs(); return p;}).
                thenAcceptBoth(pageURLs, (p,us)->us.parallelStream().forEach(u->saveHTMLasFile(u,p)));

        //get the subcategories' URLs
        CompletableFuture<List<String>> subcategoryURLs = CompletableFuture.
                supplyAsync(()->getWikiHTMLSubcategoryURLs(currentHTML),this.crawlerfactory.getExecutorService());

        CompletableFuture.allOf(saveAllHPages, subcategoryURLs);//Until all complete
        return subcategoryURLs.join();
    }

    protected String getPath() {
        return this.path;
    }

    private static boolean saveHTMLasFile(String url, String path) {
        String html = getHTML(url);
        String htmlTitle = getHTMLTitle(html);
        try(BufferedWriter bw = new BufferedWriter( new FileWriter(path+htmlTitle+".html"))) {
            bw.write(html);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static String getHTML(String url) {
        StringBuilder sb = new StringBuilder();
        try {
            URLConnection con = new URL(url).openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
            try {
                String line;
                while((line=br.readLine())!=null){
                    sb.append(line);
                }
            } finally {
                br.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }
        return sb.toString();
    }

    private static String getHTMLBody(String html){
        String head = "<[Bb][Oo][Dd][Yy].*>";
        String tail = "</[Bb][Oo][Dd][Yy]>";
        return getStringsBetween(html, head, tail).get(0);
    }

    private static String getHTMLTitle(String html) {
        String head = "\"wgTitle\":\"";
        String tail = "\"";
        String title = getStringsBetween(html, head, tail).get(0);
        title = title.replaceAll("[\\?\\\\/:|<>\\*]", "_");
        title = title.replace("\\[uU]0026", "&");
        /*
        title = title.replace("/", "u002F");
        title = title.replace(":", "u003A");
        title = title.replace("\\","u005C");
        // \*?""<>|
        */
        return title;
    }

    private List<String> getWikiHTMLSubcategoryURLs(String html){
        String head = "<div id=\"mw-subcategories\">.*<div lang=\"en\" dir=\"ltr\" class=\"mw-content-ltr\">";
        String tail = "<div id=\"mw-pages\">";
        List<String> result = getStringsBetween(html, head, tail);
        if (result.isEmpty()) {
            return result;
        }
        html = result.get(0);
        head = "href=\"";
        tail = "\" title=\"";
        return getStringsBetween(html, head, tail).
                parallelStream().map(p->URLHelper.getAbsoluteURL(p, this.crawlerfactory.getSeedURL())).
                collect(Collectors.toList());
    }

    public List<String> getWikiHTMLPageURLs(String html){
        String head = "<div id=\"mw-pages\">.*<div lang=\"en\" dir=\"ltr\" class=\"mw-content-ltr\">";
        String tail = "<div class=\"printfooter\">";
        List<String> result = getStringsBetween(html, head, tail);
        if (result.isEmpty()) {
            return result;
        }
        html = result.get(0);
        head = "href=\"";
        tail = "\" ";
        return getStringsBetween(html, head, tail).
                parallelStream().map(p->URLHelper.getAbsoluteURL(p, this.crawlerfactory.getSeedURL())).
                collect(Collectors.toList());
    }

    private static List<String> getStringsBetween(String string, String head, String tail) {
        List<String> results = new ArrayList();
        Pattern p = Pattern.compile(head+"(.*?)"+tail);
        Matcher m = p.matcher(string);
        while (m.find()) {
            results.add(m.group(1));
        }
        return results;
    }
}
