package me.zhang132212.shulkerplus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ShulkerPlus extends JavaPlugin implements Listener, PluginMessageListener {

    private NamespacedKey itemKey;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private BukkitTask cleanupTask;

    private boolean requireSneak;
    private boolean playSounds;
    private boolean enableWorkbench;
    private boolean enableStonecutter;
    private boolean enableNestedOpening;
    private boolean enableBundleMode;
    private boolean closeOnMove;
    private long cooldownMs;

    private static final Set<Material> SHULKER_BOXES;
    static {
        Set<Material> set = new HashSet<>();
        for (Material m : Material.values()) {
            String name = m.name();
            if (name.endsWith("SHULKER_BOX")) {
                set.add(m);
            }
        }
        SHULKER_BOXES = Collections.unmodifiableSet(set);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        itemKey = new NamespacedKey(this, "shulkerplus_uid");
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "quickshulker:open_shulker_packet");
        getServer().getMessenger().registerIncomingPluginChannel(this, "quickshulker:open_shulker_packet", this);

        cleanupTask = new CleanupRunnable().runTaskTimer(this, 20L, 20L);

        getLogger().info("ShulkerPlus enabled!");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        switch (channel) {
            case "quickshulker:open_shulker_packet":
                handleOpenPacket(player, message);
                break;
        }
    }

    private void handleOpenPacket(Player player, byte[] message) {
        int rawSlot = java.nio.ByteBuffer.wrap(message).getInt();

        int invIndex = (rawSlot >= 36) ? rawSlot - 36 : rawSlot;
        if (invIndex < 0 || invIndex >= 36) return;

        ItemStack item = player.getInventory().getItem(invIndex);
        if (!isOpenable(item)) return;
        if (item.getAmount() != 1) return;

        OpenableType type = getOpenableType(item);
        if (type == null) return;

        Session existing = sessions.get(player.getUniqueId());
        if (existing != null) {
            syncToSource(player, existing);
        }

        openItem(player, type, item, invIndex, EquipmentSlot.HAND);
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) cleanupTask.cancel();
        for (UUID pid : new HashSet<>(sessions.keySet())) {
            Session s = sessions.get(pid);
            Player p = Bukkit.getPlayer(pid);
            if (p != null && s != null) {
                syncToSource(p, s);
                p.closeInventory();
            }
        }
        sessions.clear();
        cooldowns.clear();
    }

    private void loadConfig() {
        requireSneak = getConfig().getBoolean("require-sneak", true);
        playSounds = getConfig().getBoolean("play-sounds", true);
        closeOnMove = getConfig().getBoolean("close-on-move", true);
        cooldownMs = getConfig().getLong("cooldown-ms", 500L);
        enableWorkbench = getConfig().getBoolean("enable-workbench", true);
        enableStonecutter = getConfig().getBoolean("enable-stonecutter", true);
        enableNestedOpening = getConfig().getBoolean("enable-nested-opening", true);
        enableBundleMode = getConfig().getBoolean("enable-bundle-mode", false);
    }

    // ─── Helpers ───────────────────────────────────────────────

    private boolean isOpenable(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material type = item.getType();
        if (SHULKER_BOXES.contains(type)) return true;
        if (enableWorkbench && type == Material.CRAFTING_TABLE) return true;
        if (enableStonecutter && type == Material.STONECUTTER) return true;
        return false;
    }

    private OpenableType getOpenableType(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        Material type = item.getType();
        if (SHULKER_BOXES.contains(type)) return OpenableType.SHULKER;
        if (type == Material.CRAFTING_TABLE) return OpenableType.WORKBENCH;
        if (type == Material.STONECUTTER) return OpenableType.STONECUTTER;
        return null;
    }

    private void syncShulkerItems(ItemStack item, Inventory virtualInv) {
        if (item == null || virtualInv == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof ShulkerBox box) {
                box.getInventory().setContents(virtualInv.getContents());
                bsm.setBlockState(box);
                item.setItemMeta(bsm);
            }
        }
    }

    private void syncToSource(Player player, Session session) {
        if (session.type != OpenableType.SHULKER || session.virtualInv == null) return;
        ItemStack current = findSourceItem(player, session);
        if (current == null) return;
        syncShulkerItems(current, session.virtualInv);
    }

    private ItemStack findSourceItem(Player player, Session session) {
        PlayerInventory inv = player.getInventory();
        if (session.equipmentSlot == EquipmentSlot.HAND) {
            return inv.getItem(session.hotbarSlot);
        }
        return inv.getItemInOffHand();
    }

    private int findSlotInInventory(Player player, ItemStack target) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private UUID getOrCreateItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return UUID.randomUUID();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(itemKey, PersistentDataType.STRING)) {
            try {
                return UUID.fromString(pdc.get(itemKey, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignored) {}
        }
        UUID id = UUID.randomUUID();
        pdc.set(itemKey, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
        return id;
    }

    private boolean checkPermissionAndCooldown(Player player) {
        if (!player.hasPermission("shulkerplus.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use ShulkerPlus.");
            return false;
        }
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && System.currentTimeMillis() - last < cooldownMs) {
            return false;
        }
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    // ─── NMS openers ───────────────────────────────────────────

    private void openNmsWorkbench(Player player) {
        ServerPlayer sp = ((CraftPlayer) player).getHandle();
        ContainerLevelAccess access = ContainerLevelAccess.create(sp.level(), sp.blockPosition());
        sp.openMenu(new SimpleMenuProvider(
            (syncId, inv, p) -> new CraftingMenu(syncId, inv, access) {
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                    return !p.isRemoved();
                }
            },
            Component.translatable("container.crafting")
        ));
    }

    private void openNmsStonecutter(Player player) {
        ServerPlayer sp = ((CraftPlayer) player).getHandle();
        ContainerLevelAccess access = ContainerLevelAccess.create(sp.level(), sp.blockPosition());
        sp.openMenu(new SimpleMenuProvider(
            (syncId, inv, p) -> new StonecutterMenu(syncId, inv, access) {
                @Override
                public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                    return !p.isRemoved();
                }
            },
            Component.translatable("container.stonecutter")
        ));
    }

    // ─── Bundle logic ───────────────────────────────────────────

    private boolean tryBundle(InventoryClickEvent event, Player player) {
        if (event.getClick() != ClickType.RIGHT) return false;
        if (event.getClickedInventory() != event.getView().getBottomInventory()) return false;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        boolean cursorIsShulker = cursor != null && SHULKER_BOXES.contains(cursor.getType());
        boolean currentIsShulker = current != null && SHULKER_BOXES.contains(current.getType());

        // Insert: cursor has shulker, clicked slot has non-shulker item
        if (cursorIsShulker && current != null && !current.getType().isAir()
                && !SHULKER_BOXES.contains(current.getType())) {
            event.setCancelled(true);
            bundleInsert(cursor, current);
            return true;
        }
        // Extract: cursor has shulker, clicked slot is empty
        if (cursorIsShulker && (current == null || current.getType().isAir())) {
            event.setCancelled(true);
            int slot = event.getSlot();
            bundleExtract(player, cursor, slot);
            return true;
        }
        // Insert reverse: cursor has item, clicked slot is shulker
        if (!cursorIsShulker && currentIsShulker && cursor != null && !cursor.getType().isAir()) {
            event.setCancelled(true);
            bundleInsert(current, cursor);
            return true;
        }
        return false;
    }

    private void bundleInsert(ItemStack shulker, ItemStack toInsert) {
        if (SHULKER_BOXES.contains(toInsert.getType())) return;
        ItemMeta meta = shulker.getItemMeta();
        if (!(meta instanceof BlockStateMeta bsm)) return;
        if (!(bsm.getBlockState() instanceof ShulkerBox box)) return;
        Inventory inv = box.getInventory();

        int remaining = toInsert.getAmount();
        // Merge into existing stacks
        for (int i = 0; i < 27 && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot != null && slot.isSimilar(toInsert)) {
                int canFit = slot.getMaxStackSize() - slot.getAmount();
                int add = Math.min(canFit, remaining);
                slot.setAmount(slot.getAmount() + add);
                remaining -= add;
            }
        }
        // Fill empty slots
        for (int i = 0; i < 27 && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || slot.getType().isAir()) {
                ItemStack newStack = toInsert.clone();
                newStack.setAmount(remaining);
                inv.setItem(i, newStack);
                remaining = 0;
            }
        }

        bsm.setBlockState(box);
        shulker.setItemMeta(bsm);
        toInsert.setAmount(remaining);
    }

    private void bundleExtract(Player player, ItemStack shulker, int targetSlot) {
        ItemMeta meta = shulker.getItemMeta();
        if (!(meta instanceof BlockStateMeta bsm)) return;
        if (!(bsm.getBlockState() instanceof ShulkerBox box)) return;
        Inventory inv = box.getInventory();

        // Find last non-empty slot
        ItemStack extracted = null;
        int fromSlot = -1;
        for (int i = 26; i >= 0; i--) {
            ItemStack slot = inv.getItem(i);
            if (slot != null && !slot.getType().isAir()) {
                extracted = slot.clone();
                slot.setAmount(0);
                fromSlot = i;
                break;
            }
        }

        if (extracted == null) return;

        bsm.setBlockState(box);
        shulker.setItemMeta(bsm);

        if (targetSlot < 0 || targetSlot >= 36) return;
        player.getInventory().setItem(targetSlot, extracted);
    }

    // ─── Unified open entry ─────────────────────────────────────

    /**
     * Opens the item for the player. Routes to virtual inv (shulker)
     * or NMS menu (workbench/stonecutter).
     */
    private void openItem(Player player, OpenableType type, ItemStack sourceItem,
                          int hotbarSlot, EquipmentSlot hand) {
        UUID itemId = getOrCreateItemId(sourceItem);

        Inventory virtualInv = null;
        if (type == OpenableType.SHULKER) {
            virtualInv = Bukkit.createInventory(null, 27, sourceItem.hasItemMeta()
                && sourceItem.getItemMeta().hasDisplayName()
                ? sourceItem.getItemMeta().getDisplayName() : "Shulker Box");
            if (sourceItem.hasItemMeta() && sourceItem.getItemMeta() instanceof BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof ShulkerBox box) {
                    virtualInv.setContents(box.getInventory().getContents());
                }
            }
        }

        Session session = new Session(type, hand, virtualInv, sourceItem, hotbarSlot, itemId);
        sessions.put(player.getUniqueId(), session);

        switch (type) {
            case SHULKER:
                player.openInventory(virtualInv);
                if (playSounds) {
                    player.playSound(player.getLocation(),
                        Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1f);
                }
                break;
            case WORKBENCH:
                openNmsWorkbench(player);
                break;
            case STONECUTTER:
                openNmsStonecutter(player);
                break;
        }
    }

    // ─── Events ────────────────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null) return;
        ItemStack item = event.getItem();
        if (!isOpenable(item)) return;

        Player player = event.getPlayer();
        if (!checkPermissionAndCooldown(player)) return;
        if (requireSneak && !player.isSneaking()) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        if (action == Action.RIGHT_CLICK_BLOCK) {
            Material clicked = event.getClickedBlock().getType();
            if (clicked.isInteractable()) return;
        }

        event.setCancelled(true);
        OpenableType type = getOpenableType(item);
        EquipmentSlot hand = event.getHand();
        int hotbarSlot = (hand == EquipmentSlot.HAND) ? player.getInventory().getHeldItemSlot() : -1;

        Bukkit.getScheduler().runTask(this, () ->
            openItem(player, type, item, hotbarSlot, hand));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (enableBundleMode && tryBundle(event, player)) return;

        Session session = sessions.get(player.getUniqueId());

        // CASE 1: Our virtual shulker UI
        if (session != null && session.virtualInv != null
                && event.getView().getTopInventory().equals(session.virtualInv)) {
            handleClickInOurUI(event, player, session);
            return;
        }

        // CASE 2: NMS menu (workbench/stonecutter)
        if (session != null && session.virtualInv == null) {
            handleClickInNmsUI(event, player, session);
            return;
        }

        // CASE 3: Vanilla container → nested open
        if (enableNestedOpening && session == null && event.getView().getTopInventory() != null) {
            handleClickInVanillaContainer(event, player);
        }
    }

    private void handleClickInOurUI(InventoryClickEvent event, Player player, Session session) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        // Prevent shulker-into-shulker
        if (session.type == OpenableType.SHULKER && clicked.equals(session.virtualInv)) {
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            if ((cursor != null && SHULKER_BOXES.contains(cursor.getType())) ||
                (current != null && SHULKER_BOXES.contains(current.getType()))) {
                if (event.getAction() == InventoryAction.PLACE_ALL ||
                    event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.PLACE_SOME ||
                    event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Nested open
        if (enableNestedOpening && clicked.equals(event.getView().getBottomInventory())) {
            ItemStack clickedItem = event.getCurrentItem();
            if (isOpenable(clickedItem) && event.getCursor().getType().isAir()
                    && event.getClick() == ClickType.RIGHT) {
                OpenableType newType = getOpenableType(clickedItem);
                if (newType == null) return;

                event.setCancelled(true);
                syncToSource(player, session);

                session.uiStack.push(new UIContext(
                    session.type, session.virtualInv, session.sourceItem, session.hotbarSlot));

                int slot = findSlotInInventory(player, clickedItem);

                // Route through scheduler for clean close-open transition
                player.closeInventory();
                Bukkit.getScheduler().runTask(this, () -> {
                    Session newSession = createNestedSession(
                        player, newType, clickedItem, slot, session);
                    if (newSession == null) {
                        // Fallback: pop stack and restore
                        if (!session.uiStack.isEmpty()) session.uiStack.pop();
                        return;
                    }
                    openItemFromSession(player, newType, newSession, clickedItem, slot);
                });
                return;
            }
        }

        // Real-time sync for shulker
        if (session.type == OpenableType.SHULKER) {
            Bukkit.getScheduler().runTask(this, () -> {
                Session s = sessions.get(player.getUniqueId());
                if (s != null) syncToSource(player, s);
            });
        }
    }

    private void handleClickInNmsUI(InventoryClickEvent event, Player player, Session session) {
        // NMS menus (workbench/stonecutter) handle crafting natively.
        // We only prevent clicking the inventory slot that holds the source item.
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (isOpenable(clickedItem) && event.getCursor().getType().isAir()
                    && event.getClick() == ClickType.RIGHT) {
                OpenableType newType = getOpenableType(clickedItem);
                if (newType == null) return;

                event.setCancelled(true);
                int slot = findSlotInInventory(player, clickedItem);
                UUID itemId = getOrCreateItemId(clickedItem);
                Session newSession = new Session(newType, session.equipmentSlot,
                    null, clickedItem, slot, itemId);
                newSession.uiStack = session.uiStack;

                player.closeInventory();
                Bukkit.getScheduler().runTask(this, () -> {
                    openItemFromSession(player, newType, newSession, clickedItem, slot);
                });
            }
        }
    }

    private Session createNestedSession(Player player, OpenableType type, ItemStack item,
                                         int slot, Session previousSession) {
        UUID itemId = getOrCreateItemId(item);
        Inventory virtualInv = null;
        if (type == OpenableType.SHULKER) {
            virtualInv = Bukkit.createInventory(null, 27,
                item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().getDisplayName() : "Shulker Box");
            if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta bsm) {
                if (bsm.getBlockState() instanceof ShulkerBox box) {
                    virtualInv.setContents(box.getInventory().getContents());
                }
            }
        }
        Session newSession = new Session(type, previousSession.equipmentSlot,
            virtualInv, item, slot, itemId);
        newSession.uiStack = previousSession.uiStack;
        return newSession;
    }

    private void openItemFromSession(Player player, OpenableType type,
                                      Session session, ItemStack sourceItem, int slot) {
        sessions.put(player.getUniqueId(), session);
        switch (type) {
            case SHULKER:
                player.openInventory(session.virtualInv);
                if (playSounds) {
                    player.playSound(player.getLocation(),
                        Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1f);
                }
                break;
            case WORKBENCH:
                openNmsWorkbench(player);
                break;
            case STONECUTTER:
                openNmsStonecutter(player);
                break;
        }
    }

    private void handleClickInVanillaContainer(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() != event.getView().getBottomInventory()) return;

        ItemStack targetItem = null;

        if (event.getClick() == ClickType.RIGHT) {
            if (event.getCursor().getType().isAir() && isOpenable(event.getCurrentItem())) {
                targetItem = event.getCurrentItem();
            } else if (SHULKER_BOXES.contains(event.getCursor().getType())
                    && (event.getAction() == InventoryAction.PICKUP_HALF
                        || event.getAction() == InventoryAction.PICKUP_ALL)) {
                targetItem = event.getCursor();
            }
        }

        if (targetItem == null) return;
        if (!checkPermissionAndCooldown(player)) return;

        OpenableType type = getOpenableType(targetItem);
        if (type == null) return;

        event.setCancelled(true);

        Inventory top = event.getView().getTopInventory();
        boolean isPlayerOwnInventory = (top.getType() == InventoryType.CRAFTING);

        ItemStack finalItem = targetItem;

        if (isPlayerOwnInventory) {
            int slot = findSlotInInventory(player, finalItem);
            int hotbarSlot = (slot >= 0 && slot <= 8) ? slot :
                player.getInventory().getHeldItemSlot();
            player.closeInventory();
            Bukkit.getScheduler().runTask(this, () ->
                openItem(player, type, finalItem, hotbarSlot, EquipmentSlot.HAND));
        } else {
            InventoryView vanillaView = event.getView();
            int slot = findSlotInInventory(player, finalItem);
            EquipmentSlot hand = (slot >= 0 && slot <= 8) ? EquipmentSlot.HAND :
                (slot == 40 ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND);
            int hotbarSlot = (slot >= 0 && slot <= 8) ? slot :
                player.getInventory().getHeldItemSlot();
            player.closeInventory();
            Bukkit.getScheduler().runTask(this, () -> {
                UUID itemId = getOrCreateItemId(finalItem);
                Inventory virtualInv = null;
                if (type == OpenableType.SHULKER) {
                    virtualInv = Bukkit.createInventory(null, 27,
                        finalItem.hasItemMeta() && finalItem.getItemMeta().hasDisplayName()
                            ? finalItem.getItemMeta().getDisplayName() : "Shulker Box");
                    if (finalItem.hasItemMeta() && finalItem.getItemMeta() instanceof BlockStateMeta bsm) {
                        if (bsm.getBlockState() instanceof ShulkerBox box) {
                            virtualInv.setContents(box.getInventory().getContents());
                        }
                    }
                }
                Session session = new Session(type, hand, virtualInv, finalItem,
                    hotbarSlot, itemId);
                session.uiStack.push(new UIContext(vanillaView));
                sessions.put(player.getUniqueId(), session);
                if (type == OpenableType.SHULKER) {
                    player.openInventory(virtualInv);
                    if (playSounds) {
                        player.playSound(player.getLocation(),
                            Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1f);
                    }
                } else if (type == OpenableType.WORKBENCH) {
                    openNmsWorkbench(player);
                } else if (type == OpenableType.STONECUTTER) {
                    openNmsStonecutter(player);
                }
            });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.type != OpenableType.SHULKER) return;
        if (session.virtualInv == null) return;
        if (!event.getInventory().equals(session.virtualInv)) return;

        Bukkit.getScheduler().runTask(this, () -> {
            Session s = sessions.get(player.getUniqueId());
            if (s != null) syncToSource(player, s);
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        // Match close against the correct inventory
        switch (session.type) {
            case SHULKER:
                if (session.virtualInv == null) return;
                if (!event.getInventory().equals(session.virtualInv)) return;
                break;
            case WORKBENCH:
                if (event.getInventory().getType() != InventoryType.WORKBENCH) return;
                break;
            case STONECUTTER:
                if (event.getInventory().getType() != InventoryType.STONECUTTER) return;
                break;
        }

        syncToSource(player, session);

        if (!session.uiStack.isEmpty()) {
            UIContext prev = session.uiStack.pop();
            sessions.remove(player.getUniqueId());

            if (prev.isVanilla) {
                Bukkit.getScheduler().runTask(this, () -> {
                    try {
                        player.openInventory(prev.topInventory);
                    } catch (Exception ignored) {}
                });
                return;
            }

            Session restored = new Session(prev.type, session.equipmentSlot,
                prev.topInventory, prev.sourceItem, prev.sourceSlot,
                getOrCreateItemId(prev.sourceItem));
            restored.uiStack = session.uiStack;
            sessions.put(player.getUniqueId(), restored);

            Bukkit.getScheduler().runTask(this, () -> {
                player.openInventory(prev.topInventory);
                if (playSounds && prev.type == OpenableType.SHULKER) {
                    player.playSound(player.getLocation(),
                        Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1f);
                }
            });
        } else {
            sessions.remove(player.getUniqueId());
            if (session.type == OpenableType.SHULKER && playSounds) {
                player.playSound(player.getLocation(),
                    Sound.BLOCK_SHULKER_BOX_CLOSE, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || session.equipmentSlot != EquipmentSlot.HAND) return;
        if (event.getNewSlot() == session.hotbarSlot) return;

        syncToSource(event.getPlayer(), session);
        event.getPlayer().closeInventory();
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (sessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!closeOnMove) return;
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        if (System.currentTimeMillis() - session.openedAt < 200) return;
        if (event.getTo() == null) return;
        if (event.getFrom().distanceSquared(event.getTo()) <= 0) return;

        syncToSource(event.getPlayer(), session);
        event.getPlayer().closeInventory();
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        ItemStack source = findSourceItem(event.getPlayer(), session);
        if (source != null && event.getItemDrop().getItemStack().equals(source)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED +
                "You cannot drop this item while it's being used!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        if (isOpenable(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED +
                "You cannot place this item while it's being used!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) {
            syncToSource(player, session);
        }
        cooldowns.remove(player.getUniqueId());
    }

    // ─── Cleanup task ──────────────────────────────────────────

    private class CleanupRunnable extends org.bukkit.scheduler.BukkitRunnable {
        @Override
        public void run() {
            for (Iterator<Map.Entry<UUID, Session>> it = sessions.entrySet().iterator(); it.hasNext();) {
                Map.Entry<UUID, Session> entry = it.next();
                UUID pid = entry.getKey();
                Session session = entry.getValue();
                Player player = Bukkit.getPlayer(pid);

                if (player == null || !player.isOnline()) {
                    syncToSourceDangling(pid, session);
                    it.remove();
                    cooldowns.remove(pid);
                    continue;
                }

                InventoryView currentView = player.getOpenInventory();
                if (currentView == null || currentView.getTopInventory() == null ||
                    (session.virtualInv != null
                        && !currentView.getTopInventory().equals(session.virtualInv))) {
                    syncToSource(player, session);
                    it.remove();
                }
            }
        }

        private void syncToSourceDangling(UUID pid, Session s) {
            Player p = Bukkit.getPlayer(pid);
            if (p == null) return;
            syncToSource(p, s);
        }
    }
}
