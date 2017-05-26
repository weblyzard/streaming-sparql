## iSparql

Provides a robust, incremental processing of streaming results received from SPARQL servers. 
The `StreamingResultSet` iterator yields results as they are received from the server.

## Example code:
```java

s = StreamingQueryExecutor.getResultSet("http://dbpedia.org/sparql", "SELECT ?s ?p ?o WHERE { ?s ?p ?o. } LIMIT 5")
while (s.hasNext()) {
    System.out.println("Tupel " + s.getRowNumber() + ": " + s.next())
}
```

## Background

We have been using Fuseki and RDF4j together with comprehensive result sets (> 100 Mio. tuple) which lead to 
instabilities with the native libraries that have been extremely difficult to debug.

Example error messages on the server site have been:

```
[2017-05-04 19:50:14] Fuseki     WARN  [1450] Runtime IO Exception (client left?) RC = 500 : org.eclipse.jetty.io.EofException      
org.apache.jena.atlas.RuntimeIOException: org.eclipse.jetty.io.EofException                                                         
``` 

```
[2017-05-04 19:50:14] Fuseki    WARN  (HttpChannel.java:468) (and one from ServletHandler.java:631):
java.io.IOException: java.util.concurrent.TimeoutException: Idle timeout expired: 30001/30000 m
```

These problems triggered the development of iSparql which has proven to be very robust - even for queries that take more than one hour to process and transfer multiple gigabytes of results.
(Note: you will need to call `getResultSet` with a higher timeout to prevent TimeoutExceptions on the server).

## Compatiblity

iSPARQL is known to work with Jena, OpenRDF and RDF4j.

