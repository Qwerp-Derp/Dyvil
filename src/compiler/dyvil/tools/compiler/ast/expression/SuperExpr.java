package dyvil.tools.compiler.ast.expression;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.IType.TypePosition;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class SuperExpr implements IValue
{
	protected ICodePosition position;
	protected IType type = Types.UNKNOWN;

	public SuperExpr(ICodePosition position)
	{
		this.position = position;
	}

	public SuperExpr(ICodePosition position, IType type)
	{
		this.position = position;
		this.type = type;
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
		return SUPER;
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
		if (context.isStatic())
		{
			markers.add(Markers.semantic(this.position, "super.access.static"));
			return;
		}

		final IClass enclosingClass = context.getThisClass();
		final IType enclosingType = enclosingClass.getType();
		if (this.type == Types.UNKNOWN)
		{
			this.type = enclosingClass.getSuperType();
			if (this.type == null)
			{
				Marker marker = Markers.semantic(this.position, "super.access.type");
				marker.addInfo(Markers.getSemantic("type.enclosing", enclosingType));
				markers.add(marker);
			}
			return;
		}

		this.type = this.type.resolveType(markers, context);
		if (!this.type.isResolved())
		{
			return;
		}

		int distance = enclosingType.getSuperTypeDistance(this.type);
		if (distance == 1)
		{
			return;
		}

		final Marker marker = Markers.semantic(this.position,
		                                       distance == 0 ? "super.type.invalid" : "super.type.indirect");
		marker.addInfo(Markers.getSemantic("type.enclosing", enclosingType));
		marker.addInfo(Markers.getSemantic("super.type.requested", this.type));
		markers.add(marker);
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
		writer.visitVarInsn(Opcodes.ALOAD, 0);

		if (type != null)
		{
			this.type.writeCast(writer, type, this.getLineNumber());
		}
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("super");

		if (this.type != Types.UNKNOWN)
		{
			buffer.append('<');
			this.type.toString(prefix, buffer);
			buffer.append('>');
		}
	}
}
