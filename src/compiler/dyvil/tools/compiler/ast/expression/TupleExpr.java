package dyvil.tools.compiler.ast.expression;

import dyvil.collection.iterator.ArrayIterator;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IImplicitContext;
import dyvil.tools.compiler.ast.expression.constant.VoidValue;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.header.IClassCompilableList;
import dyvil.tools.compiler.ast.header.ICompilableList;
import dyvil.tools.compiler.ast.parameter.ArgumentList;
import dyvil.tools.compiler.ast.reference.IReference;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.ast.type.compound.TupleType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.transform.TypeChecker;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.util.Iterator;

public final class TupleExpr implements IValue, IValueList
{
	public static final class LazyFields
	{
		public static final IClass TUPLE_CONVERTIBLE = Types.LITERALCONVERTIBLE_CLASS
			                                               .resolveClass(Name.fromRaw("FromTuple"));

		public static final IClass ENTRY = Package.dyvilCollection.resolveClass("Entry");
		public static final IClass CELL  = Package.dyvilCollection.resolveClass("Cell");

		private static final TypeChecker.MarkerSupplier ELEMENT_MARKER_SUPPLIER = TypeChecker.markerSupplier(
			"tuple.element.type.incompatible", "tuple.element.type.expected", "tuple.element.type.actual");

		private LazyFields()
		{
			// no instances
		}
	}

	protected ICodePosition position;

	protected IValue[] values;
	protected int      valueCount;

	// Metadata
	private IType tupleType;

	public TupleExpr(ICodePosition position)
	{
		this.position = position;
		this.values = new IValue[3];
	}

	public TupleExpr(ICodePosition position, IValue[] values, int valueCount)
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
		return TUPLE;
	}

	@Override
	public Iterator<IValue> iterator()
	{
		return new ArrayIterator<>(this.values);
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
		if (this.valueCount > this.values.length)
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
	public boolean isResolved()
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			if (!this.values[i].isResolved())
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public IReference toReference()
	{
		return this.valueCount != 1 ? null : this.values[0].toReference();
	}

	@Override
	public IValue toReferenceValue(MarkerList markers, IContext context)
	{
		return this.valueCount != 1 ? null : this.values[0].toReferenceValue(markers, context);
	}

	@Override
	public IValue toAssignment(IValue rhs, ICodePosition position)
	{
		return this.valueCount != 1 ? null : this.values[0].toAssignment(rhs, position);
	}

	@Override
	public IType getType()
	{
		if (this.tupleType != null)
		{
			return this.tupleType;
		}

		final TupleType tupleType = new TupleType(this.valueCount);
		for (int i = 0; i < this.valueCount; i++)
		{
			tupleType.addType(this.values[i].getType());
		}
		return this.tupleType = tupleType;
	}

	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		final IAnnotation annotation = type.getAnnotation(LazyFields.TUPLE_CONVERTIBLE);
		if (annotation != null)
		{
			return new LiteralConversion(this, annotation, new ArgumentList(this.values, this.valueCount))
				       .withType(type, typeContext, markers, context);
		}

		IClass tupleClass = TupleType.getTupleClass(this.valueCount);
		if (!Types.isSuperClass(type, tupleClass.getClassType()))
		{
			return null;
		}

		this.tupleType = null; // reset type

		tupleClass = lookupClass(tupleClass, this.valueCount);

		for (int i = 0; i < this.valueCount; i++)
		{
			final IType elementType = Types.resolveTypeSafely(type, tupleClass.getTypeParameter(i));
			this.values[i] = TypeChecker.convertValue(this.values[i], elementType, typeContext, markers, context,
			                                          LazyFields.ELEMENT_MARKER_SUPPLIER);
		}

		return this;
	}

	protected static IClass lookupClass(IClass tupleClass, int valueCount)
	{
		switch (valueCount)
		{
		case 2:
			tupleClass = LazyFields.ENTRY;
			break;
		case 3:
			tupleClass = LazyFields.CELL;
			break;
		}
		return tupleClass;
	}

	@Override
	public boolean isType(IType type)
	{
		if (this.valueCount == 1)
		{
			return this.values[0].isType(type);
		}

		return Types.isSuperType(type, this.getType()) || type.getAnnotation(LazyFields.TUPLE_CONVERTIBLE) != null;
	}

	@Override
	public int getTypeMatch(IType type, IImplicitContext implicitContext)
	{
		if (this.valueCount == 1)
		{
			return this.values[0].getTypeMatch(type, implicitContext);
		}

		IClass tupleClass = TupleType.getTupleClass(this.valueCount);
		if (!Types.isSuperClass(type, tupleClass.getClassType()))
		{
			if (type.getAnnotation(LazyFields.TUPLE_CONVERTIBLE) != null)
			{
				return CONVERSION_MATCH;
			}
			return MISMATCH;
		}

		this.tupleType = null; // reset type

		tupleClass = lookupClass(tupleClass, this.valueCount);

		int min = EXACT_MATCH;
		for (int i = 0; i < this.valueCount; i++)
		{
			final IType elementType = Types.resolveTypeSafely(type, tupleClass.getTypeParameter(i));
			final int match = TypeChecker.getTypeMatch(this.values[i], elementType, implicitContext);

			if (match == MISMATCH)
			{
				return MISMATCH;
			}
			if (match < min)
			{
				min = match;
			}
		}
		return min;
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
		if (this.valueCount == 0)
		{
			return new VoidValue(this.position);
		}
		if (this.valueCount == 1)
		{
			return this.values[0].resolve(markers, context);
		}

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
	public IValue cleanup(ICompilableList compilableList, IClassCompilableList classCompilableList)
	{
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i] = this.values[i].cleanup(compilableList, classCompilableList);
		}
		return this;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		final String internal = this.getType().getInternalName();
		writer.visitTypeInsn(Opcodes.NEW, internal);
		writer.visitInsn(Opcodes.DUP);

		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].writeExpression(writer, Types.OBJECT);
		}

		final String desc = TupleType.getConstructorDescriptor(this.valueCount);
		writer.visitMethodInsn(Opcodes.INVOKESPECIAL, internal, "<init>", desc, false);

		if (type != null)
		{
			this.tupleType.writeCast(writer, type, this.getLineNumber());
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
			if (Formatting.getBoolean("tuple.empty.space_between"))
			{
				buffer.append("( )");
			}
			else
			{
				buffer.append("()");
			}
			return;
		}

		buffer.append('(');
		if (Formatting.getBoolean("tuple.open_paren.space_after"))
		{
			buffer.append(' ');
		}

		Util.astToString(prefix, this.values, this.valueCount, Formatting.getSeparator("tuple.separator", ','), buffer);

		if (Formatting.getBoolean("tuple.close_paren.space_before"))
		{
			buffer.append(' ');
		}
		buffer.append(')');
	}
}
