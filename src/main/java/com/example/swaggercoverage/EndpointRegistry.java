package com.example.swaggercoverage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of documented vs observed endpoints.
 * Montoya's HTTP handler runs on a background thread, so all mutation
 * goes through concurrent sets.
 */
public final class EndpointRegistry {

    private final Set<Endpoint> documented = ConcurrentHashMap.newKeySet();
    private final Set<Endpoint> observed   = ConcurrentHashMap.newKeySet();

    public void setDocumented(Set<Endpoint> endpoints) {
        documented.clear();
        documented.addAll(endpoints);
    }

    public Set<Endpoint> documented() {
        return Collections.unmodifiableSet(documented);
    }

    /**
     * Record an observed request. Matches against documented endpoints
     * and, on first match, marks the endpoint as covered.
     */
    public void recordObserved(String method, String path) {
        if (documented.isEmpty()) return;
        for (Endpoint e : documented) {
            if (e.matches(method, path)) {
                observed.add(e);
                return;
            }
        }
    }

    public Set<Endpoint> covered() {
        Set<Endpoint> c = new HashSet<>(observed);
        c.retainAll(documented);
        return c;
    }

    public Set<Endpoint> missing() {
        Set<Endpoint> m = new HashSet<>(documented);
        m.removeAll(observed);
        return m;
    }

    public double coveragePercent() {
        if (documented.isEmpty()) return 0.0;
        return 100.0 * covered().size() / documented.size();
    }

    public void reset() {
        observed.clear();
    }
}
