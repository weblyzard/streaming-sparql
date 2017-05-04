package ch.htwchur.isparql;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.sparql.util.NodeFactoryExtra;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

/**
 * A streaming result set obtained from a SPARQL server.
 * 
 * @author albert.weichselbraun@htwchur.ch
 *
 */
public class StreamingResultSet  {
	private final static char TAB = '\t';
	private final static Splitter TAB_SPLITTER = Splitter.on(TAB);
	private final static Logger log = Logger.getLogger(StreamingResultSet.class.getName());
	private BufferedReader in;
	
	private String[] resultVars;
	private String[] currentTuple;
	private boolean hasNext = true;
	private int rowNumber;
	
	public StreamingResultSet(BufferedReader in) {
		this.in = in;
		// read TSV header and remove the staring "?"
		resultVars = retrieveHeader().stream()
				.map(str -> str.substring(1)).toArray(String[]::new);
		currentTuple = new String[resultVars.length];
		
		// read first result
		retrieveNextTuple();
	}
	
	/**
	 * Retrieves the next tuple from the input stream
	 */
	private List<String> retrieveHeader() {
		String line = readNextLine();
		return (line == null) ? null : TAB_SPLITTER.splitToList(line);
	}


	/**
	 * Updates currentTuple with the current tuple
	 * 
	 * @return
	 * 	  whether the next tuple has been retrieved
	 */
	private void retrieveNextTuple() {
		String line = readNextLine();
		if (line == null) {
			hasNext = false;
			return;
		};

		int idx = 0, oldidx = 0;
		int pos = 0;
		while (true) {
			idx = line.indexOf(TAB, oldidx);
			if (idx == -1) {
				// read the last value
				currentTuple[pos++] = line.substring(oldidx);
				break;
			}
			currentTuple[pos++] = line.substring(oldidx, idx);
			oldidx = idx+1;
		};
	}
		
	/**
	 * @return
	 * 		the next line from the input stream or null in case of errors.
	 */
	private String readNextLine() {
		rowNumber ++;
		try {
			return in.readLine();
		} catch (IOException e) {
			return null;
		}
	}

	public boolean hasNext() {
		if (!hasNext) {
			try {
				in.close();
			} catch (IOException e) {
				log.warning("Closing streaming connections caused an exception: " + e.toString());
			}
		}
		return hasNext;
	}

	public Map<String, Node> next() {
		if (hasNext == false) {
			throw new NoSuchElementException();
		}
		
		Map<String, Node> result = Maps.newHashMapWithExpectedSize(currentTuple.length);
		String value;
		for (int i=0; i<currentTuple.length; i++) {
			// do not create bindings for empty tuples
			value = currentTuple[i];
			try {
				if (value.length() > 0)
					result.put(resultVars[i], NodeFactoryExtra.parseNode(currentTuple[i]));
			} catch (RiotParseException e) {
				log.severe(String.format("Parsing of value '%s' contained in tuple '%s' failed: %s",
						value, Arrays.deepToString(currentTuple), e.getMessage()));
			}
		}
		retrieveNextTuple();
		return result;
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public String[] getResultVars() {
		return resultVars;
	}

}
