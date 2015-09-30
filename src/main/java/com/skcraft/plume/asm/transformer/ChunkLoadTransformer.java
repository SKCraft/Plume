package com.skcraft.plume.asm.transformer;

import com.skcraft.plume.asm.util.MethodMatcher;
import com.skcraft.plume.asm.util.ObfHelper;
import lombok.extern.java.Log;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

@Log
public class ChunkLoadTransformer implements IClassTransformer {

    private final MethodMatcher provideChunkMatcher = new MethodMatcher("net.minecraft.world.gen.ChunkProviderServer", "func_73154_d", "provideChunk", "(II)Lnet/minecraft/world/chunk/Chunk;");
    private final MethodMatcher loadChunkMatcher = new MethodMatcher("net.minecraft.world.gen.ChunkProviderServer", "func_73158_c", "loadChunk", "(II)Lnet/minecraft/world/chunk/Chunk;");

    @Override
    public byte[] transform(String name, String transformedName, byte[] data) {
        if (provideChunkMatcher.matchesClass(name)) {
            ClassReader cr = new ClassReader(data);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            cr.accept(new ChunkProviderClassVisitor(cw, name), 0);
            return cw.toByteArray();
        }
        return data;
    }

    private class ChunkProviderClassVisitor extends ClassVisitor {
        private final String owner;

        public ChunkProviderClassVisitor(ClassVisitor visitor, String owner) {
            super(ASM5, visitor);
            this.owner = owner;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (provideChunkMatcher.matchesMethod(owner, name, desc)) {
                return new UpdateEntitiesVisitor(super.visitMethod(access, name, desc, signature, exceptions));
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private class UpdateEntitiesVisitor extends MethodVisitor {
        public UpdateEntitiesVisitor(MethodVisitor mv) {
            super(ASM5, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (loadChunkMatcher.matchesMethod(owner, name, desc)) {
                mv.visitInsn(POP);
                mv.visitInsn(POP);
                mv.visitInsn(POP);

                Label defaultEmptyChunk = new Label();
                Label end = new Label();

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, ObfHelper.obfClass("net/minecraft/world/gen/ChunkProviderServer"),
                        ObfHelper.obfField("net/minecraft/world/gen/ChunkProviderServer", "field_73251_h", "worldObj", "net/minecraft/world/WorldServer"),
                        ObfHelper.obfDesc("Lnet/minecraft/world/WorldServer;"));
                mv.visitVarInsn(ILOAD, 1);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitMethodInsn(INVOKESTATIC, "com/skcraft/plume/asm/transformer/ChunkLoadHelper", "mayLoadChunk", ObfHelper.obfMethodDesc("(Lnet/minecraft/world/WorldServer;II)Z"), false);
                mv.visitJumpInsn(IFEQ, defaultEmptyChunk);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitVarInsn(ILOAD, 2);
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                mv.visitJumpInsn(GOTO, end);
                mv.visitLabel(defaultEmptyChunk);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, ObfHelper.obfClass("net/minecraft/world/gen/ChunkProviderServer"),
                        ObfHelper.obfField("net/minecraft/world/gen/ChunkProviderServer", "field_73249_c", "defaultEmptyChunk", "net/minecraft/world/chunk/Chunk"),
                        ObfHelper.obfDesc("Lnet/minecraft/world/chunk/Chunk;"));
                mv.visitLabel(end);

                log.info("**PLUME** Injected handler for ChunkLoadRequestEvent");
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
