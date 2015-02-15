package dyvil.tools.compiler.ast.access;

import java.util.List;

import dyvil.tools.compiler.ast.field.FieldMatch;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.ITyped;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.ast.value.IValueList;
import dyvil.tools.compiler.ast.value.IValued;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.util.Util;

public interface IAccess extends IValue, IValued, IValueList
{
	public boolean isResolved();
	
	public boolean resolve(IContext context, List<Marker> markers);
	
	public IValue resolve2(IContext context);
	
	public IAccess resolve3(IContext context, IAccess next);
	
	public Marker getResolveError();
	
	public static IField resolveField(IContext context, ITyped instance, String name)
	{
		FieldMatch match;
		if (instance != null)
		{
			IType type = instance.getType();
			if (type != null)
			{
				match = type.resolveField(name);
				if (match != null)
				{
					return match.theField;
				}
			}
		}
		
		match = context.resolveField(name);
		if (match != null)
		{
			return match.theField;
		}
		return null;
	}
	
	public static IMethod resolveMethod(IContext context, IValue instance, String name, List<IValue> arguments)
	{
		MethodMatch match;
		if (instance != null)
		{
			IType type = instance.getType();
			if (type != null)
			{
				match = type.resolveMethod(null, name, arguments);
				if (match != null)
				{
					return match.theMethod;
				}
			}
		}
		else if (arguments.size() == 1)
		{
			IValue v = arguments.get(0);
			IType type = v.getType();
			if (type != null)
			{
				match = type.resolveMethod(instance, name, Util.EMPTY_VALUES);
				if (match != null)
				{
					return match.theMethod;
				}
			}
		}
		
		match = context.resolveMethod(instance, name, arguments);
		if (match != null)
		{
			return match.theMethod;
		}
		
		match = Type.PREDEF_CLASS.resolveMethod(instance, name, arguments);
		if (match != null)
		{
			return match.theMethod;
		}
		return null;
	}
}
