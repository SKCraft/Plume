package com.skcraft.plume.module.backtrack;

import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.PostInitializationEvent;
import com.skcraft.plume.common.service.journal.Journal;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.module.backtrack.action.BlockBreakAction;
import com.skcraft.plume.module.backtrack.action.BlockExplodeAction;
import com.skcraft.plume.module.backtrack.action.BlockPlaceAction;
import com.skcraft.plume.module.backtrack.action.BucketFillAction;
import com.skcraft.plume.module.backtrack.action.EntityDamageAction;
import com.skcraft.plume.module.backtrack.action.ItemDropAction;
import com.skcraft.plume.module.backtrack.action.ItemPickupAction;
import com.skcraft.plume.module.backtrack.action.PlayerChatAction;
import com.skcraft.plume.module.backtrack.action.PlayerCommandAction;
import com.skcraft.plume.module.backtrack.action.PlayerDeathAction;

@Module(name = "backtrack", desc = "Logs block changes for later query [requires journal service]", enabled = false)
public class BackTrack {

    @Inject private LoggerCommands commands;
    @Inject private LoggerListener listener;
    @Inject private ActionMap actionMap;
    @Inject private Journal journal;

    @Subscribe
    public void onPostInitialization(PostInitializationEvent event) {
        actionMap.registerAction(1, BlockBreakAction.class, "block-break");
        actionMap.registerAction(2, BlockPlaceAction.class, "block-place");
        actionMap.registerAction(3, BlockExplodeAction.class, "block-explode");
        actionMap.registerAction(4, ItemPickupAction.class, "item-pickup");
        actionMap.registerAction(5, ItemDropAction.class, "item-drop");
        actionMap.registerAction(6, BucketFillAction.class, "bucket-fill");
        actionMap.registerAction(7, EntityDamageAction.class, "entity-damage");
        actionMap.registerAction(8, PlayerChatAction.class, "chat");
        actionMap.registerAction(9, PlayerCommandAction.class, "command");
        actionMap.registerAction(10, PlayerDeathAction.class, "player-death");
    }



}
