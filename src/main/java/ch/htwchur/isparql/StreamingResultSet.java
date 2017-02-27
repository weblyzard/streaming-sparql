package ch.htwchur.isparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.util.NodeFactoryExtra;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

/**
 * A streaming result set obtained from a SPARQL server.
 * 
 * @author albert.weichselbraun@htwchur.ch
 *
 */
public class StreamingResultSet {
	private final static Splitter TAB_SPLITTER = Splitter.on("\t");
	private final static Logger log = Logger.getLogger(StreamingResultSet.class.getName());
	private BufferedReader in;
	
	private List<String> resultVars;
	private List<String> nextTupel;
	private int rowNumber;
	
	public StreamingResultSet(BufferedReader in) {
		this.in = in;
		// read TSV header and remove the staring "?"
		resultVars = retrieveNextTupel().stream()
				.map(str -> str.substring(1)).collect(Collectors.toList());
		
		// read first result
		nextTupel = retrieveNextTupel();
	}
	
	/**
	 * Retrieves the next tupel from the input stream
	 */
	private List<String> retrieveNextTupel() {
		String line;
		rowNumber ++;
		try {
			line = in.readLine();
			return (line == null) ? null : TAB_SPLITTER.splitToList(line);
		} catch (IOException e) {
			return null;
		}
	}

	public boolean hasNext() {
		return nextTupel != null;
	}

	public Map<String, Node> next() {
		if (nextTupel == null) {
			try {
				in.close();
			} catch (IOException e) {
				log.warning("Closing streaming connections caused an exception: " + e.toString());
			}
			throw new NoSuchElementException();
		}
		Map<String, Node> result = Maps.newHashMapWithExpectedSize(nextTupel.size());
		for (int i=0; i<nextTupel.size(); i++) {
			result.put(resultVars.get(i), NodeFactoryExtra.parseNode(nextTupel.get(i)));
		}
		nextTupel = retrieveNextTupel();
		return result;
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public List<String> getResultVars() {
		return resultVars;
	}

}
