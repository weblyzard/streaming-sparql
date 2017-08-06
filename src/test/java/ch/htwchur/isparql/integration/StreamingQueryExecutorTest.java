package ch.htwchur.isparql.integration;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.junit.ClassRule;
import org.junit.Test;

import com.spotify.docker.client.DockerClient.ExecCreateParam;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ExecCreation;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;
import pl.domzal.junit.docker.rule.DockerRule;
import pl.domzal.junit.docker.rule.StopOption;
import pl.domzal.junit.docker.rule.WaitFor;

public class StreamingQueryExecutorTest {
    
    private static final String REPOSITORY_URL = "http://127.0.0.1:3030/test/";
    private static final String WORK_DIRECTORY = new File(".").getAbsolutePath();
    
    @ClassRule
    public static DockerRule jena = DockerRule.builder()
        .imageName("stain/jena-fuseki")
        .mountFrom(WORK_DIRECTORY + "/test-datasets").to("/staging")
        .expose("3030", "3030")        
        .waitFor(WaitFor.logMessageSequence("on port 3030"))
        .stopOptions(StopOption.KILL, StopOption.REMOVE)
        .build();
    
    @Test
    public void integrationTest() throws IOException, DockerException, InterruptedException {
        System.out.println("Working directory: " + WORK_DIRECTORY);
        // System.out.println(runDockerExecCommand(new String[]{"ls", "-latr",  "/staging"}));
        System.out.println(runDockerExecCommand(new String[]{"/bin/sh", "./load.sh", "test"}));
        StreamingResultSet s = StreamingQueryExecutor.getResultSet(REPOSITORY_URL, 
                "SELECT ?s ?p ?o WHERE { ?s ?p ?o. }");
        List<Map<String, Node>> result = new ArrayList<>();
        while (s.hasNext()) {
            result.add(s.next());
        }
        System.out.println("=====" + result);
        assertEquals(0, result.size());
    }
    
    /**
     * run a command in the docker container and return the output 
     */
    private String runDockerExecCommand(String[] args) throws DockerException, InterruptedException {
        final ExecCreation cmd = jena.getDockerClient().execCreate(jena.getContainerId(), args,
                ExecCreateParam.attachStdout(), ExecCreateParam.attachStderr());
        final String output = jena.getDockerClient().execStart(cmd.id()).readFully();
        return output;
    }
    
}
