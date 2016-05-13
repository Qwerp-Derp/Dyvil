package dyvil.tools.compiler.ast.expression;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.constant.BooleanValue;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.IType.TypePosition;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class InstanceOfOperator extends AbstractValue
{
	protected IValue value;
	protected IType  type;
	
	public InstanceOfOperator(ICodePosition position, IValue value)
	{
		this.position = position;
		this.value = value;
	}
	
	public InstanceOfOperator(IValue value, IType type)
	{
		this.value = value;
		this.type = type;
	}
	
	@Override
	public int valueTag()
	{
		return ISOF_OPERATOR;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public boolean isResolved()
	{
		return true;
	}
	
	@Override
	public IType getType()
	{
		return Types.BOOLEAN;
	}
	
	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.type != null)
		{
			this.type = this.type.resolveType(markers, context);
		}
		else
		{
			markers.add(Markers.semanticError(this.position, "instanceof.type.invalid"));
		}

		if (this.value != null)
		{
			this.value.resolveTypes(markers, context);
		}
		else
		{
			markers.add(Markers.semanticError(this.position, "instanceof.value.invalid"));
		}
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		if (this.type != null)
		{
			this.type.resolve(markers, context);
		}
		if (this.value != null)
		{
			this.value = this.value.resolve(markers, context);
		}
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		if (this.type != null)
		{
			this.type.checkType(markers, context, TypePosition.CLASS);
		}
		if (this.value != null)
		{
			this.value.checkTypes(markers, context);
		}
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		if (this.type != null)
		{
			this.type.check(markers, context);

			if (this.type.isPrimitive())
			{
				markers.add(Markers.semanticError(this.position, "instanceof.type.primitive"));
				return;
			}
		}
		else
		{
			markers.add(Markers.semanticError(this.position, "instanceof.type.primitive"));
			return;
		}

		if (this.value != null)
		{
			this.value.check(markers, context);
		}
		else
		{
			return;
		}

		if (this.value.isPrimitive())
		{
			markers.add(Markers.semanticError(this.position, "instanceof.value.primitive"));
			return;
		}
		
		final IType valueType = this.value.getType();
		if (Types.isExactType(this.type, valueType))
		{
			markers.add(Markers.semantic(this.position, "instanceof.type.equal", valueType));
			return;
		}
		if (Types.isSuperType(this.type, valueType))
		{
			markers.add(Markers.semantic(this.position, "instanceof.type.subtype", valueType, this.type));
			return;
		}
		if (!Types.isSuperType(valueType, this.type))
		{
			markers.add(Markers.semanticError(this.position, "instanceof.type.incompatible", valueType, this.type));
		}
	}
	
	@Override
	public IValue foldConstants()
	{
		this.type.foldConstants();
		this.value = this.value.foldConstants();
		return this;
	}
	
	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		this.type.cleanup(context, compilableList);
		this.value = this.value.cleanup(context, compilableList);
		
		if (this.value.isType(this.type))
		{
			return BooleanValue.TRUE;
		}
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		this.value.writeExpression(writer, Types.OBJECT);
		writer.visitTypeInsn(Opcodes.INSTANCEOF, this.type.getInternalName());

		if (type != null)
		{
			Types.BOOLEAN.writeCast(writer, type, this.getLineNumber());
		}
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.value.toString(prefix, buffer);
		buffer.append(" is ");
		this.type.toString(prefix, buffer);
	}
}
