// example of simple crawler
// run: java -jar loadtest.jar -n 3 -d 1000 Example2.groovy
import java.util.regex.Pattern;

try {
    List urls = GLOBALS.list("urls", "http://google.com");
    String url = Variations.any(urls);
    HTTP.get({ line ->
        for (String href : HTTP.parse(Pattern.compile("href=[\"\']([^\"\']+)[\"\']"), line))  {
            urls.add(HTTP.resolve(url, href));
        }
    }, url);
} catch (Throwable thr) {
    System.err.println(thr);
}
