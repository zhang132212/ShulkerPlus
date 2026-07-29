/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 */
package me.zhang132212.shulkerplus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import me.zhang132212.shulkerplus.OpenableType;
import me.zhang132212.shulkerplus.UIContext;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Session {
    final OpenableType type;
    final EquipmentSlot equipmentSlot;
    Inventory virtualInv;
    final ItemStack sourceItem;
    final int hotbarSlot;
    final UUID itemId;
    Deque<UIContext> uiStack = new ArrayDeque<UIContext>();
    boolean returnToPlayerInventory;

    Session(OpenableType type, EquipmentSlot equipmentSlot, Inventory virtualInv, ItemStack sourceItem, int hotbarSlot, UUID itemId) {
        this.type = type;
        this.equipmentSlot = equipmentSlot;
        this.virtualInv = virtualInv;
        this.sourceItem = sourceItem;
        this.hotbarSlot = hotbarSlot;
        this.itemId = itemId;
    }
}

