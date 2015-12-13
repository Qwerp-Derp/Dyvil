package dyvil.tools.compiler.ast.pattern;

import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.parameter.IParameter;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.MarkerMessages;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class CaseClassPattern extends Pattern implements IPatternList
{
	private IType type;
	private IPattern[] patterns = new IPattern[2];
	private int patternCount;
	
	public CaseClassPattern(ICodePosition position)
	{
		this.position = position;
	}
	
	@Override
	public int getPatternType()
	{
		return CASE_CLASS;
	}
	
	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
	
	@Override
	public IType getType()
	{
		return this.type;
	}
	
	@Override
	public IPattern withType(IType type, MarkerList markers)
	{
		if (!type.isSuperTypeOf(this.type))
		{
			return null;
		}
		
		IClass iclass = this.type.getTheClass();
		if (iclass == null)
		{
			return this; // skip
		}
		
		int paramCount = iclass.parameterCount();
		if (this.patternCount != paramCount)
		{
			Marker m = MarkerMessages.createMarker(this.position, "pattern.class.count", this.type.toString());
			m.addInfo(MarkerMessages.getMarker("pattern.class.count.pattern", this.patternCount));
			m.addInfo(MarkerMessages.getMarker("pattern.class.count.class", paramCount));
			markers.add(m);
			return this;
		}
		
		for (int i = 0; i < paramCount; i++)
		{
			IParameter param = iclass.getParameter(i);
			IType paramType = param.getType().getConcreteType(this.type);
			IPattern pattern = this.patterns[i];
			IPattern typedPattern = pattern.withType(paramType, markers);
			
			if (typedPattern == null)
			{
				Marker m = MarkerMessages.createMarker(this.position, "pattern.class.type", param.getName());
				m.addInfo(MarkerMessages.getMarker("pattern.type", pattern.getType()));
				m.addInfo(MarkerMessages.getMarker("classparameter.type", paramType));
				markers.add(m);
			}
			else
			{
				this.patterns[i] = typedPattern;
			}
		}
		
		if (type.classEquals(this.type))
		{
			return this;
		}
		return new TypeCheckPattern(this, this.type);
	}
	
	@Override
	public boolean isType(IType type)
	{
		return type.isSuperTypeOf(this.type);
	}
	
	@Override
	public int patternCount()
	{
		return 0;
	}
	
	@Override
	public void setPattern(int index, IPattern pattern)
	{
		this.patterns[index] = pattern;
	}
	
	@Override
	public void addPattern(IPattern pattern)
	{
		int index = this.patternCount++;
		if (index >= this.patterns.length)
		{
			IPattern[] temp = new IPattern[index + 1];
			System.arraycopy(this.patterns, 0, temp, 0, this.patterns.length);
			this.patterns = temp;
		}
		this.patterns[index] = pattern;
	}
	
	@Override
	public IPattern getPattern(int index)
	{
		return this.patterns[index];
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		for (int i = 0; i < this.patternCount; i++)
		{
			IDataMember f = this.patterns[i].resolveField(name);
			if (f != null)
			{
				return f;
			}
		}
		return null;
	}
	
	@Override
	public IPattern resolve(MarkerList markers, IContext context)
	{
		this.type = this.type.resolveType(markers, context);
		
		for (int i = 0; i < this.patternCount; i++)
		{
			this.patterns[i] = this.patterns[i].resolve(markers, context);
		}
		
		return this;
	}
	
	@Override
	public void writeInvJump(MethodWriter writer, int varIndex, Label elseLabel) throws BytecodeException
	{
		if (varIndex < 0)
		{
			varIndex = writer.localCount();
			writer.writeVarInsn(Opcodes.ASTORE, varIndex);
		}
		
		IClass iclass = this.type.getTheClass();
		for (int i = 0; i < this.patternCount; i++)
		{
			if (this.patterns[i].getPatternType() == WILDCARD)
			{
				// Skip wildcard patterns
				continue;
			}
			
			IDataMember field = iclass.getParameter(i);
			writer.writeVarInsn(Opcodes.ALOAD, varIndex);
			field.writeGet(writer, null, this.getLineNumber());
			this.patterns[i].writeInvJump(writer, -1, elseLabel);
		}
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.type.toString(prefix, buffer);
		Formatting.appendSeparator(buffer, "parameters.open_paren", '(');
		Util.astToString(prefix, this.patterns, this.patternCount, Formatting.getSeparator("parameters.separator", ','),
		                 buffer);

		if (Formatting.getBoolean("parameters.close_paren.space_before"))
		{
			buffer.append(' ');
		}
		buffer.append(')');
	}
}
