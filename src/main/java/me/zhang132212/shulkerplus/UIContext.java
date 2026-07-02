package me.zhang132212.shulkerplus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a saved UI context for nested opening.
 * Can represent either our virtual inventory or a vanilla container.
 */
public class UIContext {
    final OpenableType type;
    final Inventory topInventory;
    final ItemStack sourceItem;
    final int sourceSlot;
    final boolean isVanilla;

    UIContext(OpenableType type, Inventory topInventory, ItemStack sourceItem, int sourceSlot) {
        this.type = type;
        this.topInventory = topInventory;
        this.sourceItem = sourceItem;
        this.sourceSlot = sourceSlot;
        this.isVanilla = false;
    }

    UIContext(InventoryView vanillaView) {
        this.type = null;
        this.topInventory = vanillaView.getTopInventory();
        this.sourceItem = null;
        this.sourceSlot = -1;
        this.isVanilla = true;
    }
}
