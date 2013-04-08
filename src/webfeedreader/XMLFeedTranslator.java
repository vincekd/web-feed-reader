package webfeedreader;


import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
 
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
// import com.sun.syndication.io.SAXBuilder;

import org.xml.sax.InputSource;

import webfeedreader.Feed;
import webfeedreader.FeedItem;

//Only used by pass through mode
class XMLFeedTranslator {
 
    public static Feed<FeedItem> xmlToFeedItems(String url) throws IOException,FeedException {
        
        // SyndFeedInput input = new SyndFeedInput();
        // SAXBuilder sax = new SAXBuilder(false);
        //SyndFeed feed = input.build(sax.build(url));
        
        InputStream is = new URL(url).openConnection().getInputStream();
        InputSource source = new InputSource(is);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(source);

        List entries = feed.getEntries();
        Iterator itEntries = entries.iterator();
        
        Feed<FeedItem> items = new Feed();
        
        items.syndFeed = feed;
        while (itEntries.hasNext()) {
            SyndEntry entry = (SyndEntry)itEntries.next();
            FeedItem item = new FeedItem();
            item.title =  entry.getTitle();
            item.link = entry.getLink();
            item.author = entry.getAuthor();
            item.publishedDate = entry.getPublishedDate().toString();
            item.description = entry.getDescription().getValue();
            item.descriptionType = entry.getDescription().getType();
            //guid/id?
            items.add(item);
        }
        
        return items;
    }

    public static String feedItemsToXml(Feed<FeedItem> feed) throws IOException,FeedException {
        //take Feed of FeedItems, turn back into xml, return
        List entries = new ArrayList();
        for (int i = 0; i < feed.size(); i++) {
            entries.add(feed.get(i).toEntry());
        }
        feed.syndFeed.setEntries(entries);
        SyndFeedOutput output = new SyndFeedOutput();
        return output.outputString(feed.syndFeed);
    }
}
