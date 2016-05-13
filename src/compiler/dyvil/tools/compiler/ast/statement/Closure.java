package dyvil.tools.compiler.ast.statement;

import dyvil.tools.compiler.ast.access.FieldAccess;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.LambdaExpr;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.parameter.IParameter;
import dyvil.tools.compiler.ast.parameter.CodeParameter;
import dyvil.tools.compiler.ast.parameter.IParameterList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class Closure extends StatementList
{
	private boolean resolved;
	private IValue  implicitValue;
	
	public Closure()
	{
	}
	
	public Closure(ICodePosition position)
	{
		this.position = position;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return type.getFunctionalMethod() != null;
	}
	
	@Override
	public int getTypeMatch(IType type)
	{
		return this.isType(type) ? 1 : 0;
	}

	@Override
	public boolean isResolved()
	{
		return true;
	}

	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (this.resolved)
		{
			return super.withType(type, typeContext, markers, context);
		}

		final IMethod functionalMethod = type.getFunctionalMethod();
		if (functionalMethod == null)
		{
			return null;
		}

		final IParameterList parameterList = functionalMethod.getParameterList();
		final int parameterCount = parameterList.size();
		final IParameter[] parameters = new IParameter[parameterCount];

		for (int i = 0; i < parameterCount; i++)
		{
			parameters[i] = new CodeParameter(this.position, Name.getQualified("$" + i), Types.UNKNOWN);
		}

		if (type.isExtension() && parameterCount > 0)
		{
			this.implicitValue = new FieldAccess(parameters[0]);
		}

		final LambdaExpr lambdaExpr = new LambdaExpr(this.position, parameters, parameterCount);
		lambdaExpr.setValue(this);

		this.resolved = true;

		context = context.push(this);
		final IValue typedLambda = lambdaExpr.withType(type, typeContext, markers, context);
		context.pop();

		return typedLambda;
	}

	@Override
	public IValue getImplicit()
	{
		return this.implicitValue;
	}

	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		if (this.resolved)
		{
			this.returnType = null;
			return super.resolve(markers, context);
		}

		this.returnType = Types.UNKNOWN;
		// Do this in withType
		return this;
	}
}
