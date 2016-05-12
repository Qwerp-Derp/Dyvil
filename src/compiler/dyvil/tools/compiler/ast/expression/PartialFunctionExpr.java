package dyvil.tools.compiler.ast.expression;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.consumer.ICaseConsumer;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.pattern.ICase;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.generic.ClassGenericType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class PartialFunctionExpr implements IValue, ICaseConsumer
{
	public static final class LazyFields
	{
		public static final IClass PARTIAL_FUNCTION_CLASS = Package.dyvilFunction.resolveClass("PartialFunction1");

		private LazyFields()
		{
		}
	}

	protected MatchCase[] cases = new MatchCase[3];
	protected int caseCount;

	// Metadata
	protected ICodePosition position;

	private boolean exhaustive;

	private IType   type;
	private IType   inputType;
	private IType   returnType;

	@Override
	public int valueTag()
	{
		return PARTIAL_FUNCTION;
	}

	@Override
	public void addCase(ICase matchCase)
	{
		int index = this.caseCount++;
		if (index >= this.cases.length)
		{
			MatchCase[] temp = new MatchCase[this.caseCount];
			System.arraycopy(this.cases, 0, temp, 0, index);
			this.cases = temp;
		}
		this.cases[index] = (MatchCase) matchCase;
	}

	@Override
	public ICodePosition getPosition()
	{
		return this.position;
	}

	@Override
	public void setPosition(ICodePosition position)
	{
		this.position = position;
	}

	@Override
	public boolean isResolved()
	{
		return true;
	}

	@Override
	public IType getType()
	{
		if (this.type != null)
		{
			return this.type;
		}

		return this.type = new ClassGenericType(LazyFields.PARTIAL_FUNCTION_CLASS, this.getInputType(),
		                                        this.getReturnType());
	}

	public IType getInputType()
	{
		return this.inputType;
	}

	public IType getReturnType()
	{
		return this.returnType;
	}

	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		return null;
	}

	@Override
	public boolean isType(IType type)
	{
		return false;
	}

	@Override
	public int getTypeMatch(IType type)
	{
		return 0;
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
	}

	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		return null;
	}

	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
	}

	@Override
	public IValue foldConstants()
	{
		return null;
	}

	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		return null;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
	}

	@Override
	public String toString()
	{
		return IASTNode.toString(this);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
	}
}
