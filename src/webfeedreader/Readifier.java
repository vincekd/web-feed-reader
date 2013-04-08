//TODO: refactor the whole damn thing-- pretty much just copied from the original js
//TODO: go through newer readability file, use to enhance current script
//TODO: get titles somehow?

package webfeedreader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;

//port of javascript readability
class Readifier {
    final String SCORE_STR = "data-readifierscore";
    final String READIFY_CLASS = "readified";
    //grabArtcle
    final int MIN_TEXT_CHARACTERS = 25;
    final int CHARACTERS_PER_POINT = 100;
    final int PORTION_GRANDPARENT_POINTS = 2;
    final int MIN_SCORE_THRESHOLD = 10;
    final int MIN_SIBLING_CHARACTERS = 80;
    final int ANCESTOR_SCORING_DEPTH = 4;
    //cleanArticle
    final int LENGTH_COMPARISON = 25;
    final int MAX_NUM_IMAGES = 3;
    final int WEIGHT_THRESHOLD = 25;
    final int EMBED_LENGTH_THRESHOLD = 75;
    final double UNDER_THRESHOLD_DENSITY = 0.2;
    final double OVER_THRESHOLD_DENSITY = 0.5;
    final double CLEAN_LINK_DENSITY = 0.33;
    //sibling comparison
    final double MIN_SIBLING_SCORE_MAIN_ARTICLE = 0.2;
    final double SIBLING_LINK_DENSITY = 0.25;
    //scoring
    final int SCORE_CLASS_POSITIVE = 25;
    final int SCORE_CLASS_NEGATIVE = 25;
    final int SCORE_ID_POSITIVE = 25;
    final int SCORE_ID_NEGATIVE = 25;
    final int SCORE_GOOD_TAGS = 5;
    final int SCORE_HALF_GOOD_TAGS = 3;
    final int SCORE_BAD_TAGS = 5;
    final int SCORE_HALF_BAD_TAGS = 3;

    private static final Logger logger = Logger.getLogger(
        ReadifyFeedServlet.class.getName());

    public int maxPages = 20;
    public boolean preserveUnlikely = false;
    private String url;
    private String baseUrl = "";
    private String bodyCache = "";
    private Document doc;
    
    public Readifier(String url) {
        this.url = url;
        try {
            this.doc = Jsoup.connect(url).get();
            //this.baseUrl = this.findBaseUrl(new URL(url));
        } catch (Exception e) {}
    }

    //port readability js
    public String parse() {

        this.doc.normalise();

        //story current html
        if (this.bodyCache == "") {
            this.bodyCache = this.doc.body().html();
        }

        this.prepDocument();

        Element article = this.grabArticle(preserveUnlikely);
        
        this.prepArticle(article);

        if (article.html() == "" && !preserveUnlikely) {
            article = this.grabArticle(true);
            this.prepArticle(article);
            if (article.html() == "") {
                //failed
                return this.bodyCache;
            }
        }
        
        return article.html();
    }

    private void prepArticle(Element el) {
        //do stuff
        this.cleanStyles(el);
        el.html(Pattern.compile("(<br\\s*\\/?>(\\s|&nbsp;?)*){1,}").
                matcher(el.html()).replaceAll("<br />"));
        this.cleanArticle(el);
    }

    private void cleanArticle(Element el) {
        Elements els = null;
        Pattern video = Pattern.compile("http:\\/\\/(www\\.)?(youtube|vimeo)\\.com", Pattern.CASE_INSENSITIVE);

        els = el.select("form,object,iframe,embed");
        for (int i = 0; i < els.size(); i++) {
            Element node = els.get(i);
            if ((node.tagName().toLowerCase() == "object" ||
                 node.tagName().toLowerCase() == "embed") &&
                video.matcher(node.html()).matches()) continue;
            node.remove();
        }
        
        //clean headers
        for (int i = 1; i < 7; i++) {
            els = el.getElementsByTag("h"+ i);
            for (int k = 0; k < els.size(); k++) {
                Element node = els.get(k);
                if (this.getClassWeight(node) < 0 ||
                    this.linkDensity(node) > CLEAN_LINK_DENSITY)
                    node.remove();
            }
        }

        //safer clean on other elements
        els  = el.select("table,ul,div");
        for (int i = 0; i < els.size(); i++) {
            Element node = els.get(i);
            String tag = node.tagName().toLowerCase();
            int weight = this.getClassWeight(node);

            if (weight < 0) {
                node.remove();
                continue;
            }
            
            if (node.text().split(",").length > 10) {
                continue;
            }

            int p = node.getElementsByTag("p").size();
            int img = node.getElementsByTag("img").size();
            int li = node.getElementsByTag("li").size() - 100;
            int input = node.getElementsByTag("input").size();
            Elements embeds = node.getElementsByTag("embed");
            int embedCount = 0;
            for (int k = 0; k < embeds.size(); k++) {
                if (!video.matcher(embeds.get(k).attr("src")).matches()) {
                    embeds.get(k).remove();
                } else {
                    embedCount++;
                }
            }

            float density = this.linkDensity(node);
            int length = node.text().length();

            if (img > p || (li > p && tag != "ul" && tag != "ol") ||
                //(input > Math.floor(p/3)) ||
                (length < LENGTH_COMPARISON && (img == 0 || img > MAX_NUM_IMAGES)) ||
                (weight < WEIGHT_THRESHOLD && density > UNDER_THRESHOLD_DENSITY) ||
                (weight >= WEIGHT_THRESHOLD && density > OVER_THRESHOLD_DENSITY) ||
                ((embedCount == 1 && length < EMBED_LENGTH_THRESHOLD) ||
                 embedCount > 1)) {
                node.remove();
            }
        }

        //remove extra paragrapsh
        els = el.getElementsByTag("p");
        for (int i = 0; i < els.size(); i++) {
            Element node = els.get(i);
            int img = node.getElementsByTag("img").size();
            int embed = node.getElementsByTag("embed").size();
            int object = node.getElementsByTag("object").size();
            if (img == 0 && embed == 0 && object == 0 && node.text().trim().length() == 0 ) {
                node.remove();
            }
        }

        //finally, replace <br/>s inbetween <p>s
        el.html(Pattern.compile("<br[^>]*>\\s*<p", Pattern.CASE_INSENSITIVE).matcher(el.html()).replaceAll("<p"));
    }

    private void cleanStyles(Element el) {
        //iterate over nodes, remove inline styles
        Elements els = el.getAllElements();
        for (int i = 0; i < els.size(); i++) {
            Element node = els.get(i);
            if (node.className() != READIFY_CLASS) {
                node.removeAttr("style");
            }
        }
    }

    // private Element grabArticleFooter() {
    //     return new Element(Tag.valueOf("div"), "");
    // }

    //parses document structure looking for most likely article subtrees
    //TODO: this needs to be optimized: lots of redundat loops
    private Element grabArticle(boolean preserveUnlikely) {
        Elements els = this.doc.getAllElements();
        for (int i = 0; i < els.size(); i++) {
            Element el = els.get(i);
            Pattern re = null;
            Pattern regex = null;
            String str = null;
            if (!preserveUnlikely) {
                str = el.className() + el.id();
                re = Pattern.compile("combx|comment|disqus|foot|header|menu|meta|nav|rss|shoutbox|sidebar|sponsor", Pattern.CASE_INSENSITIVE);
                regex = Pattern.compile("and|article|body|column|main", Pattern.CASE_INSENSITIVE);
                if (el.tagName().toLowerCase() != "body" && re.matcher(str).matches() &&
                    !regex.matcher(str).matches()) {
                    el.remove();
                    continue;
                }
            }

            if (el.tagName().toLowerCase() == "div") {
                re = Pattern.compile("(a|blockquote|dl|div|img|ol|p|pre|table|ul)", Pattern.CASE_INSENSITIVE);
                if (re.matcher(el.html()).matches()) {
                    Element newEl = new Element(Tag.valueOf("p"), "");
                    newEl.html(el.html());
                    //el.parent().replaceChild(el, newEl);
                    el.replaceWith(newEl);
                    i--;
                } else {
                    //might work... add in my new test thing for blockquotes, etc.
                    List<TextNode> childs = el.textNodes();
                    for (int j = 0; j < childs.size(); j++) {
                        TextNode child = childs.get(j);
                        Element newEl = new Element(Tag.valueOf("p"), "");
                        newEl.html(child.text());
                        newEl.attr("style", "display:inline;");
                        newEl.attr("class", READIFY_CLASS);
                        //el.replaceChild(child, newEl);
                        child.replaceWith(newEl);
                    }
                }
            }
        }

        els = this.doc.getElementsByTag("p");
        Elements candidates = new Elements();
        for (int i = 0; i < els.size(); i++) {
            Element el = els.get(i);
            String text = el.text();
            int score = 1;

            if (text.length() < MIN_TEXT_CHARACTERS) continue;

            Element parent = el;
            for (int j = 1; j <= ANCESTOR_SCORING_DEPTH; j++) {
                parent = parent.parent();
                if (parent == null) break;
                if (parent.attr(SCORE_STR) == "") {
                    this.scoreElement(parent);
                    candidates.add(parent);
                }
            }

            //every comma give a point
            score += text.split(",").length;

            //every 100 characters give a point
            score += (text.length() / CHARACTERS_PER_POINT);

            //add score to parent/grandparent
            parent = el;
            int multiplier = 1;
            for (int j = 1; j <= ANCESTOR_SCORING_DEPTH; j++) {
                parent = parent.parent();
                if (parent == null) break;
                parent.attr(SCORE_STR, ""+ (Integer.parseInt(parent.attr(SCORE_STR)) + Math.round(score / multiplier)));
                multiplier = multiplier * PORTION_GRANDPARENT_POINTS;
            }
        }

        //find top element/calculate link density
        int topScore = -1;
        Element topEl = null;
        for (int i = 0; i < candidates.size(); i++) {
            Element el = candidates.get(i);
            int score = Integer.parseInt(el.attr(SCORE_STR));
            score = (int)Math.round(score * (1.0-this.linkDensity(el)));

            if (score > topScore) {
                topEl = el;
                topScore = score;
            }
        }

        if (topEl == null || topEl.tagName().toLowerCase() == "body") {
            topEl = new Element(Tag.valueOf("div"), "");
            topEl.html(this.doc.body().html());
            this.doc.body().html("");
            this.doc.body().appendChild(topEl);
            this.scoreElement(topEl);
        }

        Element content = new Element(Tag.valueOf("div"), "");
        int scoreThreshold = Math.max(MIN_SCORE_THRESHOLD,
                                      (int)Math.round(Integer.parseInt(topEl.attr(SCORE_STR)) * MIN_SIBLING_SCORE_MAIN_ARTICLE));
        content.attr("id", "readifier-content");
        Elements siblings = topEl.parent().children();
        for (int i = 0; i < siblings.size(); i++) {
            Element sib = siblings.get(i);
            boolean append = false;
            //TODO: play with this
            if (sib == topEl) {
                append = true;
            } else {
                String scoreStr = sib.attr(SCORE_STR);
                if (scoreStr != "" &&
                    Integer.parseInt(scoreStr) >= scoreThreshold) {
                    append = true;
                } else if (sib.tagName().toLowerCase() == "p") {
                    float density = this.linkDensity(sib);
                    if (sib.text().length() > MIN_SIBLING_CHARACTERS &&
                        density < SIBLING_LINK_DENSITY) {
                        append = true;
                    } else if (sib.text().length() < MIN_SIBLING_CHARACTERS &&
                               density == 0 && sib.text().matches("\\.( |$)")) {
                        append = true;
                    }
                }
            }

            if (append) {
                content.appendChild(sib);
            }
        }
        
        return content;
    }

    private float linkDensity(Element el) {
        int linkLength = 0;
        int textLength = el.text().length();
        
        if (textLength == 0) return 0;
        
        Elements els = el.getElementsByTag("a");
        for (int i = 0; i < els.size(); i++) {
            linkLength += els.get(i).text().length();
        }
        return (linkLength / textLength);
    }

    private void scoreElement(Element el) {
        int score = 0;
        switch (el.tagName().toLowerCase()) {
        case "div":
            score += SCORE_GOOD_TAGS;
            break;
        case "pre":
        case "td":
        case "blockquote":
            score += SCORE_HALF_GOOD_TAGS;
        case "address":
        case "ol":
        case "ul":
        case "dl":
        case "dd":
        case "dt":
        case "li":
        case "form":
            score -= SCORE_HALF_BAD_TAGS;
        case "h1":
        case "h2":
        case "h3":
        case "h4":
        case "h5":
        case "h6":
        case "th":
            score -= SCORE_BAD_TAGS;
        }

        score += this.getClassWeight(el);
        el.attr(SCORE_STR, ""+ score);
    }

    private int getClassWeight(Element el) {
        int weight = 0;
        Pattern pos = Pattern.compile("article|body|content|entry|hentry|page|pagination|post|text", Pattern.CASE_INSENSITIVE);
        Pattern neg = Pattern.compile("combx|comment|contact|foot|footer|footnote|link|media|meta|promo|related|scroll|shoutbox|sponsor|tags|widget", Pattern.CASE_INSENSITIVE);
        String str = el.className();
        if (str != "") {
            //TODO: else if?
            if (pos.matcher(str).matches()) {
                weight += SCORE_CLASS_POSITIVE;
            }
            if (neg.matcher(str).matches())
                weight -= SCORE_CLASS_NEGATIVE;
        }
        str = el.id();
        if (str != "") {
            //TODO: else if?
            if (pos.matcher(str).matches())
                weight += SCORE_ID_POSITIVE;
            if (neg.matcher(str).matches())
                weight -= SCORE_ID_NEGATIVE;
        }
        
        return weight;
    }

    private void prepDocument() {
        Elements els = this.doc.getElementsByTag("frame");
        if (els.size() > 0) {
            Element bestFrame = null;
            int bestFrameSize = 0;
            for (int i = 0; i < els.size(); i++) {
                //int frameSize = els.get(i);
                //TODO: need to get width/height, figure out biggest, use that
            }

            if (bestFrame != null) {
                //create new body, replace current doc.body() with it
                //set global frameHack to true
            }
        }

        //remove all scripts from document
        this.removeScripts();
    }

    private void removeScripts() {
        //remove js
        Elements els = this.doc.getElementsByTag("script");
        for (int i = 0; i < els.size(); i++) {
            els.get(i).remove();
        }

        //remove styling
        els = this.doc.getElementsByTag("style");
        for (int i = 0; i < els.size(); i++) {
            els.get(i).remove();
        }
        els = this.doc.select("link[rel=stylesheet],link[type='text/css']");
        for (int i = 0; i < els.size(); i++) {
            els.get(i).remove();
        }
    }

    private String findNextPageLink() {
        Elements els = this.doc.getElementsByTag("a");
        return "";
    }

    private String findBaseUrl(URL url) {
        String protocol = url.getProtocol();
        String host = url.getHost();
        String path = url.getPath();

        String out = "";
        ArrayList<String> cleanedSegments = new ArrayList();
        String[] pathSplit = path.split("/");
        int lastTwo = pathSplit.length - 2;

        for (int i = (pathSplit.length-1); i > 0; i--) {
            String seg = pathSplit[i];
            String possibleType = "";
            Pattern reg = null;
            int index = seg.indexOf(".");
            boolean del = false;
            if (index != -1) {
                possibleType = seg.substring(index);
                reg = Pattern.compile("[^a-zA-Z]", Pattern.CASE_INSENSITIVE);
                if (!reg.matcher(possibleType).matches()) {
                    seg = seg.substring(0, index);
                }
            }

            index = seg.indexOf(",00");
            if (index != -1) {
                seg = seg.replaceAll(",00", "");
            }
            
            reg = Pattern.compile("((_|-)?p[a-z]*|(_|-))[0-9]{1,2}$", Pattern.CASE_INSENSITIVE);
            if (i > lastTwo && reg.matcher(seg).matches()) {
                seg = reg.matcher(seg).replaceFirst("");
            }

            reg = Pattern.compile("^\\d{1,2}$");
            if (i < 2 && reg.matcher(seg).matches()) {
                del = true;
            } else if (i == (pathSplit.length-1) && seg.toLowerCase() == "index") {
                del = true;
            } else if (i > lastTwo && seg.length() < 3 && !pathSplit[pathSplit.length-1].matches("[a-zA-Z]")) {
                del = true;
            }

            if (!del) {
                cleanedSegments.add(seg);
            }
        }

        out = protocol +"://"+ host +"/";
        Collections.reverse(cleanedSegments);
        for (String s : cleanedSegments) {
            out += s +"/";
        }
        return out;
    }
}
