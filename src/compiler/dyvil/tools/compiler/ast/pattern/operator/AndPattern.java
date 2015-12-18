package dyvil.tools.compiler.ast.pattern.operator;

import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.pattern.IPattern;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class AndPattern extends BinaryPattern implements IPattern
{
	public AndPattern(IPattern left, ICodePosition position, IPattern right)
	{
		super(left, position, right);
	}

	@Override
	public int getPatternType()
	{
		return AND;
	}

	@Override
	public boolean isExhaustive()
	{
		return this.left.isExhaustive() && this.right.isExhaustive();
	}

	@Override
	public IPattern withType(IType type, MarkerList markers)
	{
		this.left = this.left.withType(type, markers);
		this.right = this.right.withType(type, markers);
		return this;
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		final IDataMember field = this.left.resolveField(name);
		if (field != null)
		{
			return field;
		}
		return this.right.resolveField(name);
	}

	@Override
	public boolean isSwitchable()
	{
		return false;
	}

	@Override
	public int switchCases()
	{
		return -1;
	}

	@Override
	public boolean switchCheck()
	{
		return false;
	}

	@Override
	public int switchValue(int index)
	{
		return 0;
	}

	@Override
	public int minValue()
	{
		return 0;
	}

	@Override
	public int maxValue()
	{
		return 0;
	}

	@Override
	public void writeInvJump(MethodWriter writer, int varIndex, Label elseLabel) throws BytecodeException
	{
		varIndex = this.checkVar(writer, varIndex);

		this.left.writeInvJump(writer, varIndex, elseLabel);
		this.right.writeInvJump(writer, varIndex, elseLabel);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.left.toString(prefix, buffer);
		buffer.append(" & ");
		this.right.toString(prefix, buffer);
	}
}