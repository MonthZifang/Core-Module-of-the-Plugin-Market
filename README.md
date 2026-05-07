[go-Mindustry](https://github.com/tomorrowsetout/go-Mindustry)

<div align="center">
  <a href="https://github.com/MonthZifang/YUEYUEDAO-TECH">
    <img src="./md/logo.png" alt="月月岛科技 Logo" width="720" />
  </a>

  <p><strong>月月岛科技维护 MDT Original Market Plugin</strong></p>

  <p>
    <a href="https://github.com/MonthZifang/YUEYUEDAO-TECH"><strong>查看月月岛科技详情</strong></a>
  </p>
</div>

# MDT 原版插件市场插件

这是一个 Mindustry 原版服务器插件，用来读取 Git 市场仓库、扫描固定登记文件，并安装其它插件。

## 固定协议

1. 先读取市场仓库根目录的 `market.json`
2. 再扫描 `src/modded/*.repo.json` 和 `src/native/*.repo.json`
3. 每个 `*.repo.json` 只登记一个插件仓库
4. 插件仓库固定读取 `market.plugin.json`
5. 再根据 `downloadUrls`、`dependencies`、`entry` 等字段完成展示和安装

## 配置文件

首次启动后会生成：

```text
config/mdt-original-market-plugin/plugin-market.properties
```

主要配置：

- `市场仓库`
- `市场分支`
- `市场缓存目录`
- `插件缓存目录`
- `安装目录`
- `市场配置文件`
- `仓库登记后缀`
- `插件元数据文件`
- `自动同步`
- `允许强制安装`

## 命令

- `market-sync`
- `market-list`
- `market-info <name>`
- `market-install <name> [force]`
- `market-install-all [force]`
- `market-status`

## 构建

```powershell
.\gradlew.bat jar
```

输出：

```text
build/libs/mdt-original-market-plugin.jar
dist/mdt-original-market-plugin.jar
```
