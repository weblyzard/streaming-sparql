package ch.htwchur.isparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Charsets;

/**
 * Performs a Streaming SPARQL query on the given SPARQL repository.
 * 
 * @author albert.weichselbraun@htwchur.ch
 *
 */
public class StreamingQueryExecutor  {
	
	public final static int DEFAULT_BUFFER_SIZE = 1024;

	public final static String USER_AGENT = "iSPARQL Library 0.0.1";
	public final static String CONTENT_TYPE = "text/tab-separated-values";
	
	private final static int MAX_GET_QUERY_LEN = 2*1024-1;
	
	static {
		System.setProperty("http.maxConnections", Integer.toString(Runtime.getRuntime().availableProcessors()));
	}
	
	/**
	 * Open a connection to the repository and add an StreamingResultSet for processing :)
	 * @return
	 * 		a {@link StreamingResultSet} for processing
	 * @throws IOException
	 */
	public static StreamingResultSet getResultSet(String repositoryUrl, String query) throws IOException {
		URLConnection conn;
		String queryString = URLEncodedUtils.format(Arrays.asList(new Pair("query", query)), StandardCharsets.UTF_8);

		// open connection
		if ((repositoryUrl.length() + queryString.length()) < MAX_GET_QUERY_LEN) {
			conn = new URL(repositoryUrl + "?" + queryString).openConnection();
			conn.setRequestProperty("User-Agent", USER_AGENT);
			conn.setRequestProperty("Accept", CONTENT_TYPE);
		} else {
			conn = openPostConnection(repositoryUrl, queryString);
		}		
		
		// create result set
		BufferedReader bin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		return new StreamingResultSet(bin);
	}
	
	private static URLConnection openPostConnection(String repositoryUrl, String queryString) throws IOException {
		URL url = new URL(repositoryUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Accept", CONTENT_TYPE);

		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		// send form
		OutputStream out = conn.getOutputStream();
		out.write(queryString.getBytes(Charsets.UTF_8));
		out.close();
		return conn;
	}

	
	public static void main(String[] argv) throws IOException {
		if (argv.length < 2) {
			System.out.println("Call with URL and query.");
			System.exit(-1);
		}
		
		StreamingResultSet s = StreamingQueryExecutor.getResultSet(argv[0], argv[1]);
		while (s.hasNext()) {
			System.out.println(s.next());
		}
	}
	
    public static class Pair extends org.apache.jena.atlas.lib.Pair<String, String> implements NameValuePair {
        public Pair(String name, String value) { super(name, value); }
        @Override
        public String getName()  { return getLeft() ;  }
        @Override
        public String getValue() { return getRight() ; }
    }
	
}
