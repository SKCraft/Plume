package com.skcraft.plume.asm.transformer;

import lombok.extern.java.Log;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;

@Log
@SortingIndex(value = -999)
public class PlayerAuthenticateTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] data) {
        if(transformedName.equals("net.minecraft.server.network.NetHandlerLoginServer$2")) {
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
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
            if(opcode == PUTFIELD && owner.equals(ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer")) && name.equals("field_147328_g")) {
                mv.visitFieldInsn(GETSTATIC, "net/minecraftforge/common/MinecraftForge", "EVENT_BUS", "Lnet/minecraftforge/fml/common/eventhandler/EventBus;");
                mv.visitTypeInsn(NEW, "com/skcraft/plume/event/network/PlayerAuthenticateEvent");
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer$1"), "this$0", "L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";");
                mv.visitFieldInsn(GETFIELD, ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer"), "field_147337_i", "Lcom/mojang/authlib/GameProfile;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer$1"), "this$0", "L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";");
                mv.visitMethodInsn(INVOKESPECIAL, "com/skcraft/plume/event/network/PlayerAuthenticateEvent", "<init>", "(Lcom/mojang/authlib/GameProfile;L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";)V", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraftforge/fml/common/eventhandler/EventBus", "post", "(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", false);
                mv.visitInsn(POP);
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKESTATIC && owner.equals("rq") && name.equals("a") && desc.equals("(Lrq;Lrt;)Lrt;")) {
                mv.visitFieldInsn(GETSTATIC, "net/minecraftforge/common/MinecraftForge", "EVENT_BUS", "Lnet/minecraftforge/fml/common/eventhandler/EventBus;");
                mv.visitTypeInsn(NEW, "com/skcraft/plume/event/network/PlayerAuthenticateEvent");
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer$1"), "this$0", "L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";");
                mv.visitFieldInsn(GETFIELD, ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer"), "field_147337_i", "Lcom/mojang/authlib/GameProfile;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer$1"), "this$0", "L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";");
                mv.visitMethodInsn(INVOKESPECIAL, "com/skcraft/plume/event/network/PlayerAuthenticateEvent", "<init>", "(Lcom/mojang/authlib/GameProfile;L" + ObfMappings.get("net/minecraft/server/network/NetHandlerLoginServer") + ";)V", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraftforge/fml/common/eventhandler/EventBus", "post", "(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", false);
                mv.visitInsn(POP);
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private static final class ObfMappings {

        private static final Map<String, String> mappings = new HashMap<>();
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
