package ch.htwchur.isparql.console;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.ext.com.google.common.collect.Lists;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;

import ch.htwchur.isparql.StreamingQueryExecutor;
import ch.htwchur.isparql.StreamingResultSet;

public class QueryEntities {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		if (args.length < 4) {
			System.out.println("QueryEntitites [URL] [Entities] [Chunksize] [Threads].");
			System.exit(-1);
		}
		
		String url = args[0];
		String entityFile = args[1];
		int chunkSize = Integer.parseInt(args[2]);
		int count = Integer.parseInt(args[3]);
		
		String jsonString = Files.toString(new File(entityFile), Charsets.UTF_8);
		@SuppressWarnings("unchecked")
		List<String> entities = new Gson().fromJson(jsonString, List.class);
		entities = entities.subList(0, chunkSize);
		String query = QueryHelper.createRelationQuery(entities);
		System.out.println(query);
		
		ForkJoinPool forkJoinPool = new ForkJoinPool(count);
		
		ConcurrentLinkedQueue<List<String>> workQueue = new ConcurrentLinkedQueue<>(); 
		Lists.partition(entities, chunkSize).forEach(l -> workQueue.add(l));

		AtomicInteger numResults = new AtomicInteger();
		forkJoinPool.submit(() -> 
			workQueue.stream().parallel().forEach(
					ee -> {
						System.out.println("Starting task....");
						StreamingResultSet s;
						try {
							s = StreamingQueryExecutor.getResultSet(url, QueryHelper.createRelationQuery(ee));
							while (s.hasNext()) {
								s.next();
								numResults.incrementAndGet();
							}
						} catch (IOException e) {
							System.err.println("Thread failed!!!");
							System.exit(-1);
						}
					})).get();
		
		// join all tasks and get the total number of lines retrieved
		System.out.println("Retrieved a total of " + numResults + " ....results.");
	}

}
