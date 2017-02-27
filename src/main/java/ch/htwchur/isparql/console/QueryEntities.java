package ch.htwchur.isparql.console;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;

public class QueryEntities {

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("Call with URL and query.");
			System.exit(-1);
		}
		
		String jsonString = Files.toString(new File(args[1]), Charsets.UTF_8);
		@SuppressWarnings("unchecked")
		List<String> entities = new Gson().fromJson(jsonString, List.class);
		String query = createRelationQuery(entities);
		System.out.println(query);
		
		StreamingResultSet s = StreamingQueryExecutor.getResultSet(args[0], query);
		while (s.hasNext()) {
			System.out.println(s.next());
		}
	}

	public final static String createRelationQuery(List<String> entityList) {
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
