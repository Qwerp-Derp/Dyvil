package dyvil.tools.compiler.backend.visitor;

import org.objectweb.asm.*;

import dyvil.reflect.Modifiers;
import dyvil.tools.compiler.DyvilCompiler;
import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.bytecode.*;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.type.AnnotationType;
import dyvil.tools.compiler.backend.ClassFormat;

public class BytecodeVisitor extends MethodVisitor
{
	private int			labelCount;
	private IMethod		method;
	
	private boolean		inline;
	private Bytecode	bytecode;
	
	public BytecodeVisitor(IMethod method)
	{
		super(DyvilCompiler.asmVersion);
		this.method = method;
	}
	
	@Override
	public void visitParameter(String name, int index)
	{
		this.method.getParameter(index).setName(Name.getQualified(name));
	}
	
	@Override
	public AnnotationVisitor visitAnnotationDefault()
	{
		return null;
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String type, boolean visible)
	{
		String packName = ClassFormat.internalToPackage2(type);
		if (packName.equals("dyvil.lang.annotation.inline"))
		{
			this.method.addModifier(Modifiers.INLINE);
			this.inline = true;
		}
		
		if (this.method.addRawAnnotation(packName))
		{
			AnnotationType atype = new AnnotationType(packName);
			Annotation annotation = new Annotation(atype);
			return new AnnotationVisitorImpl(this.method, annotation);
		}
		return null;
	}
	
	@Override
	public AnnotationVisitor visitTypeAnnotation(int id, TypePath typePath, String type, boolean visible)
	{
		return null;
	}
	
	@Override
	public AnnotationVisitor visitParameterAnnotation(int index, String type, boolean visible)
	{
		return null;
	}
	
	@Override
	public void visitAttribute(Attribute attribute)
	{
	}
	
	@Override
	public void visitCode()
	{
		if (this.inline)
		{
			this.bytecode = new Bytecode(null);
		}
	}
	
	@Override
	public void visitFrame(int type, int stackCount, Object[] stack, int localCount, Object[] locals)
	{
		// Ignore Stack Map Frames as they are computed by MethodWriter anyway.
	}
	
	@Override
	public void visitInsn(int opcode)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new Instruction(opcode));
		}
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new IntInstruction(opcode, operand));
		}
	}
	
	@Override
	public void visitVarInsn(int opcode, int index)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new VarInstruction(opcode, index));
		}
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new TypeInstruction(opcode, type));
		}
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new FieldInstruction(opcode, owner, name, desc));
		}
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new MethodInstruction(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE));
		}
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new MethodInstruction(opcode, owner, name, desc, isInterface));
		}
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String type, Handle bsm, Object... bsmArgs)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new InvokeDynamicInstruction(name, type, bsm, bsmArgs));
		}
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label target)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new JumpInstruction(opcode, new dyvil.tools.compiler.ast.statement.Label(target)));
		}
	}
	
	@Override
	public void visitLabel(Label label)
	{
		if (this.inline)
		{
			String name = "L" + this.labelCount++;
			label.info = name;
			this.bytecode.addLabel(new dyvil.tools.compiler.ast.statement.Label(label, Name.getQualified(name)));
		}
	}
	
	@Override
	public void visitLdcInsn(Object constant)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new LDCInstruction(constant));
		}
	}
	
	@Override
	public void visitIincInsn(int index, int value)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new IIncInstruction(index, value));
		}
	}
	
	@Override
	public void visitTableSwitchInsn(int start, int end, Label defaultHandler, Label... handlers)
	{
		if (this.inline)
		{
			int len = handlers.length;
			dyvil.tools.compiler.ast.statement.Label[] labels = new dyvil.tools.compiler.ast.statement.Label[len];
			for (int i = 0; i < len; i++)
			{
				labels[i] = new dyvil.tools.compiler.ast.statement.Label(handlers[i]);
			}
			this.bytecode.addInstruction(new TableSwitchInstruction(start, end, new dyvil.tools.compiler.ast.statement.Label(defaultHandler), labels));
		}
	}
	
	@Override
	public void visitLookupSwitchInsn(Label defaultHandler, int[] keys, Label[] handlers)
	{
		if (this.inline)
		{
			int len = handlers.length;
			dyvil.tools.compiler.ast.statement.Label[] labels = new dyvil.tools.compiler.ast.statement.Label[len];
			for (int i = 0; i < len; i++)
			{
				labels[i] = new dyvil.tools.compiler.ast.statement.Label(handlers[i]);
			}
			this.bytecode.addInstruction(new LookupSwitchInstruction(new dyvil.tools.compiler.ast.statement.Label(defaultHandler), keys, labels));
		}
	}
	
	@Override
	public void visitMultiANewArrayInsn(String type, int dims)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new MultiArrayInstruction(type, dims));
		}
	}
	
	@Override
	public AnnotationVisitor visitInsnAnnotation(int id, TypePath typePath, String type, boolean visible)
	{
		return null;
	}
	
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
	{
		if (this.inline)
		{
			this.bytecode.addInstruction(new TryCatchInstruction(new dyvil.tools.compiler.ast.statement.Label(start),
					new dyvil.tools.compiler.ast.statement.Label(end), new dyvil.tools.compiler.ast.statement.Label(handler), type));
		}
	}
	
	@Override
	public AnnotationVisitor visitTryCatchAnnotation(int id, TypePath typePath, String type, boolean visible)
	{
		return null;
	}
	
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index)
	{
		if (this.method.hasModifier(Modifiers.STATIC))
		{
			if (index < this.method.parameterCount())
			{
				this.method.getParameter(index).setName(Name.getQualified(name));
			}
		}
		else if (index != 0 && index <= this.method.parameterCount())
		{
			this.method.getParameter(index - 1).setName(Name.getQualified(name));
		}
	}
	
	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int id, TypePath typePath, Label[] start, Label[] end, int[] index, String type, boolean visible)
	{
		return null;
	}
	
	@Override
	public void visitLineNumber(int lineNumber, Label label)
	{
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals)
	{
	}
	
	@Override
	public void visitEnd()
	{
		if (this.inline)
		{
			this.method.setValue(this.bytecode);
		}
	}
}
