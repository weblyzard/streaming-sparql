package ch.htwchur.isparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.sparql.util.NodeFactoryExtra;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

/**
 * The streaming result set obtained from a SPARQL server.
 * 
 * An {@link Iterator} which provides results as as they are received by the server.
 * 
 * @author albert.weichselbraun@htwchur.ch
 *
 */
public class StreamingResultSet implements Iterator<Map<String, Node>> {
    private static final char TAB = '\t';
    private static final Splitter TAB_SPLITTER = Splitter.on(TAB);
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
        resultVars = retrieveHeader().stream().map(str -> str.substring(1)).toArray(String[]::new);
        currentTuple = new String[resultVars.length];

        // read first result
        rowNumber = 0;
        retrieveNextTuple();
    }

    /**
     * Retrieves the next tuple from the input stream
     */
    private List<String> retrieveHeader() throws IOException {
        String line = readNextLine();
        if (line == null) {
            throw new IOException("Cannot retrieve SPARQL result header.");
        }
        return TAB_SPLITTER.splitToList(line);
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
            log.warning(String.format("Server returned more tuples per result than expected (%d). Ignoring superfluous tuples. TSV line content: '%s'.",
                    currentTuple.length, line));
        }
    }

    /**
     * @return the next line from the input stream or null in case of errors.
     */
    private String readNextLine() {
        rowNumber++;
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        if (!hasNext) {
            try {
                in.close();
            } catch (IOException e) {
                log.warning("Closing streaming connections caused an exception: " + e.toString());
            }
        }
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

        Map<String, Node> result = Maps.newHashMapWithExpectedSize(currentTuple.length);
        String value;
        for (int i = 0; i < currentTuple.length; i++) {
            // do not create bindings for empty tuples
            value = currentTuple[i];
            try {
                if (value.length() > 0)
                    result.put(resultVars[i], NodeFactoryExtra.parseNode(currentTuple[i]));
            } catch (RiotParseException e) {
                log.severe(String.format("Parsing of value '%s' contained in tuple '%s' failed: %s",
                        value, Arrays.deepToString(currentTuple), e.getMessage()));
            }
        }
        retrieveNextTuple();
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

}
