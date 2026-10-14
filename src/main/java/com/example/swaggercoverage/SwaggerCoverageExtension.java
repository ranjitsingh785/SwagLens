package com.example.swaggercoverage;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

/**
 * Entry point. Registered when Burp loads the JAR.
 */
public final class SwaggerCoverageExtension implements BurpExtension {

    private CoverageTab tab;

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Swagger Coverage Checker");

        EndpointRegistry registry = new EndpointRegistry();

        // Passively track every request Burp sees
        api.http().registerHttpHandler(new CoverageHttpHandler(registry));

        // Register the suite tab
        tab = new CoverageTab(api, registry);
        api.userInterface().registerSuiteTab("Swagger Coverage", tab.component());

        api.logging().logToOutput(
                "Swagger Coverage Checker loaded. Open the 'Swagger Coverage' tab to begin.");

        api.extension().registerUnloadingHandler(() ->
                api.logging().logToOutput("Swagger Coverage Checker unloaded."));
    }
}
