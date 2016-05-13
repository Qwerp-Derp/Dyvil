package dyvil.tools.compiler.backend.visitor;

import dyvil.reflect.Modifiers;
import dyvil.tools.asm.*;
import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.annotation.AnnotationUtil;
import dyvil.tools.compiler.ast.bytecode.*;
import dyvil.tools.compiler.ast.method.IExternalCallableMember;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.intrinsic.InlineIntrinsicData;
import dyvil.tools.compiler.ast.parameter.IParameter;
import dyvil.tools.compiler.ast.type.raw.InternalType;
import dyvil.tools.compiler.backend.ClassFormat;
import dyvil.tools.parsing.Name;

public final class SimpleMethodVisitor implements MethodVisitor
{
	private final IExternalCallableMember method;
	private       InlineIntrinsicData     intrinsicData;
	private       boolean                 unsupportedInline;
	private       int                     parameterIndex;

	public SimpleMethodVisitor(IExternalCallableMember method)
	{
		this.method = method;
	}

	@Override
	public void visitParameter(String name, int modifiers)
	{
		final IParameter parameter = this.method.getParameterList().get(this.parameterIndex);
		if (parameter == null)
		{
			return;
		}

		this.parameterIndex++;
		parameter.setName(Name.getQualified(name));

		if (modifiers != 0)
		{
			parameter.getModifiers().addIntModifier(modifiers);
		}
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String type, boolean visible)
	{
		final IParameter param = this.method.getParameterList().get(parameter);
		if (AnnotationUtil.DYVIL_MODIFIERS.equals(type))
		{
			return new ModifierVisitor(param.getModifiers());
		}

		final String internal = ClassFormat.extendedToInternal(type);
		return param.visitAnnotation(internal);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String type, boolean visible)
	{
		if (AnnotationUtil.DYVIL_MODIFIERS.equals(type))
		{
			return new ModifierVisitor(this.method.getModifiers());
		}
		if (AnnotationUtil.RECEIVER_TYPE.equals(type))
		{
			return new ReceiverTypeVisitor((IMethod) this.method);
		}

		String internal = ClassFormat.extendedToInternal(type);
		if (this.method.addRawAnnotation(internal, null))
		{
			Annotation annotation = new Annotation(new InternalType(internal));
			return new AnnotationReader(this.method, annotation);
		}
		return null;
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault()
	{
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
	{
		return this.method.visitTypeAnnotation(typeRef, typePath, desc, visible);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index)
	{
	}

	@Override
	public void visitAttribute(Attribute attr)
	{
	}

	@Override
	public boolean visitCode()
	{
		if (this.method.hasModifier(Modifiers.INLINE))
		{
			this.intrinsicData = new InlineIntrinsicData((IMethod) this.method);
			return true;
		}
		return false;
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack)
	{
	}

	@Override
	public void visitInsn(int opcode)
	{
		this.intrinsicData.addInstruction(new Instruction(opcode));
	}

	@Override
	public void visitIntInsn(int opcode, int operand)
	{
		this.intrinsicData.addInstruction(new IntInstruction(opcode, operand));
	}

	@Override
	public void visitVarInsn(int opcode, int var)
	{
		this.intrinsicData.addInstruction(new VarInstruction(opcode, var));
	}

	@Override
	public void visitTypeInsn(int opcode, String type)
	{
		this.intrinsicData.addInstruction(new TypeInstruction(opcode, type));
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc)
	{
		this.intrinsicData.addInstruction(new FieldInstruction(opcode, owner, name, desc));
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
	{
		this.intrinsicData.addInstruction(new MethodInstruction(opcode, owner, name, desc, itf));
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs)
	{
		this.intrinsicData.addInstruction(new InvokeDynamicInstruction(name, desc, bsm, bsmArgs));
	}

	@Override
	public void visitJumpInsn(int opcode, Label label)
	{
		this.unsupportedInline = true;
	}

	@Override
	public void visitLabel(Label label)
	{
		this.unsupportedInline = true;
	}

	@Override
	public void visitLdcInsn(Object cst)
	{
		this.intrinsicData.addInstruction(new LDCInstruction(cst));
	}

	@Override
	public void visitIincInsn(int var, int increment)
	{
		this.intrinsicData.addInstruction(new IIncInstruction(var, increment));
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels)
	{
		this.intrinsicData.addInstruction(new TableSwitchInstruction(min, max, dflt, labels));
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels)
	{
		this.intrinsicData.addInstruction(new LookupSwitchInstruction(dflt, keys, labels));
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims)
	{
		this.intrinsicData.addInstruction(new MultiArrayInstruction(desc, dims));
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
	{
		return null;
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
	{
		this.unsupportedInline = true;
	}

	@Override
	public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible)
	{
		return null;
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible)
	{
		return null;
	}

	@Override
	public void visitLineNumber(int line, Label start)
	{
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals)
	{
		this.intrinsicData.setMaxLocals(maxLocals);
	}

	@Override
	public void visitEnd()
	{
		if (this.intrinsicData != null && !this.unsupportedInline)
		{
			this.method.setIntrinsicData(this.intrinsicData);
		}
	}
}
