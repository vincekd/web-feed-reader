package webfeedreader;

import java.io.IOException;
import javax.servlet.http.*;
import java.util.logging.Logger;
import java.util.ArrayList;

import webfeedreader.XMLFeedTranslator;
import webfeedreader.FeedFetcher;
import webfeedreader.FeedItem;
import webfeedreader.Feed;
import webfeedreader.Readifier;

public class ReadifyFeedServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(
        ReadifyFeedServlet.class.getName());
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
        String url, out = "";
        XMLFeedTranslator translator = new XMLFeedTranslator();
        //Readifier readifier = new Readifier();

        //FeedFetcher ff = new FeedFetcher();
        Feed<FeedItem> feed = null;
        
        resp.setContentType("text/xml");
        url = req.getParameter("url");

        if (url != null) {
            // 1. fetch xml from server
            // 2. parse xml
            try {
                feed = translator.xmlToFeedItems(url);
            } catch (Exception e) {}
            
            for (int i = 0; i < feed.size(); i++) {
                // 3. fetch actual page
                Readifier readifier = new Readifier(feed.get(i).link);
                // 4. pass into readability algorithm
                feed.get(i).body = readifier.parse();
            }

            // 5. repackage as xml
            try {
                out = translator.feedItemsToXml(feed);
            } catch (Exception e) {}
        }
        // 6. send xml in response        
        resp.getWriter().println(out);
    }
}
