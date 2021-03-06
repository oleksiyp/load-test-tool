// example of simple crawler
// run: java -jar loadtest.jar -n 3 -d 1000 -ie 'GLOBALS.instanceList("urls","http://google.com")' Crawler.groovy
import java.util.regex.Pattern;

try {
    List urls = Globals.list("urls");
    String url = Variations.takeAny(urls);
    if (url == null) {
        return;
    }
    HTTP.get({ line ->
        for (String href : HTTP.parse(Pattern.compile("href=[\"\']([^\"\']+)[\"\']"), line))  {
            urls.add(HTTP.resolve(url, href));
        }
    }, url);
} catch (Throwable thr) {
    System.err.println(thr);
}
