package webfeedreader;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndEntry;

import java.util.ArrayList;
import webfeedreader.FeedItem;

class Feed<FeedItem> extends ArrayList<FeedItem> {
    // public String title;
    // public String link;
    // public String description;
    // public String lastBuildDate;
    // public String language;
    // public String updatePeriod;
    // public String updateFrequency;o
    public SyndFeed syndFeed;
}
