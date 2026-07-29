/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemStack
 */
package me.zhang132212.shulkerplus;

import me.zhang132212.shulkerplus.OpenableType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class UIContext {
    final OpenableType type;
    final Inventory topInventory;
    final ItemStack sourceItem;
    final int sourceSlot;
    final boolean isVanilla;
    final String title;

    UIContext(OpenableType type, Inventory topInventory, ItemStack sourceItem, int sourceSlot) {
        this.type = type;
        this.topInventory = topInventory;
        this.sourceItem = sourceItem;
        this.sourceSlot = sourceSlot;
        this.isVanilla = false;
        this.title = null;
    }

    UIContext(InventoryView vanillaView) {
        this.type = null;
        this.topInventory = vanillaView.getTopInventory();
        this.sourceItem = null;
        this.sourceSlot = -1;
        this.isVanilla = true;
        this.title = vanillaView.getTitle();
    }
}

