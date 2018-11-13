package com.weblyzard.sparql.integration;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.junit.Test;
import com.weblyzard.sparql.StreamingQueryExecutor;
import com.weblyzard.sparql.StreamingResultSet;

public class AbstractsIT {

    @Test
    public void testAbstractsWithDbpedia() throws IOException {

        String endpoint = "http://dbpedia.org/sparql";
        String query =
                "select ?abstract where { <http://dbpedia.org/resource/Sophie_Scholl> dbo:abstract ?abstract. FILTER (lang(?abstract) = 'en')}";

        // query for the data:
        try (StreamingResultSet streamingResultSet =
                StreamingQueryExecutor.getResultSet(endpoint, query)) {
            System.out.println("Received the result from the service");

            // process the data:
            while (streamingResultSet.hasNext()) {
                Map<String, Node> result = streamingResultSet.next();
                System.out.println(String.format("Resultset contains %s variable names",
                        result.keySet().size()));

                Iterator<String> varNameIter = result.keySet().iterator();
                while (varNameIter.hasNext()) {
                    String varName = varNameIter.next();
                    System.out.println(String.format("Now processing variable name '%s'", varName));

                    String resultVarname = varName;
                    String resultStringValue = result.get(varName).toString();

                    System.out.println(String.format("Received value '%s' for variable name '%s'",
                            resultStringValue, resultVarname));
                }
            }
        }

    }
}
