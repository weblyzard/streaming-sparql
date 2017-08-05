package ch.htwchur.isparql;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotException;
import org.junit.Test;

public class StreamingResultSetTest {

    /**
     * Test a correct {@link StreamingResultSet}
     * 
     * @throws IOException
     */
    @Test
    public void testStreamingResultSet() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader("?s\t?p\t?o\n"
                + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"
                + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o2\"\n"
                + "<http://test.org/3>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o3\""));

        List<Map<String, Node>> result = readResultSet(new StreamingResultSet(bufferedReader));
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
        List<Map<String, Node>> result = readResultSet(new StreamingResultSet(bufferedReader));
        assertEquals(0, result.size());

    }

    /**
     * Test for inconsistencies in the handling between header and body of the TSV file.
     */
    @Test
    public void testTsvColumnInconsistencies() throws IOException {
        BufferedReader bufferedReader;
        List<Map<String, Node>> result;

        // missing tuple test
        bufferedReader = new BufferedReader(new StringReader("?s\t?p\t?o\n"
                + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"
                + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t"));
        result = readResultSet(new StreamingResultSet(bufferedReader));
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(2, result.get(1).size());
        
        // additional tuple test
        bufferedReader = new BufferedReader(new StringReader("?s\t?p\t?o\n"
                + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"
                + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o2\"\t\"o21\""));
        result = readResultSet(new StreamingResultSet(bufferedReader));
        assertEquals(2, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(3, result.get(1).size());
    }
    
    @Test(expected=RiotException.class)
    public void testInvalidData() throws IOException {
        // Invalid tuples test 
        BufferedReader bufferedReader = new BufferedReader(new StringReader("?s\t?p\t?o\n"
                + "...\t<https://www.w3.org/TR/rdf-schema/label>\t\"o1\"\n"));
        @SuppressWarnings("unused")
        List<Map<String, Node>> result = readResultSet(new StreamingResultSet(bufferedReader));
    }

    /**
     * Test an empty stream (i.e. no data at all)
     * 
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void testEmptyStream() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(""));
        @SuppressWarnings("unused")
        List<Map<String, Node>> result = readResultSet(new StreamingResultSet(bufferedReader));
    }

    private List<Map<String, Node>> readResultSet(StreamingResultSet s) {
        List<Map<String, Node>> result = new ArrayList<>();
        while (s.hasNext()) {
            result.add(s.next());
        } ;
        return result;
    }

}
