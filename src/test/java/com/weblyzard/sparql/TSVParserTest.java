package com.weblyzard.sparql;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.junit.Test;

public class TSVParserTest {

    /**
     * Test a correct {@link com.weblyzard.sparql.StreamingResultSet}
     *
     * @throws IOException
     */
    @Test
    public void testResourceEnding() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new StringReader("?s\n" + "<http://test.org/1>\n" + "<http://test.org/1>\n"));

        try (StreamingResultSet resultSet = new StreamingResultSet(bufferedReader)) {
            assertArrayEquals(new String[] {"s"}, resultSet.getResultVars());

            int rowsRead = 0;
            assertEquals(rowsRead, resultSet.getRowNumber());
            while (resultSet.hasNext()) {
                Map<String, Node> rs = resultSet.next();
                System.out.println(rs);
                // assertEquals("<http://test.org/1>", rs.get("s"));
            }
        }
    }
}
