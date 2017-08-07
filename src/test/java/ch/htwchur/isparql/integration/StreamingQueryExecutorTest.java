package ch.htwchur.isparql.integration;

import static org.junit.Assert.*;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;
import com.spotify.docker.client.DockerClient.ExecCreateParam;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ExecCreation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

public class StreamingQueryExecutorTest {

    private static final String REPOSITORY_URL = "http://127.0.0.1:3030/default/";
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
        InputStream in = new GZIPInputStream(
        		StreamingQueryExecutorTest.class.getClassLoader().getResourceAsStream(TEST_DATA));
        m.read(in, base, "TTL");

        // send model data to the sever
        System.out.println("Sending data to server...");
        final DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(REPOSITORY_URL);
        accessor.putModel(m);
        System.out.println("Completed uploading data...");
    }

    @Test
    public void integrationTest() throws IOException, DockerException, InterruptedException {
        // System.out.println(runDockerExecCommand(new String[]{"/bin/sh", "./load.sh", "test"}));
        System.out.println("***Integration test***");
        StreamingResultSet s =
                StreamingQueryExecutor.getResultSet(
                        REPOSITORY_URL, "SELECT ?s ?p ?o WHERE { ?s ?p ?o. }");
        List<Map<String, Node>> result = new ArrayList<>();
        while (s.hasNext()) {
            result.add(s.next());
        }
        System.out.println("=====" + result);
        assertEquals(0, result.size());
    }

    /** run a command in the docker container and return the output */
    private String runDockerExecCommand(String[] args)
            throws DockerException, InterruptedException {
        final ExecCreation cmd =
                jena.getDockerClient()
                        .execCreate(
                                jena.getContainerId(),
                                args,
                                ExecCreateParam.attachStdout(),
                                ExecCreateParam.attachStderr());
        final String output = jena.getDockerClient().execStart(cmd.id()).readFully();
        return output;
    }
}
