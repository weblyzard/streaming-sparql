package com.weblyzard.sparql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.junit.Test;

/**
 * Tests borderline cases in the TsvParser
 * 
 * @author Albert Weichselbraun
 *
 */
public class TsvParserTest {

    private static final String QUOTED_LITERAL =
            "\"\"Bruder Klaus\"\" ist der \"\"Schutzpatron\"\" der Schweiz.";

    /**
     * Test a correct {@link com.weblyzard.sparql.StreamingResultSet}
     *
     * @throws IOException
     */
    @Test
    public void testResourceEnding() throws IOException {
        Map<String, Node> result =
                parseSingleTuple("?s\n" + "<http://test.org/1>\n" + "<http://test.org/1>\n");
        assertEquals(NodeFactoryExtra.parseNode("<http://test.org/1>"), result.get("s"));
    }

    /**
     * Test for empty variables in results.
     *
     * @throws IOException
     */
    @Test
    public void testEmptyVariablesInResult() throws IOException {
        Map<String, Node> result =
                parseSingleTuple("?s\t?p\t?o\n" + "<http://test.org/1>\t\t\"o2\"\n");

        assertEquals(NodeFactoryExtra.parseNode("<http://test.org/1>"), result.get("s"));
        assertFalse(result.containsKey("p"));
        assertEquals(NodeFactoryExtra.parseNode("\"o2\""), result.get("o"));
    }

    /**
     * Test for multiple empty variables in results.
     *
     * @throws IOException
     */
    @Test
    public void testMultipleEmptyVariablesInResult() throws IOException {
        Map<String, Node> result;
        // at the beginning
        result = parseSingleTuple("?s\t?p\t?o\n" + "\t\t\"o2\"\n");
        assertFalse(result.containsKey("s"));
        assertFalse(result.containsKey("p"));
        assertEquals(NodeFactoryExtra.parseNode("\"o2\""), result.get("o"));

        // at the end
        result = parseSingleTuple("?s\t?p\t?o\n" + "<http://test.org/1>\t\t\n");
        assertEquals(NodeFactoryExtra.parseNode("<http://test.org/1>"), result.get("s"));
        assertFalse(result.containsKey("p"));
        assertFalse(result.containsKey("o"));

        // all empty
        result = parseSingleTuple("?s\t?p\t?o\n" + "\t\t\t\n");
        assertEquals(0, result.size());
    }

    @Test
    public void quotesInLiteralTest() throws IOException {
        Map<String, Node> result;
        result = parseSingleTuple(String.format("?s\t?p\t?o\n" + "\t\t\"%s\"\n", QUOTED_LITERAL));
        assertEquals("\"Bruder Klaus\" ist der \"Schutzpatron\" der Schweiz.",
                result.get("o").getLiteralValue());

        result = parseSingleTuple(String.format("?s\t?p\t?o\n" + "\t\"%s\"\t\n", QUOTED_LITERAL));
        assertEquals("\"Bruder Klaus\" ist der \"Schutzpatron\" der Schweiz.",
                result.get("p").getLiteralValue());

        result = parseSingleTuple(String.format("?s\t?p\t?o\n" + "\"%s\"\t\t\n", QUOTED_LITERAL));
        assertEquals("\"Bruder Klaus\" ist der \"Schutzpatron\" der Schweiz.",
                result.get("s").getLiteralValue());
    }

    /**
     * Performs a query and returns the first result tuple.
     */
    private static Map<String, Node> parseSingleTuple(String query) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(query));

        try (StreamingResultSet resultSet = new StreamingResultSet(bufferedReader)) {
            return resultSet.next();
        }
    }
}
