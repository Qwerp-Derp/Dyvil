package dyvil.tools.compiler.ast.type.typevar;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constructor.IConstructor;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.raw.IRawType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TypeVarType implements IRawType
{
	protected ITypeParameter typeParameter;
	
	public TypeVarType()
	{
	}
	
	public TypeVarType(ITypeParameter typeParameter)
	{
		this.typeParameter = typeParameter;
	}
	
	@Override
	public int typeTag()
	{
		return TYPE_VAR_TYPE;
	}
	
	@Override
	public Name getName()
	{
		return this.typeParameter.getName();
	}

	@Override
	public IType atPosition(ICodePosition position)
	{
		return new ResolvedTypeVarType(this.typeParameter, position);
	}

	@Override
	public ITypeParameter getTypeVariable()
	{
		return this.typeParameter;
	}
	
	@Override
	public boolean isGenericType()
	{
		return false;
	}
	
	@Override
	public IClass getTheClass()
	{
		return this.typeParameter.getTheClass();
	}

	@Override
	public IType asParameterType()
	{
		return this.typeParameter.getCovariantType();
	}

	@Override
	public int subTypeCheckLevel()
	{
		return SUBTYPE_TYPEVAR;
	}

	@Override
	public boolean isSameType(IType type)
	{
		return this.typeParameter == type.asReturnType().getTypeVariable();
	}

	@Override
	public boolean isSameClass(IType type)
	{
		return this.typeParameter.isSameClass(type);
	}

	@Override
	public boolean isSuperTypeOf(IType type)
	{
		return this.isSameType(type);
	}

	@Override
	public boolean isSuperClassOf(IType subType)
	{
		return this.typeParameter.isSuperClassOf(subType);
	}

	@Override
	public boolean isSubTypeOf(IType superType)
	{
		return this.typeParameter.isSubTypeOf(superType);
	}

	@Override
	public boolean isSubClassOf(IType superType)
	{
		return this.typeParameter.isSubClassOf(superType);
	}

	@Override
	public boolean hasTypeVariables()
	{
		return true;
	}
	
	@Override
	public IType getConcreteType(ITypeContext context)
	{
		if (context == null)
		{
			return this;
		}

		final IType concreteType = context.resolveType(this.typeParameter);
		return concreteType != null ? concreteType : this;
	}
	
	@Override
	public IType resolveType(ITypeParameter typeParameter)
	{
		return this.typeParameter == typeParameter ? this : this.typeParameter.getDefaultType().resolveType(typeParameter);
	}
	
	@Override
	public void inferTypes(IType concrete, ITypeContext typeContext)
	{
		typeContext.addMapping(this.typeParameter, concrete);
	}
	
	@Override
	public boolean isResolved()
	{
		return true;
	}
	
	@Override
	public IType resolveType(MarkerList markers, IContext context)
	{
		return this;
	}
	
	@Override
	public void checkType(MarkerList markers, IContext context, TypePosition position)
	{
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		return this.typeParameter.resolveField(name);
	}
	
	@Override
	public void getMethodMatches(MatchList<IMethod> list, IValue receiver, Name name, IArguments arguments)
	{
		this.typeParameter.getMethodMatches(list, receiver, name, arguments);
	}

	@Override
	public void getImplicitMatches(MatchList<IMethod> list, IValue value, IType targetType)
	{
		this.typeParameter.getImplicitMatches(list, value, targetType);
	}

	@Override
	public void getConstructorMatches(MatchList<IConstructor> list, IArguments arguments)
	{
	}
	
	@Override
	public IMethod getFunctionalMethod()
	{
		return null;
	}
	
	@Override
	public String getInternalName()
	{
		if (this.typeParameter.upperBoundCount() > 0)
		{
			return this.typeParameter.getUpperBound(0).getInternalName();
		}
		return "java/lang/Object";
	}
	
	@Override
	public void appendExtendedName(StringBuilder buffer)
	{
		buffer.append('L').append(this.getInternalName()).append(';');
	}
	
	@Override
	public String getSignature()
	{
		StringBuilder buffer = new StringBuilder();
		this.appendSignature(buffer);
		return buffer.toString();
	}
	
	@Override
	public void appendSignature(StringBuilder buffer)
	{
		buffer.append('T').append(this.typeParameter.getName().qualified).append(';');
	}

	@Override
	public void writeClassExpression(MethodWriter writer) throws BytecodeException
	{
		final ITypeParameter.ReifiedKind reifiedKind = this.typeParameter.getReifiedKind();
		if (reifiedKind != ITypeParameter.ReifiedKind.NOT_REIFIED)
		{
			// Get the parameter
			final int parameterIndex = this.typeParameter.getParameterIndex();
			writer.visitVarInsn(Opcodes.ALOAD, parameterIndex);

			// The generic Type is reified -> extract erasure class
			if (reifiedKind == ITypeParameter.ReifiedKind.REIFIED_TYPE)
			{
				writer.visitMethodInsn(Opcodes.INVOKEINTERFACE, "dyvilx/lang/model/type/Type", "erasure",
				                       "()Ljava/lang/Class;", true);
			}
			return;
		}

		throw new Error("Type Variable Types cannot be used in Class Operators");
	}

	@Override
	public void writeTypeExpression(MethodWriter writer) throws BytecodeException
	{
		if (this.typeParameter.getReifiedKind() == ITypeParameter.ReifiedKind.REIFIED_TYPE)
		{
			final int parameterIndex = this.typeParameter.getParameterIndex();
			writer.visitVarInsn(Opcodes.ALOAD, parameterIndex);
			return;
		}

		throw new Error("Type Variable Types cannot be used in Type Operators");
	}
	
	@Override
	public void write(DataOutput out) throws IOException
	{
		out.writeUTF(this.typeParameter.getName().qualified);
	}
	
	@Override
	public void read(DataInput in) throws IOException
	{
		in.readUTF();
		throw new Error("Cannot decode Type Variable Type");
	}
	
	@Override
	public IType clone()
	{
		return new TypeVarType(this.typeParameter);
	}
	
	@Override
	public String toString()
	{
		return this.typeParameter.getName().toString();
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(this.typeParameter.getName());
	}
}
