package com.skcraft.plume.asm.util;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public final class BytecodeUtils {

    private BytecodeUtils() {
    }

    public static void visitDefaultValue(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
            case Type.VOID:
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(ICONST_0);
                break;
            case Type.FLOAT:
                mv.visitInsn(FCONST_0);
                break;
            case Type.LONG:
                mv.visitInsn(LCONST_0);
                break;
            case Type.DOUBLE:
                mv.visitInsn(DCONST_0);
                break;
            case Type.OBJECT:
                switch (type.getClassName()) {
                    case "java.lang.Boolean":
                        mv.visitInsn(ICONST_1);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                        break;
                    case "java.lang.Character":
                        mv.visitInsn(ICONST_1);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                        break;
                    case "java.lang.Byte":
                        mv.visitInsn(ICONST_1);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                        break;
                    case "java.lang.Short":
                        mv.visitInsn(ICONST_1);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                        break;
                    case "java.lang.Integer":
                        mv.visitInsn(ICONST_1);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                        break;
                    case "java.lang.Float":
                        mv.visitInsn(FCONST_0);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                        break;
                    case "java.lang.Long":
                        mv.visitInsn(LCONST_0);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                        break;
                    case "java.lang.Double":
                        mv.visitInsn(DCONST_0);
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                        break;
                    default:
                        mv.visitInsn(ACONST_NULL);
                        break;
                }
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

}
