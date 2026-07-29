/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.SimpleMenuProvider
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ContainerLevelAccess
 *  net.minecraft.world.inventory.CraftingMenu
 *  net.minecraft.world.inventory.ShulkerBoxMenu
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.inventory.StonecutterMenu
 *  net.minecraft.world.level.Level
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Sound
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.ShulkerBox
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.craftbukkit.inventory.CraftItemStack
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.plugin.messaging.PluginMessageListener
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package me.zhang132212.shulkerplus;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.zhang132212.shulkerplus.OpenableType;
import me.zhang132212.shulkerplus.Session;
import me.zhang132212.shulkerplus.UIContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ShulkerPlus
extends JavaPlugin
implements Listener,
PluginMessageListener {
    private NamespacedKey itemKey;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final Map<UUID, Session> closingSessions = new HashMap<UUID, Session>();
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
    private BukkitTask cleanupTask;
    private boolean playSounds;
    private boolean enableWorkbench;
    private boolean enableStonecutter;
    private boolean enableEnderChest;
    private boolean enableNestedOpening;
    private boolean enableBundleMode;
    private long cooldownMs;
    private static final Set<Material> SHULKER_BOXES;

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        this.itemKey = new NamespacedKey((Plugin)this, "shulkerplus_uid");
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getServer().getMessenger().registerOutgoingPluginChannel((Plugin)this, "quickshulker:open_shulker_packet");
        this.getServer().getMessenger().registerOutgoingPluginChannel((Plugin)this, "quickshulker:openinv");
        this.getServer().getMessenger().registerIncomingPluginChannel((Plugin)this, "quickshulker:open_shulker_packet", (PluginMessageListener)this);
        this.cleanupTask = new CleanupRunnable().runTaskTimer((Plugin)this, 20L, 20L);
        this.getLogger().info("ShulkerPlus enabled!");
    }

    public void onPluginMessageReceived(String channel, org.bukkit.entity.Player player, byte[] message) {
        switch (channel) {
            case "quickshulker:open_shulker_packet": {
                this.handleOpenPacket(player, message);
            }
        }
    }

    private void handleOpenPacket(org.bukkit.entity.Player player, byte[] message) {
        if (message.length != 4) {
            return;
        }
        int rawSlot = ByteBuffer.wrap(message).getInt();
        InventoryView originView = player.getOpenInventory();
        int invIndex = this.resolvePlayerInventorySlot(player, originView, rawSlot);
        if (invIndex < 0) {
            return;
        }
        ItemStack item = player.getInventory().getItem(invIndex);
        if (!this.isOpenable(item)) {
            return;
        }
        if (item.getAmount() != 1) {
            return;
        }
        OpenableType type = this.getOpenableType(item);
        if (type == null) {
            return;
        }
        Session existing = this.sessions.get(player.getUniqueId());
        if (existing != null) {
            if (invIndex == this.getSourceSlot(existing) || existing.itemId.equals(this.getItemId(item))) {
                return;
            }
            this.syncToSource(player, existing);
            boolean returnToPlayerInventory = existing.returnToPlayerInventory;
            this.openItem(player, type, item, invIndex, invIndex == 40 ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
            Session opened = this.sessions.get(player.getUniqueId());
            if (opened != null) {
                opened.returnToPlayerInventory = returnToPlayerInventory;
            }
            return;
        }
        this.openItemFromInventoryView(player, type, item, invIndex, originView);
    }

    private int resolvePlayerInventorySlot(org.bukkit.entity.Player player, InventoryView view, int rawSlot) {
        if (rawSlot < view.getTopInventory().getSize()) {
            return -1;
        }
        ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
        AbstractContainerMenu menu = serverPlayer.containerMenu;
        if (rawSlot < 0 || rawSlot >= menu.slots.size()) {
            return -1;
        }
        Slot slot = menu.getSlot(rawSlot);
        if (slot.container != serverPlayer.getInventory()) {
            return -1;
        }
        int inventorySlot = slot.getContainerSlot();
        if (inventorySlot >= 0 && inventorySlot < 36 || inventorySlot == 40) {
            return inventorySlot;
        }
        return -1;
    }

    private void openItemFromInventoryView(org.bukkit.entity.Player player, OpenableType type, ItemStack sourceItem, int sourceSlot, InventoryView originView) {
        UUID itemId = this.getOrCreateItemId(sourceItem);
        player.getInventory().setItem(sourceSlot, sourceItem);
        boolean restoreOrigin = originView.getTopInventory().getType() != InventoryType.CRAFTING;
        player.closeInventory();
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            EquipmentSlot hand;
            if (!player.isOnline() || this.sessions.containsKey(player.getUniqueId())) {
                return;
            }
            ItemStack current = player.getInventory().getItem(sourceSlot);
            if (!itemId.equals(this.getItemId(current))) {
                return;
            }
            EquipmentSlot equipmentSlot = hand = sourceSlot == 40 ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
            if (!restoreOrigin) {
                this.openItem(player, type, current, sourceSlot, hand);
                return;
            }
            Session session = new Session(type, hand, null, current, sourceSlot, itemId);
            session.returnToPlayerInventory = true;
            this.openItemFromSession(player, type, session, current, sourceSlot);
        });
    }

    private void openPlayerInventoryScreen(org.bukkit.entity.Player player) {
        if (!player.isOnline()) {
            return;
        }
        byte[] payload = ByteBuffer.allocate(4).putInt(0).array();
        player.sendPluginMessage((Plugin)this, "quickshulker:openinv", payload);
    }

    public void onDisable() {
        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
        }
        for (UUID pid : new HashSet<UUID>(this.sessions.keySet())) {
            Session s = this.sessions.get(pid);
            org.bukkit.entity.Player p = Bukkit.getPlayer((UUID)pid);
            if (p == null || s == null) continue;
            this.syncToSource(p, s);
            p.closeInventory();
        }
        this.sessions.clear();
        this.cooldowns.clear();
    }

    private void loadConfig() {
        this.playSounds = this.getConfig().getBoolean("play-sounds", true);
        this.cooldownMs = this.getConfig().getLong("cooldown-ms", 500L);
        this.enableWorkbench = this.getConfig().getBoolean("enable-workbench", true);
        this.enableStonecutter = this.getConfig().getBoolean("enable-stonecutter", true);
        this.enableEnderChest = this.getConfig().getBoolean("enable-ender-chest", true);
        this.enableNestedOpening = this.getConfig().getBoolean("enable-nested-opening", true);
        this.enableBundleMode = this.getConfig().getBoolean("enable-bundle-mode", false);
    }

    private boolean isOpenable(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Material type = item.getType();
        if (SHULKER_BOXES.contains(type)) {
            return true;
        }
        if (this.enableWorkbench && type == Material.CRAFTING_TABLE) {
            return true;
        }
        if (this.enableStonecutter && type == Material.STONECUTTER) {
            return true;
        }
        return this.enableEnderChest && type == Material.ENDER_CHEST;
    }

    private OpenableType getOpenableType(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        Material type = item.getType();
        if (SHULKER_BOXES.contains(type)) {
            return OpenableType.SHULKER;
        }
        if (type == Material.CRAFTING_TABLE) {
            return OpenableType.WORKBENCH;
        }
        if (type == Material.STONECUTTER) {
            return OpenableType.STONECUTTER;
        }
        if (type == Material.ENDER_CHEST) {
            return OpenableType.ENDER_CHEST;
        }
        return null;
    }

    private void syncShulkerItems(ItemStack item, Inventory virtualInv) {
        BlockStateMeta bsm;
        BlockState blockState;
        if (item == null || virtualInv == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta && (blockState = (bsm = (BlockStateMeta)meta).getBlockState()) instanceof ShulkerBox) {
            ShulkerBox box = (ShulkerBox)blockState;
            box.getInventory().setContents(virtualInv.getContents());
            bsm.setBlockState((BlockState)box);
            item.setItemMeta((ItemMeta)bsm);
        }
    }

    private void syncToSource(org.bukkit.entity.Player player, Session session) {
        if (session.type != OpenableType.SHULKER || session.virtualInv == null) {
            return;
        }
        ItemStack current = this.findSourceItem(player, session);
        if (current == null) {
            current = this.recoverSourceItem(player, session);
        }
        if (current == null) {
            return;
        }
        this.syncShulkerItems(current, session.virtualInv);
    }

    private ItemStack recoverSourceItem(org.bukkit.entity.Player player, Session session) {
        ItemStack offhand;
        ItemStack item;
        int i;
        UUID targetId = session.itemId;
        int sourceSlot = this.getSourceSlot(session);
        Inventory vInv = session.virtualInv;
        for (i = 0; i < vInv.getSize(); ++i) {
            item = vInv.getItem(i);
            if (item == null || !targetId.equals(this.getItemId(item))) continue;
            ItemStack recovered = item.clone();
            vInv.setItem(i, null);
            player.getInventory().setItem(sourceSlot, recovered);
            return recovered;
        }
        for (i = 0; i < 36; ++i) {
            if (i == sourceSlot || (item = player.getInventory().getItem(i)) == null || !targetId.equals(this.getItemId(item))) continue;
            ItemStack moved = item.clone();
            player.getInventory().setItem(i, null);
            player.getInventory().setItem(sourceSlot, moved);
            return moved;
        }
        if (sourceSlot != 40 && (offhand = player.getInventory().getItemInOffHand()) != null && targetId.equals(this.getItemId(offhand))) {
            ItemStack moved = offhand.clone();
            player.getInventory().setItemInOffHand(null);
            player.getInventory().setItem(sourceSlot, moved);
            return moved;
        }
        return null;
    }

    private int getSourceSlot(Session session) {
        return session.equipmentSlot == EquipmentSlot.HAND ? session.hotbarSlot : 40;
    }

    private ItemStack findSourceItem(org.bukkit.entity.Player player, Session session) {
        PlayerInventory inv = player.getInventory();
        if (session.equipmentSlot == EquipmentSlot.HAND) {
            return inv.getItem(session.hotbarSlot);
        }
        return inv.getItemInOffHand();
    }

    private void scheduleSourceIntegrityCheck(org.bukkit.entity.Player player, Session session, Inventory closedInventory) {
        if (session.type != OpenableType.WORKBENCH && session.type != OpenableType.ENDER_CHEST) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            ItemStack sourceSlotItem;
            org.bukkit.entity.Player onlinePlayer = Bukkit.getPlayer((UUID)playerId);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                return;
            }
            if (this.hasSessionItem(onlinePlayer, session.itemId, closedInventory)) {
                return;
            }
            ItemStack restored = session.sourceItem.clone();
            restored.setAmount(1);
            PlayerInventory inventory = onlinePlayer.getInventory();
            int sourceSlot = this.getSourceSlot(session);
            ItemStack itemStack = sourceSlotItem = sourceSlot >= 0 && sourceSlot < inventory.getSize() ? inventory.getItem(sourceSlot) : null;
            if (sourceSlot >= 0 && sourceSlot < inventory.getSize() && (sourceSlotItem == null || sourceSlotItem.getType().isAir())) {
                inventory.setItem(sourceSlot, restored);
            } else {
                java.util.HashMap<Integer, ItemStack> leftovers = (java.util.HashMap<Integer, ItemStack>) inventory.addItem(new ItemStack[]{restored});
                for (ItemStack leftover : leftovers.values()) {
                    onlinePlayer.getWorld().dropItemNaturally(onlinePlayer.getLocation(), leftover);
                }
            }
            this.getLogger().warning("Restored missing " + String.valueOf((Object)session.type) + " source item for " + onlinePlayer.getName() + " (session " + String.valueOf(session.itemId) + ")");
        }, 1L);
    }

    private boolean hasSessionItem(org.bukkit.entity.Player player, UUID itemId, Inventory closedInventory) {
        if (this.containsSessionItem((Inventory)player.getInventory(), itemId)) {
            return true;
        }
        if (this.isSessionItem(player.getItemOnCursor(), itemId)) {
            return true;
        }
        if (this.containsSessionItem(player.getEnderChest(), itemId)) {
            return true;
        }
        if (closedInventory != null && this.containsSessionItem(closedInventory, itemId)) {
            return true;
        }
        for (Entity entity : player.getNearbyEntities(4.0, 4.0, 4.0)) {
            Item dropped;
            if (!(entity instanceof Item) || !this.isSessionItem((dropped = (Item)entity).getItemStack(), itemId)) continue;
            return true;
        }
        return false;
    }

    private boolean containsSessionItem(Inventory inventory, UUID itemId) {
        if (inventory == null) {
            return false;
        }
        for (ItemStack item : inventory.getContents()) {
            if (!this.isSessionItem(item, itemId)) continue;
            return true;
        }
        return false;
    }

    private boolean isSessionItem(ItemStack item, UUID itemId) {
        return itemId != null && itemId.equals(this.getItemId(item));
    }

    private UUID getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(this.itemKey, PersistentDataType.STRING)) {
            try {
                return UUID.fromString((String)pdc.get(this.itemKey, PersistentDataType.STRING));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        return null;
    }

    private int findSlotInInventory(org.bukkit.entity.Player player, ItemStack target) {
        UUID targetId = this.getItemId(target);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; ++i) {
            if (contents[i] == null) continue;
            if (targetId != null && targetId.equals(this.getItemId(contents[i]))) {
                return i;
            }
            if (targetId != null || !contents[i].equals((Object)target)) continue;
            return i;
        }
        return -1;
    }

    private UUID getOrCreateItemId(ItemStack item) {
        if (item == null) {
            return UUID.randomUUID();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return UUID.randomUUID();
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(this.itemKey, PersistentDataType.STRING)) {
            try {
                return UUID.fromString((String)pdc.get(this.itemKey, PersistentDataType.STRING));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        UUID id = UUID.randomUUID();
        pdc.set(this.itemKey, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
        return id;
    }

    private boolean checkPermissionAndCooldown(org.bukkit.entity.Player player) {
        if (!player.hasPermission("shulkerplus.use")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use ShulkerPlus.");
            return false;
        }
        Long last = this.cooldowns.get(player.getUniqueId());
        if (last != null && System.currentTimeMillis() - last < this.cooldownMs) {
            return false;
        }
        this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    private void openNmsWorkbench(org.bukkit.entity.Player player) {
        ServerPlayer sp = ((CraftPlayer)player).getHandle();
        ContainerLevelAccess access = ContainerLevelAccess.create((Level)sp.level(), (BlockPos)sp.blockPosition());
        sp.openMenu((MenuProvider)new SimpleMenuProvider((syncId, inv, p) -> new CraftingMenu(syncId, inv, access){

            public boolean stillValid(Player p) {
                return !p.isRemoved();
            }
        }, (Component)Component.literal((String)"\u5de5\u4f5c\u53f0")));
    }

    private void openNmsStonecutter(org.bukkit.entity.Player player) {
        ServerPlayer sp = ((CraftPlayer)player).getHandle();
        ContainerLevelAccess access = ContainerLevelAccess.create((Level)sp.level(), (BlockPos)sp.blockPosition());
        sp.openMenu((MenuProvider)new SimpleMenuProvider((syncId, inv, p) -> new StonecutterMenu(syncId, inv, access){

            public boolean stillValid(Player p) {
                return !p.isRemoved();
            }
        }, (Component)Component.literal((String)"\u5207\u77f3\u673a")));
    }

    private void openEnderChest(org.bukkit.entity.Player player) {
        InventoryView view = player.openInventory(player.getEnderChest());
        view.setTitle("\u672b\u5f71\u7bb1");
    }

    private String localizeDefaultGuiTitle(String title) {
        if (title == null) {
            return null;
        }
        return switch (title) {
            case "Chest" -> "\u7bb1\u5b50";
            case "Large Chest" -> "\u5927\u7bb1\u5b50";
            case "Shulker Box" -> "\u6f5c\u5f71\u76d2";
            case "Ender Chest" -> "\u672b\u5f71\u7bb1";
            case "Crafting", "Crafting Table" -> "\u5de5\u4f5c\u53f0";
            case "Stonecutter" -> "\u5207\u77f3\u673a";
            case "Barrel" -> "\u6728\u6876";
            case "Furnace" -> "\u7194\u7089";
            case "Blast Furnace" -> "\u9ad8\u7089";
            case "Smoker" -> "\u70df\u718f\u7089";
            case "Brewing Stand" -> "\u917f\u9020\u53f0";
            case "Dispenser" -> "\u53d1\u5c04\u5668";
            case "Dropper" -> "\u6295\u63b7\u5668";
            case "Hopper" -> "\u6f0f\u6597";
            case "Anvil" -> "\u94c1\u7827";
            case "Enchanting" -> "\u9644\u9b54\u53f0";
            case "Beacon" -> "\u4fe1\u6807";
            case "Loom" -> "\u7ec7\u5e03\u673a";
            case "Cartography Table" -> "\u5236\u56fe\u53f0";
            case "Grindstone", "Repair & Disenchant" -> "\u7802\u8f6e";
            case "Smithing Table" -> "\u953b\u9020\u53f0";
            case "Trading" -> "\u4ea4\u6613";
            default -> title;
        };
    }

    private boolean tryBundle(InventoryClickEvent event, org.bukkit.entity.Player player) {
        boolean currentIsShulker;
        boolean clickedTop;
        if (event.getClick() != ClickType.RIGHT) {
            return false;
        }
        boolean clickedBottom = event.getClickedInventory() == event.getView().getBottomInventory();
        boolean bl = clickedTop = event.getClickedInventory() == event.getView().getTopInventory();
        if (!clickedBottom && !clickedTop) {
            return false;
        }
        if (clickedTop) {
            Session session = this.sessions.get(player.getUniqueId());
            if (session != null && session.virtualInv != null && event.getClickedInventory().equals((Object)session.virtualInv)) {
                return false;
            }
            if (event.getView().getTopInventory().getType() == InventoryType.ENDER_CHEST) {
                return false;
            }
        }
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean cursorIsShulker = cursor != null && SHULKER_BOXES.contains(cursor.getType());
        boolean bl2 = currentIsShulker = current != null && SHULKER_BOXES.contains(current.getType());
        if (cursorIsShulker && current != null && !current.getType().isAir() && !SHULKER_BOXES.contains(current.getType())) {
            event.setCancelled(true);
            this.bundleInsert(cursor, current);
            if (clickedTop) {
                event.getClickedInventory().setItem(event.getSlot(), current);
            }
            return true;
        }
        if (cursorIsShulker && (current == null || current.getType().isAir())) {
            event.setCancelled(true);
            int slot = event.getSlot();
            Inventory targetInv = clickedBottom ? player.getInventory() : event.getClickedInventory();
            this.bundleExtract(player, cursor, (Inventory)targetInv, slot);
            return true;
        }
        if (!cursorIsShulker && currentIsShulker && cursor != null && !cursor.getType().isAir()) {
            event.setCancelled(true);
            this.bundleInsert(current, cursor);
            if (clickedTop) {
                event.getClickedInventory().setItem(event.getSlot(), current);
            }
            return true;
        }
        return false;
    }

    private void bundleInsert(ItemStack shulker, ItemStack toInsert) {
        ItemStack slot;
        int i;
        if (SHULKER_BOXES.contains(toInsert.getType())) {
            return;
        }
        ItemMeta meta = shulker.getItemMeta();
        if (!(meta instanceof BlockStateMeta)) {
            return;
        }
        BlockStateMeta bsm = (BlockStateMeta)meta;
        BlockState blockState = bsm.getBlockState();
        if (!(blockState instanceof ShulkerBox)) {
            return;
        }
        ShulkerBox box = (ShulkerBox)blockState;
        Inventory inv = box.getInventory();
        int remaining = toInsert.getAmount();
        for (i = 0; i < 27 && remaining > 0; ++i) {
            slot = inv.getItem(i);
            if (slot == null || !slot.isSimilar(toInsert)) continue;
            int canFit = slot.getMaxStackSize() - slot.getAmount();
            int add = Math.min(canFit, remaining);
            slot.setAmount(slot.getAmount() + add);
            remaining -= add;
        }
        for (i = 0; i < 27 && remaining > 0; ++i) {
            slot = inv.getItem(i);
            if (slot != null && !slot.getType().isAir()) continue;
            ItemStack newStack = toInsert.clone();
            newStack.setAmount(remaining);
            inv.setItem(i, newStack);
            remaining = 0;
        }
        bsm.setBlockState((BlockState)box);
        shulker.setItemMeta((ItemMeta)bsm);
        toInsert.setAmount(remaining);
    }

    private void bundleExtract(org.bukkit.entity.Player player, ItemStack shulker, Inventory targetInv, int targetSlot) {
        ItemMeta meta = shulker.getItemMeta();
        if (!(meta instanceof BlockStateMeta)) {
            return;
        }
        BlockStateMeta bsm = (BlockStateMeta)meta;
        BlockState blockState = bsm.getBlockState();
        if (!(blockState instanceof ShulkerBox)) {
            return;
        }
        ShulkerBox box = (ShulkerBox)blockState;
        Inventory inv = box.getInventory();
        ItemStack extracted = null;
        int fromSlot = -1;
        for (int i = 26; i >= 0; --i) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType().isAir()) continue;
            extracted = slot.clone();
            slot.setAmount(0);
            fromSlot = i;
            break;
        }
        if (extracted == null) {
            return;
        }
        bsm.setBlockState((BlockState)box);
        shulker.setItemMeta((ItemMeta)bsm);
        if (targetSlot < 0 || targetSlot >= targetInv.getSize()) {
            return;
        }
        targetInv.setItem(targetSlot, extracted);
    }

    private Inventory openShulkerGUI(org.bukkit.entity.Player player, String title, ItemStack sourceItem) {
        BlockStateMeta bsm;
        BlockState blockState;
        ItemMeta itemMeta;
        ServerPlayer sp = ((CraftPlayer)player).getHandle();
        SimpleContainer nms = new SimpleContainer(27);
        if (sourceItem.hasItemMeta() && (itemMeta = sourceItem.getItemMeta()) instanceof BlockStateMeta && (blockState = (bsm = (BlockStateMeta)itemMeta).getBlockState()) instanceof ShulkerBox) {
            ShulkerBox box = (ShulkerBox)blockState;
            ItemStack[] contents = box.getInventory().getContents();
            for (int i = 0; i < 27 && i < contents.length; ++i) {
                if (contents[i] == null || contents[i].getType().isAir()) continue;
                nms.setItem(i, CraftItemStack.asNMSCopy((ItemStack)contents[i]));
            }
        }
        sp.openMenu((MenuProvider)new SimpleMenuProvider((syncId, inv, p) -> new ShulkerBoxMenu(syncId, inv, (Container)nms), (Component)Component.literal((String)title)));
        return player.getOpenInventory().getTopInventory();
    }

    private void openItem(org.bukkit.entity.Player player, OpenableType type, ItemStack sourceItem, int hotbarSlot, EquipmentSlot hand) {
        UUID itemId = this.getOrCreateItemId(sourceItem);
        int srcSlot = hand == EquipmentSlot.HAND ? hotbarSlot : 40;
        player.getInventory().setItem(srcSlot, sourceItem);
        Inventory virtualInv = null;
        if (type == OpenableType.SHULKER) {
            String title = sourceItem.hasItemMeta() && sourceItem.getItemMeta().hasDisplayName() ? sourceItem.getItemMeta().getDisplayName() : "\u6f5c\u5f71\u76d2";
            virtualInv = this.openShulkerGUI(player, title, sourceItem);
            if (this.playSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
            }
        }
        Session session = new Session(type, hand, virtualInv, sourceItem, hotbarSlot, itemId);
        this.sessions.put(player.getUniqueId(), session);
        switch (type) {
            case WORKBENCH: {
                this.openNmsWorkbench(player);
                break;
            }
            case STONECUTTER: {
                this.openNmsStonecutter(player);
                break;
            }
            case ENDER_CHEST: {
                this.openEnderChest(player);
                if (!this.playSounds) break;
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (!this.isOpenable(item)) {
            return;
        }
        org.bukkit.entity.Player player = event.getPlayer();
        OpenableType type = this.getOpenableType(item);
        if (!this.checkPermissionAndCooldown(player)) {
            return;
        }
        event.setCancelled(true);
        EquipmentSlot hand = event.getHand();
        int hotbarSlot = hand == EquipmentSlot.HAND ? player.getInventory().getHeldItemSlot() : -1;
        Bukkit.getScheduler().runTask((Plugin)this, () -> this.openItem(player, type, item, hotbarSlot, hand));
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof org.bukkit.entity.Player)) {
            return;
        }
        org.bukkit.entity.Player player = (org.bukkit.entity.Player)humanEntity;
        Session closing = this.closingSessions.get(player.getUniqueId());
        if (closing != null && (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getAction() == InventoryAction.HOTBAR_SWAP)) {
            event.setCancelled(true);
            return;
        }
        if (this.enableBundleMode && this.tryBundle(event, player)) {
            return;
        }
        Session session = this.sessions.get(player.getUniqueId());
        if (session != null && session.virtualInv != null && event.getView().getTopInventory().equals((Object)session.virtualInv)) {
            this.handleClickInOurUI(event, player, session);
            return;
        }
        if (session != null && session.virtualInv == null) {
            this.handleClickInNmsUI(event, player, session);
            return;
        }
        if (this.enableNestedOpening && session == null && event.getView().getTopInventory() != null) {
            this.handleClickInVanillaContainer(event, player);
        }
    }

    private void handleClickInOurUI(InventoryClickEvent event, org.bukkit.entity.Player player, Session session) {
        ItemStack clickedItem;
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        if (clicked.equals((Object)event.getView().getBottomInventory())) {
            UUID clickedId;
            if (event.getSlot() == this.getSourceSlot(session)) {
                event.setCancelled(true);
                return;
            }
            clickedItem = event.getCurrentItem();
            if (clickedItem != null && (clickedId = this.getItemId(clickedItem)) != null && clickedId.equals(session.itemId)) {
                event.setCancelled(true);
                return;
            }
        }
        if (this.enableNestedOpening && clicked.equals((Object)event.getView().getBottomInventory()) && this.isOpenable(clickedItem = event.getCurrentItem()) && event.getCursor().getType().isAir() && event.getClick() == ClickType.RIGHT) {
            OpenableType newType = this.getOpenableType(clickedItem);
            if (newType == null) {
                return;
            }
            event.setCancelled(true);
            this.syncToSource(player, session);
            session.uiStack.push(new UIContext(session.type, session.virtualInv, session.sourceItem, session.hotbarSlot));
            int slot = this.findSlotInInventory(player, clickedItem);
            player.closeInventory();
            Bukkit.getScheduler().runTask((Plugin)this, () -> {
                Session newSession = this.createNestedSession(player, newType, clickedItem, slot, session);
                if (newSession == null) {
                    if (!session.uiStack.isEmpty()) {
                        session.uiStack.pop();
                    }
                    return;
                }
                this.openItemFromSession(player, newType, newSession, clickedItem, slot);
            });
            return;
        }
        if (session.type == OpenableType.SHULKER) {
            Bukkit.getScheduler().runTask((Plugin)this, () -> {
                Session s = this.sessions.get(player.getUniqueId());
                if (s != null) {
                    this.syncToSource(player, s);
                }
            });
        }
    }

    private void handleClickInNmsUI(InventoryClickEvent event, org.bukkit.entity.Player player, Session session) {
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            UUID clickedId;
            if (event.getSlot() == this.getSourceSlot(session)) {
                event.setCancelled(true);
                return;
            }
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && (clickedId = this.getItemId(clickedItem)) != null && clickedId.equals(session.itemId)) {
                event.setCancelled(true);
                return;
            }
            if (this.isOpenable(clickedItem) && event.getCursor().getType().isAir() && event.getClick() == ClickType.RIGHT) {
                OpenableType newType = this.getOpenableType(clickedItem);
                if (newType == null) {
                    return;
                }
                event.setCancelled(true);
                int slot = this.findSlotInInventory(player, clickedItem);
                Session newSession = this.createNestedSession(player, newType, clickedItem, slot, session);
                player.closeInventory();
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.openItemFromSession(player, newType, newSession, clickedItem, slot));
            }
        }
    }

    private Session createNestedSession(org.bukkit.entity.Player player, OpenableType type, ItemStack item, int slot, Session previousSession) {
        UUID itemId = this.getOrCreateItemId(item);
        Session newSession = new Session(type, previousSession.equipmentSlot, null, item, slot, itemId);
        newSession.uiStack = previousSession.uiStack;
        newSession.returnToPlayerInventory = previousSession.returnToPlayerInventory;
        return newSession;
    }

    private void openItemFromSession(org.bukkit.entity.Player player, OpenableType type, Session session, ItemStack sourceItem, int slot) {
        this.sessions.put(player.getUniqueId(), session);
        switch (type) {
            case SHULKER: {
                String title = sourceItem.hasItemMeta() && sourceItem.getItemMeta().hasDisplayName() ? sourceItem.getItemMeta().getDisplayName() : "\u6f5c\u5f71\u76d2";
                session.virtualInv = this.openShulkerGUI(player, title, sourceItem);
                if (!this.playSounds) break;
                player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
                break;
            }
            case WORKBENCH: {
                this.openNmsWorkbench(player);
                break;
            }
            case STONECUTTER: {
                this.openNmsStonecutter(player);
                break;
            }
            case ENDER_CHEST: {
                this.openEnderChest(player);
                if (!this.playSounds) break;
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
            }
        }
    }

    private void handleClickInVanillaContainer(InventoryClickEvent event, org.bukkit.entity.Player player) {
        if (event.getClickedInventory() != event.getView().getBottomInventory()) {
            return;
        }
        if (event.getClick() != ClickType.RIGHT) {
            return;
        }
        if (!event.getCursor().getType().isAir()) {
            return;
        }
        ItemStack targetItem = event.getCurrentItem();
        if (!this.isOpenable(targetItem)) {
            return;
        }
        if (!this.checkPermissionAndCooldown(player)) {
            return;
        }
        OpenableType type = this.getOpenableType(targetItem);
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        this.openItemFromInventoryView(player, type, targetItem, event.getSlot(), event.getView());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof org.bukkit.entity.Player)) {
            return;
        }
        org.bukkit.entity.Player player = (org.bukkit.entity.Player)humanEntity;
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null || session.type != OpenableType.SHULKER) {
            return;
        }
        if (session.virtualInv == null) {
            return;
        }
        if (!event.getInventory().equals((Object)session.virtualInv)) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            Session s = this.sessions.get(player.getUniqueId());
            if (s != null) {
                this.syncToSource(player, s);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof org.bukkit.entity.Player)) {
            return;
        }
        org.bukkit.entity.Player player = (org.bukkit.entity.Player)humanEntity;
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        switch (session.type) {
            case SHULKER: {
                if (session.virtualInv == null) {
                    return;
                }
                if (event.getInventory().equals((Object)session.virtualInv)) break;
                return;
            }
            case WORKBENCH: {
                if (event.getInventory().getType() == InventoryType.WORKBENCH) break;
                return;
            }
            case STONECUTTER: {
                if (event.getInventory().getType() == InventoryType.STONECUTTER) break;
                return;
            }
            case ENDER_CHEST: {
                if (event.getInventory().getType() == InventoryType.ENDER_CHEST) break;
                return;
            }
        }
        this.syncToSource(player, session);
        this.scheduleSourceIntegrityCheck(player, session, event.getInventory());
        UUID playerId = player.getUniqueId();
        this.closingSessions.put(playerId, session);
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.closingSessions.remove(playerId), 2L);
        if (!session.uiStack.isEmpty()) {
            UIContext prev = session.uiStack.pop();
            this.sessions.remove(playerId);
            if (prev.isVanilla) {
                Bukkit.getScheduler().runTask((Plugin)this, () -> {
                    try {
                        InventoryView restoredView = player.openInventory(prev.topInventory);
                        String localizedTitle = this.localizeDefaultGuiTitle(prev.title);
                        if (localizedTitle != null && !localizedTitle.equals(prev.title)) {
                            restoredView.setTitle(localizedTitle);
                        }
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                });
                return;
            }
            Session restored = new Session(prev.type, session.equipmentSlot, prev.topInventory, prev.sourceItem, prev.sourceSlot, this.getOrCreateItemId(prev.sourceItem));
            restored.uiStack = session.uiStack;
            restored.returnToPlayerInventory = session.returnToPlayerInventory;
            this.sessions.put(player.getUniqueId(), restored);
            Bukkit.getScheduler().runTask((Plugin)this, () -> {
                player.openInventory(prev.topInventory);
                if (this.playSounds && prev.type == OpenableType.SHULKER) {
                    player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
                }
            });
        } else {
            this.sessions.remove(playerId);
            if (session.type == OpenableType.SHULKER && this.playSounds) {
                player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 1.0f, 1.0f);
            }
            if (session.returnToPlayerInventory) {
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.openPlayerInventoryScreen(player));
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Session session = this.sessions.get(event.getPlayer().getUniqueId());
        if (session == null || session.equipmentSlot != EquipmentSlot.HAND) {
            return;
        }
        if (event.getNewSlot() == session.hotbarSlot) {
            return;
        }
        this.syncToSource(event.getPlayer(), session);
        event.getPlayer().closeInventory();
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (this.sessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Session session = this.sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        ItemStack source = this.findSourceItem(event.getPlayer(), session);
        if (source != null && event.getItemDrop().getItemStack().equals((Object)source)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.RED) + "You cannot drop this item while it's being used!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Session session = this.sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        if (this.isOpenable(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.RED) + "You cannot place this item while it's being used!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        Session session = this.sessions.remove(player.getUniqueId());
        if (session != null) {
            this.syncToSource(player, session);
        }
        this.cooldowns.remove(player.getUniqueId());
    }

    static {
        HashSet<Material> set = new HashSet<Material>();
        for (Material m : Material.values()) {
            String name = m.name();
            if (!name.endsWith("SHULKER_BOX")) continue;
            set.add(m);
        }
        SHULKER_BOXES = Collections.unmodifiableSet(set);
    }

    private class CleanupRunnable
    extends BukkitRunnable {
        private CleanupRunnable() {
        }

        public void run() {
            Iterator<Map.Entry<UUID, Session>> it = ShulkerPlus.this.sessions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Session> entry = it.next();
                UUID pid = entry.getKey();
                Session session = entry.getValue();
                org.bukkit.entity.Player player = Bukkit.getPlayer((UUID)pid);
                if (player == null || !player.isOnline()) {
                    this.syncToSourceDangling(pid, session);
                    it.remove();
                    ShulkerPlus.this.cooldowns.remove(pid);
                    continue;
                }
                InventoryView currentView = player.getOpenInventory();
                if (currentView != null && currentView.getTopInventory() != null && (session.virtualInv == null || currentView.getTopInventory().equals((Object)session.virtualInv))) continue;
                ShulkerPlus.this.syncToSource(player, session);
                it.remove();
            }
        }

        private void syncToSourceDangling(UUID pid, Session s) {
            org.bukkit.entity.Player p = Bukkit.getPlayer((UUID)pid);
            if (p == null) {
                return;
            }
            ShulkerPlus.this.syncToSource(p, s);
        }
    }
}

