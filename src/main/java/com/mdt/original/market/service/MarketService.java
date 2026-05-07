package com.mdt.original.market.service;

import arc.util.Log;
import arc.util.serialization.Jval;
import com.mdt.original.market.config.MarketSettings;
import com.mdt.original.market.git.GitClient;
import com.mdt.original.market.model.MarketCatalog;
import com.mdt.original.market.model.PluginMetadata;
import com.mdt.original.market.model.RegistryEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarketService {
    private final MarketSettings settings;
    private final GitClient gitClient;

    private MarketCatalog catalog;

    public MarketService(MarketSettings settings, GitClient gitClient) {
        this.settings = settings;
        this.gitClient = gitClient;
    }

    public void syncMarket() throws IOException {
        gitClient.syncRepository(settings.marketRepository(), settings.marketBranch(), settings.marketCacheDir(), settings.gitProxy());
        this.catalog = loadCatalog();
    }

    public void loadFromCache() throws IOException {
        this.catalog = loadCatalog();
    }

    public void useEmptyCatalog() {
        this.catalog = new MarketCatalog("unknown", settings.pluginMetadataFile(), new LinkedHashMap<String, RegistryEntry>());
    }

    public MarketCatalog catalog() {
        return catalog;
    }

    public PluginMetadata resolvePluginMetadata(String pluginName) throws IOException {
        ensureCatalog();
        RegistryEntry entry = catalog.get(pluginName);
        if (entry == null) {
            throw new IOException("未找到插件登记: " + pluginName);
        }

        File repoDir = new File(settings.pluginCacheDir(), sanitize(entry.name()));
        gitClient.syncRepository(entry.gitRepository(), entry.gitBranch(), repoDir, settings.gitProxy());

        File metadataFile = new File(repoDir, entry.pluginMetadataFile());
        if (!metadataFile.exists()) {
            throw new IOException("插件仓库缺少固定元数据文件: " + metadataFile);
        }

        return PluginMetadata.fromJval(readJval(metadataFile));
    }

    public void installPlugin(String pluginName, boolean force) throws IOException {
        installPlugin(pluginName, force, new java.util.HashSet<String>());
    }

    public List<RegistryEntry> registryEntries() {
        ensureCatalogUnchecked();
        return new ArrayList<RegistryEntry>(catalog.entries().values());
    }

    private MarketCatalog loadCatalog() throws IOException {
        File marketJson = new File(settings.marketCacheDir(), settings.marketConfigFile());
        if (!marketJson.exists()) {
            throw new IOException("市场配置文件不存在: " + marketJson);
        }

        Jval root = readJval(marketJson);
        String marketVersion = root.getString("version", "v1");
        String pluginMetadataFile = root.getString("pluginMetadataFile", settings.pluginMetadataFile());

        Map<String, RegistryEntry> entries = new LinkedHashMap<String, RegistryEntry>();
        for (String dirName : root.get("scanDirectories").asArray().map(Jval::asString)) {
            File dir = new File(settings.marketCacheDir(), dirName);
            File[] files = dir.listFiles((currentDir, name) -> name.endsWith(settings.registrySuffix()));
            if (files == null) continue;
            for (File file : files) {
                RegistryEntry entry = RegistryEntry.fromJval(readJval(file));
                if (entry.name().isEmpty()) continue;
                entries.put(entry.name(), entry);
            }
        }

        return new MarketCatalog(marketVersion, pluginMetadataFile, entries);
    }

    private void validateInstallable(PluginMetadata metadata, boolean force) throws IOException {
        String marketVersion = catalog.marketVersion();
        boolean versionMatch = marketVersion.equals(metadata.version()) && marketVersion.equals(metadata.requiredMarketVersion());
        if (!versionMatch && !force) {
            throw new IOException(
                "插件版本不匹配，插件版本=" + metadata.version() +
                "，需求市场版本=" + metadata.requiredMarketVersion() +
                "，当前市场版本=" + marketVersion +
                "。如果你仍然要安装，请使用强制安装。"
            );
        }
    }

    private void installJar(File jarFile, PluginMetadata metadata) throws IOException {
        File installDir = settings.installDir();
        if (!installDir.exists() && !installDir.mkdirs()) {
            throw new IOException("无法创建安装目录: " + installDir);
        }

        File target = new File(installDir, jarFile.getName());
        java.nio.file.Files.copy(jarFile.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Log.info("已安装 jar: @ -> @", metadata.name(), target.getAbsolutePath());
    }

    private void download(String url, File destination) throws IOException {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try (java.io.InputStream inputStream = openRemoteStream(url)) {
                java.nio.file.Files.copy(inputStream, destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        if (url.startsWith("file:")) {
            java.nio.file.Path source = java.nio.file.Paths.get(java.net.URI.create(url));
            java.nio.file.Files.copy(source, destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        File source = new File(url);
        if (!source.exists()) {
            throw new IOException("下载源不存在: " + url);
        }
        java.nio.file.Files.copy(source.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private java.io.InputStream openRemoteStream(String url) throws IOException {
        URL remoteUrl = new URL(url);
        Proxy proxy = buildProxy(settings.gitProxy());
        if (proxy == Proxy.NO_PROXY) {
            return remoteUrl.openStream();
        }
        return remoteUrl.openConnection(proxy).getInputStream();
    }

    private Proxy buildProxy(String proxyUrl) {
        if (proxyUrl == null || proxyUrl.trim().isEmpty()) {
            return Proxy.NO_PROXY;
        }

        try {
            URI uri = URI.create(proxyUrl.trim());
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(uri.getHost(), port));
        } catch (Exception ignored) {
            return Proxy.NO_PROXY;
        }
    }

    private Jval readJval(File file) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return Jval.read(builder.toString());
        }
    }

    private void ensureCatalog() throws IOException {
        if (catalog == null) {
            throw new IOException("市场目录尚未加载，请先同步市场。");
        }
    }

    private void ensureCatalogUnchecked() {
        if (catalog == null) {
            throw new IllegalStateException("市场目录尚未加载，请先同步市场。");
        }
    }

    private void installPlugin(String pluginName, boolean force, java.util.Set<String> stack) throws IOException {
        ensureCatalog();
        if (!stack.add(pluginName)) {
            throw new IOException("检测到循环依赖: " + pluginName);
        }

        PluginMetadata metadata = resolvePluginMetadata(pluginName);
        validateInstallable(metadata, force);

        for (String dependency : metadata.dependencies()) {
            String dependencyName = dependency.trim();
            if (!dependencyName.isEmpty() && !dependencyName.equalsIgnoreCase(pluginName)) {
                installPlugin(dependencyName, force, stack);
            }
        }

        File cacheDir = new File(settings.pluginCacheDir(), sanitize(metadata.name()) + File.separator + sanitize(metadata.version()));
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("无法创建下载缓存目录: " + cacheDir);
        }

        for (String url : metadata.downloadUrls()) {
            File destination = new File(cacheDir, fileNameOf(url));
            download(url, destination);
            if (destination.getName().toLowerCase().endsWith(".jar")) {
                installJar(destination, metadata);
            }
        }

        Log.info("已完成安装: @", metadata.displayName());
        stack.remove(pluginName);
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String fileNameOf(String url) {
        String clean = url;
        int query = clean.indexOf('?');
        if (query >= 0) clean = clean.substring(0, query);
        int slash = clean.lastIndexOf('/');
        return slash >= 0 ? clean.substring(slash + 1) : clean;
    }
}
