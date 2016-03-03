package dyvil.tools.compiler.ast.type.generic;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constructor.ConstructorMatchList;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.raw.ClassType;
import dyvil.tools.compiler.transform.Deprecation;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ClassGenericType extends GenericType
{
	protected IClass theClass;
	
	public ClassGenericType()
	{
	}
	
	public ClassGenericType(IClass iclass)
	{
		super(iclass.typeParameterCount());
		this.theClass = iclass;
	}
	
	public ClassGenericType(IClass iclass, IType[] typeArguments, int typeArgumentCount)
	{
		super(typeArguments, typeArgumentCount);
		this.theClass = iclass;
	}
	
	@Override
	public int typeTag()
	{
		return GENERIC;
	}
	
	// ITypeList Overrides
	
	@Override
	public boolean isGenericType()
	{
		return this.theClass.isTypeParametric();
	}
	
	@Override
	public Name getName()
	{
		return this.theClass.getName();
	}
	
	// IType Overrides
	
	@Override
	public IClass getTheClass()
	{
		return this.theClass;
	}
	
	@Override
	public boolean isSameType(IType type)
	{
		if (this == type)
		{
			return true;
		}
		
		if (!super.isSameType(type))
		{
			return false;
		}
		
		return this.argumentsMatch(type);
	}
	
	@Override
	public boolean isSuperTypeOf(IType type)
	{
		if (this == type)
		{
			return true;
		}
		
		if (!super.isSuperTypeOf(type))
		{
			return false;
		}
		
		return !type.isGenericType() || this.argumentsMatch(type);
	}
	
	protected boolean argumentsMatch(IType type)
	{
		int count = Math.min(this.typeArgumentCount, this.theClass.typeParameterCount());
		for (int i = 0; i < count; i++)
		{
			ITypeParameter typeVar = this.theClass.getTypeParameter(i);
			
			IType otherType = type.resolveTypeSafely(typeVar);
			if (!typeVar.getVariance().checkCompatible(this.typeArguments[i], otherType))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public IType combine(IType type)
	{
		if (this.argumentsMatch(type))
		{
			return this;
		}
		
		return new ClassType(this.theClass);
	}
	
	@Override
	public IType resolveType(ITypeParameter typeParameter)
	{
		int index = typeParameter.getIndex();
		
		if (this.theClass.getTypeParameter(index) != typeParameter)
		{
			return this.theClass.resolveType(typeParameter, this);
		}
		if (index > this.typeArgumentCount)
		{
			return null;
		}
		return this.typeArguments[index];
	}
	
	@Override
	public void inferTypes(IType concrete, ITypeContext typeContext)
	{
		for (int i = 0; i < this.typeArgumentCount; i++)
		{
			ITypeParameter typeVar = this.theClass.getTypeParameter(i);
			IType concreteType = concrete.resolveType(typeVar);
			if (concreteType != null)
			{
				this.typeArguments[i].inferTypes(concreteType, typeContext);
			}
		}
	}
	
	@Override
	public boolean isResolved()
	{
		return true;
	}
	
	@Override
	public void checkType(MarkerList markers, IContext context, TypePosition position)
	{
		IClass iclass = this.theClass;
		if (iclass != null)
		{
			Deprecation.checkAnnotations(markers, this.getPosition(), iclass);

			if (IContext.getVisibility(context, iclass) == IContext.INTERNAL)
			{
				markers.add(Markers.semantic(this.getPosition(), "type.access.internal", iclass.getName()));
			}
		}
		
		super.checkType(markers, context, position);
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		return this.theClass.resolveField(name);
	}
	
	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		this.theClass.getMethodMatches(list, instance, name, arguments);
	}
	
	@Override
	public void getConstructorMatches(ConstructorMatchList list, IArguments arguments)
	{
		this.theClass.getConstructorMatches(list, arguments);
	}
	
	@Override
	public IMethod getFunctionalMethod()
	{
		return this.theClass.getFunctionalMethod();
	}
	
	@Override
	public String getInternalName()
	{
		return this.theClass.getInternalName();
	}
	
	@Override
	public void write(DataOutput out) throws IOException
	{
		out.writeUTF(this.theClass.getInternalName());
		this.writeTypeArguments(out);
	}
	
	@Override
	public void read(DataInput in) throws IOException
	{
		String internal = in.readUTF();
		this.theClass = Package.rootPackage.resolveInternalClass(internal);
		this.readTypeArguments(in);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(this.theClass.getFullName());
		this.appendFullTypes(sb);
		return sb.toString();
	}
	
	@Override
	public ClassGenericType clone()
	{
		ClassGenericType t = new ClassGenericType(this.theClass);
		this.copyTypeArguments(t);
		return t;
	}
}