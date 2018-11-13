package com.weblyzard.sparql;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.junit.Test;

public class MultilineLiteralTest {

    private static final String MULTILINE_TEXT =
            "(For the film, see Sophie Scholl – The Final Days.)(See also: Hans and Sophie Scholl) Sophia Magdalena Scholl\n"
                    + "(9 May 1921 – 22 February 1943) was a German student and anti-Nazi political activist, active within the White Rose non-violent resistance group in Nazi Germany.";

    /**
     * Test a correct {@link com.weblyzard.sparql.StreamingResultSet}
     *
     * @throws IOException
     */
    @Test
    public void testNewlineResponse() throws IOException {
        String queryResponse = String.format(
                "?s\t?p\t?o\n"
                        + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"%s\"\n"
                        + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o2\"\n"
                        + "<http://test.org/3>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o3\"",
                MULTILINE_TEXT);
        BufferedReader bufferedReader = new BufferedReader(new StringReader(queryResponse));

        List<Map<String, Node>> result = new ArrayList<>();
        try (StreamingResultSet resultSet = new StreamingResultSet(bufferedReader)) {
            assertArrayEquals(new String[] {"s", "p", "o"}, resultSet.getResultVars());

            int rowsRead = 0;
            assertEquals(rowsRead, resultSet.getRowNumber());
            while (resultSet.hasNext()) {
                Map<String, Node> rs = resultSet.next();
                System.out.println(rs);
                result.add(rs);
                rowsRead++;
                assertEquals(rowsRead, resultSet.getRowNumber());
            }
        }
        assertEquals(3, result.size());
    }

    /**
     * Test a correct {@link com.weblyzard.sparql.StreamingResultSet}
     *
     * @throws IOException
     */
    @Test
    public void testMultiNewlineMixedResponse() throws IOException {
        String queryResponse = String.format(
                "?o1\t?o2\t?s\n" + "\"o1\"\t\"%s\"\t<http://test.org/1>\n"
                        + "\"%s\"\t\"o2\"\t<http://test.org/1>\n"
                        + "\"%s\"\t\"%s\"\t<http://test.org/1>\n",
                MULTILINE_TEXT, MULTILINE_TEXT, MULTILINE_TEXT, MULTILINE_TEXT);
        BufferedReader bufferedReader = new BufferedReader(new StringReader(queryResponse));

        List<Map<String, Node>> result = new ArrayList<>();
        try (StreamingResultSet resultSet = new StreamingResultSet(bufferedReader)) {
            assertArrayEquals(new String[] {"o1", "o2", "s"}, resultSet.getResultVars());

            int rowsRead = 0;
            assertEquals(rowsRead, resultSet.getRowNumber());
            while (resultSet.hasNext()) {
                Map<String, Node> rs = resultSet.next();
                System.out.println(rs);
                result.add(rs);
                rowsRead++;
                assertEquals(rowsRead, resultSet.getRowNumber());
            }
        }
        assertEquals(3, result.size());
    }


}
