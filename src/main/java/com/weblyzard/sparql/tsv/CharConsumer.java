package com.weblyzard.sparql.tsv;

/**
 * Represents a state of the TsvParser which translates characters from the input stream to tuples
 * of an RDF response.
 * 
 * @author Albert Weichselbraun
 *
 */
public interface CharConsumer {
    public void consumeChars(TsvParser p);
}
