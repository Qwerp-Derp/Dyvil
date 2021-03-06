package dyvil.tools.compiler.ast.expression.operator;

import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.access.MethodCall;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MatchList;
import dyvil.tools.compiler.ast.parameter.ArgumentList;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class PostfixCall extends MethodCall
{
	public PostfixCall(ICodePosition position, IValue receiver, Name name)
	{
		super(position, receiver, name);
	}

	@Override
	public int valueTag()
	{
		return PREFIX_CALL;
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		final IOperator operator = IContext.resolveOperator(context, this.name, IOperator.POSTFIX);
		if (operator == null)
		{
			markers.add(Markers.semantic(this.position, "operator.unresolved", this.name));
		}
		else
		{
			OperatorElement.checkPosition(markers, this.position, operator, IOperator.POSTFIX);
		}

		super.resolveTypes(markers, context);
	}

	@Override
	public IValue toAssignment(IValue rhs, ICodePosition position)
	{
		final Name name = Util.addEq(this.name);
		return new MethodCall(this.position, this.arguments.getFirstValue(), name, new ArgumentList(rhs));
	}

	@Override
	public IValue resolveCall(MarkerList markers, IContext context, boolean report)
	{
		// Normal Method Resolution
		final MatchList<IMethod> candidates = this.resolveCandidates(context);
		if (candidates.hasCandidate())
		{
			return this.checkArguments(markers, context, candidates.getBestMember());
		}

		// No Implicit or Apply Resolution
		if (report)
		{
			this.reportResolve(markers, candidates);
			return this;
		}
		return null;
	}

	@Override
	public String toString()
	{
		return IASTNode.toString(this);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.receiver != null)
		{
			this.receiver.toString(prefix, buffer);
		}
		buffer.append(this.name);
	}
}
