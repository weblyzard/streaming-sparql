package com.weblyzard.sparql.tsv;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses TSV files containing multi-line literals and quotes.
 *
 * <ol>
 * <li>Double quotes: <code>""</code> are translated to <code>\"</code>.</li>
 * <li>Newlines are replaced with space.</li>
 * </ol>
 *
 * @author Albert Weichselbraun
 *
 */
@Slf4j
public class TsvParser {

    private enum State {
        START, RESOURCE, LITERAL
    }

    @Getter
    private String[] tsvHeader;
    private BufferedReader in;
    private String currentLine;
    private int idx = 0;
    protected int currentTupleIdx = 0;
    protected Map<String, Node> currentTuple;
    protected CharConsumer currentConsumer;
    private static Map<State, CharConsumer> consumers = new EnumMap<>(State.class);

    static {
        // Start Tuple
        consumers.put(State.START, t -> {
            switch (t.getIfAvailable()) {
                // end of line -> new tuple
                case '\0':
                    return false;
                // tab -> next value
                case '\t':
                    t.pop();
                    t.currentTupleIdx++;
                    break;
                // start of literal
                case '"':
                    t.currentConsumer = consumers.get(State.LITERAL);
                    break;
                // start of resource
                default:
                    t.currentConsumer = consumers.get(State.RESOURCE);
            }
            return true;
        });

        // Consume resource
        consumers.put(State.RESOURCE, t -> {
            String r = t.popTo('\t');
            t.popIfAvailable();
            parseNode(r)
                    .ifPresent(node -> t.currentTuple.put(t.tsvHeader[t.currentTupleIdx], node));
            t.currentTupleIdx++;
            t.currentConsumer = consumers.get(State.START);
            return true;
        });

        // Consume literal
        consumers.put(State.LITERAL, t -> {
            StringBuilder s = new StringBuilder("\"");
            t.pop();
            char ch;
            // consume literal value
            while (true) {
                ch = t.pop();
                if (ch == '"') {
                    if (t.getIfAvailable() == '"') {
                        t.pop();
                        s.append("\\\"");
                        continue;
                    }
                    s.append(ch);
                    break;
                }
                s.append(ch);
            }
            s.append(t.popTo('\t'));
            t.popIfAvailable();
            parseNode(s.toString())
                    .ifPresent(node -> t.currentTuple.put(t.tsvHeader[t.currentTupleIdx], node));
            t.currentTupleIdx++;
            t.currentConsumer = consumers.get(State.START);
            return true;
        });
    }

    public TsvParser(BufferedReader in) throws IOException {
        this.in = in;
        setTsvHeader();
    }

    /**
     * Parses the next tuple.
     *
     * @return an array of the tuples or <code>null</code> if no further lines are available.
     */
    public Map<String, Node> getTuple() {
        currentLine = readNextLine();
        // end of document -> no further tuples are available
        if (currentLine == null) {
            return null;
        }

        // prepare finit state machine
        idx = 0;
        currentTupleIdx = 0;
        currentTuple = new HashMap<>(tsvHeader.length);
        currentConsumer = consumers.get(State.START);

        try {
            while (currentConsumer.consumeChars(this)) {
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn(
                    "Server returned more tuples than expected ({}). Ignoring superfluous tuples. TSV line content: {}",
                    tsvHeader.length, currentLine);
        }
        if (currentTupleIdx != tsvHeader.length) {
            log.warn("Missing {} tuples in line '{}'", (tsvHeader.length - currentTupleIdx),
                    currentLine);
        }
        return currentTuple;
    }

    /**
     * Translates a String into a {@link Node}.
     * 
     * @param nodeString String representation of the RDF node as obtained from the TSV input
     * @return the corresponding RDF {@link Node}
     */
    private static Optional<Node> parseNode(String nodeString) {
        try {
            return Optional.of(NodeFactoryExtra.parseNode(nodeString));
        } catch (RiotException e) {
            log.error("Parsing of value '{}' failed: {}", nodeString, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves the first line from the TSV input stream to set the TSV header information.
     */
    private void setTsvHeader() throws IOException {
        String headerLine = readNextLine();
        if (headerLine == null) {
            throw new IOException("Cannot retrieve SPARQL result header.");
        }

        String[] result = headerLine.split(Character.toString('\t'));
        // standarize output variables (remove "?" or quotes, if necessary)
        for (int i = 0; i < result.length; i++) {
            if (result[i].startsWith("?")) {
                // fuseki, rdf4j: remove "?" prefix
                result[i] = result[i].substring(1);
            } else if (result[i].startsWith("\"") && result[i].endsWith("\"")) {
                // virtuoso: remove starting and ending quotes
                result[i] = result[i].substring(1, result[i].length() - 1);
            }
        }
        tsvHeader = result;
    }

    /**
     * Reads the next line from the SPARQL server's response.
     *
     * @return the next line from the input stream or null in case of errors.
     */
    private String readNextLine() {
        try {
            String line = in.readLine();
            return line;
        } catch (IOException e) {
            return null;
        }
    }

    private char pop() {
        if (idx >= currentLine.length()) {
            currentLine = readNextLine();
            if (currentLine == null) {
                throw new NoSuchElementException();
            }
            currentLine += " ";
            idx = 0;
        }
        return currentLine.charAt(idx++);
    }

    private char popIfAvailable() {
        return idx < currentLine.length() ? currentLine.charAt(idx++) : 0;
    }

    private char getIfAvailable() {
        return idx < currentLine.length() ? currentLine.charAt(idx) : 0;
    }

    private String popTo(char needle) {
        int endIdx = currentLine.indexOf(needle, idx);
        if (endIdx == -1) {
            endIdx = currentLine.length();
        }
        String str = currentLine.substring(idx, endIdx);
        idx = endIdx;
        return str;
    }

}
