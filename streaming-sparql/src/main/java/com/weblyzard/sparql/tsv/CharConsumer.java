package com.weblyzard.sparql.tsv;

/**
 * Represents a state of the TsvParser which translates characters from the input stream to tuples
 * of an RDF response.
 * 
 * @author Albert Weichselbraun
 *
 */
public interface CharConsumer {
    /**
     * Consumes the next characters of the input stream.
     * 
     * @return whether the current tuple has been completed.
     */
    public boolean consumeChars(TsvParser p);
}
