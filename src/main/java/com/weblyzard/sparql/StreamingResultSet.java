package com.weblyzard.sparql;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.jena.graph.Node;
import com.weblyzard.sparql.tsv.TsvParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The streaming result set obtained from a SPARQL server.
 *
 * <p>
 * An {@link Iterator} which provides results as as they are received by the server.
 *
 * @author Albert Weichselbraun
 * @author Philipp Kuntschik
 */
@Slf4j
public class StreamingResultSet implements Iterator<Map<String, Node>>, Closeable {
    private BufferedReader in;

    private final TsvParser tsvParser;
    private Map<String, Node> currentTuple;
    private boolean hasNext = true;
    @Getter
    private int rowNumber;

    /**
     * Create a {@link StreamingResultSet} that consumes the given {@link BufferedReader}.
     *
     * @param in the {@link BufferedReader} to consume.
     * @throws IOException in case of IO errors.
     */
    public StreamingResultSet(BufferedReader in) throws IOException {
        this.in = in;
        tsvParser = new TsvParser(this);
    }

    /**
     * Create a {link {@link StreamingResultSet} that consumes data from the given
     * {@link HttpURLConnection}.
     *
     * @param conn the connection to read the data from.
     * @throws IOException in case of IO errors.
     */
    public StreamingResultSet(HttpURLConnection conn) throws IOException {
        in = StreamingQueryExecutor.COMPRESSED_CONTENT_ENCODING.equalsIgnoreCase(conn.getContentEncoding())
                ? new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream())))
                : new BufferedReader(new InputStreamReader(conn.getInputStream()));

        if (!conn.getContentType().startsWith(StreamingQueryExecutor.ACCEPT_CONTENT_TYPE)) {

            final String logMessage = String.format("Server returned incorrect content type '%s' rather than '%s'.",
                    conn.getContentType(), StreamingQueryExecutor.ACCEPT_CONTENT_TYPE);
            log.error(logMessage);
            log.error("Content returned by the server (first 3 lines): "
                    + in.lines().limit(3).collect(Collectors.joining("\n")));
            in.close();
            throw new IOException(logMessage);
        }
        tsvParser = new TsvParser(this);
    }


    /**
     * Reads the next line from the SPARQL server's response.
     *
     * @return the next line from the input stream or null in case of errors.
     */
    public String readNextLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Return the next result set for the current query.
     *
     * @return a Mapping of binding set names to the corresponding {@link Node}.
     */
    @Override
    public Map<String, Node> next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        Map<String, Node> result = currentTuple;
        if ((currentTuple = tsvParser.getTuple()) == null) {
            hasNext = false;
        }
        rowNumber++;
        return result;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }


    @Override
    public boolean hasNext() {
        return hasNext;
    }
}
