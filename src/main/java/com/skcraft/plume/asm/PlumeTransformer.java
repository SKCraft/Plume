package com.skcraft.plume.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

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
                mv.visitMethodInsn(INVOKESPECIAL, "com/skcraft/plume/event/network/PlayerAuthenticateEvent", "<init>", "(Lcom/mojang/authlib/GameProfile;)V", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "cpw/mods/fml/common/eventhandler/EventBus", "post", "(Lcpw/mods/fml/common/eventhandler/Event;)Z", false);
                mv.visitInsn(POP);
            }
        }
    }
}
