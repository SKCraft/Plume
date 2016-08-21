package com.skcraft.plume.util.config;

import com.google.common.reflect.TypeToken;
import com.skcraft.plume.util.inventory.SingleItemMatcher;
import com.skcraft.plume.util.inventory.TypeDataMatcher;
import com.skcraft.plume.util.inventory.TypeMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.Map;

public class SingleItemMatcherTypeSerializer implements TypeSerializer<SingleItemMatcher> {

    @Override
    public SingleItemMatcher deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
        String name;
        Byte damage = null;

        if (node.hasMapChildren()) {
            Map<Object, ? extends ConfigurationNode> map = node.getChildrenMap();
            if (map.containsKey("name")) {
                name = map.get("name").getString();
            } else {
                throw new ObjectMappingException("No item name specified");
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
            if (damage != null) {
                return new TypeDataMatcher(new ItemStack(item, 1, damage));
            } else {
                return new TypeMatcher(new ItemStack(item, 1, 0));
            }
        } else {
            throw new ObjectMappingException("No such item as " + name);
        }
    }

    @Override
    public void serialize(TypeToken<?> type, SingleItemMatcher obj, ConfigurationNode value) throws ObjectMappingException {
        ItemStack itemStack = obj.getItemStack();
        String name = Item.itemRegistry.getNameForObject(itemStack.getItem()).toString();
        if (obj instanceof TypeDataMatcher) {
            value.getNode("name").setValue(name);
            value.getNode("damage").setValue(itemStack.getItemDamage());
        } else if (obj instanceof TypeMatcher) {
            value.setValue(name);
        } else {
            throw new ObjectMappingException("Cannot serialize SingleItemMatchers of type " + obj.getClass().getName());
        }
    }
    
}
