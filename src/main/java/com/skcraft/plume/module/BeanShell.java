package com.skcraft.plume.module;

import bsh.EvalError;
import bsh.Interpreter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Text;
import com.sk89q.intake.util.auth.AuthorizationException;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

@Module(name = "beanshell", desc = "Provides BeanShell REPL via console", enabled = false)
@Log
public class BeanShell {

    @Inject private Injector injector;

    public Interpreter createInterpreter() {
        Interpreter interpreter = new Interpreter();
        try {
            interpreter.set("out", System.out);
            interpreter.set("injector", injector);
            interpreter.set("server", MinecraftServer.getServer());
            interpreter.set("worlds", Arrays.asList(MinecraftServer.getServer().worldServers));
            interpreter.set("component", this);
        } catch (EvalError e) {
            log.log(Level.WARNING, "Error evaluating", e);
        }

        return interpreter;
    }

    private void checkPermission(ICommandSender sender) throws AuthorizationException {
        if (!(sender instanceof MinecraftServer)) {
            throw new AuthorizationException("Only server consoles can use this command.");
        }
    }

    @Command(aliases = {"bsh", "beanshell"}, desc = "Run BeanShell code")
    @Require("plume.beanshell.execute")
    public void bsh(@Sender ICommandSender sender, @Text String script) throws AuthorizationException {
        checkPermission(sender);

        try {
            Interpreter interpreter = createInterpreter();
            Object obj = interpreter.eval(script);

            if (obj != null) {
                sender.addChatMessage(new ChatComponentText(">>> " + String.valueOf(obj)));
            } else {
                sender.addChatMessage(new ChatComponentText("Executed with no output"));
            }
        } catch (EvalError e) {
            sender.addChatMessage(new ChatComponentText("!!! " + e.getMessage()));
        }
    }

    @Command(aliases = {"bshfile"}, desc = "Run a BeanShell file")
    @Require("plume.beanshell.executefile")
    public void bshFile(@Sender ICommandSender sender, @Text String path) throws AuthorizationException {
        checkPermission(sender);

        try {
            Interpreter interpreter = createInterpreter();
            Object obj = interpreter.source(path);

            if (obj != null) {
                sender.addChatMessage(new ChatComponentText(">>> " + String.valueOf(obj)));
            } else {
                sender.addChatMessage(new ChatComponentText("Executed with no output"));
            }
        } catch (IOException | EvalError e) {
            sender.addChatMessage(new ChatComponentText("!!! " + e.getMessage()));
        }
    }

}
