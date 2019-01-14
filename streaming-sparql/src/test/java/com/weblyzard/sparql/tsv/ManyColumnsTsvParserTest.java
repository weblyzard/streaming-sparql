package com.weblyzard.sparql.tsv;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.junit.Test;

import com.weblyzard.sparql.StreamingResultSet;

/**
 * Tests TSV parsing for different TSV column configurations.
 * 
 * @author Albert Weichselbraun
 *
 */
public class ManyColumnsTsvParserTest {

    private static final String LITERAL_TEMPLATE = "\"\"%d\"\" and \"\"%d\"\"";
    private static final String RESOURCE_TEMPLATE = "<http://www.htwchur.ch/%d#>";
    
    @Test
    public void advancedTsvParserTest() throws IOException {
    	for (String tsvPattern: Arrays.asList("LRLL", "RRRR", "LLLL", "LLRRLL", "RRLLRR", "LRLRLRLR", "RRRLLRLRL", "RRLLLLLRR", "LLRRRRRL")) {
    		TsvTestTuple t = getTestTsv(tsvPattern);
    		
    		Map<String, Node> parseResult = parseSingleTuple(t.getTsv());
    		assertEquals(t.getExpectedResult(), parseResult);
    	}
    }

    /**
     * Creates a test TSV based on the given tsvPattern.
     * <p>
     * 
     * <b>Example</b>
     * <code>RLRLL</code> ... creates a TSV with a resource, literal, resource, literal, literal 
     */
    private static TsvTestTuple getTestTsv(String tsvPattern) {
    	int counter = 0;

    	List<String> header = new ArrayList<>();
    	List<String> row = new ArrayList<>();
    	Map<String, Node> expectedResult = new HashMap<>();
    	
    	String s;
    	for (char ch: tsvPattern.toCharArray()) {
    		switch(ch) {
    		case 'R': 
    			header.add("?res" + counter);
    			s = String.format(RESOURCE_TEMPLATE, counter);
    			row.add(s);
    			expectedResult.put("res" + counter, NodeFactoryExtra.parseNode(s));
    			break;
    		case 'L':
    			header.add("?lit" + counter);
    			s = "\"" + String.format(LITERAL_TEMPLATE, counter, counter).replace("\"\"",  "\\\"") + "\"";
    			row.add(s);
    			expectedResult.put("lit" + counter, NodeFactoryExtra.parseNode(s));
    			break;
    		}
    		counter ++;
    	}
    	String tsv = String.join("\t", header) + "\n" + String.join("\t", row);
    	return new TsvTestTuple(tsv, expectedResult);
    }
    
    /**
     * Performs a query and returns the first result tuple.
     */
    private static Map<String, Node> parseSingleTuple(String query) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(query));

        try (StreamingResultSet resultSet = new StreamingResultSet(bufferedReader)) {
            return resultSet.next();
        }
    }
}
