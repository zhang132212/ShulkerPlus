# ShulkerPlus

Item-opened shulker boxes use vanilla `ShulkerBoxMenu`. Its 27
`ShulkerBoxSlot` instances call `Item.canFitInsideContainerItems()`, exactly like
a placed vanilla shulker box. The no-nesting rule does not depend on Bukkit
click or drag event cancellation and does not apply to workbench or ender-chest
interfaces.

Paper 插件，支持手持潜影盒/工作台/切石机/末影箱右键打开，背包内嵌套切换，收纳袋功能。

## 功能

| 功能 | 说明 |
|------|------|
| **右键空气打开** | 主手持潜影盒/工作台/切石机/末影箱 → 对空气右键 → 直接打开对应 UI；无需潜行，副手不触发，右键方块时保留原版交互和放置 |
| **背包右键打开** | 在任意容器界面右键玩家背包里的潜影盒/工作台/切石机/末影箱 → 打开（需装 Fabric mod） |
| **嵌套打开** | 在任意容器 UI 里右键下方玩家背包的可打开物品 → 原容器关闭；关闭快捷 GUI 后返回玩家背包；上方容器物品不会触发 |
| **收纳袋** | 光标持潜影盒 → 右键物品塞入 / 右键空格取出 / 右键非潜影盒物品填充 |
| **工作台合成** | 真实 3x3 合成（非配方书），结果格预览，Shift 批量 |
| **切石机切割** | 真实切石机，结果预览，Shift 批量 |
| **中文界面标题** | 潜影盒、箱子、末影箱、工作台等原版默认 GUI 标题显示为中文，自定义标题保持不变 |
| **实时物品同步** | 潜影盒内物品操作立即写回 NBT，关闭不丢物品 |
| **防套娃** | 禁止把潜影盒塞进潜影盒 |
| **防误触** | 可开启潜行检查 |

## 安装

1. 把 `ShulkerPlus-3.3.14.jar` 放入 `plugins/` 目录
2. 重启服务器
3. 编辑 `plugins/ShulkerPlus/config.yml` 按需调整配置

## 配置

```yaml
# config.yml
play-sounds: true            # 播放原版开/关音效
cooldown-ms: 500             # 打开冷却（毫秒）
enable-workbench: true       # 允许打开工作台
enable-stonecutter: true     # 允许打开切石机
enable-nested-opening: true  # 允许嵌套打开
enable-bundle-mode: false    # 收纳袋功能（默认关闭）
```

## 权限

```
shulkerplus.use    # 默认所有人可用
```

## Fabric mod 集成

### 什么需要 mod

**只有 1 个功能需要 Fabric mod：按 E → 右键背包里的物品打开。**

原因是 Bukkit 的 `InventoryClickEvent` 在右键单个物品时拿不到正确的光标/物品状态，需要 Fabric 客户端拦截右键并在处理前发 `OpenShulkerPacket` 通知服务端。

其余所有功能（手持物品右键空气打开、嵌套切换、收纳袋、工作台合成等）均为纯服务端实现，**不装 mod 也能用**。

### 编译 mod

修改版 Fabric mod 已通过 GitHub Actions 自动构建，基于 [Haocen2004/quickshulker](https://github.com/Haocen2004/quickshulker)（1.21 分支）修改 `ItemMixin.java`（三处改动：客户端只发包不执行、服务端执行）。到仓库 **Actions** 标签手动触发 **Build QuickShulker Fabric Mod** 即可下载 JAR，放入客户端 `.minecraft/mods/`。

## 依赖

- Paper 1.21.x（使用 NMS `CraftingMenu` / `StonecutterMenu`）
- ProtocolLib（不再需要，已移除）

## 致谢

本项目功能参考 [jorbay/ShulkerPlus](https://modrinth.com/plugin/shulker-plus)（闭源，ARR），独立实现。感谢原作者的创意。

## 许可证

MIT
