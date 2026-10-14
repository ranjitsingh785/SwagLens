package com.example.swaggercoverage;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;

import java.util.List;

/**
 * Retroactively credits traffic already in Burp's site map.
 * Useful when the extension is loaded after crawling has begun.
 */
public final class SiteMapAnalyzer {

    private SiteMapAnalyzer() {}

    public static int analyze(MontoyaApi api, EndpointRegistry registry) {
        List<HttpRequestResponse> entries = api.siteMap().requestResponses();
        int count = 0;
        for (HttpRequestResponse rr : entries) {
            try {
                String path = rr.request().path();
                int q = path.indexOf('?');
                if (q >= 0) path = path.substring(0, q);
                registry.recordObserved(rr.request().method(), path);
                count++;
            } catch (Throwable ignored) {}
        }
        return count;
    }
}
