package com.skcraft.plume.util.config;

import com.google.common.reflect.TypeToken;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.Map;

public class ItemStackTypeSerializer implements TypeSerializer<ItemStack> {

    @Override
    public ItemStack deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
        String name;
        byte damage = 0;
        int amount = 1;

        if (node.hasMapChildren()) {
            Map<Object, ? extends ConfigurationNode> map = node.getChildrenMap();
            if (map.containsKey("name")) {
                name = map.get("name").getString();
            } else {
                throw new ObjectMappingException("No item name specified");
            }

            if (map.containsKey("amount")) {
                amount = map.get("amount").getInt();
                if (amount < 1) {
                    throw new ObjectMappingException("Stack size " + amount + " is less than 1");
                }
            }

            if (map.containsKey("damage")) {
                damage = (byte) map.get("damage").getInt();
                if (damage < 0) {
                    throw new ObjectMappingException("Damage " + damage + " is less than 0");
                }
                if (damage > 15) {
                    throw new ObjectMappingException("Damage " + damage + " is greater than 15");
                }
            }
        } else {
            name = node.getString();
        }

        Item item = (Item) Item.itemRegistry.getObject(new ResourceLocation(name));
        if (item != null) {
            return new ItemStack(item, amount, damage);
        } else {
            throw new ObjectMappingException("No such item as " + name);
        }
    }

    @Override
    public void serialize(TypeToken<?> type, ItemStack obj, ConfigurationNode value) throws ObjectMappingException {
        String name = Item.itemRegistry.getNameForObject(obj.getItem()).toString();
        int damage = obj.getItemDamage();
        int amount = obj.stackSize;
        if (damage == 0 && amount == 1) {
            value.setValue(name);
        } else {
            value.getNode("name").setValue(name);
            if (damage != 0) value.getNode("damage").setValue(damage);
            if (amount != 1) value.getNode("amount").setValue(amount);
        }
    }

}
