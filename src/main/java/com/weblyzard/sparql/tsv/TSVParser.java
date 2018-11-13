package com.weblyzard.sparql.tsv;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;
import com.weblyzard.sparql.StreamingResultSet;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses TSV files with multi-line literals and quotes
 * 
 * <ol>
 * <li>Quotes: "" -> \".</li>
 * <li>Replace newlines with space.</li>
 * 
 * 
 * @author Albert Weichselbraun
 *
 */
@Slf4j
public class TSVParser {

    private StreamingResultSet resultSet;
    private String line;
    private int idx = 0;
    protected int currentTupleIdx = 0;
    protected String[] currentTuple;

    private enum State {
        START, RESOURCE, LITERAL
    }

    protected CharConsumer currentConsumer;

    private static Map<State, CharConsumer> consumers = new EnumMap<>(State.class);
    static {
        // Start Tuple
        consumers.put(State.START, t -> {
            System.out.println("CHAR: >" + (int) t.get() + "|" + t.get() + "<");
            switch (t.get()) {
                // end of line -> new tuple
                case '\0':
                    throw new NoSuchElementException();
                    // tab -> next value
                case '\t':
                    t.pop();
                    t.currentTuple[t.currentTupleIdx++] = "";
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
            t.pop();
            t.currentTuple[t.currentTupleIdx++] = r;
            t.currentConsumer = consumers.get(State.START);
        });

        // Consume literal
        consumers.put(State.LITERAL, t -> {
            StringBuilder s = new StringBuilder("\"");
            t.pop();
            char ch;
            boolean endOfQuote = false;
            while (!endOfQuote) {
                ch = t.pop();
                if (ch == '"') {
                    if (t.get() == '"') {
                        t.pop();
                        s.append("\\\"");
                        continue;
                    }
                    endOfQuote = true;
                } else if (ch == '\n') {
                    ch = ' ';
                }
                s.append(ch);
            }
            // consume \t
            if (t.get() == '\t') {
                t.pop();
            }
            t.currentTuple[t.currentTupleIdx++] = s.toString();
            t.currentConsumer = consumers.get(State.START);
        });
    }

    public TSVParser(StreamingResultSet rs) {
        resultSet = rs;
        currentTuple = new String[rs.getResultVars().length];
    }


    /**
     * Parses the next tuple
     * 
     * @return an array of the tuples or <code>null</code> if no further lines are available.
     */
    public String[] getTuple() {
        // end of document -> no further tuples are available
        Arrays.fill(currentTuple, null);
        currentConsumer = consumers.get(State.START);
        line = resultSet.readNextLine();
        idx = 0;
        if (line == null) {
            return null;
        }

        try {
            while (true) {
                currentConsumer.consumeChars(this);
            }
        } catch (NoSuchElementException e) {
            if (currentTupleIdx != currentTuple.length - 1) {
                currentTupleIdx = 0;
                log.warn("Missing {} tuples in line '{}'",
                        (currentTuple.length - currentTupleIdx - 1), line);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn(
                    "Server returned more tuples than expected ({}). Ignoring superfluous tuples. TSV line content: {}",
                    currentTuple.length, line);
        }
        System.out.println(Arrays.toString(currentTuple));
        return currentTuple;
    }

    protected char pop() {
        if (idx >= line.length()) {
            line = resultSet.readNextLine();
            if (line == null) {
                throw new NoSuchElementException();
            }
            idx = 0;
        }
        return line.charAt(idx++);
    }

    protected char get() {
        return idx < line.length() ? line.charAt(idx) : 0;
    }

    protected String popTo(char needle) {
        int endIdx = line.indexOf(needle, idx);
        endIdx = endIdx == -1 ? line.length() : endIdx;
        String str = line.substring(idx, endIdx);
        idx = endIdx;
        return str;
    }

}
