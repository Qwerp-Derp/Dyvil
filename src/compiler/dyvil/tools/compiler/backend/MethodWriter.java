package dyvil.tools.compiler.backend;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

import java.util.LinkedList;

import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.PrimitiveType;

public final class MethodWriter extends MethodVisitor
{
	public static final Long		LONG_MINUS_ONE	= Long.valueOf(-1);
	public static final Integer		TOP				= jdk.internal.org.objectweb.asm.Opcodes.TOP;
	public static final Integer		INT				= jdk.internal.org.objectweb.asm.Opcodes.INTEGER;
	public static final Object[]	EMPTY_STACK		= new Object[0];
	
	private boolean					hasReturn;
	private int						maxLocals;
	private int						maxStack;
	
	private int						localCount;
	private Object[]				locals			= new Object[2];
	private LinkedList				typeStack		= new LinkedList();
	
	public MethodWriter(MethodVisitor mv)
	{
		super(ASM5, mv);
	}
	
	public void setConstructor(IType type)
	{
		this.locals[0] = UNINITIALIZED_THIS;
		this.push(UNINITIALIZED_THIS);
	}
	
	private void ensureLocals(int count)
	{
		if (count > this.locals.length)
		{
			Object[] newLocals = new Object[count];
			System.arraycopy(this.locals, 0, newLocals, 0, this.locals.length);
			this.locals = newLocals;
			this.localCount = count;
			this.maxLocals = count;
			return;
		}
		if (count > this.maxLocals)
		{
			this.maxLocals = count;
			this.localCount = count;
			return;
		}
		if (count > this.localCount)
		{
			this.localCount = count;
		}
	}
	
	public void addLocal(int index, IType type)
	{
		this.ensureLocals(index + 1);
		this.locals[index] = type.getFrameType();
	}
	
	public void addLocal(int index, Object type)
	{
		this.ensureLocals(index + 1);
		this.locals[index] = type;
	}
	
	public void removeLocals(int count)
	{
		this.localCount -= count;
	}
	
	protected void set(Object type)
	{
		this.typeStack.removeFirst();
		this.typeStack.push(type);
	}
	
	protected void push(Object type)
	{
		this.typeStack.push(type);
		int size = this.typeStack.size();
		if (size > this.maxStack)
		{
			this.maxStack = size;
		}
	}
	
	public void push(IType type)
	{
		Object frameType = type.getFrameType();
		if (frameType != null)
		{
			this.typeStack.push(frameType);
			int size = this.typeStack.size();
			if (size > this.maxStack)
			{
				this.maxStack = size;
			}
		}
	}
	
	public void pop()
	{
		this.typeStack.pop();
	}
	
	@Override
	@Deprecated
	public void visitParameter(String desc, int index)
	{
	}
	
	public void visitParameter(String name, IType type, int index)
	{
		this.addLocal(index, type.getFrameType());
		this.mv.visitParameter(name, index);
	}
	
	public void visitParameter(String name, Object type, int index)
	{
		this.addLocal(index, type);
		this.mv.visitParameter(name, index);
	}
	
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index)
	{
		this.mv.visitLocalVariable(name, desc, signature, start, end, index);
	}
	
	public void visitLdcInsn(int value)
	{
		this.push(INTEGER);
		switch (value)
		{
		case -1:
			this.mv.visitInsn(ICONST_M1);
			return;
		case 0:
			this.mv.visitInsn(ICONST_0);
			return;
		case 1:
			this.mv.visitInsn(ICONST_1);
			return;
		case 2:
			this.mv.visitInsn(ICONST_2);
			return;
		case 3:
			this.mv.visitInsn(ICONST_3);
			return;
		case 4:
			this.mv.visitInsn(ICONST_4);
			return;
		case 5:
			this.mv.visitInsn(ICONST_5);
			return;
		}
		this.mv.visitLdcInsn(Integer.valueOf(value));
	}
	
	public void visitLdcInsn(long value)
	{
		this.push(LONG);
		if (value == 0L)
		{
			this.mv.visitInsn(LCONST_0);
			return;
		}
		if (value == 1L)
		{
			this.mv.visitInsn(LCONST_1);
			return;
		}
		this.mv.visitLdcInsn(Long.valueOf(value));
	}
	
	public void visitLdcInsn(float value)
	{
		this.push(FLOAT);
		if (value == 0F)
		{
			this.mv.visitInsn(FCONST_0);
			return;
		}
		if (value == 1F)
		{
			this.mv.visitInsn(FCONST_1);
			return;
		}
		if (value == 2F)
		{
			this.mv.visitInsn(FCONST_2);
			return;
		}
		this.mv.visitLdcInsn(Float.valueOf(value));
	}
	
	public void visitLdcInsn(double value)
	{
		this.push(DOUBLE);
		if (value == 0D)
		{
			this.mv.visitInsn(DCONST_0);
			return;
		}
		if (value == 1D)
		{
			this.mv.visitInsn(DCONST_1);
			return;
		}
		this.mv.visitLdcInsn(Double.valueOf(value));
	}
	
	public void visitLdcInsn(String value)
	{
		this.push("Ljava/lang/String;");
		this.mv.visitLdcInsn(value);
	}
	
	@Override
	@Deprecated
	public void visitLdcInsn(Object obj)
	{
		Class c = obj.getClass();
		if (c == String.class)
		{
			this.push("Ljava/lang/String;");
		}
		else if (c == Integer.class)
		{
			this.push(INTEGER);
		}
		else if (c == Long.class)
		{
			this.push(LONG);
		}
		else if (c == Float.class)
		{
			this.push(FLOAT);
		}
		else if (c == Double.class)
		{
			this.push(DOUBLE);
		}
		this.mv.visitLdcInsn(obj);
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label)
	{
		if (opcode >= IFEQ && opcode <= IFLE)
		{
			this.visitFrame();
			this.typeStack.pop();
		}
		if (opcode >= IF_ICMPEQ && opcode <= IF_ICMPLE)
		{
			this.visitFrame();
			this.typeStack.pop();
			this.typeStack.pop();
		}
		this.mv.visitJumpInsn(opcode, label);
	}
	
	public void visitJumpInsn2(int opcode, Label label)
	{
		if (opcode >= IFEQ && opcode <= IFLE)
		{
			this.typeStack.pop();
		}
		if (opcode >= IF_ICMPEQ && opcode <= IF_ICMPLE)
		{
			this.typeStack.pop();
			this.typeStack.pop();
		}
		this.mv.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLabel(Label label)
	{
		this.visitFrame();
		this.mv.visitLabel(label);
	}
	
	public void visitLabel2(Label label)
	{
		this.mv.visitLabel(label);
	}
	
	private void visitFrame()
	{
		if (this.maxStack == 0)
		{
			this.mv.visitFrame(F_SAME, 0, null, 0, null);
			return;
		}
		
		int len = this.typeStack.size();
		if (len == 0)
		{
			this.mv.visitFrame(F_NEW, this.localCount, this.locals, 0, EMPTY_STACK);
			return;
		}
		
		Object[] o = new Object[len];
		for (int i = 0; i < len; i++)
		{
			o[i] = this.typeStack.get(i);
		}
		this.mv.visitFrame(F_NEW, this.localCount, this.locals, len, o);
	}
	
	@Override
	public void visitInsn(int opcode)
	{
		if (opcode > 255)
		{
			this.visitSpecialInsn(opcode);
			return;
		}
		else if (opcode >= IASTORE && opcode <= SASTORE)
		{
			this.typeStack.pop(); // Index
			this.typeStack.pop(); // Array
			this.typeStack.pop(); // Value
		}
		else if (opcode >= IRETURN && opcode <= ARETURN)
		{
			this.typeStack.pop();
		}
		else
		{
			this.processInsn(opcode);
		}
		this.mv.visitInsn(opcode);
	}
	
	private void processInsn(int opcode)
	{
		switch (opcode)
		{
		case DUP:
			this.typeStack.push(this.typeStack.peek());
			return;
		case SWAP:
		{
			Object o1 = this.typeStack.pop();
			Object o2 = this.typeStack.pop();
			this.typeStack.push(o1);
			this.typeStack.push(o2);
			return;
		}
		case POP:
			this.typeStack.pop();
			return;
		case ACONST_NULL:
			this.push(NULL);
			return;
		case ARRAYLENGTH:
			this.set(INTEGER);
			return;
		case RETURN:
			this.hasReturn = true;
			return;
		case BALOAD:
		case SALOAD:
		case CALOAD:
		case IALOAD:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(INTEGER);
			break;
		case LALOAD:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(LONG);
			break;
		case FALOAD:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(FLOAT);
			break;
		case DALOAD:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(DOUBLE);
			break;
		case AALOAD:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(TOP);
		case IADD:
		case ISUB:
		case IMUL:
		case IDIV:
		case IREM:
		case ISHL:
		case ISHR:
		case IUSHR:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(INTEGER);
			return;
		case LADD:
		case LSUB:
		case LMUL:
		case LDIV:
		case LREM:
		case LSHL:
		case LSHR:
		case LUSHR:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(LONG);
			return;
		case FADD:
		case FSUB:
		case FMUL:
		case FDIV:
		case FREM:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(FLOAT);
			return;
		case DADD:
		case DSUB:
		case DMUL:
		case DDIV:
		case DREM:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(DOUBLE);
			return;
			// Casts
		case L2I:
		case F2I:
		case D2I:
			this.set(INTEGER);
		case I2L:
		case F2L:
		case D2L:
			this.set(LONG);
			return;
		case I2F:
		case L2F:
		case D2F:
			this.set(FLOAT);
			return;
			// Comparison Operators
		case LCMP:
		case FCMPL:
		case FCMPG:
		case DCMPL:
		case DCMPG:
			this.typeStack.pop();
			this.typeStack.pop();
			this.push(INTEGER);
			return;
		}
	}
	
	public void visitSpecialInsn(int opcode)
	{
		switch (opcode)
		{
		case Opcodes.LCONST_M1:
			this.mv.visitLdcInsn(LONG_MINUS_ONE);
			return;
		case Opcodes.IBIN:
			this.mv.visitInsn(Opcodes.ICONST_M1);
			this.mv.visitInsn(Opcodes.IXOR);
			return;
		case Opcodes.LBIN:
			this.mv.visitLdcInsn(LONG_MINUS_ONE);
			this.mv.visitInsn(Opcodes.IXOR);
			return;
		case Opcodes.L2B:
			this.mv.visitInsn(Opcodes.L2I);
			this.mv.visitInsn(Opcodes.I2B);
			this.set(INTEGER);
			return;
		case Opcodes.L2S:
			this.mv.visitInsn(Opcodes.L2I);
			this.mv.visitInsn(Opcodes.I2S);
			this.set(INTEGER);
			return;
		case Opcodes.L2C:
			this.mv.visitInsn(Opcodes.L2I);
			this.mv.visitInsn(Opcodes.I2C);
			this.set(INTEGER);
			return;
		case Opcodes.F2B:
			this.mv.visitInsn(Opcodes.F2I);
			this.mv.visitInsn(Opcodes.I2B);
			this.set(INTEGER);
			return;
		case Opcodes.F2S:
			this.mv.visitInsn(Opcodes.F2I);
			this.mv.visitInsn(Opcodes.I2S);
			this.set(INTEGER);
			return;
		case Opcodes.F2C:
			this.mv.visitInsn(Opcodes.F2I);
			this.mv.visitInsn(Opcodes.I2C);
			this.set(INTEGER);
			return;
		case Opcodes.D2B:
			this.mv.visitInsn(Opcodes.D2I);
			this.mv.visitInsn(Opcodes.I2B);
			this.set(INTEGER);
			return;
		case Opcodes.D2S:
			this.mv.visitInsn(Opcodes.D2I);
			this.mv.visitInsn(Opcodes.I2S);
			this.set(INTEGER);
			return;
		case Opcodes.D2C:
			this.mv.visitInsn(Opcodes.D2I);
			this.mv.visitInsn(Opcodes.I2C);
			this.set(INTEGER);
			return;
		case Opcodes.IF_LCMPEQ:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.LCMP);
			this.mv.visitInsn(Opcodes.IFEQ);
			return;
		case Opcodes.IF_LCMPNE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.LCMP);
			this.mv.visitInsn(Opcodes.IFNE);
			return;
		case Opcodes.IF_LCMPLT:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.LCMP);
			this.mv.visitInsn(Opcodes.IFLT);
			return;
		case Opcodes.IF_LCMPGE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.LCMP);
			this.mv.visitInsn(Opcodes.IFGE);
			return;
		case Opcodes.IF_LCMPGT:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.LCMP);
			this.mv.visitInsn(Opcodes.IFGT);
			return;
		case Opcodes.IF_LCMPLE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.LCMP);
			this.mv.visitInsn(Opcodes.IFLE);
			return;
		case Opcodes.IF_FCMPEQ:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.FCMPL);
			this.mv.visitInsn(Opcodes.IFEQ);
			return;
		case Opcodes.IF_FCMPNE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.FCMPL);
			this.mv.visitInsn(Opcodes.IFNE);
			return;
		case Opcodes.IF_FCMPLT:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.FCMPL);
			this.mv.visitInsn(Opcodes.IFLT);
			return;
		case Opcodes.IF_FCMPGE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.FCMPG);
			this.mv.visitInsn(Opcodes.IFGE);
			return;
		case Opcodes.IF_FCMPGT:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.FCMPG);
			this.mv.visitInsn(Opcodes.IFGT);
			return;
		case Opcodes.IF_FCMPLE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.FCMPL);
			this.mv.visitInsn(Opcodes.IFLE);
			return;
		case Opcodes.IF_DCMPEQ:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitInsn(Opcodes.IFEQ);
			return;
		case Opcodes.IF_DCMPNE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitInsn(Opcodes.IFNE);
			return;
		case Opcodes.IF_DCMPLT:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitInsn(Opcodes.IFLT);
			return;
		case Opcodes.IF_DCMPGE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.DCMPG);
			this.mv.visitInsn(Opcodes.IFGE);
			return;
		case Opcodes.IF_DCMPGT:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.DCMPG);
			this.mv.visitInsn(Opcodes.IFGT);
			return;
		case Opcodes.IF_DCMPLE:
			this.typeStack.pop();
			this.typeStack.pop();
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitInsn(Opcodes.IFLE);
			return;
		}
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type)
	{
		if (opcode == NEW)
		{
			this.push(type);
		}
		this.mv.visitTypeInsn(opcode, type);
	}
	
	public void visitTypeInsn(int opcode, IType type)
	{
		if ((opcode == ANEWARRAY || opcode == NEWARRAY) && type instanceof PrimitiveType)
		{
			this.mv.visitIntInsn(NEWARRAY, ((PrimitiveType) type).typecode);
			return;
		}
		if (opcode == NEW)
		{
			this.push(type);
		}
		this.mv.visitTypeInsn(opcode, type.getInternalName());
	}
	
	@Override
	public void visitVarInsn(int opcode, int index)
	{
		if (opcode >= ILOAD && opcode <= ALOAD)
		{
			this.push(this.locals[index]);
		}
		else if (opcode >= ISTORE && opcode <= ASTORE)
		{
			this.typeStack.pop();
		}
		this.mv.visitVarInsn(opcode, index);
	}
	
	public void visitVarInsn(int opcode, int index, IType type)
	{
		if (type != null)
		{
			this.push(type);
		}
		else
		{
			this.typeStack.pop();
		}
		this.mv.visitVarInsn(opcode, index);
	}
	
	@Override
	@Deprecated
	public void visitFieldInsn(int opcode, String owner, String name, String desc)
	{
		this.mv.visitFieldInsn(opcode, owner, name, desc);
	}
	
	public void visitGetStatic(String owner, String name, String desc, IType type)
	{
		if (type != null)
		{
			this.push(type);
		}
		this.mv.visitFieldInsn(GETSTATIC, owner, name, desc);
	}
	
	public void visitPutStatic(String owner, String name, String desc)
	{
		this.typeStack.pop(); // Value
		this.mv.visitFieldInsn(PUTSTATIC, owner, name, desc);
	}
	
	public void visitGetField(String owner, String name, String desc, IType type)
	{
		this.typeStack.pop(); // Instance
		if (type != null)
		{
			this.push(type);
		}
		this.mv.visitFieldInsn(GETFIELD, owner, name, desc);
	}
	
	public void visitPutField(String owner, String name, String desc)
	{
		this.typeStack.pop(); // Instance
		this.typeStack.pop(); // Value
		this.mv.visitFieldInsn(PUTFIELD, owner, name, desc);
	}
	
	@Override
	@Deprecated
	public void visitMethodInsn(int opcode, String owner, String name, String desc)
	{
		this.mv.visitMethodInsn(opcode, owner, name, desc, false);
	}
	
	public void visitMethodInsn(int opcode, String owner, String name, String desc, int args, IType returnType)
	{
		this.mv.visitMethodInsn(opcode, owner, name, desc, false);
		for (int i = 0; i < args; i++)
		{
			this.typeStack.pop();
		}
		if (returnType != null)
		{
			this.push(returnType);
		}
	}
	
	@Override
	@Deprecated
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface)
	{
		this.mv.visitMethodInsn(opcode, owner, name, desc, isInterface);
	}
	
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface, int args, IType returnType)
	{
		this.mv.visitMethodInsn(opcode, owner, name, desc, isInterface);
		for (int i = 0; i < args; i++)
		{
			this.typeStack.pop();
		}
		if (returnType != null)
		{
			this.push(returnType);
		}
	}
	
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface, int args, Object returnType)
	{
		this.mv.visitMethodInsn(opcode, owner, name, desc, isInterface);
		for (int i = 0; i < args; i++)
		{
			this.typeStack.pop();
		}
		if (returnType != null)
		{
			this.push(returnType);
		}
	}
	
	@Override
	public void visitEnd()
	{
		this.mv.visitMaxs(this.maxStack, this.maxLocals);
		this.mv.visitEnd();
	}
	
	public void visitEnd(IType type)
	{
		if (!this.hasReturn)
		{
			this.mv.visitInsn(type.getReturnOpcode());
		}
		this.mv.visitMaxs(this.maxStack, this.maxLocals);
		this.mv.visitEnd();
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("MethodWriter {stack=").append(this.typeStack);
		builder.append(", locals=[");
		
		for (int i = 0; i < this.localCount; i++)
		{
			builder.append(this.locals[i]).append(", ");
		}
		
		builder.append("], maxLocals=").append(this.maxLocals);
		builder.append(", maxStack=").append(this.maxStack);
		builder.append("}");
		return builder.toString();
	}
}
