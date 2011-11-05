// example of simple crawler
// run: java -jar loadtest.jar -n 3 -d 1 Example2.groovy
import java.util.regex.Pattern

try {
    List urls = Globals.list("urls", "http://google.com");
    String url = Variations.takeAny(urls);
    System.out.println(url);
    HTTP.get({ line ->
        System.out.println(line);
        for (String href : HTTP.parse(Pattern.compile("href=[\"\']([^\"\']+)[\"\']"), line))  {
            urls.add(HTTP.resolve(url, href));
        }
    }, url);
} catch (Throwable thr) {
    System.err.println(thr);
}
