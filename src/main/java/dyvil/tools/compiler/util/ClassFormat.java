package dyvil.tools.compiler.util;

import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.Parameter;
import dyvil.tools.compiler.ast.type.Type;

public class ClassFormat
{
	public static String packageToInternal(String name)
	{
		return name.replace('.', '/');
	}
	
	public static String internalToPackage(String name)
	{
		return name.replace('/', '.');
	}
	
	private static Type parseBaseType(char c)
	{
		switch (c)
		{
		case 'V':
			return Type.VOID;
		case 'Z':
			return Type.BOOL;
		case 'B':
			return Type.BYTE;
		case 'S':
			return Type.SHORT;
		case 'C':
			return Type.CHAR;
		case 'I':
			return Type.INT;
		case 'J':
			return Type.LONG;
		case 'F':
			return Type.FLOAT;
		case 'D':
			return Type.DOUBLE;
		}
		return null;
	}
	
	public static Type internalToType(String internal)
	{
		int len = internal.length();
		int arrayDimensions = 0;
		int i = 0;
		while (i < len && internal.charAt(i) == '[')
		{
			arrayDimensions++;
			i++;
		}
		
		char c = internal.charAt(i);
		Type type = null;
		
		if (c == 'L')
		{
			int l = len - 1;
			if (internal.charAt(l) == ';')
			{
				internal = internal.substring(i + 1, l);
			}
			type = internalToType2(internal);
		}
		else if (len - i == 1)
		{
			type = parseBaseType(c);
		}
		else
		{
			type = internalToType2(internal);
		}
		
		type.arrayDimensions = arrayDimensions;
		return type;
	}
	
	public static void readMethodType(String internal, IMethod method)
	{
		String methodName = method.getName();
		int params = 0;
		int index = internal.indexOf(')');
		int arrayDimensions = 0;
		
		for (int i = 1; i < index; i++)
		{
			char c = internal.charAt(i);
			if (c == '[')
			{
				arrayDimensions++;
			}
			else if (c == 'L')
			{
				int end = internal.indexOf(';', i);
				String s = internal.substring(i + 1, end);
				Type type = internalToType2(s);
				type.arrayDimensions = arrayDimensions;
				arrayDimensions = 0;
				method.addParameter(new Parameter("par" + params, type, 0));
				params++;
				i = end;
			}
			else
			{
				Type type = parseBaseType(c);
				type.arrayDimensions = arrayDimensions;
				arrayDimensions = 0;
				method.addParameter(new Parameter("par" + params, type, 0));
				params++;
			}
		}
		
		Type t = internalToType(internal.substring(index + 1));
		method.setType(t);
	}
	
	protected static Type internalToType2(String internal)
	{
		// TODO Generics
		return new Type(internalToPackage(internal));
	}
}
