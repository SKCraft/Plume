package com.skcraft.plume.module.inventory;

import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.inventory.Inventories;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import java.util.Set;
import java.util.stream.Collectors;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "give-item", desc = "Commands to give items to players")
public class GiveItem {

    @Command(aliases = "giveinven", desc = "Gives items from a virtual inventory")
    @Require("plume.giveitem")
    public void giveInven(@Sender EntityPlayer sender, Set<EntityPlayer> targets) {
        Set<GameProfile> profiles = targets.stream().map(EntityPlayer::getGameProfile).collect(Collectors.toSet());
        GiveInventory inventory = new GiveInventory(tr("giveItem.giveInvenTitle"), sender, profiles);
        sender.displayGUIChest(inventory);
    }

    private static class GiveInventory extends InventoryBasic {
        private final EntityPlayer sender;
        private final Set<GameProfile> targets;

        public GiveInventory(String name, EntityPlayer sender, Set<GameProfile> targets) {
            super(name, true, 54);
            this.sender = sender;
            this.targets = targets;
        }

        @Override
        public void closeInventory() {
            super.closeInventory();

            Set<EntityPlayer> receivedItems = Sets.newHashSet();

            for (EntityPlayer player : Server.getOnlinePlayers()) {
                if (targets.contains(player.getGameProfile())) {
                    receivedItems.add(player);

                    for (int i = 0; i < getSizeInventory(); i++) {
                        ItemStack item = getStackInSlot(i);
                        if (item != null) {
                            Inventories.giveItem(player, item.copy());
                        }
                    }
                }
            }

            sender.addChatMessage(Messages.info(tr("giveItem.itemsGivenOut", receivedItems.size())));
            for (EntityPlayer target : receivedItems) {
                target.addChatMessage(Messages.info(tr("giveItem.receivedItemsFrom", sender.getCommandSenderName())));
            }
        }
    }

}
