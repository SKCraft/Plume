package com.skcraft.plume.util.inventory;

import lombok.Getter;
import net.minecraft.item.ItemStack;

import static com.google.common.base.Preconditions.checkNotNull;

public class TypeMatcher implements SingleItemMatcher {

    @Getter
    private final ItemStack itemStack;

    public TypeMatcher(ItemStack itemStack) {
        checkNotNull(itemStack, "stack");
        this.itemStack = itemStack;
    }

    @Override
    public boolean apply(ItemStack input) {
        return input.getItem() == itemStack.getItem();
    }

}
