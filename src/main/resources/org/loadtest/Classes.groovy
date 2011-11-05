import java.util.regex.Pattern
import java.util.regex.Matcher
import org.loadtest.ScriptRunner

class Context {
    private static def getLocalShellBindings() {
        if (ScriptRunner == null || ScriptRunner.getGroovyShell() == null) {
            return null;
        }
        return ScriptRunner.getGroovyShell().getContext();
    }

    public static void main(String[] args) {}
}

public class Variations{
    private static final RANDOM = new Random();

    public static def select(...args) {
        double v = 0;
        for (int i = 0; i < args.length; i += 2) {
            v += args[i];
            args[i] = v;
        }
        for (int i = 0; i < args.length; i += 2) {
            args[i] /= v;
        }
        double coinExperiment = RANDOM.nextDouble((double)1.0);
        for (int i = 0; i < args.length; i += 2) {
            if (coinExperiment < args[i]) {
                Closure closure = (Closure)args[i+1];
                closure.call();
                break;
            }
        }
    }

    public static def takeAny(Object ...args) {
        return takeAny(Arrays.asList(args));
    }

    public static def takeAny(List args) {
        synchronized(args) {
            if (args.size() == 0) {
                return null
            };

            return args.get(RANDOM.nextInt(args.size()));
        }
    }

    public static void main(String[] args) {}
}

public class HTTP {
    public static def get(Closure reporter, String ...urls) {
        long start = System.currentTimeMillis();
        String urlString = Variations.takeAny(urls);
        if (urlString == null) {
            throw new IllegalArgumentException("name should not be null");
        }
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
                result = reporter.call(line);
            }
        }
        reader.close();
        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection)conn).disconnect();
        }
        long end = System.currentTimeMillis();
        Binding bindings = Context.getLocalShellBindings();
        if (bindings != null) {
            bindings.STATS.addSample(urlString, end - start);
        }
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


public class Globals {
    private Map values = new HashMap();

    private synchronized void instancePut(String name, Object value) {
        values.put(name, value);
    }

    private synchronized Object instanceGet(String name) {
        return values.get(name);
    }

    private synchronized List instanceList(String name, String []defaultValues) {
        List list = (List) values.get(name);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList(Arrays.asList(defaultValues)));
            values.put(name, list);
        }
        return list;
    }

    private synchronized long instanceIncrement(String name) {
        Number num = (Number)values.get(name);
        if (num == null) {
            num = 0L;
        }
        long res = num.longValue();
        values.put(name, res + 1);
        return res;
    }

    // used for alone script runs
    private static Globals INSTANCE = new Globals();

    private synchronized static Globals getGlobals() {
        Binding bindings = Context.getLocalShellBindings();
        if (!(bindings == null || bindings.getProperty("GLOBAL_VALUES") == null)) {
            INSTANCE.values = bindings.GLOBAL_VALUES;
        } else {
            System.err.println("Local context used - globals not working");
        }
        return INSTANCE;
    }

    // static part

    public static void put(String name, Object value) {
        getGlobals().instancePut(name, value);
    }

    public static Object get(String name) {
        return getGlobals().instanceGet(name);
    }

    public static List list(String name, String ...defaultValues) {
        return getGlobals().instanceList(name, defaultValues);
    }

    public static long increment(String name) {
        return getGlobals().instanceIncrement(name);
    }
}
