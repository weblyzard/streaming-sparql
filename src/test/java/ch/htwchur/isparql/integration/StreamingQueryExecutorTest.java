package ch.htwchur.isparql.integration;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.junit.ClassRule;
import org.junit.Test;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;
import pl.domzal.junit.docker.rule.DockerRule;
import pl.domzal.junit.docker.rule.WaitFor;

public class StreamingQueryExecutorTest {
    
    private static final String REPOSITORY_URL = "http://127.0.0.1:3030";
    
    @ClassRule
    public static DockerRule jena = DockerRule.builder()
        .imageName("stain/jena-fuseki")
        .expose("3030:3030")
        .waitFor(WaitFor.logMessageSequence("Initializing Shiro environment", "started", "on port 3030"))
        .build();
    
    @Test
    public void test() throws IOException {
        StreamingResultSet s = StreamingQueryExecutor.getResultSet(REPOSITORY_URL, 
                "SELECT ?s ?p ?o WHERE { ?s ?p ?o. }");
        List<Map<String, Node>> result = new ArrayList<>();
        while (s.hasNext()) {
            result.add(s.next());
        }
        assertEquals(0, result.size());
    }

}
