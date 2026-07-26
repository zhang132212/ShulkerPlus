package me.zhang132212.shulkerplus;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Intrinsic storage backing for an item-opened shulker box.
 *
 * <p>Vanilla slots delegate placement checks to this container, so normal
 * clicks, quick-move, number-key swaps and quick-craft all share the same
 * no-nesting rule without Bukkit click or drag event cancellation.</p>
 */
final class NonNestingShulkerContainer extends SimpleContainer {
    private static final int SHULKER_SIZE = 27;

    NonNestingShulkerContainer() {
        super(SHULKER_SIZE);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock);
    }
}
