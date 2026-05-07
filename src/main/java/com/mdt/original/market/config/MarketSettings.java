package com.mdt.original.market.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

public final class MarketSettings {
    private static final String DEFAULT_RESOURCE = "plugin-market.properties";
    private static final String DEFAULT_FILE_NAME = "plugin-market.properties";

    private final File dataRoot;
    private final String marketRepository;
    private final String marketBranch;
    private final String gitProxy;
    private final String marketCacheDirectory;
    private final String pluginCacheDirectory;
    private final String installDirectory;
    private final String marketConfigFile;
    private final String registrySuffix;
    private final String pluginMetadataFile;
    private final boolean autoSync;
    private final boolean allowForceInstall;

    private MarketSettings(
        File dataRoot,
        String marketRepository,
        String marketBranch,
        String gitProxy,
        String marketCacheDirectory,
        String pluginCacheDirectory,
        String installDirectory,
        String marketConfigFile,
        String registrySuffix,
        String pluginMetadataFile,
        boolean autoSync,
        boolean allowForceInstall
    ) {
        this.dataRoot = dataRoot;
        this.marketRepository = marketRepository;
        this.marketBranch = marketBranch;
        this.gitProxy = gitProxy;
        this.marketCacheDirectory = marketCacheDirectory;
        this.pluginCacheDirectory = pluginCacheDirectory;
        this.installDirectory = installDirectory;
        this.marketConfigFile = marketConfigFile;
        this.registrySuffix = registrySuffix;
        this.pluginMetadataFile = pluginMetadataFile;
        this.autoSync = autoSync;
        this.allowForceInstall = allowForceInstall;
    }

    public static MarketSettings load(File dataRoot) throws IOException {
        if (!dataRoot.exists() && !dataRoot.mkdirs()) {
            throw new IOException("无法创建数据目录: " + dataRoot);
        }

        File configFile = new File(dataRoot, DEFAULT_FILE_NAME);
        if (!configFile.exists()) {
            copyDefaultConfig(configFile);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        String marketRepository = read(properties, "市场仓库", "https://github.com/MonthZifang/mdt-Plugin-Market.git");
        String marketBranch = read(properties, "市场分支", "main");
        String gitProxy = read(properties, "Git代理", "");
        String marketCacheDirectory = read(properties, "市场缓存目录", "market-cache");
        String pluginCacheDirectory = read(properties, "插件缓存目录", "plugin-cache");
        String installDirectory = read(properties, "安装目录", "config/mods");
        String marketConfigFile = read(properties, "市场配置文件", "market.json");
        String registrySuffix = read(properties, "仓库登记后缀", ".repo.json");
        String pluginMetadataFile = read(properties, "插件元数据文件", "market.plugin.json");
        boolean autoSync = readBoolean(properties, "自动同步", true);
        boolean allowForceInstall = readBoolean(properties, "允许强制安装", true);

        return new MarketSettings(
            dataRoot,
            marketRepository,
            marketBranch,
            gitProxy,
            marketCacheDirectory,
            pluginCacheDirectory,
            installDirectory,
            marketConfigFile,
            registrySuffix,
            pluginMetadataFile,
            autoSync,
            allowForceInstall
        );
    }

    private static void copyDefaultConfig(File configFile) throws IOException {
        try (InputStream inputStream = MarketSettings.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("找不到默认配置资源: " + DEFAULT_RESOURCE);
            }
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("无法创建配置目录: " + parent);
            }
            Files.copy(inputStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String read(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    public File dataRoot() {
        return dataRoot;
    }

    public String marketRepository() {
        return marketRepository;
    }

    public String marketBranch() {
        return marketBranch;
    }

    public String gitProxy() {
        return gitProxy;
    }

    public File marketCacheDir() {
        return new File(dataRoot, marketCacheDirectory);
    }

    public File pluginCacheDir() {
        return new File(dataRoot, pluginCacheDirectory);
    }

    public File installDir() {
        return new File(installDirectory);
    }

    public String marketConfigFile() {
        return marketConfigFile;
    }

    public String registrySuffix() {
        return registrySuffix;
    }

    public String pluginMetadataFile() {
        return pluginMetadataFile;
    }

    public boolean autoSync() {
        return autoSync;
    }

    public boolean allowForceInstall() {
        return allowForceInstall;
    }
}
