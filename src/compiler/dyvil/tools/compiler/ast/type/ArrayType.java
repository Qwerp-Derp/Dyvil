package dyvil.tools.compiler.ast.type;

import java.util.List;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.member.Name;
import dyvil.tools.compiler.ast.method.ConstructorMatch;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.lexer.marker.MarkerList;

public class ArrayType implements IType, ITyped
{
	private IType	type;
	
	public ArrayType()
	{
	}
	
	public ArrayType(IType type)
	{
		this.type = type;
	}
	
	public static IType getArrayType(IType type, int dims)
	{
		switch (dims)
		{
		case 0:
			return type;
		case 1:
			return new ArrayType(type);
		case 2:
			return new ArrayType(new ArrayType(type));
		default:
			for (; dims > 0; dims--)
			{
				type = new ArrayType(type);
			}
			return type;
		}
	}
	
	@Override
	public int typeTag()
	{
		return ARRAY_TYPE;
	}
	
	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
	
	@Override
	public IType getType()
	{
		return this.type;
	}
	
	@Override
	public void setName(Name name)
	{
		this.type.setName(name);
	}
	
	@Override
	public Name getName()
	{
		return this.type.getName();
	}
	
	@Override
	public void setClass(IClass theClass)
	{
	}
	
	@Override
	public IClass getTheClass()
	{
		return this.type.getArrayClass();
	}
	
	@Override
	public boolean isArrayType()
	{
		return true;
	}
	
	@Override
	public int getArrayDimensions()
	{
		return 1 + this.type.getArrayDimensions();
	}
	
	@Override
	public IType getElementType()
	{
		return this.type;
	}
	
	@Override
	public IType getSuperType()
	{
		return Types.OBJECT;
	}
	
	@Override
	public boolean isSuperTypeOf(IType type)
	{
		int arrayDims = type.getArrayDimensions();
		if (arrayDims == 0)
		{
			return false;
		}
		int thisDims = this.getArrayDimensions();
		if (arrayDims > thisDims)
		{
			return this.type.getTheClass() == Types.OBJECT_CLASS;
		}
		IType elementType = type.getElementType();
		if (elementType.isPrimitive() != this.type.isPrimitive())
		{
			return false;
		}
		return this.type.isSuperTypeOf(elementType);
	}
	
	@Override
	public boolean isResolved()
	{
		return this.type.isResolved();
	}
	
	@Override
	public IType resolve(MarkerList markers, IContext context)
	{
		this.type = this.type.resolve(markers, context);
		return this;
	}
	
	@Override
	public boolean hasTypeVariables()
	{
		return this.type.hasTypeVariables();
	}
	
	@Override
	public IType getConcreteType(ITypeContext context)
	{
		return new ArrayType(this.type.getConcreteType(context));
	}
	
	@Override
	public IType resolveType(ITypeVariable typeVar)
	{
		return this.type.resolveType(typeVar);
	}
	
	@Override
	public IType resolveType(ITypeVariable typeVar, IType concrete)
	{
		return concrete.isArrayType() ? this.type.resolveType(typeVar, concrete.getElementType()) : null;
	}
	
	@Override
	public boolean isStatic()
	{
		return true;
	}
	
	@Override
	public IClass getThisClass()
	{
		return this.type.getArrayClass();
	}
	
	@Override
	public Package resolvePackage(Name name)
	{
		return null;
	}
	
	@Override
	public IClass resolveClass(Name name)
	{
		return null;
	}
	
	@Override
	public ITypeVariable resolveTypeVariable(Name name)
	{
		return null;
	}
	
	@Override
	public IField resolveField(Name name)
	{
		return null;
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, IValue instance, Name name, IArguments arguments)
	{
		this.type.getArrayClass().getMethodMatches(list, instance, name, arguments);
	}
	
	@Override
	public void getConstructorMatches(List<ConstructorMatch> list, IArguments arguments)
	{
	}
	
	@Override
	public byte getVisibility(IMember member)
	{
		return this.type.getArrayClass().getVisibility(member);
	}
	
	@Override
	public IMethod getFunctionalMethod()
	{
		return null;
	}
	
	@Override
	public void setInternalName(String name)
	{
	}
	
	@Override
	public String getInternalName()
	{
		return this.getExtendedName();
	}
	
	@Override
	public void appendExtendedName(StringBuilder buffer)
	{
		buffer.append('[');
		this.type.appendExtendedName(buffer);
	}
	
	@Override
	public String getSignature()
	{
		String s = this.type.getSignature();
		if (s != null)
		{
			return '[' + s;
		}
		return null;
	}
	
	@Override
	public void appendSignature(StringBuilder buffer)
	{
		buffer.append('[');
		this.type.appendSignature(buffer);
	}
	
	@Override
	public void writeTypeExpression(MethodWriter writer) throws BytecodeException
	{
		this.type.writeTypeExpression(writer);
		writer.writeInvokeInsn(Opcodes.INVOKESTATIC, "dyvil/reflect/type/ArrayType", "apply", "(Ldyvil/lang/Type;)Ldyvil/reflect/type/ArrayType;", false);
	}
	
	@Override
	public IType clone()
	{
		return new ArrayType(this.type);
	}
	
	@Override
	public String toString()
	{
		return "[" + this.type.toString() + "]";
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append('[');
		this.type.toString(prefix, buffer);
		buffer.append(']');
	}
}
