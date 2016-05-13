package dyvil.tools.compiler.ast.expression;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.field.IAccessible;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.IType.TypePosition;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class ThisExpr implements IValue
{
	protected ICodePosition position;
	protected IType type = Types.UNKNOWN;

	// Metadata
	protected IAccessible getter;

	public ThisExpr(IType type)
	{
		this.type = type;
	}

	public ThisExpr(IType type, IAccessible getter)
	{
		this.type = type;
		this.getter = getter;
	}

	public ThisExpr(ICodePosition position)
	{
		this.position = position;
	}

	public ThisExpr(ICodePosition position, IType type, IContext context, MarkerList markers)
	{
		this.position = position;
		this.type = type;
		this.resolveTypes(markers, context);
		this.checkTypes(markers, context);
	}

	public ThisExpr(ICodePosition position, IType type, IAccessible getter)
	{
		this.position = position;
		this.type = type;
		this.getter = getter;
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
		return THIS;
	}

	@Override
	public boolean isResolved()
	{
		return this.type.isResolved();
	}

	@Override
	public IType getType()
	{
		return this.type;
	}

	@Override
	public void setType(IType type)
	{
		this.type = type;
	}

	@Override
	public Object toObject()
	{
		return null;
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.type != Types.UNKNOWN)
		{
			this.type = this.type.resolveType(markers, context);
			return;
		}

		final IType thisType = context.getThisType();
		if (thisType != null)
		{
			this.type = thisType;
		}
	}

	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		this.type.resolve(markers, context);
		return this;
	}

	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		this.type.checkType(markers, context, TypePosition.CLASS);
		if (context.isStatic())
		{
			markers.add(Markers.semanticError(this.position, "this.access.static"));
			return;
		}

		if (!this.type.isResolved())
		{
			return;
		}

		final IClass theClass = this.type.getTheClass();
		this.getter = context.getAccessibleThis(theClass);
		if (this.getter == null)
		{
			markers.add(Markers.semanticError(this.position, "this.instance", this.type));
		}
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
		this.type.check(markers, context);
	}

	@Override
	public IValue foldConstants()
	{
		this.type.foldConstants();
		return this;
	}

	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		this.type.cleanup(context, compilableList);
		return this;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		this.getter.writeGet(writer);

		if (type != null)
		{
			this.type.writeCast(writer, type, this.getLineNumber());
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
		buffer.append("this");

		if (this.type != Types.UNKNOWN)
		{
			buffer.append('<');
			this.type.toString(prefix, buffer);
			buffer.append('>');
		}
	}
}
