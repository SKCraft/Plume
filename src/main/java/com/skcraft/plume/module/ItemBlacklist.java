package com.skcraft.plume.module;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.List;

@Module(name = "item-blacklist", desc = "Simple blacklisting of items")
public class ItemBlacklist {

    @InjectConfig("item_blacklist") private Config<ItemBanConfig> config;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.worldObj.isRemote) return;

        if(event.phase == TickEvent.Phase.END && event.side == Side.SERVER) {
            for(int i = 0; i < event.player.inventory.mainInventory.length; i++) {
                ItemStack stack = event.player.inventory.mainInventory[i];
                if(stack != null) {
                    for (String bannedItem : config.get().bannedItems) {
                        if (Item.itemRegistry.getNameForObject(stack.getItem()).equals(bannedItem)) {
                            event.player.addChatMessage(Messages.error(stack.getDisplayName() + " is forbidden. Removing..."));
                            event.player.inventory.setInventorySlotContents(i, null);

                            Slot slot = event.player.openContainer.getSlotFromInventory(event.player.inventory, i);
                            ((EntityPlayerMP) event.player).playerNetServerHandler.sendPacket(new S2FPacketSetSlot(event.player.openContainer.windowId, slot.slotNumber, null));
                        }
                    }
                }
            }
        }
    }

    private static class ItemBanConfig {
        @Setting(value = "bannedItems", comment = "List of banned items/blocks that will be removed from player's inventories. Format: modId:itemName")
        private List<String> bannedItems = Lists.newArrayList("");
    }
}
