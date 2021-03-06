package dyvil.tools.compiler.ast.pattern.constant;

import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.pattern.IPattern;
import dyvil.tools.compiler.ast.pattern.Pattern;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class IntPattern extends Pattern
{
	private int value;
	
	public IntPattern(ICodePosition position, int value)
	{
		this.position = position;
		this.value = value;
	}
	
	@Override
	public int getPatternType()
	{
		return INT;
	}
	
	@Override
	public IType getType()
	{
		return Types.INT;
	}
	
	@Override
	public IPattern withType(IType type, MarkerList markers)
	{
		return IPattern.primitiveWithType(this, type, Types.INT);
	}
	
	@Override
	public boolean isSwitchable()
	{
		return true;
	}
	
	@Override
	public int subPatterns()
	{
		return 1;
	}
	
	@Override
	public int switchValue()
	{
		return this.value;
	}
	
	@Override
	public int minValue()
	{
		return this.value;
	}
	
	@Override
	public int maxValue()
	{
		return this.value;
	}
	
	@Override
	public void writeInvJump(MethodWriter writer, int varIndex, IType matchedType, Label elseLabel)
			throws BytecodeException
	{
		IPattern.loadVar(writer, varIndex, matchedType);
		matchedType.writeCast(writer, Types.INT, this.getLineNumber());
		writer.visitLdcInsn(this.value);
		writer.visitJumpInsn(Opcodes.IF_ICMPNE, elseLabel);
	}

	@Override
	public String toString()
	{
		return java.lang.Integer.toString(this.value);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(this.value);
	}
}
