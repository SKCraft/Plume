package com.skcraft.plume.util;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.WorldVector3i;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class NBTUtils {

    private NBTUtils() {
    }

    public static NBTTagCompound writeToCompound(Consumer<NBTTagCompound> consumer) {
        NBTTagCompound tag = new NBTTagCompound();
        consumer.accept(tag);
        return tag;
    }

    public static <T> NBTTagList listToNBTCompounds(Collection<T> collection, BiConsumer<T, NBTTagCompound> converter) {
        checkNotNull(collection, "collection");
        checkNotNull(converter, "converter");
        NBTTagList tagList = new NBTTagList();
        for (T entry : collection) {
            NBTTagCompound tag = new NBTTagCompound();
            converter.accept(entry, tag);
            tagList.appendTag(tag);
        }
        return tagList;
    }

    public static <T> List<T> nbtCompoundsToList(NBTTagList tagList, Function<NBTTagCompound, T> converter) {
        checkNotNull(tagList, "list");
        checkNotNull(converter, "converter");
        List<T> entries = Lists.newArrayList();
        for (int i = 0; i < tagList.tagCount(); i++) {
            entries.add(converter.apply(tagList.getCompoundTagAt(i)));
        }
        return entries;
    }

    public static NBTTagCompound userIdToNBT(UserId userId) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("UUID", userId.getUuid().toString());
        tag.setString("Name", userId.getName());
        return tag;
    }

    public static UserId nbtToUserId(NBTTagCompound tag) {
        return new UserId(UUID.fromString(tag.getString("UUID")), tag.getString("Name"));
    }

    public static void writeWorldVector3iToNBT(WorldVector3i position, NBTTagCompound tag) {
        tag.setString("World", position.getWorldId());
        tag.setInteger("X", position.getX());
        tag.setInteger("Y", position.getY());
        tag.setInteger("Z", position.getZ());
    }

    public static WorldVector3i readWorldVector3iFromNBT(NBTTagCompound tag) {
        return new WorldVector3i(tag.getString("World"), tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
    }

    public static NBTTagList itemStacksToNBT(Collection<ItemStack> itemStacks) {
        checkNotNull(itemStacks, "itemStacks");
        return listToNBTCompounds(itemStacks, ItemStack::writeToNBT);
    }

    public static List<ItemStack> nbtToItemStacks(NBTTagList tagList) {
        checkNotNull(tagList, "tagList");
        return nbtCompoundsToList(tagList, ItemStack::loadItemStackFromNBT);
    }

    public static byte[] compoundToBytes(NBTTagCompound tag) throws IOException {
        checkNotNull(tag, "tag");
        try (Closer closer = Closer.create()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = closer.register(new DataOutputStream(baos));
            CompressedStreamTools.write(tag, dos);
            closer.close();
            return baos.toByteArray();
        }
    }

    public static NBTTagCompound compoundFromBytes(byte[] bytes) throws IOException {
        checkNotNull(bytes, "bytes");
        try (Closer closer = Closer.create()) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = closer.register(new DataInputStream(bais));
            return CompressedStreamTools.read(dis);
        }
    }

    @SuppressWarnings("unchecked")
    public static void copy(NBTTagCompound from, NBTTagCompound to) {
        checkNotNull(from, "from");
        checkNotNull(to, "to");
        for (String key : from.getKeySet()) {
            NBTBase value = from.getTag(key);
            if (value != null) {
                to.setTag(key, value.copy());
            }
        }
    }

}
