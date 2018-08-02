package com.weblyzard.sparql;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.util.NodeFactoryExtra;

/**
 * The streaming result set obtained from a SPARQL server.
 *
 * <p>An {@link Iterator} which provides results as as they are received by the server.
 *
 * @author albert.weichselbraun@htwchur.ch
 */
public class StreamingResultSet implements Iterator<Map<String, Node>>, Closeable {
    private static final char TAB = '\t';
    private static final Logger log = Logger.getLogger(StreamingResultSet.class.getName());
    private BufferedReader in;

    private String[] resultVars;
    private String[] currentTuple;
    private boolean hasNext = true;
    private int rowNumber;

    /**
     * Create a {@link StreamingResultSet} that consumes the given {@link BufferedReader}.
     *
     * @param in the {@link BufferedReader} to consume.
     */
    public StreamingResultSet(BufferedReader in) throws IOException {
        this.in = in;
        // read TSV header and remove the staring "?"
        resultVars = retrieveHeader();
        currentTuple = new String[resultVars.length];

        // read first result
        rowNumber = 0;
        retrieveNextTuple();
    }

    /** Retrieves the next tuple from the input stream */
    private String[] retrieveHeader() throws IOException {
        String line = readNextLine();
        if (line == null) {
            throw new IOException("Cannot retrieve SPARQL result header.");
        }

        String[] result = line.split(Character.toString(TAB));
        // remove leading question marks:
        for (int i = 0; i < result.length; i++) result[i] = result[i].substring(1);

        return result;
    }

    /**
     * Updates currentTuple with the current tuple
     *
     * @return whether the next tuple has been retrieved
     */
    private void retrieveNextTuple() {
        String line = readNextLine();
        if (line == null) {
            hasNext = false;
            return;
        }

        int idx = 0;
        int oldidx = 0;
        int pos = 0;
        try {
            while (true) {
                idx = line.indexOf(TAB, oldidx);
                if (idx == -1) {
                    // read the last value
                    currentTuple[pos++] = line.substring(oldidx);
                    break;
                }
                currentTuple[pos++] = line.substring(oldidx, idx);
                oldidx = idx + 1;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warning(
                    String.format(
                            "Server returned more tuples per result than expected (%d). Ignoring superfluous tuples. TSV line content: '%s'.",
                            currentTuple.length, line));
        }
    }

    /** @return the next line from the input stream or null in case of errors. */
    private String readNextLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return hasNext;
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
                if (value.length() > 0)
                    result.put(resultVars[i], NodeFactoryExtra.parseNode(escape(currentTuple[i])));
            } catch (RiotException e) {
                log.severe(
                        String.format(
                                "Parsing of value '%s' contained in tuple '%s' failed: %s",
                                value, Arrays.deepToString(currentTuple), e.getMessage()));
            }
        }
        retrieveNextTuple();
        rowNumber++;
        return result;
    }

    /**
     * Return the number of rows retrieved so far.
     *
     * @return the number of rows received so far
     */
    public int getRowNumber() {
        return rowNumber;
    }

    /**
     * Returns the array of binding set names used in the query.
     *
     * @return the binding set names used in the query.
     */
    public String[] getResultVars() {
        return resultVars;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
    
    private static String escape(String in) {
        String result = in.replaceAll("\"\"", "'");
        return result;
    }
}
