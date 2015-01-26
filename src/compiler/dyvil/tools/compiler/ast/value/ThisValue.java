package dyvil.tools.compiler.ast.value;

import java.util.List;

import jdk.internal.org.objectweb.asm.Opcodes;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.constant.IConstantValue;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.Markers;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public class ThisValue extends ASTNode implements IConstantValue
{
	public IType	type;
	
	public ThisValue(ICodePosition position)
	{
		this.position = position;
	}
	
	public ThisValue(ICodePosition position, IType type)
	{
		this.position = position;
		this.type = type;
	}
	
	@Override
	public int getValueType()
	{
		return THIS;
	}
	
	@Override
	public IType getType()
	{
		return this.type;
	}
	
	@Override
	public Object toObject()
	{
		return null;
	}
	
	@Override
	public void resolveTypes(List<Marker> markers, IContext context)
	{
		if (this.type == null)
		{
			if (context.isStatic())
			{
				markers.add(Markers.create(this.position, "access.this.static"));
			}
			else
			{
				this.type = context.getThisType();
			}
		}
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("this");
	}
	
	@Override
	public void writeExpression(MethodWriter writer)
	{
		writer.visitVarInsn(Opcodes.ALOAD, 0, this.type);
	}
	
	@Override
	public void writeStatement(MethodWriter writer)
	{
		writer.visitVarInsn(Opcodes.ALOAD, 0, this.type);
		writer.visitInsn(Opcodes.ARETURN);
	}
}
