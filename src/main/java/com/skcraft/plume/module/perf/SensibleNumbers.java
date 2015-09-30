package com.skcraft.plume.module.perf;

import com.google.common.base.Predicate;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.module.perf.SensibleNumbersConfig.CountPerRadius;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lombok.extern.java.Log;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;

@Module(name = "sensible-numbers", desc = "Limits mob spawning")
@Log
public class SensibleNumbers {

    private final ClassCounter zombieCounter = new ClassCounter(EntityZombie.class);
    private final ClassCounter skeletonCounter = new ClassCounter(EntitySkeleton.class);
    private final ClassCounter creeperCounter = new ClassCounter(EntityCreeper.class);
    private final ClassCounter spiderCounter = new ClassCounter(EntitySpider.class);
    private final ClassCounter endermanCounter = new ClassCounter(EntityEnderman.class);
    private final ClassCounter witchCounter = new ClassCounter(EntityWitch.class);
    private final ClassCounter squidCounter = new ClassCounter(EntitySquid.class);
    private final ClassCounter sheepCounter = new ClassCounter(EntitySheep.class);
    private final ClassCounter pigCounter = new ClassCounter(EntityPig.class);
    private final ClassCounter cowCounter = new ClassCounter(EntityCow.class);
    private final ClassCounter chickenCounter = new ClassCounter(EntityChicken.class);
    private final ClassCounter petCounter = new ClassCounter(EntityTameable.class);

    @InjectConfig("sensible_numbers")
    private Config<SensibleNumbersConfig> config;

    private void setCreatureTypeQuota(EnumCreatureType type, int quota) {
        try {
            Field field;

            try {
                field = type.getClass().getDeclaredField("field_75606_e");
                field.setAccessible(true);
            } catch (Exception e) {
                field = type.getClass().getDeclaredField("maxNumberOfCreature");
                field.setAccessible(true);
            }

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(type, quota);

            log.info("Set creature type quota on " + type + " to " + quota);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to set creature type quota on " + type, e);
        }
    }

    @Subscribe
    public void onLoadConfig(LoadConfigEvent event) {
        setCreatureTypeQuota(EnumCreatureType.monster, config.get().quotas.monster);
        setCreatureTypeQuota(EnumCreatureType.creature, config.get().quotas.animal);
        setCreatureTypeQuota(EnumCreatureType.ambient, config.get().quotas.ambient);
        setCreatureTypeQuota(EnumCreatureType.waterCreature, config.get().quotas.water);
    }

    private int countEntities(Chunk chunk, Predicate<Entity> counter) {
        int count = 0;
        for (List<?> entityList : chunk.entityLists) {
            for (Object entry : entityList) {
                if (counter.apply((Entity) entry)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countEntitiesInRadius(World world, int baseX, int baseZ, int apothem, Predicate<Entity>counter) {
        int count = 0;
        for (int x = baseX - apothem; x <= baseX + apothem; x++) {
            for (int z = baseZ - apothem; z <= baseZ + apothem; z++) {
                if (world.getChunkProvider().chunkExists(x, z)) {
                    Chunk chunk = world.getChunkFromChunkCoords(x, z);
                    count += countEntities(chunk, counter);
                }
            }
        }
        return count;
    }

    private boolean canSpawn(Entity entity) {
        if (!config.get().radiusHardLimit.enabled) {
            return true;
        }

        CountPerRadius setting;
        Predicate<Entity> counter;

        if (entity instanceof EntityZombie) {
            setting = config.get().radiusHardLimit.zombie;
            counter = zombieCounter;
        } else if (entity instanceof EntitySkeleton) {
            setting = config.get().radiusHardLimit.skeleton;
            counter = skeletonCounter;
        } else if (entity instanceof EntityCreeper) {
            setting = config.get().radiusHardLimit.creeper;
            counter = creeperCounter;
        } else if (entity instanceof EntitySpider) {
            setting = config.get().radiusHardLimit.spider;
            counter = spiderCounter;
        } else if (entity instanceof EntityEnderman) {
            setting = config.get().radiusHardLimit.enderman;
            counter = skeletonCounter;
        } else if (entity instanceof EntityWitch) {
            setting = config.get().radiusHardLimit.witch;
            counter = witchCounter;
        } else if (entity instanceof EntitySquid) {
            setting = config.get().radiusHardLimit.squid;
            counter = squidCounter;
        } else if (entity instanceof EntitySheep) {
            setting = config.get().radiusHardLimit.sheep;
            counter = sheepCounter;
        } else if (entity instanceof EntityPig) {
            setting = config.get().radiusHardLimit.pig;
            counter = pigCounter;
        } else if (entity instanceof EntityCow) {
            setting = config.get().radiusHardLimit.cow;
            counter = cowCounter;
        } else if (entity instanceof EntityChicken) {
            setting = config.get().radiusHardLimit.chicken;
            counter = chickenCounter;
        } else if (entity instanceof EntityTameable) {
            setting = config.get().radiusHardLimit.pet;
            counter = petCounter;
        } else {
            return true;
        }

        if (setting.limit <= 0) {
            return false;
        }

        int count = countEntitiesInRadius(entity.worldObj, ((int) entity.posX) >> 4, ((int) entity.posZ) >> 4, setting.radius, counter);
        return count < setting.limit;
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.world.isRemote) return;

        Entity entity = event.entity;
        if (!canSpawn(entity)) {
            event.setResult(Result.DENY);
            entity.setDead();
        }
    }

    @SubscribeEvent
    public void onCheckSpawn(CheckSpawn event) {
        if (event.world.isRemote) return;

        Entity entity = event.entity;
        if (!canSpawn(entity)) {
            event.setResult(Result.DENY);
        }
    }

    private static class ClassCounter implements Predicate<Entity> {
        private final Class<? extends Entity> type;

        private ClassCounter(Class<? extends Entity> type) {
            this.type = type;
        }

        @Override
        public boolean apply(Entity input) {
            return type.isAssignableFrom(input.getClass());
        }
    }

}
