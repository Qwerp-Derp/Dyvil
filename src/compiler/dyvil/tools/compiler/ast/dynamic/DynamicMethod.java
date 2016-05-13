package dyvil.tools.compiler.ast.dynamic;

import dyvil.array.ObjectArray;
import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Handle;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.context.IDefaultContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.generic.GenericData;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.modifiers.EmptyModifiers;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.IParameterList;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.ClassFormat;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.ElementType;

public class DynamicMethod implements IMethod, IDefaultContext
{
	public static final Handle BOOTSTRAP = new Handle(ClassFormat.H_INVOKESTATIC, "dyvil/runtime/DynamicLinker",
	                                                  "linkMethod",
	                                                  "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");

	public Name name;
	
	public DynamicMethod(Name name)
	{
		this.name = name;
	}
	
	@Override
	public ICodePosition getPosition()
	{
		return null;
	}
	
	@Override
	public void setPosition(ICodePosition position)
	{
	}
	
	@Override
	public void setEnclosingClass(IClass enclosingClass)
	{
	}
	
	@Override
	public IClass getEnclosingClass()
	{
		return null;
	}
	
	@Override
	public void setType(IType type)
	{
	}
	
	@Override
	public IType getType()
	{
		return Types.DYNAMIC;
	}

	@Override
	public void setTypeParametric()
	{
	}
	
	@Override
	public boolean isTypeParametric()
	{
		return false;
	}
	
	@Override
	public int typeParameterCount()
	{
		return 0;
	}
	
	@Override
	public void setTypeParameters(ITypeParameter[] typeParameters, int count)
	{
	}
	
	@Override
	public void setTypeParameter(int index, ITypeParameter typeParameter)
	{
	}
	
	@Override
	public void addTypeParameter(ITypeParameter typeParameter)
	{
	}
	
	@Override
	public ITypeParameter[] getTypeParameters()
	{
		return null;
	}
	
	@Override
	public ITypeParameter getTypeParameter(int index)
	{
		return null;
	}
	
	@Override
	public void setValue(IValue value)
	{
	}
	
	@Override
	public IValue getValue()
	{
		return null;
	}
	
	// Name
	
	@Override
	public void setName(Name name)
	{
		this.name = name;
	}
	
	@Override
	public Name getName()
	{
		return this.name;
	}
	
	// Modifiers
	
	@Override
	public void setModifiers(ModifierSet modifiers)
	{
	}
	
	@Override
	public ModifierSet getModifiers()
	{
		return EmptyModifiers.INSTANCE;
	}
	
	@Override
	public boolean hasModifier(int mod)
	{
		return false;
	}
	
	@Override
	public boolean isAbstract()
	{
		return false;
	}

	@Override
	public boolean isObjectMethod()
	{
		return false;
	}

	@Override
	public int getAccessLevel()
	{
		return 0;
	}
	
	@Override
	public AnnotationList getAnnotations()
	{
		return null;
	}
	
	@Override
	public void setAnnotations(AnnotationList annotations)
	{
	}
	
	@Override
	public void addAnnotation(IAnnotation annotation)
	{
	}
	
	@Override
	public ElementType getElementType()
	{
		return ElementType.METHOD;
	}
	
	@Override
	public IAnnotation getAnnotation(IClass type)
	{
		return null;
	}

	@Override
	public IParameterList getParameterList()
	{
		return IParameterList.EMPTY;
	}

	@Override
	public int exceptionCount()
	{
		return 0;
	}
	
	@Override
	public void setException(int index, IType exception)
	{
	}
	
	@Override
	public void addException(IType exception)
	{
	}
	
	@Override
	public IType getException(int index)
	{
		return null;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void resolve(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public boolean checkOverride(IMethod candidate, ITypeContext typeContext)
	{
		return false;
	}

	@Override
	public void addOverride(IMethod method)
	{
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void foldConstants()
	{
	}
	
	@Override
	public void cleanup(IContext context, IClassCompilableList compilableList)
	{
	}
	
	@Override
	public float getSignatureMatch(Name name, IValue instance, IArguments arguments)
	{
		return 0;
	}
	
	@Override
	public IValue checkArguments(MarkerList markers, ICodePosition position, IContext context, IValue instance, IArguments arguments, GenericData genericData)
	{
		return instance;
	}
	
	@Override
	public void checkCall(MarkerList markers, ICodePosition position, IContext context, IValue instance, IArguments arguments, ITypeContext typeContext)
	{
	}
	
	@Override
	public boolean hasTypeVariables()
	{
		return false;
	}
	
	@Override
	public GenericData getGenericData(GenericData data, IValue instance, IArguments arguments)
	{
		return null;
	}
	
	@Override
	public boolean isIntrinsic()
	{
		return false;
	}
	
	@Override
	public int getInvokeOpcode()
	{
		return Opcodes.INVOKEDYNAMIC;
	}

	@Override
	public Handle toHandle()
	{
		return null;
	}

	@Override
	public String getDescriptor()
	{
		return null;
	}
	
	@Override
	public String getSignature()
	{
		return null;
	}
	
	@Override
	public String[] getInternalExceptions()
	{
		return null;
	}
	
	@Override
	public void write(ClassWriter writer) throws BytecodeException
	{
	}
	
	@Override
	public void writeCall(MethodWriter writer, IValue instance, IArguments arguments, ITypeContext typeContext, IType targetType, int lineNumber)
			throws BytecodeException
	{
		StringBuilder desc = new StringBuilder();
		desc.append('(');
		
		if (instance != null)
		{
			instance.writeExpression(writer, Types.OBJECT);
			instance.getType().appendExtendedName(desc);
		}
		
		for (IValue v : arguments)
		{
			v.writeExpression(writer, Types.OBJECT);
			v.getType().appendExtendedName(desc);
		}
		desc.append(')');
		desc.append("Ljava/lang/Object;");
		
		writer.visitInvokeDynamicInsn(this.name.qualified, desc.toString(), BOOTSTRAP, ObjectArray.EMPTY);

		Types.OBJECT.writeCast(writer, targetType, lineNumber);
	}
	
	@Override
	public void writeInvoke(MethodWriter writer, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
			throws BytecodeException
	{
		throw new BytecodeException();
	}
	
	@Override
	public void writeJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
			throws BytecodeException
	{
		this.writeCall(writer, instance, arguments, typeContext, Types.BOOLEAN, lineNumber);
		writer.visitJumpInsn(Opcodes.IFNE, dest);
	}
	
	@Override
	public void writeInvJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
			throws BytecodeException
	{
		this.writeCall(writer, instance, arguments, typeContext, Types.BOOLEAN, lineNumber);
		writer.visitJumpInsn(Opcodes.IFEQ, dest);
	}
	
	@Override
	public void writeSignature(DataOutput out) throws IOException
	{
	}
	
	@Override
	public void readSignature(DataInput in) throws IOException
	{
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
	}
}
