package com.skcraft.plume.module.inventory;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.inventory.Items;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "modify-item", desc = "Commands to allow easy modifying of item properties")
public class ModifyItem {

    @Command(aliases = "name", desc = "Change the display name of an item")
    @Group(@At("modifyitem"))
    @Require("plume.modifyitem.name")
    public void name(@Sender EntityPlayerMP player, String value) {
        ItemStack item = player.getHeldItem();
        if (item != null) {
            NBTTagCompound display = Items.getOrCreateTagCompound(item).getCompoundTag("display");
            display.setString("Name", value);
            item.setTagInfo("display", display);
            player.addChatMessage(Messages.info(tr("modifyItem.itemModified")));
        } else {
            player.addChatMessage(Messages.error(tr("notHoldingItem")));
        }
    }

    @Command(aliases = "lore", desc = "Change the lore of an item")
    @Group(@At("modifyitem"))
    @Require("plume.modifyitem.lore")
    public void lore(@Sender EntityPlayerMP player, String value) {
        ItemStack item = player.getHeldItem();
        if (item != null) {
            NBTTagCompound display = Items.getOrCreateTagCompound(item).getCompoundTag("display");
            NBTTagList lore = new NBTTagList();
            for (String line : value.split("\\|")) {
                lore.appendTag(new NBTTagString(line));
            }
            display.setTag("Lore", lore);
            item.setTagInfo("display", display);
            player.addChatMessage(Messages.info(tr("modifyItem.itemModified")));
        } else {
            player.addChatMessage(Messages.error(tr("notHoldingItem")));
        }
    }

}
