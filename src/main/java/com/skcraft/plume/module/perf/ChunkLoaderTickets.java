package com.skcraft.plume.module.perf;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.ChunkManagerUtils;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "chunk-ticket-manager", desc = "Commands to manage chunk loader tickets")
public class ChunkLoaderTickets {

    private int unloadChunkTickets(BiPredicate<World, Ticket> predicate) throws NoSuchFieldException, IllegalAccessException {
        List<Ticket> removingTickets = Lists.newArrayList();

        Map<World, Multimap<String, Ticket>> tickets = ChunkManagerUtils.getTickets();
        for (Map.Entry<World, Multimap<String, Ticket>> entry : tickets.entrySet()) {
            for (Ticket ticket : entry.getValue().values()) {
                if (predicate.test(entry.getKey(), ticket)) {
                    removingTickets.add(ticket);
                }
            }
        }

        if (!removingTickets.isEmpty()) {
            for (Ticket ticket : removingTickets) {
                ForgeChunkManager.releaseTicket(ticket);
            }

            return removingTickets.size();
        } else {
            return 0;
        }
    }

    private void unloadChunkTickets(ICommandSender sender, BiPredicate<World, Ticket> predicate) throws NoSuchFieldException, IllegalAccessException {
        int unloadCount = unloadChunkTickets(predicate);
        if (unloadCount > 0) {
            sender.addChatMessage(Messages.info(tr("ticketManager.released", unloadCount)));
        } else {
            sender.addChatMessage(Messages.error(tr("ticketManager.noMatching")));
        }
    }

    @Command(aliases = "unloadmod", desc = "Unload chunk tickets by mod ID")
    @Group(@At("chunktickets"))
    @Require("plume.ticketmanager.unload")
    public void unloadMod(@Sender ICommandSender sender, String modId) throws NoSuchFieldException, IllegalAccessException {
        unloadChunkTickets(sender, (world, ticket) -> ticket.getModId().equalsIgnoreCase(modId));
    }

    @Command(aliases = "list", desc = "List chunk tickets by mod ID")
    @Group(@At("chunktickets"))
    @Require("plume.ticketmanager.list")
    public void list(@Sender ICommandSender sender) throws NoSuchFieldException, IllegalAccessException {
        Map<String, TicketCount> counts = Maps.newHashMap();

        Map<World, Multimap<String, Ticket>> tickets = ChunkManagerUtils.getTickets();
        for (Map.Entry<World, Multimap<String, Ticket>> entry : tickets.entrySet()) {
            for (Ticket ticket : entry.getValue().values()) {
                TicketCount count = counts.get(ticket.getModId());
                if (count == null) {
                    count = new TicketCount(ticket.getModId());
                    counts.put(ticket.getModId(), count);
                }
                count.ticketCount++;
                count.chunkCount += ticket.getChunkList().size();
            }
        }

        if (counts.isEmpty()) {
            sender.addChatMessage(Messages.info(tr("ticketManager.noTickets")));
        } else {
            List<TicketCount> countsList = Lists.newArrayList(counts.values());
            Collections.sort(countsList);

            for (TicketCount count : countsList) {
                sender.addChatMessage(Messages.info(count.modId + ": " + count.ticketCount + " tickets, " + count.chunkCount + " chunks"));
            }
        }
    }

    private static class TicketCount implements Comparable<TicketCount> {
        private final String modId;
        private int ticketCount = 0;
        private int chunkCount = 0;

        private TicketCount(String modId) {
            this.modId = modId;
        }

        @Override
        public int compareTo(TicketCount o) {
            if (chunkCount > o.chunkCount) {
                return -1;
            } else if (chunkCount < o.chunkCount) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
