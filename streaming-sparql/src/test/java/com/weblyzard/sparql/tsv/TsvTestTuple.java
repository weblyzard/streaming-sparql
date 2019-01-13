package com.weblyzard.sparql.tsv;

import java.util.Map;

import org.apache.jena.graph.Node;

import lombok.Value;

@Value
public class TsvTestTuple {
	String tsv;
	Map<String, Node> expectedResult;
}
