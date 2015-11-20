package com.skcraft.plume.module.commune;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = Chat.class, name = "chat"),
        @Type(value = Command.class, name = "command"),
        @Type(value = GameAction.class, name = "game_action"),
        @Type(value = Presence.class, name = "presence"),
        @Type(value = ServerStatus.class, name = "server_status"),
})
@Data
public abstract class Message {

    private String source;

    public abstract void execute(Commune commune);

}
