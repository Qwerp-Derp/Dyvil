package dyvil.tools.repl.command;

import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.repl.DyvilREPL;
import dyvil.tools.repl.REPLContext;

public class MethodsCommand implements ICommand
{
	@Override
	public String getName()
	{
		return "methods";
	}
	
	@Override
	public String getDescription()
	{
		return "Prints the signatures of all available methods";
	}
	
	@Override
	public void execute(DyvilREPL repl, String... args)
	{
		REPLContext context = repl.getContext();
		for (IMethod method : context.methods)
		{
			StringBuilder builder = new StringBuilder();
			Util.methodSignatureToString(method, builder);
			System.out.println(builder.toString());
		}
	}
}