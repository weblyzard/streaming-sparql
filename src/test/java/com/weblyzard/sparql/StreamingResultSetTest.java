package com.weblyzard.sparql;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotException;
import org.junit.Test;

public class StreamingResultSetTest {

    /**
     * Test a correct {@link com.weblyzard.sparql.StreamingResultSet}
     *
     * @throws IOException
     */
    @Test
    public void testStreamingResultSet() throws IOException {
        BufferedReader bufferedReader =
                new BufferedReader(
                        new StringReader(
                                "?s\t?p\t?o\n"
                                        + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"
                                        + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o2\"\n"
                                        + "<http://test.org/3>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o3\""));

        List<Map<String, Node>> result = new ArrayList<>();
        try (StreamingResultSet resultSet = new StreamingResultSet(bufferedReader)) {
            assertArrayEquals(new String[] {"s", "p", "o"}, resultSet.getResultVars());

            int rowsRead = 0;
            assertEquals(rowsRead, resultSet.getRowNumber());
            while (resultSet.hasNext()) {
                result.add(resultSet.next());
                rowsRead++;
                assertEquals(rowsRead, resultSet.getRowNumber());
            }
        }
        assertEquals(3, result.size());
    }

    /**
     * Test an empty result (i.e. headers but not tuples)
     *
     * @throws IOException
     */
    @Test
    public void testEmptyStreamingResultSet() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader("?s\t?p\t?o\n"));
        try (StreamingResultSet s = new StreamingResultSet(bufferedReader)) {
            List<Map<String, Node>> result = Lists.newArrayList(s);
            assertEquals(0, result.size());
        }
    }

    /** Test for inconsistencies in the handling between header and body of the TSV file. */
    @Test
    public void testTsvColumnInconsistencies() throws IOException {
        BufferedReader bufferedReader;
        List<Map<String, Node>> result;

        // missing tuple test
        bufferedReader =
                new BufferedReader(
                        new StringReader(
                                "?s\t?p\t?o\n"
                                        + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"
                                        + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t"));
        try (StreamingResultSet s = new StreamingResultSet(bufferedReader)) {
            result = Lists.newArrayList(s);
            assertEquals(2, result.size());
            assertEquals(3, result.get(0).size());
            assertEquals(2, result.get(1).size());
        }

        // additional tuple test
        bufferedReader =
                new BufferedReader(
                        new StringReader(
                                "?s\t?p\t?o\n"
                                        + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"
                                        + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o2\"\t\"o21\""));
        try (StreamingResultSet s = new StreamingResultSet(bufferedReader)) {
            result = Lists.newArrayList(s);
            assertEquals(2, result.size());
            assertEquals(3, result.get(0).size());
            assertEquals(3, result.get(1).size());
        }
    }

    public void testInvalidData() throws IOException {
        // Invalid tuples test
        BufferedReader bufferedReader =
                new BufferedReader(
                        new StringReader(
                                "?s\t?p\t?o\n"
                                        + "...\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"));

        try (StreamingResultSet s = new StreamingResultSet(bufferedReader)) {
            @SuppressWarnings("unused")
            List<Map<String, Node>> result = Lists.newArrayList(s);
            assertTrue(result.isEmpty());
        }
    }

    /**
     * Test an empty stream (i.e. no data at all)
     *
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void testEmptyStream() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(""));
        try (StreamingResultSet s = new StreamingResultSet(bufferedReader)) {
            @SuppressWarnings("unused")
            List<Map<String, Node>> result = Lists.newArrayList(s);
        }
    }
}
