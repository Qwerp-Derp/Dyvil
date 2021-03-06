package dyvil.tools.compiler.ast.field;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;

public class VariableThis implements IAccessible
{
	public static final VariableThis DEFAULT = new VariableThis();
	
	private final int index;
	
	public VariableThis()
	{
		this.index = 0;
	}
	
	public VariableThis(int index)
	{
		this.index = index;
	}
	
	@Override
	public IType getType()
	{
		return null;
	}
	
	@Override
	public void writeGet(MethodWriter writer) throws BytecodeException
	{
		writer.visitVarInsn(Opcodes.ALOAD, this.index);
	}
}
