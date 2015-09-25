package com.skcraft.plume.module.stats;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.InitializationEvent;
import com.skcraft.plume.common.util.Metrics;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.Worlds;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;

@Module(name = "metrics-server", desc = "Record metrics related to the server")
public class ServerMetrics {

    private static final long GATHER_INTERVAL = TimeUnit.SECONDS.toNanos(10);

    @Inject private MetricRegistry metricRegistry;
    private final Map<String, MetricContainer> containers = Maps.newConcurrentMap();
    private long lastGatherTime = 0;

    @Subscribe
    public void onInitialization(InitializationEvent event) {
        metricRegistry.register(name("playerCount"), (Gauge<Integer>) () -> Server.getOnlinePlayers().size());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote) return;

        String worldId = Worlds.getWorldId(event.world);
        if (!containers.containsKey(worldId)) {
            MetricContainer container = new MetricContainer(event.world);
            container.register();
            containers.put(worldId, container);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) return;

        MetricContainer container = containers.remove(Worlds.getWorldId(event.world));
        if (container != null) {
            container.unregister();
        }
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent event) {
        if (event.side != Side.SERVER) return;

        long now = System.nanoTime();

        if (now - lastGatherTime >= GATHER_INTERVAL) {
            for (WorldServer world : MinecraftServer.getServer().worldServers) {
                MetricContainer container = containers.get(Worlds.getWorldId(world));
                if (container != null) {
                    container.gather();
                }
            }

            lastGatherTime = now;
        }
    }

    private class MetricContainer {
        private final World world;
        private final Set<String> registeredMetrics = Sets.newHashSet();
        private final CachedGauge<Integer> chunkCount = new CachedGauge<>();
        private final CachedGauge<Integer> playerCount = new CachedGauge<>();

        private final CachedGauge<Integer> tileEntityCount = new CachedGauge<>();
        private final CachedGauge<Integer> tickingTileEntityCount = new CachedGauge<>();

        private final CachedGauge<Integer> deadEntityCount = new CachedGauge<>();
        private final CachedGauge<Integer> entityCount = new CachedGauge<>();
        private final CachedGauge<Integer> livingCount = new CachedGauge<>();
        private final CachedGauge<Integer> fallingCount = new CachedGauge<>();
        private final CachedGauge<Integer> xpOrbCount = new CachedGauge<>();
        private final CachedGauge<Integer> itemCount = new CachedGauge<>();
        private final CachedGauge<Integer> ambientCount = new CachedGauge<>();
        private final CachedGauge<Integer> animalCount = new CachedGauge<>();
        private final CachedGauge<Integer> villagerCount = new CachedGauge<>();
        private final CachedGauge<Integer> waterMobCount = new CachedGauge<>();

        private MetricContainer(World world) {
            checkNotNull(world, "world");
            this.world = world;
        }

        private String track(String name) {
            registeredMetrics.add(name);
            return name;
        }

        public void register() {
            String id = Metrics.escape(Worlds.getWorldId(world));
            metricRegistry.register(track(name("worlds", id, "loadedChunkCount")), chunkCount);
            metricRegistry.register(track(name("worlds", id, "playerCount")), chunkCount);
            metricRegistry.register(track(name("worlds", id, "tileEntities", "count")), tileEntityCount);
            metricRegistry.register(track(name("worlds", id, "tileEntities", "ticking")), tickingTileEntityCount);
            metricRegistry.register(track(name("worlds", id, "entities", "deadCount")), deadEntityCount);
            metricRegistry.register(track(name("worlds", id, "entities", "count")), entityCount);
            metricRegistry.register(track(name("worlds", id, "entities", "livingCount")), livingCount);
            metricRegistry.register(track(name("worlds", id, "entities", "fallingBlockCount")), fallingCount);
            metricRegistry.register(track(name("worlds", id, "entities", "xpOrbCount")), xpOrbCount);
            metricRegistry.register(track(name("worlds", id, "entities", "itemCount")), itemCount);
            metricRegistry.register(track(name("worlds", id, "entities", "ambientCount")), ambientCount);
            metricRegistry.register(track(name("worlds", id, "entities", "animalCount")), animalCount);
            metricRegistry.register(track(name("worlds", id, "entities", "villagerCount")), villagerCount);
            metricRegistry.register(track(name("worlds", id, "entities", "waterMobCount")), waterMobCount);
        }

        public void unregister() {
            for (String name : registeredMetrics) {
                metricRegistry.remove(name);
            }
        }

        @SuppressWarnings("unchecked")
        public void gather() {
            chunkCount.setValue(world.getChunkProvider().getLoadedChunkCount());
            playerCount.setValue(world.playerEntities.size());

            int tileEntities = 0;
            int tickingTileEntities = 0;

            for (TileEntity tileEntity : (List<TileEntity>) world.loadedTileEntityList) {
                tileEntities++;
                if (tileEntity.canUpdate()) {
                    tickingTileEntities++;
                }
            }

            tileEntityCount.setValue(tileEntities);
            tickingTileEntityCount.setValue(tickingTileEntities);

            int deadEntities = 0;
            int entities = 0;
            int living = 0;
            int falling = 0;
            int xpOrbs = 0;
            int items = 0;
            int ambients = 0;
            int animals = 0;
            int villagers = 0;
            int waterMobs = 0;

            for (Entity entity : (List<Entity>) world.loadedEntityList) {
                if (entity.isDead) {
                    deadEntities++;
                } else {
                    entities++;
                    if (entity instanceof EntityLivingBase) living++;
                    if (entity instanceof EntityFallingBlock) falling++;
                    if (entity instanceof EntityXPOrb) xpOrbs++;
                    if (entity instanceof EntityItem) items++;
                    if (entity instanceof EntityAmbientCreature) ambients++;
                    if (entity instanceof EntityAnimal) animals++;
                    if (entity instanceof EntityVillager) villagers++;
                    if (entity instanceof EntityWaterMob) waterMobs++;
                }
            }

            deadEntityCount.setValue(deadEntities);
            entityCount.setValue(entities);
            livingCount.setValue(living);
            fallingCount.setValue(falling);
            xpOrbCount.setValue(xpOrbs);
            itemCount.setValue(items);
            ambientCount.setValue(ambients);
            animalCount.setValue(animals);
            villagerCount.setValue(villagers);
            waterMobCount.setValue(waterMobs);
        }
    }

}
