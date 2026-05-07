package com.mdt.original.market;

import arc.util.CommandHandler;
import arc.util.Log;
import com.mdt.original.market.config.MarketSettings;
import com.mdt.original.market.git.GitClient;
import com.mdt.original.market.model.PluginMetadata;
import com.mdt.original.market.model.RegistryEntry;
import com.mdt.original.market.service.MarketService;
import java.io.File;
import java.io.IOException;
import mindustry.mod.Plugin;

public final class OriginalMarketPlugin extends Plugin {
    private static final String CONFIG_FOLDER_NAME = "core-module-of-the-plugin-market";

    private MarketSettings settings;
    private MarketService service;
    private GitClient gitClient;

    @Override
    public void init() {
        try {
            File modsRoot = new File(mindustry.Vars.dataDirectory.absolutePath(), "mods");
            File dataRoot = new File(new File(modsRoot, "config"), CONFIG_FOLDER_NAME);
            settings = MarketSettings.load(dataRoot);
            gitClient = new GitClient();
            service = new MarketService(settings, gitClient);

            if (!gitClient.isGitInstalled(settings.gitProxy())) {
                throw new IllegalStateException("当前环境没有可用的 git 命令。");
            }

            if (settings.autoSync()) {
                try {
                    service.syncMarket();
                } catch (IOException exception) {
                    Log.err("市场同步失败，但插件仍继续启动: @", exception.getMessage());
                    try {
                        service.loadFromCache();
                    } catch (IOException cacheException) {
                        Log.err("缓存加载失败，插件以空市场模式启动: @", cacheException.getMessage());
                        service.useEmptyCatalog();
                    }
                }
            } else {
                try {
                    service.loadFromCache();
                } catch (IOException exception) {
                    Log.err("市场缓存不存在，插件以空市场模式启动: @", exception.getMessage());
                    service.useEmptyCatalog();
                }
            }

            Log.info("MDT 原版插件市场已启动。");
        } catch (Exception exception) {
            throw new RuntimeException("原版插件市场初始化失败。", exception);
        }
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("market-sync", "同步 Git 插件市场仓库并重新扫描。", args -> {
            try {
                service.syncMarket();
                Log.info("市场同步完成，共 @ 个插件。", service.registryEntries().size());
            } catch (Exception exception) {
                Log.err("市场同步失败: @", exception.getMessage());
            }
        });

        handler.register("market-list", "列出插件市场中的全部插件。", args -> {
            try {
                for (RegistryEntry entry : service.registryEntries()) {
                    try {
                        PluginMetadata metadata = service.resolvePluginMetadata(entry.name());
                        Log.info(
                            "登记名=@ | 插件名=@ | 显示名=@ | 作者=@ | 版本=@ | 简介=@",
                            entry.name(),
                            metadata.name(),
                            metadata.preferredDisplayName(),
                            metadata.author(),
                            metadata.version(),
                            metadata.preferredDescription()
                        );
                    } catch (Exception metadataException) {
                        Log.info(
                            "登记名=@ | 显示名=@ | 分类=@ | 仓库=@ | 元数据读取失败=@",
                            entry.name(),
                            entry.displayName(),
                            entry.channel(),
                            entry.gitRepository(),
                            metadataException.getMessage()
                        );
                    }
                }
            } catch (Exception exception) {
                Log.err("读取市场失败: @", exception.getMessage());
            }
        });

        handler.register("market-info", "<name>", "查看指定插件的详细信息。", args -> {
            try {
                RegistryEntry entry = service.catalog().get(args[0]);
                PluginMetadata metadata = service.resolvePluginMetadata(args[0]);
                if (entry != null) {
                    Log.info("登记名=@ | 登记显示名=@ | 登记分类=@ | 登记仓库=@", entry.name(), entry.displayName(), entry.channel(), entry.gitRepository());
                }
                Log.info("插件名=@ | 显示名=@ | 作者=@", metadata.name(), metadata.preferredDisplayName(), metadata.author());
                Log.info("版本=@ | 需求市场版本=@ | 分类=@ | 目标端=@", metadata.version(), metadata.requiredMarketVersion(), metadata.channel(), metadata.targets());
                Log.info("简介=@", metadata.preferredDescription());
                Log.info("依赖=@ | 入口类=@ | 仓库=@", metadata.dependencies(), metadata.entry(), metadata.repositoryUrl());
                Log.info("下载链接=@", metadata.downloadUrls());
            } catch (Exception exception) {
                Log.err("查看插件信息失败: @", exception.getMessage());
            }
        });

        handler.register("market-install", "<name> [force]", "安装单个插件，可选强制安装。", args -> {
            try {
                boolean force = isForce(args);
                if (force && !settings.allowForceInstall()) {
                    Log.err("当前配置不允许强制安装。");
                    return;
                }
                service.installPlugin(args[0], force);
            } catch (Exception exception) {
                Log.err("安装失败: @", exception.getMessage());
            }
        });

        handler.register("market-install-all", "[force]", "安装市场中的全部插件，可选强制安装。", args -> {
            try {
                boolean force = isForce(args);
                if (force && !settings.allowForceInstall()) {
                    Log.err("当前配置不允许强制安装。");
                    return;
                }
                for (RegistryEntry entry : service.registryEntries()) {
                    service.installPlugin(entry.name(), force);
                }
            } catch (Exception exception) {
                Log.err("批量安装失败: @", exception.getMessage());
            }
        });

        handler.register("market-status", "查看当前插件市场状态。", args -> {
            try {
                Log.info("market=@ branch=@ cache=@ install=@ entries=@",
                    settings.marketRepository(),
                    settings.marketBranch(),
                    settings.marketCacheDir().getAbsolutePath(),
                    settings.installDir().getAbsolutePath(),
                    service.registryEntries().size()
                );
            } catch (Exception exception) {
                Log.err("查看状态失败: @", exception.getMessage());
            }
        });

        handler.register("market-help", "显示插件市场命令帮助。", args -> {
            Log.info("插件市场命令：");
            Log.info("  market-sync - 同步 Git 插件市场仓库并重新扫描");
            Log.info("  market-list - 列出市场中的全部插件");
            Log.info("  market-info <name> - 查看指定插件的详细信息");
            Log.info("  market-install <name> [force] - 安装单个插件");
            Log.info("  market-install-all [force] - 安装全部插件");
            Log.info("  market-status - 查看市场缓存、分支与插件数量");
        });
    }

    private boolean isForce(String[] args) {
        for (String value : args) {
            if ("force".equalsIgnoreCase(value) || "--force".equalsIgnoreCase(value) || "强制".equals(value)) {
                return true;
            }
        }
        return false;
    }
}
