package dyvil.tools.compiler.ast.expression;

import java.util.ArrayList;
import java.util.List;

import dyvil.tools.compiler.ast.api.IValueList;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.config.Formatting;

public class ValueList implements IValue, IValueList
{
	protected List<IValue>	values	= new ArrayList();
	
	@Override
	public void setValues(List<IValue> list)
	{
		this.values = list;
	}
	
	@Override
	public List<IValue> getValues()
	{
		return this.values;
	}
	
	@Override
	public IValue fold()
	{
		return this;
	}
	
	@Override
	public Type getType()
	{
		if (this.values == null || this.values.isEmpty())
		{
			return Type.VOID;
		}
		return this.values.get(this.values.size() - 1).getType();
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		int size = this.values.size();
		if (size == 0)
		{
			buffer.append(Formatting.Expression.emptyExpression);
		}
		else if (size == 1)
		{
			this.values.get(0).toString("", buffer);
		}
		else
		{
			buffer.append('\n').append(prefix).append('{').append('\n');
			for (IValue value : this.values)
			{
				buffer.append(prefix).append(Formatting.Method.indent);
				value.toString("", buffer);
				buffer.append(";\n");
			}
			buffer.append(prefix).append('}');
		}
	}
}