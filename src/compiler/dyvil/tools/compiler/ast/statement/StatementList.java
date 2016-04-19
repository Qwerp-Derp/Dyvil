package dyvil.tools.compiler.ast.statement;

import dyvil.collection.List;
import dyvil.collection.iterator.ArrayIterator;
import dyvil.collection.mutable.ArrayList;
import dyvil.tools.compiler.ast.access.ICall;
import dyvil.tools.compiler.ast.access.MethodCall;
import dyvil.tools.compiler.ast.context.CombiningLabelContext;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.context.IDefaultContext;
import dyvil.tools.compiler.ast.context.ILabelContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.IValueList;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.IVariable;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.member.IClassMember;
import dyvil.tools.compiler.ast.member.MemberKind;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.SingleArgument;
import dyvil.tools.compiler.ast.statement.control.Label;
import dyvil.tools.compiler.ast.statement.loop.ILoop;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.compiler.transform.TypeChecker;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.util.Iterator;

public class StatementList implements IValue, IValueList, IDefaultContext, ILabelContext
{
	protected ICodePosition position;

	protected IValue[] values;
	protected int      valueCount;
	protected Label[]  labels;

	// Metadata
	protected List<IVariable> variables;
	protected List<IMethod>   methods;
	protected IType           returnType;

	public StatementList()
	{
		this.values = new IValue[3];
	}

	public StatementList(ICodePosition position)
	{
		this.position = position;
		this.values = new IValue[3];
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
	public void expandPosition(ICodePosition position)
	{
		this.position = this.position.to(position);
	}

	@Override
	public int valueTag()
	{
		return STATEMENT_LIST;
	}

	@Override
	public boolean isPrimitive()
	{
		return this.returnType != null && this.returnType.isPrimitive();
	}

	@Override
	public boolean isStatement()
	{
		return true;
	}

	@Override
	public boolean isUsableAsStatement()
	{
		return true;
	}

	@Override
	public boolean isResolved()
	{
		return this.valueCount == 0 || this.values[this.valueCount - 1].isResolved();
	}

	@Override
	public IType getType()
	{
		if (this.returnType != null)
		{
			return this.returnType;
		}
		if (this.valueCount == 0)
		{
			return this.returnType = Types.VOID;
		}
		return this.returnType = this.values[this.valueCount - 1].getType();
	}

	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (this.valueCount <= 0)
		{
			return Types.isSameType(type, Types.VOID) ? this : null;
		}

		context = context.push(this);

		final IValue value = this.values[this.valueCount - 1];
		final IValue typed = value.withType(type, typeContext, markers, context);

		context.pop();

		if (typed != null)
		{
			this.values[this.valueCount - 1] = typed;
			this.returnType = typed.getType();
			return this;
		}

		if (type != Types.VOID)
		{
			return null;
		}

		final IValue applyStatementCall = resolveApplyStatement(markers, context, value);
		if (applyStatementCall != null)
		{
			this.returnType = type;
			this.values[this.valueCount - 1] = applyStatementCall;
			return this;
		}

		markers.add(TypeChecker
			            .typeError(value.getPosition(), type, value.getType(), "statementlist.return", "type.expected",
			                       "return.type"));
		return this;
	}

	@Override
	public boolean isType(IType type)
	{
		if (this.valueCount > 0)
		{
			return this.values[this.valueCount - 1].isType(type);
		}
		return Types.isSameType(type, Types.VOID);
	}

	@Override
	public int getTypeMatch(IType type)
	{
		if (this.valueCount > 0)
		{
			return this.values[this.valueCount - 1].getTypeMatch(type);
		}
		return 0;
	}

	@Override
	public Iterator<IValue> iterator()
	{
		return new ArrayIterator<>(this.values, this.valueCount);
	}

	@Override
	public int valueCount()
	{
		return this.valueCount;
	}

	@Override
	public boolean isEmpty()
	{
		return this.valueCount == 0;
	}

	@Override
	public void setValue(int index, IValue value)
	{
		this.values[index] = value;
	}

	@Override
	public void addValue(IValue value)
	{
		int index = this.valueCount++;
		if (index >= this.values.length)
		{
			IValue[] temp = new IValue[this.valueCount];
			System.arraycopy(this.values, 0, temp, 0, index);
			this.values = temp;
		}
		this.values[index] = value;
	}

	@Override
	public void addValue(IValue value, Label label)
	{
		int index = this.valueCount++;
		if (this.valueCount > this.values.length)
		{
			IValue[] temp = new IValue[this.valueCount];
			System.arraycopy(this.values, 0, temp, 0, index);
			this.values = temp;
		}
		this.values[index] = value;

		if (this.labels == null)
		{
			this.labels = new Label[index + 1];
			this.labels[index] = label;
			return;
		}
		if (index >= this.labels.length)
		{
			Label[] temp = new Label[index + 1];
			System.arraycopy(this.labels, 0, temp, 0, this.labels.length);
			this.labels = temp;
		}
		this.labels[index] = label;
	}

	@Override
	public void addValue(int index, IValue value)
	{
		IValue[] temp = new IValue[++this.valueCount];
		System.arraycopy(this.values, 0, temp, 0, index);
		temp[index] = value;
		System.arraycopy(this.values, index, temp, index + 1, this.valueCount - index - 1);
		this.values = temp;
	}

	@Override
	public IValue getValue(int index)
	{
		return this.values[index];
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		if (this.variables != null)
		{
			// Intentionally start at the last variable
			for (int i = this.variables.size() - 1; i >= 0; i--)
			{
				final IVariable variable = this.variables.get(i);
				if (variable.getName() == name)
				{
					return variable;
				}
			}
		}

		return null;
	}

	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		if (this.methods == null)
		{
			return;
		}

		for (IMethod method : this.methods)
		{
			final float match = method.getSignatureMatch(name, instance, arguments);
			if (match > 0)
			{
				list.add(method, match);
			}
		}
	}

	@Override
	public Label resolveLabel(Name name)
	{
		if (this.labels != null)
		{
			for (Label label : this.labels)
			{
				if (label != null && name == label.name)
				{
					return label;
				}
			}
		}

		return null;
	}

	@Override
	public ILoop getEnclosingLoop()
	{
		return null;
	}

	@Override
	public boolean isMember(IVariable variable)
	{
		return this.variables != null && this.variables.contains(variable);
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		// We don't push this context because Statement Lists can't define any types (yet)

		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].resolveTypes(markers, context);
		}
	}

	@Override
	public void resolveStatement(ILabelContext context, MarkerList markers)
	{
		final ILabelContext labelContext = new CombiningLabelContext(this, context);
		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].resolveStatement(labelContext, markers);
		}
	}

	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		if (this.valueCount <= 0)
		{
			return this;
		}

		context = context.push(this);

		// Resolve and check all values
		final int lastIndex = this.valueCount - 1;
		for (int i = 0; i < this.valueCount; i++)
		{
			final IValue resolvedValue = this.values[i] = this.values[i].resolve(markers, context);
			final int valueTag = resolvedValue.valueTag();

			if (valueTag == IValue.VARIABLE)
			{
				this.addVariable((FieldInitializer) resolvedValue, markers, context);
				continue;
			}
			if (valueTag == IValue.MEMBER_STATEMENT)
			{
				this.addMethod((MemberStatement) resolvedValue, markers);
				continue;
			}

			if (i == lastIndex)
			{
				break;
			}

			// Try to resolve an applyStatement method

			if (!resolvedValue.isStatement())
			{
				final IValue applyStatementCall = resolveApplyStatement(markers, context, resolvedValue);
				if (applyStatementCall != null)
				{
					this.values[i] = applyStatementCall;
					continue;
				}
			}

			this.values[i] = IStatement.checkStatement(markers, context, resolvedValue, "statementlist.statement");
		}

		context.pop();

		return this;
	}

	private static IValue resolveApplyStatement(MarkerList markers, IContext context, IValue resolvedValue)
	{
		final SingleArgument argument = new SingleArgument(resolvedValue);

		IMethod method = ICall.resolveMethod(context, null, Names.applyStatement, argument);
		if (method != null)
		{
			final MethodCall call = new MethodCall(resolvedValue.getPosition(), null, method, argument);
			call.checkArguments(markers, context);
			return call;
		}

		final IValue implicitValue = context.getImplicit();
		if (implicitValue == null)
		{
			return null;
		}

		method = ICall.resolveMethod(context, implicitValue, Names.applyStatement, argument);
		if (method != null)
		{
			final MethodCall call = new MethodCall(resolvedValue.getPosition(), implicitValue, method, argument);
			call.checkArguments(markers, context);
			return call;
		}

		return null;
	}

	protected void addVariable(FieldInitializer initializer, MarkerList markers, IContext context)
	{
		final IVariable variable = initializer.variable;
		final Name variableName = variable.getName();

		// Additional Semantic Analysis

		// Uninitialized Variables
		if (variable.getValue() == null)
		{
			variable.setValue(variable.getType().getDefaultValue());
			markers.add(Markers.semanticError(this.position, "variable.value", variableName));
		}

		// Variable Name Shadowing
		final IDataMember dataMember = context.resolveField(variableName);
		if (dataMember != null && dataMember.isVariable())
		{
			markers.add(Markers.semantic(initializer.getPosition(), "variable.shadow", variableName));
		}

		// Actually add the Variable to the List (this has to happen after checking for shadowed variables)

		if (this.variables == null)
		{
			this.variables = new ArrayList<>();
		}
		this.variables.add(variable);
	}

	protected void addMethod(MemberStatement memberStatement, MarkerList markers)
	{
		final IClassMember member = memberStatement.member;
		final MemberKind memberKind = member.getKind();
		if (memberKind != MemberKind.METHOD)
		{
			markers.add(
				Markers.semantic(member.getPosition(), "statementlist.declaration.invalid", Util.memberNamed(member)));

			return;
		}

		final IMethod method = (IMethod) member;

		if (this.methods == null)
		{
			this.methods = new ArrayList<>();
			this.methods.add(method);
			return;
		}

		final Name methodName = method.getName();
		final int parameterCount = method.parameterCount();
		final String desc = method.getDescriptor();
		for (IMethod candidate : this.methods)
		{
			if (candidate.getName() == methodName // same name
				    && candidate.parameterCount() == parameterCount && candidate.getDescriptor().equals(desc))
			{
				markers.add(Markers.semanticError(memberStatement.getPosition(), "method.duplicate", methodName, desc));
			}
		}

		this.methods.add(method);
	}

	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		context = context.push(this);

		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].checkTypes(markers, context);
		}

		context.pop();
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
		context = context.push(this);

		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i].check(markers, context);
		}

		context.pop();
	}

	@Override
	public IValue foldConstants()
	{
		if (this.valueCount == 1)
		{
			return this.values[0].foldConstants();
		}

		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i] = this.values[i].foldConstants();
		}
		return this;
	}

	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		context = context.push(this);

		if (this.valueCount == 1)
		{
			return this.values[0].cleanup(context, compilableList);
		}

		for (int i = 0; i < this.valueCount; i++)
		{
			this.values[i] = this.values[i].cleanup(context, compilableList);
		}

		context.pop();
		return this;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		int statementCount = this.valueCount - 1;
		if (statementCount < 0)
		{
			return;
		}

		dyvil.tools.asm.Label start = new dyvil.tools.asm.Label();
		dyvil.tools.asm.Label end = new dyvil.tools.asm.Label();

		writer.visitLabel(start);
		int localCount = writer.localCount();

		if (this.labels == null)
		{
			// Write all statements except the last one
			for (int i = 0; i < statementCount; i++)
			{
				this.values[i].writeExpression(writer, Types.VOID);
			}

			// Write the last expression
			this.values[statementCount].writeExpression(writer, type);
		}
		else
		{
			final int labelCount = this.labels.length;
			Label label;

			// Write all statements except the last one
			for (int i = 0; i < statementCount; i++)
			{
				if (i < labelCount && (label = this.labels[i]) != null)
				{
					writer.visitLabel(label.getTarget());
				}

				this.values[i].writeExpression(writer, Types.VOID);
			}

			// Write last expression (and label)
			if (statementCount < labelCount && (label = this.labels[statementCount]) != null)
			{
				writer.visitLabel(label.getTarget());
			}

			this.values[statementCount].writeExpression(writer, type);
		}

		writer.resetLocals(localCount);
		writer.visitLabel(end);

		if (this.variables == null)
		{
			return;
		}

		for (IVariable variable : this.variables)
		{
			variable.writeLocal(writer, start, end);
		}
	}

	@Override
	public String toString()
	{
		return IASTNode.toString(this);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.valueCount == 0)
		{
			if (Formatting.getBoolean("statement.empty.newline"))
			{
				buffer.append('{').append('\n').append(prefix).append('}');
			}
			else if (Formatting.getBoolean("statement.empty.space_between"))
			{
				buffer.append("{ }");
			}
			else
			{
				buffer.append("{}");
			}
			return;
		}

		buffer.append('{').append('\n');

		String indentedPrefix = Formatting.getIndent("statement.indent", prefix);
		int prevLine = 0;
		Label label;

		for (int i = 0; i < this.valueCount; i++)
		{
			IValue value = this.values[i];
			ICodePosition pos = value.getPosition();
			buffer.append(indentedPrefix);

			if (pos != null)
			{
				if (pos.startLine() - prevLine > 1 && i > 0)
				{
					buffer.append('\n').append(indentedPrefix);
				}
				prevLine = pos.endLine();
			}

			if (this.labels != null && i < this.labels.length && (label = this.labels[i]) != null)
			{
				buffer.append(label.name);

				if (Formatting.getBoolean("label.separator.space_before"))
				{
					buffer.append(' ');
				}
				buffer.append(':');
				if (Formatting.getBoolean("label.separator.newline_after"))
				{
					buffer.append('\n').append(indentedPrefix);
				}
				else if (Formatting.getBoolean("label.separator.newline_after"))
				{
					buffer.append(' ');
				}
			}

			value.toString(indentedPrefix, buffer);

			if (Formatting.getBoolean("statement.semicolon"))
			{
				buffer.append(';');
			}
			buffer.append('\n');
		}

		buffer.append(prefix).append('}');
	}
}
