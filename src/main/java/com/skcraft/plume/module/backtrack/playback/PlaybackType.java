package com.skcraft.plume.module.backtrack.playback;

import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.util.Order;
import com.skcraft.plume.module.backtrack.action.Action;
import lombok.Getter;

public enum PlaybackType {

    UNDO(Order.DESC) {
        @Override
        public void apply(Action action, Record record) {
            action.undo(record);
        }
    },
    REDO(Order.ASC) {
        @Override
        public void apply(Action action, Record record) {
            action.redo(record);
        }
    };

    @Getter
    private final Order order;

    PlaybackType(Order order) {
        this.order = order;
    }

    public abstract void apply(Action action, Record record);

}
