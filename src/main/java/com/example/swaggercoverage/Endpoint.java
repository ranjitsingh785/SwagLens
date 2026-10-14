package com.example.swaggercoverage;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A single documented API endpoint: HTTP method + path template.
 * Example: GET /pets/{petId}
 */
public final class Endpoint {

    private final String method;
    private final String pathTemplate;
    private final Pattern pathRegex;

    public Endpoint(String method, String pathTemplate) {
        this.method = method.toUpperCase();
        this.pathTemplate = pathTemplate;
        this.pathRegex = compileTemplate(pathTemplate);
    }

    public String method()       { return method; }
    public String pathTemplate() { return pathTemplate; }

    /** Match a concrete path (no query string) against this template. */
    public boolean matches(String httpMethod, String concretePath) {
        return method.equalsIgnoreCase(httpMethod)
            && pathRegex.matcher(concretePath).matches();
    }

    private static Pattern compileTemplate(String template) {
        StringBuilder sb = new StringBuilder("^");
        String[] parts = template.split("(?=\\{)|(?<=\\})");
        for (String part : parts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                sb.append("[^/]+");
            } else {
                sb.append(Pattern.quote(part));
            }
        }
        sb.append("/?$");
        return Pattern.compile(sb.toString());
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Endpoint e)) return false;
        return method.equals(e.method) && pathTemplate.equals(e.pathTemplate);
    }

    @Override public int hashCode() { return Objects.hash(method, pathTemplate); }

    @Override public String toString() { return method + " " + pathTemplate; }
}
