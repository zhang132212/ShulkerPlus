# ShulkerPlus

Paper 插件，支持手持潜影盒/工作台/切石机右键打开，背包内嵌套切换，收纳袋功能。

## 功能

| 功能 | 说明 |
|------|------|
| **潜行+右键打开** | 手持潜影盒/工作台/切石机 → 潜行+右键 → 直接打开对应 UI |
| **背包右键打开** | 按 E → 右键背包里的潜影盒/工作台 → 打开（需装 Fabric mod） |
| **嵌套打开** | 在任意 UI 里右键背包的可打开物品 → 自动切换，ESC 逐层退回 |
| **收纳袋** | 光标持潜影盒 → 右键物品塞入 / 右键空格取出 / 右键非潜影盒物品填充 |
| **工作台合成** | 真实 3x3 合成（非配方书），结果格预览，Shift 批量 |
| **切石机切割** | 真实切石机，结果预览，Shift 批量 |
| **实时物品同步** | 潜影盒内物品操作立即写回 NBT，关闭不丢物品 |
| **防套娃** | 禁止把潜影盒塞进潜影盒 |
| **防误触** | 可开启潜行检查 |

## 安装

1. 把 `ShulkerPlus-3.5.0.jar` 放入 `plugins/` 目录
2. 重启服务器
3. 编辑 `plugins/ShulkerPlus/config.yml` 按需调整配置

## 配置

```yaml
# config.yml
require-sneak: true          # 是否需要潜行才能打开
play-sounds: true            # 播放原版开/关音效
close-on-move: true          # 移动时自动关闭 UI
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

其余所有功能（潜行右键打开、嵌套切换、收纳袋、工作台合成等）均为纯服务端实现，**不装 mod 也能用**。

### 编译 mod

1. 克隆 [kyrptonaught/quickshulker](https://github.com/kyrptonaught/quickshulker)
2. 修改 `src/main/java/net/kyrptonaught/quickshulker/mixin/ItemMixin.java`

三处改动均是把原来的"同时发包+本地执行"改成"客户端只发包、服务端才本地执行"：

**改法 1 — `onClicked` 的 bundle 插入分支：**
```java
// 找到 supportsBundlingInsert 那块的 if-else
// 原始：发完包后无论客户端服务端都调用 bundleItemIntoStack
// 改为：
if (player.getEntityWorld().isClient()) {
    if (slot.inventory instanceof PlayerInventory) {
        QuickBundlePacket.sendPacket(ClientUtil.getPlayerInvSlot(player.currentScreenHandler, slot), insertStack);
    }
} else {
    BundleHelper.bundleItemIntoStack(player, hostStack, insertStack, cir);
}
```

**改法 2 — `onStackClicked` 的 bundleHeld 分支：**
```java
// 找到 supportsBundlingPickup 那块的 if-else，同原理改为 client 发包 / server 执行
if (player.getEntityWorld().isClient()) {
    if (slot.inventory instanceof PlayerInventory) {
        QuickBundlePacket.BundleIntoHeld.sendPacket(insertStack, hostStack, ClientUtil.getPlayerInvSlot(player.currentScreenHandler, slot));
    }
} else {
    // 保持原有 server 端逻辑不变
}
```

**改法 3 — `onStackClicked` 的 unbundle 分支：**
```java
// 找到 supportsBundlingExtract 那块，同上
if (player.getEntityWorld().isClient()) {
    if (slot.inventory instanceof PlayerInventory) {
        QuickBundlePacket.UnbundlePacket.sendPacket(ClientUtil.getPlayerInvSlot(player.currentScreenHandler, slot), hostStack);
    }
} else {
    // 保持原有 server 端逻辑不变
}
```

3. 编译：`./gradlew build`
4. jar 在 `build/libs/`，放入客户端 `.minecraft/mods/`

## 依赖

- Paper 1.21.x（使用 NMS `CraftingMenu` / `StonecutterMenu`）
- ProtocolLib（不再需要，已移除）

## 许可证

MIT
