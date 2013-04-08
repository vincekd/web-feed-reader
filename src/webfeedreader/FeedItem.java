package webfeedreader;

import java.util.Date;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndContentImpl;

class FeedItem {
    protected static int lastId = 0;
    
    public final int id;
    //public final int parentId;
    public String title = "";
    public String link = "";
    public String author = "";
    public String publishedDate = "";
    public String description = "";
    public String descriptionType = "";
    public String body = "";

    public FeedItem() {
        this.id = lastId++;
        //this.parentId = parentId;
    }

    public SyndEntryImpl toEntry() {
        SyndEntryImpl entry = new SyndEntryImpl();
        entry.setTitle(this.title);
        entry.setLink(this.link);
        entry.setAuthor(this.author);
        entry.setPublishedDate(new Date(this.publishedDate));
        SyndContentImpl desc = new SyndContentImpl();
        if (this.body != "") {
            desc.setType("text/html");
            desc.setValue(this.body);
        } else {
            desc.setType(this.descriptionType);
            desc.setValue(this.description);
        }
        entry.setDescription(desc);
        return entry;
    }
}
