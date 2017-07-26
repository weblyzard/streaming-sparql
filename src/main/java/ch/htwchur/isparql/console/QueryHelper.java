package ch.htwchur.isparql.console;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;

/**
 * Transforms a list of resources into a corresponding SPARQL query.
 * 
 * @author albert.weichselbraun@htwchur.ch
 *
 */
public class QueryHelper {
	
	private QueryHelper() {
	}

	public static StreamingResultSet prepareResultSet(String url, List<String> entityList) {
		StreamingResultSet s;
		try {
			s = StreamingQueryExecutor.getResultSet(url, createRelationQuery(entityList));
		} catch (IOException e) {
			StreamingQueryExecutor.log.severe("Failed to prepare result set: " + e.getMessage());
			return null;
		}
		return s;
	}

	/**
	 * Creates a query that obtains all relations relevant to the given list of
	 * entities
	 * 
	 * @param entityList
	 *            a list of URLs to query for
	 * @return the corresponding SPARQL query.
	 */
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
