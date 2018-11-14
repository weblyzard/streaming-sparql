package com.weblyzard.sparql.tsv;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import com.weblyzard.sparql.StreamingResultSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses TSV files with multi-line literals and quotes.
 *
 * <ol>
 * <li>Quotes: "" -> \".</li>
 * <li>Replace newlines with space.</li>
 * </ol>
 *
 * @author Albert Weichselbraun
 *
 */
@Slf4j
public class TsvParser {

    private StreamingResultSet resultSet;
    private String line;
    private int idx = 0;
    protected int currentTupleIdx = 0;
    @Getter
    private String[] resultVars;
    protected Map<String, Node> currentTuple;

    private enum State {
        START, RESOURCE, LITERAL
    }

    protected CharConsumer currentConsumer;

    private static Map<State, CharConsumer> consumers = new EnumMap<>(State.class);

    static {
        // Start Tuple
        consumers.put(State.START, t -> {
            switch (t.getIfAvailable()) {
                // end of line -> new tuple
                case '\0':
                    throw new NoSuchElementException();
                    // tab -> next value
                case '\t':
                    t.pop();
                    // TODO: write unittest and check whether this is fine (!)
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
        });

        // Consume resource
        consumers.put(State.RESOURCE, t -> {
            String r = t.popTo('\t');
            t.popIfAvailble();
            parseNode(r).ifPresent(node -> t.currentTuple.put(t.resultVars[t.currentTupleIdx++], node));
            t.currentConsumer = consumers.get(State.START);
        });

        // Consume literal
        consumers.put(State.LITERAL, t -> {
            StringBuilder s = new StringBuilder("\"");
            t.pop();
            char ch;
            while (true) {
                ch = t.pop();
                if (ch == '"') {
                    if (t.popIfAvailble() == '"') {
                        s.append("\\\"");
                        continue;
                    }
                    s.append("\"");
                    break;
                }
                s.append(ch);
            }
            parseNode(s.toString()).ifPresent(node -> t.currentTuple.put(t.resultVars[t.currentTupleIdx++], node));
            t.currentConsumer = consumers.get(State.START);
        });
    }

    private static Optional<Node> parseNode(String nodeString) {
        try {
            return Optional.of(NodeFactoryExtra.parseNode(nodeString));
        } catch (RiotException e) {
            log.error("Parsing of value '{}' failed: {}", nodeString, e.getMessage());
            return Optional.empty();
        }
    }

    public TsvParser(StreamingResultSet rs) throws IOException {
        resultSet = rs;
        resultVars = retrieveHeader();
    }

    /**
     * Retrieves the next tuple from the input stream.
     */
    private String[] retrieveHeader() throws IOException {
        String headerLine = resultSet.readNextLine();
        if (headerLine == null) {
            throw new IOException("Cannot retrieve SPARQL result header.");
        }

        String[] result = headerLine.split(Character.toString('\t'));
        // remove leading question marks:
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].startsWith("?") ? result[i].substring(1) : result[i];
        }

        return result;
    }

    /**
     * Parses the next tuple.
     *
     * @return an array of the tuples or <code>null</code> if no further lines are available.
     */
    public Map<String, Node> getTuple() {
        line = resultSet.readNextLine();
        // end of document -> no further tuples are available
        if (line == null) {
            return null;
        }

        // prepare finit state machine
        idx = 0;
        currentTupleIdx = 0;
        currentTuple = new HashMap<>(resultVars.length);
        currentConsumer = consumers.get(State.START);

        try {
            while (true) {
                currentConsumer.consumeChars(this);
            }
        } catch (NoSuchElementException e) {
            if (currentTupleIdx != resultVars.length) {
                log.warn("Missing {} tuples in line '{}'", (resultVars.length - currentTupleIdx - 1), line);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn(
                    "Server returned more tuples than expected ({}). Ignoring superfluous tuples. TSV line content: {}",
                    resultVars.length, line);
        }
        return currentTuple;
    }

    private char pop() {
        if (idx >= line.length()) {
            line = " " + resultSet.readNextLine();
            if (line == null) {
                throw new NoSuchElementException();
            }
            idx = 0;
        }
        return line.charAt(idx++);
    }

    private char popIfAvailble() {
        return idx < line.length() ? line.charAt(idx++) : 0;
    }

    private char getIfAvailable() {
        return idx < line.length() ? line.charAt(idx) : 0;
    }

    private String popTo(char needle) {
        int endIdx = line.indexOf(needle, idx);
        endIdx = endIdx == -1 ? line.length() : endIdx;
        String str = line.substring(idx, endIdx);
        idx = endIdx;
        return str;
    }

}
