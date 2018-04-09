/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ubl.camel.jsonld;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_CHARACTER_ENCODING;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.ubl.camel.jsonld.FromRdf.toJsonLd;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonLdSerializeRouteTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLdSerializeRouteTest.class);

    private static final String contentTypeJsonLd = "application/ld+json";
    private static final String HTTP_ACCEPT = "Accept";
    private static final String SPARQL_QUERY = "type";

    private JsonLdSerializeRouteTest() {
    }

    public static void main(final String[] args) throws Exception {
        LOGGER.info("About to run JsonLd Serializer API...");

        final CamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new RouteBuilder() {
            public void configure() {
                final PropertiesComponent pc = getContext().getComponent("properties", PropertiesComponent.class);
                pc.setLocation("classpath:application.properties");

                from("jetty:http://{{rest.host}}:{{rest.port}}{{rest.prefix}}?"
                        + "optionsEnabled=true&matchOnUriPrefix=true&sendServerVersion=false"
                        + "&httpMethodRestrict=POST,OPTIONS")
                        .routeId("fromRDF")
                        .removeHeaders(HTTP_ACCEPT)
                        .setHeader("Access-Control-Allow-Origin")
                        .constant("*")
                        .choice()
                        .when(header(HTTP_METHOD).isEqualTo("POST"))
                        .to("direct:routeType");
                from("direct:routeType")
                        .routeId("routeType")
                        .log(LoggingLevel.INFO, LOGGER, "Serializing n-triples as Json-Ld")
                        .convertBodyTo(String.class)
                        .choice()
                        .when(header(SPARQL_QUERY).isEqualTo("meta"))
                            .to("direct:serializeMeta")
                        .when(header(SPARQL_QUERY).isEqualTo("collection"))
                            .to("direct:serializeCollection");
                from("direct:serializeMeta")
                        .process(e -> {
                            try {
                                final String contextUri = "context.json";
                                final String frameUri = "anno-frame.json";
                                e.getIn()
                                 .setBody(toJsonLd(e.getIn()
                                                    .getBody()
                                                    .toString(), contextUri, frameUri));
                            } catch (final Exception ex) {
                                throw new RuntimeCamelException("Couldn't serialize to JsonLd", ex);
                            }
                        })
                        .removeHeader(HTTP_ACCEPT)
                        .setHeader(HTTP_CHARACTER_ENCODING)
                        .constant("UTF-8")
                        .setHeader(CONTENT_TYPE)
                        .constant(contentTypeJsonLd);
                from("direct:serializeCollection")
                        .process(e -> {
                            try {
                                final String contextUri = "context.json";
                                final String frameUri = "collection-frame.json";
                                e.getIn()
                                 .setBody(toJsonLd(e.getIn()
                                                    .getBody()
                                                    .toString(), contextUri, frameUri));
                            } catch (final Exception ex) {
                                throw new RuntimeCamelException("Couldn't serialize to JsonLd", ex);
                            }
                        })
                        .removeHeader(HTTP_ACCEPT)
                        .setHeader(HTTP_CHARACTER_ENCODING)
                        .constant("UTF-8")
                        .setHeader(CONTENT_TYPE)
                        .constant(contentTypeJsonLd);
            }
        });

        camelContext.start();

        // let it run for 5 minutes before shutting down
        Thread.sleep(5 * 60 * 1000);

        camelContext.stop();
    }
}