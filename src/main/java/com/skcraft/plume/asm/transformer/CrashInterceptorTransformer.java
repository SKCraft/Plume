package com.skcraft.plume.asm.transformer;

import com.google.common.collect.Lists;
import com.skcraft.plume.asm.util.BytecodeUtils;
import com.skcraft.plume.asm.util.MethodMatcher;
import lombok.extern.java.Log;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

@Log
public class CrashInterceptorTransformer implements IClassTransformer {

    private final List<CrashCatcher> catchers = Lists.newArrayList();

    public CrashInterceptorTransformer() {
        // Item use
        catchers.add(new CrashCatcher(
                new MethodMatcher("net.minecraftforge.common.ForgeHooks", "onPlaceItemIntoWorld", "onPlaceItemIntoWorld", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z"),
                new MethodMatcher("net.minecraft.item.Item", "func_77648_a", "onItemUse", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z")
        ));

        // Creating tile entities
        catchers.add(new CrashCatcher(
                new MethodMatcher("net.minecraft.world.chunk.storage.AnvilChunkLoader", "loadEntities", "loadEntities", "(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/world/chunk/Chunk;)V"),
                new MethodMatcher("net.minecraft.tileentity.TileEntity", "func_145827_c", "createAndLoadEntity", "(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/tileentity/TileEntity;")
        ));
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] data) {
        List<CrashCatcher> matching = Lists.newArrayList();

        for (CrashCatcher catcher : catchers) {
            if (catcher.getMethodMatcher().matchesClass(name)) {
                matching.add(catcher);
            }
        }

        if (!matching.isEmpty()) {
            ClassReader cr = new ClassReader(data);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            cr.accept(new InterceptorClassVisitor(cw, name, matching), 0);
            return cw.toByteArray();
        }
        return data;
    }

    private static class InterceptorClassVisitor extends ClassVisitor {
        private final String owner;
        private final List<CrashCatcher> catchers;

        public InterceptorClassVisitor(ClassVisitor visitor, String owner, List<CrashCatcher> catchers) {
            super(ASM5, visitor);
            this.owner = owner;
            this.catchers = catchers;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            List<CrashCatcher> matching = Lists.newArrayList();

            for (CrashCatcher catcher : catchers) {
                if (catcher.getMethodMatcher().matchesMethod(owner, name, desc)) {
                    matching.add(catcher);
                }
            }

            if (!matching.isEmpty()) {
                return new InterceptorVisitor(super.visitMethod(access, name, desc, signature, exceptions), matching);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private static class InterceptorVisitor extends MethodVisitor {
        private final List<CrashCatcher> catchers;

        public InterceptorVisitor(MethodVisitor mv, List<CrashCatcher> catchers) {
            super(ASM5, mv);
            this.catchers = catchers;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            for (CrashCatcher catcher : catchers) {
                if (catcher.getInvokeMatcher().matchesMethod(owner, name, desc)) {
                    Type methodType = Type.getMethodType(desc);
                    Type returnType = methodType.getReturnType();

                    Label code = new Label();
                    Label afterCode = new Label();
                    Label exceptionHandler = new Label();
                    Label afterTryCatch = new Label();

                    mv.visitTryCatchBlock(code, afterCode, exceptionHandler, "java/lang/Exception");
                    mv.visitLabel(code);
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    mv.visitLabel(afterCode);
                    mv.visitJumpInsn(GOTO, afterTryCatch);
                    mv.visitLabel(exceptionHandler);
                    // [exc]
                    mv.visitInsn(DUP);
                    // [exc, exc]
                    mv.visitTypeInsn(NEW, "com/skcraft/plume/event/CrashEvent");
                    // [exc, exc, event]
                    mv.visitInsn(DUP_X1);
                    // [exc, event, exc, event]
                    mv.visitInsn(SWAP);
                    // [exc, event, event, exc]
                    mv.visitMethodInsn(INVOKESPECIAL, "com/skcraft/plume/event/CrashEvent", "<init>", "(Ljava/lang/Throwable;)V", false);
                    // [exc, event]
                    mv.visitInsn(DUP);
                    // [exc, event, event]
                    mv.visitFieldInsn(GETSTATIC, "com/skcraft/plume/common/util/event/PlumeEventBus", "INSTANCE", "Lcom/skcraft/plume/common/util/event/EventBus;");
                    // [exc, event, event, bus]
                    mv.visitInsn(SWAP);
                    // [exc, event, bus, event]
                    mv.visitMethodInsn(INVOKEINTERFACE, "com/skcraft/plume/common/util/event/EventBus", "post", "(Ljava/lang/Object;)Z", true);
                    // [exc, event, boolean]
                    mv.visitInsn(POP);
                    // [exc, event]
                    mv.visitMethodInsn(INVOKEVIRTUAL, "com/skcraft/plume/event/CrashEvent", "isCancelled", "()Z", false);
                    // [exc, cancelled]
                    Label pushDefaultValue = new Label();

                    mv.visitJumpInsn(IFNE, pushDefaultValue);
                    // [exc]
                    mv.visitInsn(ATHROW);

                    mv.visitLabel(pushDefaultValue);
                    // [exc]
                    mv.visitInsn(POP);
                    BytecodeUtils.visitDefaultValue(mv, returnType);

                    mv.visitLabel(afterTryCatch);

                    log.info("**PLUME** Injected crash interceptor for " + catcher.getInvokeMatcher().getClassName() + "." + catcher.getInvokeMatcher().getDevName() + "()");

                    return;
                }
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }



}
