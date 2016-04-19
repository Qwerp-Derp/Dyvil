package dyvil.tools.compiler.ast.constant;

import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.LiteralConversion;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class DoubleValue implements IConstantValue
{
	private static DoubleValue NULL;

	protected ICodePosition position;
	protected double        value;

	public DoubleValue(double value)
	{
		this.value = value;
	}

	public DoubleValue(ICodePosition position, double value)
	{
		this.position = position;
		this.value = value;
	}

	public static DoubleValue getNull()
	{
		if (NULL == null)
		{
			NULL = new DoubleValue(0D);
		}
		return NULL;
	}

	@Override
	public ICodePosition getPosition()
	{
		return this.position;
	}

	@Override
	public void setPosition(ICodePosition position)
	{
		this.position = position;
	}

	@Override
	public int valueTag()
	{
		return DOUBLE;
	}

	@Override
	public boolean isPrimitive()
	{
		return true;
	}

	@Override
	public IType getType()
	{
		return Types.DOUBLE;
	}

	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (Types.isSuperType(type, Types.DOUBLE))
		{
			return this;
		}

		final IAnnotation annotation = type.getAnnotation(Types.DOUBLE_CONVERTIBLE_CLASS);
		if (annotation != null)
		{
			return new LiteralConversion(this, annotation).withType(type, typeContext, markers, context);
		}
		return null;
	}

	@Override
	public boolean isType(IType type)
	{
		return Types.isSuperType(type, Types.DOUBLE) || type.getAnnotation(Types.DOUBLE_CONVERTIBLE_CLASS) != null;
	}

	@Override
	public int getTypeMatch(IType type)
	{
		if (type == Types.DOUBLE)
		{
			return 1;
		}
		if (type.getAnnotation(Types.DOUBLE_CONVERTIBLE_CLASS) != null)
		{
			return CONVERSION_MATCH;
		}
		return Types.getDistance(type, Types.DOUBLE);
	}

	@Override
	public int intValue()
	{
		return (int) this.value;
	}

	@Override
	public long longValue()
	{
		return (long) this.value;
	}

	@Override
	public float floatValue()
	{
		return (float) this.value;
	}

	@Override
	public double doubleValue()
	{
		return this.value;
	}

	@Override
	public Double toObject()
	{
		return this.value;
	}

	@Override
	public int stringSize()
	{
		return Double.toString(this.value).length();
	}

	@Override
	public boolean toStringBuilder(StringBuilder builder)
	{
		builder.append(this.value);
		return true;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		writer.visitLdcInsn(this.value);

		if (type != null)
		{
			Types.DOUBLE.writeCast(writer, type, this.getLineNumber());
		}
	}

	@Override
	public String toString()
	{
		if (!Double.isFinite(this.value))
		{
			return String.valueOf(this.value);
		}
		return this.value + "D";
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(this.value);
		if (Double.isFinite(this.value))
		{
			buffer.append('D');
		}
	}
}
