## camel-jsonld

An Apache Camel Jetty implementation that accepts an n-triples POST and returns a framed JSON-LD serialization

## Configuration
 * `application.properties` 

## Building
This requires JDK9 or higher.
To build run
```bash
./gradlew clean build
```

## Endpoint
The test query endpoint is exposed at `http://localhost:9096/toJsonLd`

## Example POST
This example posts an n-triples graph (see test resources)

```bash
$ curl -X POST -H "Content-Type: application/n-triples" --data-binary "@n3-test.n3" http://localhost:9096/toJsonLd?type=meta
```

## Dependencies
None

## Use Case
To decouple the JsonLd marshalling into a different endpoint from the Fuseki service.