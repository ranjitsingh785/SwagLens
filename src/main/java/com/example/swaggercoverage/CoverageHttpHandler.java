package com.example.swaggercoverage;

import burp.api.montoya.http.handler.*;

/**
 * Passive observer: every request through Proxy, Repeater, Scanner,
 * or Intruder is matched against the loaded spec.
 */
public final class CoverageHttpHandler implements HttpHandler {

    private final EndpointRegistry registry;

    public CoverageHttpHandler(EndpointRegistry registry) {
        this.registry = registry;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        try {
            String path = stripQuery(request.path());
            registry.recordObserved(request.method(), path);
        } catch (Throwable ignored) {
            // Never break Burp's traffic flow
        }
        return RequestToBeSentAction.continueWith(request);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        return ResponseReceivedAction.continueWith(response);
    }

    private static String stripQuery(String path) {
        int q = path.indexOf('?');
        return (q >= 0) ? path.substring(0, q) : path;
    }
}
