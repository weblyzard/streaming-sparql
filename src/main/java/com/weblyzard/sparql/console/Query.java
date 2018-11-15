package com.weblyzard.sparql.console;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import com.weblyzard.sparql.StreamingQueryExecutor;
import com.weblyzard.sparql.StreamingResultSet;

/**
 * Example console client to query a SPARQL repository using the iSPARQL library.
 *
 * <p>
 * This client can be used to perform SPARQL queries and retrieve the results in the TSV
 * (tab-separated values) format.
 *
 * <p>
 * Example usage: <code>
 * java -jar isparql.jar http://localhost:8080/rdf4j-sesame/test "SELECT ?s ?p ?o WHERE {?s ?p ?o. }"
 * </code>
 *
 * @author Albert Weichselbraun
 */
public class Query {

    /**
     * Main method.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("QueryEntitites [URL] [Query]");
            System.out.println(" URL   ... URL to the linked data repository");
            System.out.println(" Query ... The query to perform on the server");
            System.exit(-1);
        }

        String url = args[0];
        String query = args[1];
        AtomicInteger numResults = new AtomicInteger(0);

        System.out.println("Starting task....");
        try (StreamingResultSet s = StreamingQueryExecutor.getResultSet(url, query)) {
            while (s.hasNext()) {
                System.out.println(s.next());
                numResults.incrementAndGet();
            }
        } catch (IOException e) {
            System.err.println("Thread failed!!!");
            System.exit(-1);
        }
    }
}
