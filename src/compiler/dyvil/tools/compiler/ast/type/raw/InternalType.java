package dyvil.tools.compiler.ast.type.raw;

import dyvil.tools.compiler.ast.annotation.AnnotationUtil;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constructor.IConstructor;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.PrimitiveType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.ClassFormat;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class InternalType implements IRawType, IUnresolvedType
{
	protected String internalName;

	public InternalType(String internalName)
	{
		this.internalName = internalName;
	}

	@Override
	public int typeTag()
	{
		return INTERNAL;
	}

	@Override
	public Name getName()
	{
		return Name.fromRaw(this.internalName.substring(this.internalName.lastIndexOf('/') + 1));
	}

	@Override
	public IClass getTheClass()
	{
		return null;
	}

	@Override
	public boolean isResolved()
	{
		return false;
	}

	@Override
	public IType resolveType(MarkerList markers, IContext context)
	{
		switch (this.internalName)
		{
		case "dyvil/ref/BooleanRef":
			return Types.BOOLEAN.getRefType();
		case "dyvil/ref/ByteRef":
			return Types.BYTE.getRefType();
		case "dyvil/ref/ShortRef":
			return Types.SHORT.getRefType();
		case "dyvil/ref/CharRef":
			return Types.CHAR.getRefType();
		case "dyvil/ref/IntRef":
			return Types.INT.getRefType();
		case "dyvil/ref/LongRef":
			return Types.LONG.getRefType();
		case "dyvil/ref/FloatRef":
			return Types.FLOAT.getRefType();
		case "dyvil/ref/DoubleRef":
			return Types.DOUBLE.getRefType();
		case "dyvil/ref/StringRef":
			return Types.STRING.getRefType();
		case "dyvil/lang/Null":
			return Types.NULL;
		}

		final IClass resolvedClass = Package.rootPackage.resolveInternalClass(this.internalName);
		if (resolvedClass == null)
		{
			return Types.UNKNOWN;
		}
		return resolvedClass.getClassType();
	}

	@Override
	public void checkType(MarkerList markers, IContext context, int position)
	{
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		return null;
	}

	@Override
	public void getMethodMatches(MatchList<IMethod> list, IValue receiver, Name name, IArguments arguments)
	{
	}

	@Override
	public void getImplicitMatches(MatchList<IMethod> list, IValue value, IType targetType)
	{
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
	public IType withAnnotation(IAnnotation annotation)
	{
		if (AnnotationUtil.PRIMITIVE_INTERNAL.equals(annotation.getType().getInternalName()))
		{
			return PrimitiveType.getPrimitiveType(this.internalName);
		}
		return null;
	}

	@Override
	public String getInternalName()
	{
		return this.internalName;
	}

	@Override
	public void write(DataOutput out) throws IOException
	{
		out.writeUTF(this.internalName);
	}

	@Override
	public void read(DataInput in) throws IOException
	{
		this.internalName = in.readUTF();
	}

	@Override
	public String toString()
	{
		return ClassFormat.internalToPackage(this.internalName);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(ClassFormat.internalToPackage(this.internalName));
	}

	@Override
	public IType clone()
	{
		return new InternalType(this.internalName);
	}
}
