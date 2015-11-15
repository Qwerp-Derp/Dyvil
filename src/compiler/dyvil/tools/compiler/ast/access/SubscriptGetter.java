package dyvil.tools.compiler.ast.access;

import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.ArrayExpr;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.parameter.ArgumentList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class SubscriptGetter extends AbstractCall
{
	public SubscriptGetter(ICodePosition position)
	{
		this.position = position;
		this.arguments = new ArgumentList();
	}
	
	public SubscriptGetter(ICodePosition position, IValue instance)
	{
		this.position = position;
		this.receiver = instance;
		this.arguments = new ArgumentList();
	}
	
	public SubscriptGetter(ICodePosition position, IValue instance, IArguments arguments)
	{
		this.position = position;
		this.receiver = instance;
		this.arguments = arguments;
	}
	
	@Override
	public int valueTag()
	{
		return SUBSCRIPT_GET;
	}
	
	@Override
	public ArgumentList getArguments()
	{
		return (ArgumentList) this.arguments;
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		if (this.receiver instanceof ICall && this.arguments instanceof ArgumentList)
		// false if receiver == null
		{
			ICall call = (ICall) this.receiver;
			
			// Resolve Receiver if necessary
			call.resolveReceiver(markers, context);
			call.resolveArguments(markers, context);
			
			IArguments oldArgs = call.getArguments();
			
			ArrayExpr array = new ArrayExpr(this.position, ((ArgumentList) this.arguments).getValues(), this.arguments.size());
			call.setArguments(oldArgs.withLastValue(Names.subscript, array));
			
			IValue resolvedCall = call.resolveCall(markers, context);
			if (resolvedCall != null)
			{
				return resolvedCall;
			}
			
			// Revert
			call.setArguments(oldArgs);
			
			this.receiver = call.resolveCall(markers, context);
			resolvedCall = this.resolveCall(markers, context);
			if (resolvedCall != null)
			{
				return resolvedCall;
			}
			
			this.reportResolve(markers, context);
			return this;
		}
		
		return super.resolve(markers, context);
	}
	
	@Override
	public IValue resolveCall(MarkerList markers, IContext context)
	{
		IMethod m = ICall.resolveMethod(context, this.receiver, Names.subscript, this.arguments);
		if (m != null)
		{
			this.method = m;
			this.checkArguments(markers, context);
			return this;
		}
		
		return null;
	}
	
	@Override
	public void reportResolve(MarkerList markers, IContext context)
	{
		ICall.addResolveMarker(markers, this.position, this.receiver, Names.subscript, this.arguments);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.receiver != null)
		{
			this.receiver.toString(prefix, buffer);
		}
		
		buffer.append('[');
		int count = this.arguments.size();
		this.arguments.getValue(0, null).toString(prefix, buffer);
		for (int i = 1; i < count; i++)
		{
			buffer.append(Formatting.Expression.arraySeperator);
			this.arguments.getValue(i, null).toString(prefix, buffer);
		}
		buffer.append(']');
	}
}
