package dyvil.tools.compiler.ast.expression;

import dyvil.collection.iterator.ArrayIterator;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.parameter.ArgumentList;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Mutability;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.ast.type.compound.ArrayType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.transform.TypeChecker;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.util.Iterator;

public final class ArrayExpr implements IValue, IValueList
{
	public static final class LazyFields
	{
		public static final IClass ARRAY_CONVERTIBLE = Package.dyvilLangLiteral.resolveClass("ArrayConvertible");

		private static final TypeChecker.MarkerSupplier ELEMENT_MARKER_SUPPLIER = TypeChecker.markerSupplier(
			"array.element.type.incompatible", "array.element.type.expected", "array.element.type.actual");

		private LazyFields()
		{
			// no instances
		}
	}

	protected ICodePosition position;

	protected IValue[] values;
	protected int      valueCount;

	// Metadata
	protected IType arrayType;
	protected IType elementType;

	public ArrayExpr()
	{
		this.values = new IValue[4];
	}

	public ArrayExpr(IValue[] values, int valueCount)
	{
		this.values = values;
		this.valueCount = valueCount;
	}

	public ArrayExpr(ICodePosition position)
	{
		this.position = position;
		this.values = new IValue[3];
	}

	public ArrayExpr(ICodePosition position, int capacity)
	{
		this.position = position;
		this.values = new IValue[capacity];
	}

	public ArrayExpr(ICodePosition position, IValue[] values, int valueCount)
	{
		this.position = position;
		this.values = values;
		this.valueCount = valueCount;
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
		return ARRAY;
	}

	@Override
	public boolean isAnnotationConstant()
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			if (!this.values[i].isAnnotationConstant())
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isPrimitive()
	{
		return false;
	}

	public IType getElementType()
	{
		if (this.elementType != null)
		{
			return this.elementType;
		}

		return this.elementType = getCommonType(this.values, this.valueCount);
	}

	public static IType getCommonType(IValue[] values, int valueCount)
	{
		if (valueCount == 0)
		{
			return Types.ANY;
		}

		IType type = values[0].getType();
		for (int i = 1; i < valueCount; i++)
		{
			final IType valueType = values[i].getType();
			type = Types.combine(type, valueType);

			if (type == null)
			{
				return Types.ANY;
			}
		}

		return type;
	}

	@Override
	public boolean isResolved()
	{
		if (this.arrayType != null)
		{
			return this.arrayType.isResolved();
		}
		return this.elementType != null && this.elementType.isResolved();
	}

	@Override
	public IType getType()
	{
		if (this.arrayType != null)
		{
			return this.arrayType;
		}

		return this.arrayType = new ArrayType(this.getElementType(), Mutability.IMMUTABLE);
	}

	@Override
	public void setType(IType type)
	{
		this.arrayType = type;
		this.elementType = type.getElementType();
	}

	@Override
	public IValue toAnnotationConstant(MarkerList markers, IContext context, int depth)
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			final IValue value = this.values[i];
			final IValue constant = value.toAnnotationConstant(markers, context, depth);
			if (constant == null)
			{
				return null;
			}
			else
			{
				this.values[i] = constant;
			}
		}

		return this;
	}

	@Override
	public IValue withType(IType arrayType, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		IType elementType;
		if (!arrayType.isArrayType())
		{
			final IAnnotation annotation;
			if ((annotation = arrayType.getAnnotation(LazyFields.ARRAY_CONVERTIBLE)) != null)
			{
				return new LiteralConversion(this, annotation, new ArgumentList(this.values, this.valueCount))
					       .withType(arrayType, typeContext, markers, context);
			}
			if (arrayType.getTheClass() != Types.OBJECT_CLASS)
			{
				return null;
			}
			elementType = this.getType().getElementType();
		}
		else
		{
			// If the type is an array type, get it's element type
			this.elementType = elementType = arrayType.getElementType();
			this.arrayType = arrayType;
		}


		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i] = TypeChecker.convertValue(this.values[i], elementType, typeContext, markers, context,
			                                          LazyFields.ELEMENT_MARKER_SUPPLIER);
		}

		return this;
	}

	private boolean isConvertibleFrom(IType type)
	{
		return type.getTheClass() == Types.OBJECT_CLASS || type.getAnnotation(LazyFields.ARRAY_CONVERTIBLE) != null;
	}

	@Override
	public boolean isType(IType type)
	{
		if (!type.isArrayType())
		{
			return this.isConvertibleFrom(type);
		}

		// Skip getting the element type if this is an empty array
		if (this.valueCount == 0)
		{
			return true;
		}

		// If the type is an array type, get it's element type
		final IType elementType = type.getElementType();

		// Check for every value if it is the element type
		for (int i = 0; i < this.valueCount; i++)
		{
			if (!this.values[i].isType(elementType))
			{
				// If not, this is not the type
				return false;
			}
		}

		return true;
	}

	@Override
	public int getTypeMatch(IType type)
	{
		if (!type.isArrayType())
		{
			// isConvertibleFrom also returns true for Object, but the
			// CONVERSION_MATCH return value here is intentional.
			return this.isConvertibleFrom(type) ? CONVERSION_MATCH : 0;
		}

		// Skip getting the element type if this is an empty array
		if (this.valueCount == 0)
		{
			return 1;
		}

		// If the type is an array type, get it's element type
		final IType elementType = type.getElementType();

		int total = 0;
		// Get the type match for every value in the array
		for (int i = 0; i < this.valueCount; i++)
		{
			final int match = this.values[i].getTypeMatch(elementType);
			if (match <= 0)
			{
				// If the type match for one value is zero, return 0
				return 0;
			}
			total += match;
		}

		// Divide by the count
		return 1 + total / this.valueCount;
	}

	@Override
	public Iterator<IValue> iterator()
	{
		return new ArrayIterator<>(this.values, this.valueCount);
	}

	@Override
	public int valueCount()
	{
		return this.valueCount;
	}

	@Override
	public boolean isEmpty()
	{
		return this.valueCount == 0;
	}

	@Override
	public void setValue(int index, IValue value)
	{
		this.values[index] = value;
	}

	@Override
	public void addValue(IValue value)
	{
		int index = this.valueCount++;
		if (index >= this.values.length)
		{
			IValue[] temp = new IValue[this.valueCount];
			System.arraycopy(this.values, 0, temp, 0, index);
			this.values = temp;
		}
		this.values[index] = value;
	}

	@Override
	public void addValue(int index, IValue value)
	{
		IValue[] temp = new IValue[++this.valueCount];
		System.arraycopy(this.values, 0, temp, 0, index);
		temp[index] = value;
		System.arraycopy(this.values, index, temp, index + 1, this.valueCount - index - 1);
		this.values = temp;
	}

	@Override
	public IValue getValue(int index)
	{
		return this.values[index];
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].resolveTypes(markers, context);
		}
	}

	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i] = this.values[i].resolve(markers, context);
		}
		return this;
	}

	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].checkTypes(markers, context);
		}
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].check(markers, context);
		}
	}

	@Override
	public IValue foldConstants()
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i] = this.values[i].foldConstants();
		}
		return this;
	}

	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i] = this.values[i].cleanup(context, compilableList);
		}
		return this;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		final IType elementType = this.getElementType();
		final int arrayStoreOpcode = elementType.getArrayStoreOpcode();

		writer.visitLdcInsn(this.valueCount);
		writer.visitMultiANewArrayInsn(this.getType(), 1);

		for (int i = 0; i < this.valueCount; i++)
		{
			writer.visitInsn(Opcodes.DUP);
			writer.visitLdcInsn(i);
			this.values[i].writeExpression(writer, elementType);
			writer.visitInsn(arrayStoreOpcode);
		}
	}

	@Override
	public String toString()
	{
		return IASTNode.toString(this);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.valueCount == 0)
		{
			if (Formatting.getBoolean("array.empty.space_between"))
			{
				buffer.append("[ ]");
			}
			else
			{
				buffer.append("[]");
			}
			return;
		}

		buffer.append('[');
		if (Formatting.getBoolean("array.open_bracket.space_after"))
		{
			buffer.append(' ');
		}

		Util.astToString(prefix, this.values, this.valueCount, Formatting.getSeparator("array.separator", ','), buffer);

		if (Formatting.getBoolean("array.close_bracket.space_before"))
		{
			buffer.append(' ');
		}
		buffer.append(']');
	}
}
