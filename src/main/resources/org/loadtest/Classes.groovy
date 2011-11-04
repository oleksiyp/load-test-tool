import java.util.regex.Pattern
import java.util.regex.Matcher
import org.loadtest.ScriptRunner


class Variations{
    private static def getBindings() {
        return ScriptRunner.getGroovyShell().getContext();
    }

    public static def select(...args) {
        double v = 0;
        for (int i = 0; i < args.length; i += 2) {
            v += args[i];
            args[i] = v;
        }
        for (int i = 0; i < args.length; i += 2) {
            args[i] /= v;
        }
        double coinExperiment = getBindings().RANDOM.nextDouble(1.0);
        for (int i = 0; i < args.length; i += 2) {
            if (coinExperiment < args[i]) {
                args[i+1]();
                break;
            }
        }
    }

    public static def any(Object ...args) {
        return args[getBindings().RANDOM.nextInt(args.length)];
    }

    public static def any(List args) {
        synchronized(args) {
            return args.get(getBindings().RANDOM.nextInt(args.size()));
        }
    }

    public static void main(String[] args) {}
}

class HTTP {
    private static def getBindings() {
        return ScriptRunner.getGroovyShell().getContext();
    }

    public static def get(Closure reporter, String ...urls) {
        long start = System.currentTimeMillis();
        String urlString = Variations.any(urls);
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        String contentType = conn.getContentType();
        String charset = "UTF-8";
        if (contentType != null && !contentType.isEmpty()) {
            Matcher matcher = Pattern.compile("charset=([^ ]+)", Pattern.CASE_INSENSITIVE)
                .matcher(contentType);
            if (matcher.find()) {
                charset = matcher.group(1);
            }
        }
        InputStream input = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));
        String line;
        Object result = null;
        while ((line = reader.readLine()) != null) {
            if (reporter != null) {
                result = reporter(line);
            }
        }
        reader.close();
        long end = System.currentTimeMillis();
        getBindings().STATS.addQuery(urlString, end - start);
        return result;
    }

    public static def get(String ...urls) {
        List result = new ArrayList();
        HTTP.get({line->result.add(line);}, urls);
        return result.toArray(new String[result.size()]);
    }

    public static def parse(Pattern regex, String ...html) {
        List result = new ArrayList();
        for (String line : html) {
            Matcher matcher = regex.matcher(line);
            while (matcher.find()) {
                String []res = new String[matcher.groupCount()];
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    res[i - 1] = matcher.group(i);
                }
                result.add(res);
            }
        }
        boolean oneSizer = true;;
        for (String []arr : result) {
            if (arr.length != 1) oneSizer = false;
        }
        if (oneSizer) {
            String []resultArray = new String[result.size()];
            int i = 0;
            for (String []arr : result) {
                resultArray[i++] = arr[0];
            }
            return resultArray;
        } else {
            String [][]resultArray = result.toArray(new String[result.size()][0]);
            return resultArray;
        }
    }

    public static def resolve(String uri, String path) {
        return new URI(uri).resolve(path).toString();
    }

    public static void main(String[] args) {}
}


