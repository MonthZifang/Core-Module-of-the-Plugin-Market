package com.mdt.original.market.model;

import arc.util.serialization.Jval;
import java.util.ArrayList;
import java.util.List;

public final class PluginMetadata {
    private final String name;
    private final String displayName;
    private final String displayNameZh;
    private final String author;
    private final String description;
    private final String descriptionZh;
    private final String version;
    private final String requiredMarketVersion;
    private final String channel;
    private final List<String> targets;
    private final List<String> downloadUrls;
    private final List<String> dependencies;
    private final String repositoryUrl;
    private final String entry;

    private PluginMetadata(
        String name,
        String displayName,
        String displayNameZh,
        String author,
        String description,
        String descriptionZh,
        String version,
        String requiredMarketVersion,
        String channel,
        List<String> targets,
        List<String> downloadUrls,
        List<String> dependencies,
        String repositoryUrl,
        String entry
    ) {
        this.name = name;
        this.displayName = displayName;
        this.displayNameZh = displayNameZh;
        this.author = author;
        this.description = description;
        this.descriptionZh = descriptionZh;
        this.version = version;
        this.requiredMarketVersion = requiredMarketVersion;
        this.channel = channel;
        this.targets = targets;
        this.downloadUrls = downloadUrls;
        this.dependencies = dependencies;
        this.repositoryUrl = repositoryUrl;
        this.entry = entry;
    }

    public static PluginMetadata fromJval(Jval root) {
        return new PluginMetadata(
            root.getString("name", ""),
            root.getString("displayName", root.getString("name", "")),
            root.getString("displayNameZh", ""),
            root.getString("author", ""),
            root.getString("description", ""),
            root.getString("descriptionZh", ""),
            root.getString("version", ""),
            root.getString("requiredMarketVersion", root.getString("version", "")),
            root.getString("channel", ""),
            readList(root, "targets"),
            readList(root, "downloadUrls"),
            readList(root, "dependencies"),
            root.getString("repositoryUrl", ""),
            root.getString("entry", "")
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

    public String description() {
        return description;
    }

    public String displayNameZh() {
        return displayNameZh;
    }

    public String descriptionZh() {
        return descriptionZh;
    }

    public String version() {
        return version;
    }

    public String requiredMarketVersion() {
        return requiredMarketVersion;
    }

    public String channel() {
        return channel;
    }

    public List<String> targets() {
        return targets;
    }

    public List<String> downloadUrls() {
        return downloadUrls;
    }

    public List<String> dependencies() {
        return dependencies;
    }

    public String repositoryUrl() {
        return repositoryUrl;
    }

    public String entry() {
        return entry;
    }

    public String preferredDisplayName() {
        return displayNameZh == null || displayNameZh.trim().isEmpty() ? displayName : displayNameZh;
    }

    public String preferredDescription() {
        return descriptionZh == null || descriptionZh.trim().isEmpty() ? description : descriptionZh;
    }
}
