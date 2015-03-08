package dyvil.tools.compiler.ast.pattern;

import org.objectweb.asm.Label;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public class CharPattern extends ASTNode implements IPattern
{
	private char	value;
	
	public CharPattern(ICodePosition position, char value)
	{
		this.position = position;
		this.value = value;
	}
	
	@Override
	public IType getType()
	{
		return Type.CHAR;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return type == Type.CHAR || type.isSuperTypeOf(Type.CHAR);
	}
	
	@Override
	public void writeJump(MethodWriter writer, Label elseLabel)
	{
		writer.writeLDC(this.value);
		writer.writeFrameJump(Opcodes.IF_ICMPNE, elseLabel);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append('\'').append(this.value).append('\'');
	}
}
