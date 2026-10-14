package com.example.swaggercoverage;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Parses an OpenAPI 3.x or Swagger 2.0 spec (file path or URL)
 * into a set of Endpoint objects.
 */
public final class SpecParser {

    private SpecParser() {}

    public static Set<Endpoint> parse(String location) {
        SwaggerParseResult result = new OpenAPIParser()
                .readLocation(location, null, null);

        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI == null) {
            String msg = (result.getMessages() != null && !result.getMessages().isEmpty())
                    ? String.join("; ", result.getMessages())
                    : "Unknown parse error";
            throw new IllegalArgumentException("Failed to parse spec: " + msg);
        }

        Set<Endpoint> endpoints = new HashSet<>();
        openAPI.getPaths().forEach((path, item) -> {
            add(endpoints, path, item.getGet(),     "GET");
            add(endpoints, path, item.getPost(),    "POST");
            add(endpoints, path, item.getPut(),     "PUT");
            add(endpoints, path, item.getDelete(),  "DELETE");
            add(endpoints, path, item.getPatch(),   "PATCH");
            add(endpoints, path, item.getOptions(), "OPTIONS");
            add(endpoints, path, item.getHead(),    "HEAD");
            add(endpoints, path, item.getTrace(),   "TRACE");
        });
        return endpoints;
    }

    private static void add(Set<Endpoint> set, String path, Operation op, String method) {
        if (op != null) set.add(new Endpoint(method, path));
    }
}
