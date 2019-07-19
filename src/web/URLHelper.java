package web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class URLHelper {

    public static String getFinalURL(String url) {
        try{
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            try {
                for (String redirectURL; (redirectURL = con.getHeaderField("Location")) != null; ) {
                    url = redirectURL;
                    con = (HttpURLConnection) new URL(url).openConnection();
                }
            } finally {
                con.disconnect();
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String getAbsoluteURL(String url, String seedURL) {
        try {
            url = new URL(seedURL).getProtocol() + "://" + new URL(seedURL).getHost() + url;
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static String prependHttp(String url) {
        try {
            if (new URL(url).getProtocol() == null) {
                url = "http://" + url;
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static boolean isValidURL(String url) {
        /* Try creating a valid URL */
        try {
            new URL(url).toURI();
            return true;
        }

        // If there was an Exception
        // while creating URL object
        catch (Exception e) {
            return false;
        }
    }
}
