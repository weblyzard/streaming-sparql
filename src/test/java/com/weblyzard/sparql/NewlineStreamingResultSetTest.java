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

public class NewlineStreamingResultSetTest {

    private static final String MULTILINE_LITERAL =
            "(For the film, see Sophie Scholl – The Final Days.)(See also: Hans and Sophie Scholl)\n"
                    + "Sophia Magdalena Scholl\n (9 May 1921 – 22 February 1943) was a German student and anti-Nazi political activist, active within the White Rose non-violent resistance group in Nazi Germany. She was convicted of high treason after having been found distributing anti-war leaflets at the University of Munich (LMU) with her brother Hans. As a result, they were both executed by guillotine. Since the 1970s, Scholl has been extensively commemorated for her anti-Nazi resistance work.";

    /**
     * Test a correct {@link com.weblyzard.sparql.StreamingResultSet}
     *
     * @throws IOException
     */
    @Test
    public void testStreamingResultSet() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(String.format(
                "?s\t?p\t?o\n"
                        + "<http://test.org/1>\t<https://www.w3.org/TR/rdf-schema/label>\t\"%s\"\n"
                        + "<http://test.org/2>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o2\"\n"
                        + "<http://test.org/3>\t<https://www.w3.org/TR/rdf-schema/label>\t\"o3\"",
                MULTILINE_LITERAL)));

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

}
