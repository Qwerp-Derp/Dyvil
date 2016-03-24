package dyvil.tools.compiler.ast.dynamic;

import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.ast.type.builtin.UnknownType;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.parsing.Name;

public final class DynamicType extends UnknownType
{
	@Override
	public int typeTag()
	{
		return DYNAMIC;
	}

	@Override
	public Name getName()
	{
		return Names.dynamic;
	}

	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		Types.OBJECT_CLASS.getMethodMatches(list, instance, name, arguments);

		if (list.isEmpty())
		{
			list.add(new DynamicMethod(name), 1);
		}
	}

	@Override
	public String toString()
	{
		return "dynamic";
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("dynamic");
	}
}
