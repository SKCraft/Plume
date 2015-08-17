package com.skcraft.plume.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;

public class PlumeTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] data) {
        if(name.equals("net.minecraft.server.network.NetHandlerLoginServer$1") || name.equals("no")) {
            ClassReader cr = new ClassReader(data);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cr.accept(new LoginServerVisitor(cw), ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        }
        return data;
    }

    private class LoginServerVisitor extends ClassVisitor {

        public LoginServerVisitor(ClassVisitor visitor) {
            super(ASM5, visitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if(name.equals("run")) {
                return new RunVisitor(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private class RunVisitor extends AdviceAdapter {

        public RunVisitor(MethodVisitor mv, int access, String name, String desc) {
            super(ASM5, mv, access, name, desc);
        }

        @Override
        public void onMethodExit(int opcode) {
            if(opcode != ATHROW) {
                mv.visitFieldInsn(GETSTATIC, "net/minecraftforge/common/MinecraftForge", "EVENT_BUS", "Lcpw/mods/fml/common/eventhandler/EventBus;");
                mv.visitTypeInsn(NEW, "com/skcraft/plume/event/network/PlayerAuthenticateEvent");
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer$1"), "this$0", "L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";");
                mv.visitMethodInsn(INVOKESPECIAL, "com/skcraft/plume/event/network/PlayerAuthenticateEvent", "<init>", "(Lcom/mojang/authlib/GameProfile;L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";)V", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "cpw/mods/fml/common/eventhandler/EventBus", "post", "(Lcpw/mods/fml/common/eventhandler/Event;)Z", false);
                mv.visitInsn(POP);
            }
        }
    }

    private static final class ObfMappings {

        public static final Map<String, String> mappings = new HashMap<>();
        private static final boolean devEnvironment = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

        public static String get(String key) {
            return devEnvironment ? key : mappings.get(key);
        }

        static {
            mappings.put("net/minecraft/server/network/NetHandlerLoginServer", "nn");
            mappings.put("net/minecraft/server/network/NetHandlerLoginServer$1", "no");
        }
    }
}
