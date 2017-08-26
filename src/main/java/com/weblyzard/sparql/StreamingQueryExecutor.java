package com.weblyzard.sparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Performs a streaming SPARQL query on the given SPARQL repository.
 *
 * <p>The returned {@link StreamingResultSet} allows processing as the results are sent by the
 * server.
 *
 * @author albert.weichselbraun@htwchur.ch
 */
public class StreamingQueryExecutor {

    private static final String USER_AGENT = "iSPARQL Library 0.0.1";
    private static final String CONTENT_TYPE = "text/tab-separated-values";
    private static final String COMPRESSED_CONTENT_ENCODING = "gzip";

    private static final int MAX_GET_QUERY_LEN = 2 * 1024 - 1;
    public static final Logger log =
            Logger.getLogger(StreamingQueryExecutor.class.getCanonicalName());

    static {
        System.setProperty(
                "http.maxConnections",
                Integer.toString(Runtime.getRuntime().availableProcessors()));
    }

    private StreamingQueryExecutor() {}

    /**
     * Open a connection to the repository and return a {@link StreamingResultSet} for processing.
     *
     * @param repositoryUrl the url of the repository to query
     * @param query the query to perform on the repository
     * @param timeout query timeout in milliseconds.
     * @return a {@link StreamingResultSet} for processing
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static StreamingResultSet getResultSet(String repositoryUrl, String query, int timeout)
            throws IOException {
        HttpURLConnection conn;
        String queryString =
                URLEncodedUtils.format(
                        Arrays.asList(new Pair("query", query)), StandardCharsets.UTF_8);

        // open connection
        if ((repositoryUrl.length() + queryString.length()) < MAX_GET_QUERY_LEN) {
            conn = (HttpURLConnection) new URL(repositoryUrl + "?" + queryString).openConnection();
            setCommonHeaders(conn, timeout);
        } else {
            conn = openPostConnection(repositoryUrl, queryString, timeout);
        }

        // create result set
        log.info(
                String.format(
                        "iSparql receiving '%s' bytes of content type '%s' with encoding '%s'.",
                        conn.getContentLengthLong(),
                        conn.getContentType(),
                        conn.getContentEncoding()));
        BufferedReader bin =
                COMPRESSED_CONTENT_ENCODING.equalsIgnoreCase(conn.getContentEncoding())
                        ? new BufferedReader(
                                new InputStreamReader(new GZIPInputStream(conn.getInputStream())))
                        : new BufferedReader(new InputStreamReader(conn.getInputStream()));

        if (!conn.getContentType().startsWith(CONTENT_TYPE)) {

            final String logMessage =
                    String.format(
                            "Server returned incorrect content type '%s' rather than '%s'.",
                            conn.getContentType(), CONTENT_TYPE);
            log.severe(logMessage);
            log.severe(
                    "Content returned by the server (first 3 lines): "
                            + bin.lines().limit(3).collect(Collectors.joining("\n")));
            bin.close();
            throw new IOException(logMessage);
        }

        return new StreamingResultSet(bin);
    }

    /**
     * Open a connection to the repository and return a {@link StreamingResultSet} for processing.
     *
     * @param repositoryUrl the url of the repository to query
     * @param query the query to perform on the repository
     * @return a {@link StreamingResultSet} for processing
     * @throws IOException
     */
    public static StreamingResultSet getResultSet(String repositoryUrl, String query)
            throws IOException {
        return getResultSet(repositoryUrl, query, -1);
    }

    /**
     * Handle large queries which require a POST query.
     *
     * @param repositoryUrl
     * @param queryString
     * @return
     * @throws IOException
     */
    private static HttpURLConnection openPostConnection(
            String repositoryUrl, String queryString, int timeout) throws IOException {
        URL url = new URL(repositoryUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setCommonHeaders(conn, timeout);

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        // send form
        OutputStream out = conn.getOutputStream();
        out.write(queryString.getBytes(Charset.forName("UTF-8")));
        out.close();
        return conn;
    }

    /**
     * Set the HTTP header common to all connections
     *
     * @param conn
     */
    private static void setCommonHeaders(HttpURLConnection conn, int timeout) {
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", CONTENT_TYPE);
        conn.setRequestProperty("Accept-Encoding", COMPRESSED_CONTENT_ENCODING);
        if (timeout > 0) {
            conn.setRequestProperty("Timeout", Integer.toString(timeout));
        }
    }

    private static class Pair extends org.apache.jena.atlas.lib.Pair<String, String>
            implements NameValuePair {
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