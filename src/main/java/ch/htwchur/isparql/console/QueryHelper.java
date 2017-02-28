package ch.htwchur.isparql.console;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;

public class QueryHelper {
	
	public static StreamingResultSet prepareResultSet(String url, List<String> entityList)  {
		StreamingResultSet s;
		try {
			s = StreamingQueryExecutor.getResultSet(url, createRelationQuery(entityList));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return s;
	}

	public static String createRelationQuery(List<String> entityList) {
		String entity1 = " ?s";
		String entity2 = " ?o";
		String relation = " ?p";
	
		StringBuilder q = new StringBuilder();
		q.append("SELECT DISTINCT" + entity1 + entity2 + relation);
		q.append(" WHERE {");
		q.append(" FILTER (" + entity1 + " !=" + entity2 + ")");
		q.append(" FILTER (!isLiteral(" + entity2 + "))");
		
		q.append(" {{ ");
		q.append(entity1 + relation + entity2 + ".");
		q.append(" } UNION {");
		q.append(entity2 + relation + entity1 + ".");
		q.append(" }.}");
		q.append(" VALUES " + entity1 + " {");
		q.append(entityList.stream().map(e -> "<" + e + ">").collect(Collectors.joining(" ")));
		q.append("}}");
		return q.toString();
	}

}
