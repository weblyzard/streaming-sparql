package com.weblyzard.sparql;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs a streaming SPARQL query on the given SPARQL repository.
 *
 * <p>
 * The returned {@link StreamingResultSet} allows processing as the results are sent by the server.
 *
 * @author Albert Weichselbraun
 */

@Slf4j
public class StreamingQueryExecutor {

    private static final String USER_AGENT = "iSPARQL Library 0.0.6";
    protected static final String ACCEPT_CONTENT_TYPE = "text/tab-separated-values";
    private static final String POST_CONTENT_TYPE = "application/sparql-query";
    protected static final String COMPRESSED_CONTENT_ENCODING = "gzip";

    private static final int MAX_GET_QUERY_LEN = 2 * 1024 - 1;

    static {
        System.setProperty("http.maxConnections", Integer.toString(Runtime.getRuntime().availableProcessors()));
    }

    private StreamingQueryExecutor() {
    }

    /**
     * Open a connection to the repository and return a {@link StreamingResultSet} for processing.
     *
     * @param repositoryUrl the url of the repository to query
     * @param query the query to perform on the repository
     * @param timeout query timeout in milliseconds.
     * @return a {@link StreamingResultSet} for processing
     * @throws IOException in case of IO errors.
     */
    public static StreamingResultSet getResultSet(String repositoryUrl, String query, int timeout) throws IOException {
        HttpURLConnection conn;
        String queryString = URLEncodedUtils.format(Arrays.asList(new Pair("query", query)), StandardCharsets.UTF_8);

        // open connection
        if ((repositoryUrl.length() + queryString.length()) < MAX_GET_QUERY_LEN) {
            conn = (HttpURLConnection) new URL(repositoryUrl + "?" + queryString).openConnection();
            setCommonHeaders(conn, timeout);
        } else {
            conn = openPostConnection(repositoryUrl, queryString, timeout);
        }

        // create result set
        log.info("iSparql receiving '{}' bytes of content type '{}' with encoding '{}'.", conn.getContentLengthLong(),
                conn.getContentType(), conn.getContentEncoding());
        return new StreamingResultSet(conn);
    }

    /**
     * Open a connection to the repository and return a {@link StreamingResultSet} for processing.
     *
     * @param repositoryUrl the url of the repository to query
     * @param query the query to perform on the repository
     * @return a {@link StreamingResultSet} for processing
     * @throws IOException in case of IO erros
     */
    public static StreamingResultSet getResultSet(String repositoryUrl, String query) throws IOException {
        return getResultSet(repositoryUrl, query, -1);
    }

    /**
     * Handle large queries which require a POST query.
     *
     * @param repositoryUrl the URL of the repository to query
     * @param queryString the SPARQL query
     * @return a {@link HttpURLConnection} to the given repository
     * @throws IOException in case of IO errors
     */
    private static HttpURLConnection openPostConnection(String repositoryUrl, String queryString, int timeout)
            throws IOException {
        URL url = new URL(repositoryUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setCommonHeaders(conn, timeout);
        conn.setRequestProperty("Content-Type", POST_CONTENT_TYPE);

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // send form
        try (OutputStream out = conn.getOutputStream()) {
            out.write(queryString.getBytes(Charset.forName("UTF-8")));
        }
        return conn;
    }

    /**
     * Set the HTTP header common to all connections.
     *
     * @param conn the {@link HttpURLConnection} to the SPARQL repository.
     */
    private static void setCommonHeaders(HttpURLConnection conn, int timeout) {
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", ACCEPT_CONTENT_TYPE);
        conn.setRequestProperty("Accept-Encoding", COMPRESSED_CONTENT_ENCODING);
        if (timeout > 0) {
            conn.setRequestProperty("Timeout", Integer.toString(timeout));
        }
    }

    private static class Pair extends org.apache.jena.atlas.lib.Pair<String, String> implements NameValuePair {
        public Pair(String name, String value) {
            super(name, value);
        }

        @Override
        public String getName() {
            return getLeft();
        }

        @Override
        public String getValue() {
            return getRight();
        }
    }
}
