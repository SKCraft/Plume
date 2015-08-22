package com.skcraft.plume.module.claim;

import lombok.Getter;
import net.minecraft.item.ItemStack;

import java.util.List;

public class TransactionException extends Exception {

    @Getter
    private final List<ItemStack> removed;

    public TransactionException(Throwable cause, List<ItemStack> removed) {
        super(cause);
        this.removed = removed;
    }

}
