package com.skcraft.plume.module.backtrack;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Switch;
import com.sk89q.intake.parametric.annotation.Text;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.concurrency.EvenMoreExecutors;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.journal.Journal;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.service.journal.criteria.Criteria;
import com.skcraft.plume.common.service.journal.criteria.CriteriaParser;
import com.skcraft.plume.common.service.journal.criteria.CriteriaParser.ParseException;
import com.skcraft.plume.common.util.Order;
import com.skcraft.plume.common.util.Vectors;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.common.util.pagination.ListPagination;
import com.skcraft.plume.common.util.pagination.Page;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.module.backtrack.action.Action;
import com.skcraft.plume.module.backtrack.playback.PlaybackAgent;
import com.skcraft.plume.module.backtrack.playback.PlaybackType;
import com.skcraft.plume.module.backtrack.playback.RejectedPlaybackException;
import com.skcraft.plume.util.*;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.profile.ProfileService;
import com.skcraft.plume.util.profile.Profiles;
import com.skcraft.plume.util.worldedit.WorldEditAPI;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.javatuples.Pair;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@AutoRegister
@Log
public class LoggerCommands {

    private static final SimpleDateFormat SHORT_DATA_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss");
    private static final SimpleDateFormat LONG_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @InjectConfig("backtrack") private Config<LoggerConfig> config;
    @Inject private ActionMap actionMap;
    @InjectService private Service<Journal> journal;
    private final ListeningExecutorService loggerExecutor = MoreExecutors.listeningDecorator(EvenMoreExecutors.newBoundedCachedThreadPool(0, 1, 2));
    @Inject private BackgroundExecutor backgroundExecutor;
    @Inject private TickExecutorService tickExecutor;
    @Inject private ProfileService profileService;
    @Inject private PlaybackAgent playbackAgent;
    @Inject private QueryCache queryCache;

    public int getLimit(Integer limit) {
        return limit == null ? config.get().search.defaultLimit : Math.max(1, Math.min(config.get().search.maxLimit, limit));
    }

    private void printLog(ICommandSender sender, Page<Record> page) {
        Deferred<?> deferred = Deferreds
                .when(() -> {
                    List<IChatComponent> messages = Lists.newArrayList();

                    // Watson detects Page A/B so the following must not be changed
                    messages.add(Messages.info("Page " + page.page() + "/" + page.getPagination().pageCount())); //NON-NLS

                    String worldId = "";
                    if (sender instanceof EntityPlayer) {
                        worldId = Worlds.getWorldId(((EntityPlayer) sender).worldObj);
                    }

                    int i = 0;
                    for (Record record : page) {
                        try {
                            Action action = actionMap.readRecord(record);

                            ChatComponentText text = new ChatComponentText(String.format("(%d) ", page.getAbsoluteIndex(i) + 1));
                            text.getChatStyle().setColor(EnumChatFormatting.GREEN);

                            if (!worldId.equals(record.getLocation().getWorldId())) {
                                ChatComponentText worldText = new ChatComponentText("(" + record.getLocation().getWorldId() + ") ");
                                worldText.getChatStyle().setColor(EnumChatFormatting.BLUE);
                                text.appendSibling(worldText);
                            }

                            ChatComponentText dateText = new ChatComponentText(SHORT_DATA_FORMAT.format(record.getTime()) + " ");
                            dateText.getChatStyle().setColor(EnumChatFormatting.GRAY);
                            text.appendSibling(dateText);

                            IChatComponent actionText = action.toQueryMessage(record);
                            actionText.getChatStyle().setColor(EnumChatFormatting.YELLOW);
                            text.appendSibling(actionText);

                            messages.add(text);
                        } catch (ActionReadException e) {
                            log.log(Level.WARNING, "Could not read action", e);
                        }

                        i++;
                    }

                    return messages;
                }, backgroundExecutor.getExecutor())
                .done(messages -> {
                    Messages.sendMessages(sender, messages);
                }, tickExecutor)
                .fail(e -> {
                    sender.addChatMessage(Messages.exception(e));
                }, tickExecutor);

        backgroundExecutor.notifyOnDelay(deferred, sender);
    }

    private void processSingleRecord(ICommandSender sender, String input, BiConsumer<Record, Action> callable) {
        UserId userId = Profiles.fromCommandSender(sender);
        CriteriaParser parser = createCriteriaParser(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    try {
                        int id = Integer.parseInt(input);
                        ListPagination<Record> results = queryCache.getIfPresent(userId);
                        if (results != null) {
                            List<Record> data = results.getData();
                            int index = id - 1;
                            if (index >= 0 && index < data.size()) {
                                Record record = data.get(index);
                                Action action = actionMap.readRecord(record);
                                return new Pair<>(record, action);
                            } else {
                                throw new CommandException(tr("logger.noSuchRecord", id));
                            }
                        } else {
                            throw new CommandException(tr("logger.noCachedResults"));
                        }
                    } catch (NumberFormatException e) {
                        Criteria criteria = parser.parse(input).build();
                        List<Record> records = journal.provide().findRecords(criteria, Order.DESC, 1);
                        if (records.isEmpty()) {
                            throw new CommandException(tr("logger.noResults", input.trim()));
                        }
                        Record record = records.get(0);
                        Action action = actionMap.readRecord(record);
                        return new Pair<>(record, action);
                    }
                }, backgroundExecutor.getExecutor())
                .done(pair -> {
                    callable.accept(pair.getValue0(), pair.getValue1());
                }, tickExecutor)
                .fail(e -> {
                    if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(e.getMessage()));
                    } else {
                        sender.addChatMessage(Messages.exception(e));
                    }
                }, tickExecutor);

        backgroundExecutor.notifyOnDelay(deferred, sender);
    }

    private CriteriaParser createCriteriaParser(ICommandSender sender) {
        Region selection = WorldEditAPI.getSelectionIfExists(sender);
        WorldVector3i center = sender instanceof EntityPlayer ? Locations.getWorldVector3i((EntityPlayer) sender) : null;
        CriteriaParser parser = new CriteriaParser(profileService, actionMap::getActionIdByName);
        parser.setCenter(center);
        parser.setSelection(selection);
        return parser;
    }

    private void createPlayback(ICommandSender sender, String input, PlaybackType playbackType, boolean confirm) {
        CriteriaParser parser = createCriteriaParser(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Criteria criteria = parser.parse(input).build();
                    if (!confirm && config.get().playback.confirmNoDateNoPlayer && criteria.getSince() == null && criteria.getBefore() == null && criteria.getUserId() == null) {
                        throw new CommandException(tr("logger.playback.confirmNoDateNoPlayer"));
                    }
                    return journal.provide().findRecords(criteria, playbackType.getOrder());
                }, loggerExecutor)
                .done(cursor -> {
                    if (cursor.hasNext()) {
                        try {
                            playbackAgent.addPlayback(cursor, sender, playbackType);
                        } catch (RejectedPlaybackException e) {
                            cursor.close();
                            throw e;
                        }
                    } else {
                        sender.addChatMessage(Messages.error(tr("logger.noResults", input)));
                        cursor.close();
                    }
                }, tickExecutor)
                .fail(e -> {
                    if (e instanceof CommandException) {
                        sender.addChatMessage(Messages.error(e.getMessage()));
                    } else if (e instanceof ParseException) {
                        sender.addChatMessage(Messages.error(e.getMessage()));
                    } else if (e instanceof RejectedPlaybackException) {
                        sender.addChatMessage(Messages.error(tr("logger.playback.tooManyConcurrent")));
                    } else {
                        sender.addChatMessage(Messages.exception(e));
                    }
                });

        backgroundExecutor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = {"search", "query", "find"}, desc = "Search the block log")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.query")
    public void search(@Sender ICommandSender sender, @Text String input, @Switch('a') boolean ascending, @Switch('l') Integer limit) {
        UserId userId = Profiles.fromCommandSender(sender);
        Order order = ascending ? Order.ASC : Order.DESC;
        int perPage = Messages.LINES_PER_PAGE - 1;
        CriteriaParser parser = createCriteriaParser(sender);

        Deferred<?> deferred = Deferreds
                .when(() -> {
                    Criteria criteria = parser.parse(input).build();
                    List<Record> records = journal.provide().findRecords(criteria, order, getLimit(limit));
                    return new ListPagination<>(records, perPage);
                }, loggerExecutor)
                .done(pagination -> {
                    if (!pagination.isEmpty()) {
                        queryCache.put(userId, pagination);
                        printLog(sender, pagination.first());
                    } else {
                        sender.addChatMessage(Messages.error(tr("logger.noResults", input)));
                    }
                }, tickExecutor)
                .fail(e -> {
                    if (e instanceof ParseException) {
                        sender.addChatMessage(Messages.error(e.getMessage()));
                    } else {
                        sender.addChatMessage(Messages.exception(e));
                    }
                });

        backgroundExecutor.notifyOnDelay(deferred, sender);
    }

    @Command(aliases = {"near"}, desc = "Search for entries near your current location")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.query")
    public void near(@Sender EntityPlayer player, @Text String input) {
        search(player, (config.get().near.defaultParameters + " " + input).trim(), false, null);
    }

    @Command(aliases = {"page", "pg"}, desc = "View a particular page")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.query")
    public void viewPage(@Sender ICommandSender sender, int page) {
        UserId userId = Profiles.fromCommandSender(sender);
        ListPagination<Record> results = queryCache.getIfPresent(userId);
        if (results != null) {
            if (results.has(page)) {
                printLog(sender, results.at(page));
            } else {
                sender.addChatMessage(Messages.error(tr("logger.noPage", page)));
            }
        } else {
            sender.addChatMessage(Messages.error(tr("logger.noCachedResults")));
        }
    }

    @Command(aliases = "rollback", desc = "Rollback using logged data")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.rollback")
    public void rollback(@Sender ICommandSender sender, @Text String input, @Switch('y') boolean confirm) {
        createPlayback(sender, input, PlaybackType.UNDO, confirm);
    }

    @Command(aliases = "replay", desc = "Re-play using logged data")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.replay")
    public void replay(@Sender ICommandSender sender, @Text String input, @Switch('y') boolean confirm) {
        createPlayback(sender, input, PlaybackType.REDO, confirm);
    }

    @Command(aliases = {"details", "info"}, desc = "View detailed information about a given record")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.details")
    public void details(@Sender EntityPlayerMP sender, @Text String input) throws ActionReadException {
        processSingleRecord(sender, input, (record, action) -> {
            WorldVector3i loc = record.getLocation();
            sender.addChatMessage(new ChatComponentText(tr("logger.details.location", loc.getX(), loc.getY(), loc.getZ(), loc.getWorldId())));
            sender.addChatMessage(new ChatComponentText(tr("logger.details.time", LONG_DATA_FORMAT.format(record.getTime()))));
            List<IChatComponent> messages = Lists.newArrayList();
            action.addDetailMessages(record, messages);
            Messages.sendMessages(sender, messages);
        });
    }

    @Command(aliases = {"teleport", "tp"}, desc = "Teleport to the location of a given record")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.teleport")
    public void teleport(@Sender EntityPlayerMP sender, @Text String input) throws ActionReadException {
        processSingleRecord(sender, input, (record, action) -> {
            try {
                Location3d location = Vectors.toCenteredLocation3d(record.getLocation());
                TeleportHelper.teleport(sender, location);
                sender.addChatMessage(Messages.info(tr("logger.teleport.teleported", location.getX(), location.getY(), location.getZ(), Worlds.getWorldId(location.getWorld()))));
            } catch (NoSuchWorldException e) {
                sender.addChatMessage(Messages.error(tr("logger.worldNotLoaded", record.getLocation().getWorldId())));
            }
        });
    }

    @Command(aliases = {"items"}, desc = "Get the items from a record")
    @Group({@At("backtrack"), @At("bt"), @At("lb")})
    @Require("plume.logger.items")
    public void items(@Sender EntityPlayerMP sender, @Text String input) throws ActionReadException {
        processSingleRecord(sender, input, (record, action) -> {
            List<ItemStack> items = action.copyItems();
            if (!items.isEmpty()) {
                Inventories.openVirtualInventory(tr("logger.items.inventoryName"), sender, items);
            } else {
                sender.addChatMessage(Messages.error(tr("logger.items.noItems")));
            }
        });
    }

}
