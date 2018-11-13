package com.weblyzard.sparql;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamingTSVParser {

    protected static final char TAB = '\t';

    private final StreamingResultSet resultSet;

    public StreamingTSVParser(StreamingResultSet resultSet) {
        this.resultSet = resultSet;
    }


    public void computeNextTuple(String[] currentTuple, String line) {
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
            log.warn(
                    "Server returned more tuples per result than expected ({}). Ignoring superfluous tuples. TSV line content: '%s'.",
                    currentTuple.length, line);
        }
    }


    /**
     * Reads the value of the given tupleString considering line breaks.
     *
     * @param tupleString the String to read
     * @return the complete tuple String
     */
    private String readTupleValue(String tupleString) {
        // multi tuple strings are only possible for literals
        // => return for non literals or if the literal is complete.
        if (!tupleString.startsWith("\"") || isCompletedLiteral(tupleString)) {
            return tupleString;
        }

        StringBuilder literal = new StringBuilder(tupleString);
        do {
            tupleString = resultSet.readNextLine();
            literal.append(tupleString);
        } while (!isCompletedLiteral(tupleString));
        return literal.toString();
    }

    /**
     * Determines whether the given literal is complete or is continued in the next line.
     *
     * @param literal
     */
    private static boolean isCompletedLiteral(String literal) {
        int tabIndex = literal.indexOf(TAB);
        int endOfTuple = tabIndex == -1 ? literal.length() - 1 : tabIndex;
        for (int i = endOfTuple; i > 0; i--) {
            // potential end of literal
            if (literal.charAt(i) == '"') {
                i--;
                if (i > 0 && literal.charAt(i) != '"') {
                    return true;
                }
            }
        }
        return false;
    }

}
