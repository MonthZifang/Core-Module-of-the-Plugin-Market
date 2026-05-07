package com.mdt.original.market.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MarketCatalog {
    private final String marketVersion;
    private final String pluginMetadataFile;
    private final Map<String, RegistryEntry> entries;

    public MarketCatalog(String marketVersion, String pluginMetadataFile, Map<String, RegistryEntry> entries) {
        this.marketVersion = marketVersion;
        this.pluginMetadataFile = pluginMetadataFile;
        this.entries = new LinkedHashMap<String, RegistryEntry>(entries);
    }

    public String marketVersion() {
        return marketVersion;
    }

    public String pluginMetadataFile() {
        return pluginMetadataFile;
    }

    public Map<String, RegistryEntry> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public RegistryEntry get(String name) {
        return entries.get(name);
    }
}
