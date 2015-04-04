package dyvil.tools.compiler.ast.operator;

import org.objectweb.asm.Label;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.constant.BooleanValue;
import dyvil.tools.compiler.ast.expression.BoxedValue;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public final class OrOperator extends ASTNode implements IValue
{
	public IValue	left;
	public IValue	right;
	
	public OrOperator(IValue left, IValue right)
	{
		this.left = left;
		this.right = right;
	}
	
	public OrOperator(ICodePosition position, IValue left, IValue right)
	{
		this.position = position;
		this.left = left;
		this.right = right;
	}
	
	@Override
	public int getValueType()
	{
		return BOOLEAN_AND;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public IType getType()
	{
		return Types.BOOLEAN;
	}
	
	@Override
	public IValue withType(IType type)
	{
		if (type == Types.BOOLEAN)
		{
			return this;
		}
		return type.isSuperTypeOf(Types.BOOLEAN) ? new BoxedValue(this, Types.BOOLEAN.boxMethod) : null;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return type == Types.BOOLEAN || type.isSuperTypeOf(Types.BOOLEAN);
	}
	
	@Override
	public int getTypeMatch(IType type)
	{
		if (type == Types.BOOLEAN)
		{
			return 3;
		}
		if (type.isSuperTypeOf(Types.BOOLEAN))
		{
			return 2;
		}
		return 0;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		this.left.resolveTypes(markers, context);
		this.right.resolveTypes(markers, context);
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		this.left = this.left.resolve(markers, context);
		this.right = this.right.resolve(markers, context);
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		this.left.checkTypes(markers, context);
		this.right.checkTypes(markers, context);
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		this.left.check(markers, context);
		this.right.check(markers, context);
	}
	
	@Override
	public IValue foldConstants()
	{
		int t1 = this.left.getValueType();
		int t2 = this.right.getValueType();
		if (t1 == BOOLEAN && ((BooleanValue) this.left).value)
		{
			return BooleanValue.TRUE;
		}
		if (t2 == BOOLEAN && ((BooleanValue) this.left).value)
		{
			return BooleanValue.TRUE;
		}
		
		this.left.foldConstants();
		this.right.foldConstants();
		
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer)
	{
		Label label = new Label();
		Label label2 = new Label();
		this.left.writeJump(writer, label);
		this.right.writeJump(writer, label);
		writer.writeLDC(0);
		writer.writeJumpInsn(Opcodes.GOTO, label2);
		writer.writeLabel(label);
		writer.writeLDC(1);
		writer.writeLabel(label2);
	}
	
	@Override
	public void writeStatement(MethodWriter writer)
	{
		this.writeExpression(writer);
		writer.writeInsn(Opcodes.IRETURN);
	}
	
	@Override
	public void writeJump(MethodWriter writer, Label dest)
	{
		Label label = new Label();
		this.left.writeInvJump(writer, label);
		this.right.writeJump(writer, dest);
		writer.writeLabel(label);
	}
	
	@Override
	public void writeInvJump(MethodWriter writer, Label dest)
	{
		Label label = new Label();
		this.left.writeJump(writer, label);
		this.right.writeInvJump(writer, dest);
		writer.writeLabel(label);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.left.toString(prefix, buffer);
		buffer.append(" || ");
		this.right.toString(prefix, buffer);
	}
}