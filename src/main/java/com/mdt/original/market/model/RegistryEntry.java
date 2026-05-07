package com.mdt.original.market.model;

import arc.util.serialization.Jval;
import java.util.ArrayList;
import java.util.List;

public final class RegistryEntry {
    private final String name;
    private final String displayName;
    private final String author;
    private final String channel;
    private final List<String> targets;
    private final String gitRepository;
    private final String gitBranch;
    private final String pluginMetadataFile;

    private RegistryEntry(
        String name,
        String displayName,
        String author,
        String channel,
        List<String> targets,
        String gitRepository,
        String gitBranch,
        String pluginMetadataFile
    ) {
        this.name = name;
        this.displayName = displayName;
        this.author = author;
        this.channel = channel;
        this.targets = targets;
        this.gitRepository = gitRepository;
        this.gitBranch = gitBranch;
        this.pluginMetadataFile = pluginMetadataFile;
    }

    public static RegistryEntry fromJval(Jval root) {
        return new RegistryEntry(
            root.getString("name", ""),
            root.getString("displayName", root.getString("name", "")),
            root.getString("author", ""),
            root.getString("channel", ""),
            readList(root, "targets"),
            root.getString("gitRepository", ""),
            root.getString("gitBranch", "main"),
            root.getString("pluginMetadataFile", "market.plugin.json")
        );
    }

    private static List<String> readList(Jval root, String key) {
        List<String> values = new ArrayList<String>();
        if (!root.has(key) || root.get(key) == null || root.get(key).isNull()) {
            return values;
        }

        Jval value = root.get(key);
        if (value.isArray()) {
            for (Jval item : value.asArray()) {
                values.add(item.asString());
            }
        } else {
            values.add(value.asString());
        }
        return values;
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return displayName;
    }

    public String author() {
        return author;
    }

    public String channel() {
        return channel;
    }

    public List<String> targets() {
        return targets;
    }

    public String gitRepository() {
        return gitRepository;
    }

    public String gitBranch() {
        return gitBranch;
    }

    public String pluginMetadataFile() {
        return pluginMetadataFile;
    }
}
