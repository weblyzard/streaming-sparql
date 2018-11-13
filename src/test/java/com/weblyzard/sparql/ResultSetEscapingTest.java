package com.weblyzard.sparql;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.junit.Test;

public class ResultSetEscapingTest {

    @Test
    public void testQuoteHandling() {
        String literal = "\"\"Crocodile\"_Dundee\"";
        Node parseNode = NodeFactoryExtra.parseNode(literal);
        System.out.println(parseNode.getLiteralValue());
    }

}
