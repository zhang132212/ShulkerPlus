package me.zhang132212.shulkerplus;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Tracks an active open-able session for a player.
 */
public class Session {
    final OpenableType type;
    final EquipmentSlot equipmentSlot;
    final Inventory virtualInv;
    final ItemStack sourceItem;
    final int hotbarSlot;
    final UUID itemId;
    final long openedAt = System.currentTimeMillis();
    Deque<UIContext> uiStack = new ArrayDeque<>();

    Session(OpenableType type, EquipmentSlot equipmentSlot, Inventory virtualInv,
            ItemStack sourceItem, int hotbarSlot, UUID itemId) {
        this.type = type;
        this.equipmentSlot = equipmentSlot;
        this.virtualInv = virtualInv;
        this.sourceItem = sourceItem;
        this.hotbarSlot = hotbarSlot;
        this.itemId = itemId;
    }
}
