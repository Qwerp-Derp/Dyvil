package dyvil.tools.compiler.ast.pattern;

import org.objectweb.asm.Label;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public final class NullPattern extends ASTNode implements IPattern
{
	public NullPattern(ICodePosition position)
	{
		this.position = position;
	}
	
	@Override
	public int getPatternType()
	{
		return NULL;
	}
	
	@Override
	public IType getType()
	{
		return Type.NONE;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return !type.isPrimitive();
	}
	
	@Override
	public void writeJump(MethodWriter writer, Label elseLabel)
	{
		writer.writeFrameJump(Opcodes.IFNONNULL, elseLabel);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append((String) null);
	}
}