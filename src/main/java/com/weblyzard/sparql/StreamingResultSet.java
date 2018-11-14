package com.weblyzard.sparql;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.util.NodeFactoryExtra;
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

    private TsvParser tsvParser;
    @Getter
    private String[] resultVars;
    private String[] currentTuple;
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
        initializeResultSet();
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
        initializeResultSet();
    }

    /**
     * Initializes the result set.
     * <ol>
     * <li>determine the used bindings and</li>
     * <li>retrieve the first tuple</li>
     * </ol>
     *
     * @throws IOException in case of IO errors.
     */
    private void initializeResultSet() throws IOException {
        // read TSV header and remove the staring "?"
        resultVars = retrieveHeader();
        currentTuple = new String[resultVars.length];
        tsvParser = new TsvParser(this);

        // read first result
        rowNumber = 0;
        retrieveNextTuple();
    }


    /**
     * Retrieves the next tuple from the input stream.
     */
    private String[] retrieveHeader() throws IOException {
        String line = readNextLine();
        if (line == null) {
            throw new IOException("Cannot retrieve SPARQL result header.");
        }

        String[] result = line.split(Character.toString('\t'));
        // remove leading question marks:
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].startsWith("?") ? result[i].substring(1) : result[i];
        }

        return result;
    }

    /**
     * Updates currentTuple with the current tuple.
     *
     * @return whether the next tuple has been retrieved
     */
    private void retrieveNextTuple() {
        if ((currentTuple = tsvParser.getTuple()) == null) {
            hasNext = false;
        }
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

        Map<String, Node> result = new HashMap<>(currentTuple.length * 4 / 3 + 1);
        String value;
        for (int i = 0; i < currentTuple.length; i++) {
            // do not create bindings for empty tuples
            value = currentTuple[i];
            try {
                if (value != null && value.length() > 0) {
                    result.put(resultVars[i], NodeFactoryExtra.parseNode(currentTuple[i]));
                }
            } catch (RiotException e) {
                log.error("Parsing of value '{}' contained in tuple '{}' failed: {}", value,
                        Arrays.deepToString(currentTuple), e.getMessage());
            }
        }
        retrieveNextTuple();
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
