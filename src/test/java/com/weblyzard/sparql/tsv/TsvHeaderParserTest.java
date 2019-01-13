package com.weblyzard.sparql.tsv;

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import org.junit.Test;

import com.weblyzard.sparql.StreamingResultSet;

/**
 * TSV header parsing tests for Fuseki, RDF4j and Virtuoso.
 * 
 * @author Albert Weichselbraun
 *
 */
public class TsvHeaderParserTest {

    /**
     * Tests the correct parsing of TSV headers used by Fuseki and RDF4j
     *
     * @throws IOException
     */
    @Test
    public void testFusekiHeader() throws IOException {
        String[] header=
                getResultHeader("?s\t?p\t?o");
        System.out.println(Arrays.toString(header));
        assertArrayEquals(new String[]{"s", "p", "o"}, header);
    }

    /**
     * Tests the correct parsing of TSV headers used by Virtuoso and rdf4j
     *
     * @throws IOException
     */
    @Test
    public void testVirtuosoHeader() throws IOException {
        String[] header=
                getResultHeader("\"s\"\t\"p\"\t\"o\"");
        System.out.println(Arrays.toString(header));
        assertArrayEquals(new String[]{"s", "p", "o"}, header);
    }
    
    /**
     * Performs a query and returns the first result tuple.
     */
    private static String[] getResultHeader(String query) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(query));

        try (StreamingResultSet resultSet = new StreamingResultSet(bufferedReader)) {
            return resultSet.getResultVars();
        }
    }
}
