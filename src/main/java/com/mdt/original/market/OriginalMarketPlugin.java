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
    private MarketSettings settings;
    private MarketService service;

    @Override
    public void init() {
        try {
            File dataRoot = new File(mindustry.Vars.dataDirectory.absolutePath(), "mdt-original-market-plugin");
            settings = MarketSettings.load(dataRoot);
            service = new MarketService(settings, new GitClient());

            if (!new GitClient().isGitInstalled()) {
                throw new IllegalStateException("当前环境没有可用的 git 命令。");
            }

            if (settings.autoSync()) {
                try {
                    service.syncMarket();
                } catch (IOException exception) {
                    Log.err("市场同步失败，但插件仍继续启动: @", exception.getMessage());
                    service.loadFromCache();
                }
            } else {
                service.loadFromCache();
            }

            Log.info("MDT 原版插件市场已启动。");
        } catch (Exception exception) {
            throw new RuntimeException("原版插件市场初始化失败。", exception);
        }
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("插件市场同步", "同步 Git 插件市场仓库并重新扫描。", args -> {
            try {
                service.syncMarket();
                Log.info("市场同步完成，共 @ 个插件。", service.registryEntries().size());
            } catch (Exception exception) {
                Log.err("市场同步失败: @", exception.getMessage());
            }
        });

        handler.register("插件市场列表", "列出插件市场中的全部插件。", args -> {
            try {
                for (RegistryEntry entry : service.registryEntries()) {
                    Log.info("[@] @ | @ | @", entry.channel(), entry.name(), entry.displayName(), entry.gitRepository());
                }
            } catch (Exception exception) {
                Log.err("读取市场失败: @", exception.getMessage());
            }
        });

        handler.register("插件市场信息", "<名称>", "查看指定插件的详细信息。", args -> {
            try {
                PluginMetadata metadata = service.resolvePluginMetadata(args[0]);
                Log.info(
                    "name=@ version=@ requiredMarketVersion=@ author=@ targets=@",
                    metadata.name(),
                    metadata.version(),
                    metadata.requiredMarketVersion(),
                    metadata.author(),
                    metadata.targets()
                );
            } catch (Exception exception) {
                Log.err("查看插件信息失败: @", exception.getMessage());
            }
        });

        handler.register("插件市场安装", "<名称> [强制]", "安装单个插件，可选强制安装。", args -> {
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

        handler.register("插件市场安装全部", "[强制]", "安装市场中的全部插件，可选强制安装。", args -> {
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

        handler.register("插件市场状态", "查看当前插件市场状态。", args -> {
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

        handler.register("插件市场帮助", "显示插件市场命令帮助。", args -> {
            Log.info("插件市场命令：");
            Log.info("  插件市场同步 - 同步 Git 插件市场仓库并重新扫描");
            Log.info("  插件市场列表 - 列出市场中的全部插件");
            Log.info("  插件市场信息 <名称> - 查看指定插件的详细信息");
            Log.info("  插件市场安装 <名称> [强制] - 安装单个插件");
            Log.info("  插件市场安装全部 [强制] - 安装全部插件");
            Log.info("  插件市场状态 - 查看市场缓存、分支与插件数量");
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
