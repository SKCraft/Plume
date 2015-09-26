package com.skcraft.plume.asm.transformer;

import com.skcraft.plume.asm.util.MethodMatcher;
import com.skcraft.plume.asm.util.ObfHelper;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import lombok.extern.java.Log;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

@Log
@SortingIndex(value = 999) // Run late because we're currently replacing the tick methods
public class TickCallbackTransformer implements IClassTransformer {

    private final MethodMatcher worldUpdateMatcher = new MethodMatcher("net.minecraft.world.World", "func_72939_s", "updateEntities", "()V");
    private final MethodMatcher updateEntityMatcher = new MethodMatcher("net.minecraft.world.World", "func_72870_g", "updateEntity", "(Lnet/minecraft/entity/Entity;)V");
    private final MethodMatcher updateTileEntityMatcher = new MethodMatcher("net.minecraft.tileentity.TileEntity", "func_145845_h", "updateEntity", "()V");

    @Override
    public byte[] transform(String name, String transformedName, byte[] data) {
        if (worldUpdateMatcher.matchesClass(name)) {
            ClassReader cr = new ClassReader(data);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            cr.accept(new TickCallbackClassVisitor(cw, name), 0);
            return cw.toByteArray();
        }
        return data;
    }

    private class TickCallbackClassVisitor extends ClassVisitor {
        private final String owner;

        public TickCallbackClassVisitor(ClassVisitor visitor, String owner) {
            super(ASM5, visitor);
            this.owner = owner;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (worldUpdateMatcher.matchesMethod(owner, name, desc)) {
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
            if (updateEntityMatcher.matchesMethod(owner, name, desc)) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "com/skcraft/plume/asm/transformer/TickCallback", "tickEntity", ObfHelper.obfMethodDesc("(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;)V"), false);
                mv.visitInsn(POP); // Get rid of "this."
                log.info("**PLUME** Injected tick callback for entity updates");
            } else if (updateTileEntityMatcher.matchesMethod(owner, name, desc)) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "com/skcraft/plume/asm/transformer/TickCallback", "tickTileEntity", ObfHelper.obfMethodDesc("(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/world/World;)V"), false);
                log.info("**PLUME** Injected tick callback for title entity updates");
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }

}
