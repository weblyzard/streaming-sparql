package ch.htwchur.isparql.integration;

import static org.junit.Assert.*;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;
import com.google.common.collect.Lists;
import com.spotify.docker.client.exceptions.DockerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import pl.domzal.junit.docker.rule.DockerRule;
import pl.domzal.junit.docker.rule.StopOption;

public class StreamingQueryExecutorIT {

    private static final String REPOSITORY_URL = "http://127.0.0.1:3030/test/";
    private static final String REPOSITORY_URL_MISSING_DATASET = "http://127.0.0.1:3030/default/";
    private static final String REPOSITORY_URL_INVALID = "https://weichselbraun.net";
    private static final String TEST_DATA = "cafes.ttl.gz";
    private static final String FUSEKI_REPOSITORY_CONFIG =
            new File(".").getAbsolutePath() + File.separator + "integration-test/test.ttl";

    @ClassRule
    public static DockerRule jena =
            DockerRule.builder()
                    .imageName("stain/jena-fuseki")
                    .mountFrom(FUSEKI_REPOSITORY_CONFIG)
                    .to("/fuseki/configuration/test.ttl")
                    .expose("3030", "3030")
                    .stopOptions(StopOption.KILL, StopOption.REMOVE)
                    .build();

    @BeforeClass
    public static void setUp() throws IOException, TimeoutException, InterruptedException {
        jena.waitForLogMessage("on port 3030", 1000000);
        // read model data
        System.out.println("Uploading data!!!");
        Model m = ModelFactory.createDefaultModel();
        String base = "http://test.org";
        InputStream in =
                new GZIPInputStream(
                        StreamingQueryExecutorIT.class
                                .getClassLoader()
                                .getResourceAsStream(TEST_DATA));
        m.read(in, base, "TTL");

        // send model data to the sever
        System.out.println("Sending data to server...");
        final DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(REPOSITORY_URL);
        accessor.putModel(m);
        System.out.println("Completed uploading data...");
    }

    @Test
    public void queryRepositoryTest() throws IOException, DockerException, InterruptedException {
        StreamingResultSet s =
                StreamingQueryExecutor.getResultSet(
                        REPOSITORY_URL, "SELECT ?s ?p ?o WHERE { ?s ?p ?o. }");
        List<Map<String, Node>> result = Lists.newArrayList(s);
        assertEquals(41, result.size());
    }

    @Test(expected = FileNotFoundException.class)
    public void missingRepositoryIT() throws IOException {
        @SuppressWarnings("unused")
        StreamingResultSet s =
                StreamingQueryExecutor.getResultSet(
                        REPOSITORY_URL_MISSING_DATASET, "SELECT ?s ?p ?o WHERE { ?s ?p ?o. }");
    }

    @Test(expected = IOException.class)
    public void invalidRepositoryTest() throws IOException {
        StreamingResultSet s =
                StreamingQueryExecutor.getResultSet(
                        REPOSITORY_URL_INVALID, "SELECT ?s ?p ?o WHERE { ?s ?p ?o. }");
        List<Map<String, Node>> result = Lists.newArrayList(s);
        assertEquals(0, result.size());
    }
}
