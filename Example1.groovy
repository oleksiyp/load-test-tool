// run: java -jar loadtest.jar -n 10 -d 1 Example1.groovy
try {
    HTTP.get("http://google.com", "http://facebook.com");
} catch (Throwable thr) {
    System.err.println(thr);
}