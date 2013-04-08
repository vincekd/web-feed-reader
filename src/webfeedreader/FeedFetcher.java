package webfeedreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

class FeedFetcher {
    public String fetchPage(String url) {
        BufferedReader reader = null;
        URL page;
        String str = "";
        try {
            page = new URL(url);
            reader = new BufferedReader(
                new InputStreamReader(page.openStream(), "UTF-8"));
            for (String line; (line = reader.readLine()) != null;) {
                str += line;
            }
        } catch (Exception ignore) {
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {}
        }
        return str;
    }
}
