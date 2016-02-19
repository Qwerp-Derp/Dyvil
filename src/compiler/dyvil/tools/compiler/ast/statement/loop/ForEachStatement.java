package dyvil.tools.compiler.ast.statement.loop;

import dyvil.tools.compiler.ast.context.*;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.Variable;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.operator.RangeOperator;
import dyvil.tools.compiler.ast.statement.IStatement;
import dyvil.tools.compiler.ast.statement.control.Label;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import static dyvil.tools.compiler.ast.statement.loop.ForStatement.*;

public class ForEachStatement implements IStatement, IDefaultContext, ILoop
{
	protected ICodePosition position;
	
	protected Variable variable;
	protected IValue   action;
	
	// Metadata
	protected Label startLabel;
	protected Label updateLabel;
	protected Label endLabel;
	
	public ForEachStatement(ICodePosition position, Variable var)
	{
		this.position = position;
		this.variable = var;
		
		this.startLabel = new Label($forStart);
		this.updateLabel = new Label($forUpdate);
		this.endLabel = new Label($forEnd);
	}
	
	public ForEachStatement(ICodePosition position, Variable var, IValue action)
	{
		this(position, var);
		this.action = action;
	}
	
	@Override
	public int valueTag()
	{
		return FOR;
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
	
	public void setVariable(Variable variable)
	{
		this.variable = variable;
	}
	
	public Variable getVariable()
	{
		return this.variable;
	}
	
	@Override
	public void setAction(IValue action)
	{
		this.action = action;
	}
	
	@Override
	public IValue getAction()
	{
		return this.action;
	}
	
	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (type != Types.VOID)
		{
			return null;
		}
		
		return this;
	}
	
	@Override
	public Label getContinueLabel()
	{
		return this.updateLabel;
	}
	
	@Override
	public Label getBreakLabel()
	{
		return this.endLabel;
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		if (this.variable.getName() == name)
		{
			return this.variable;
		}
		
		return null;
	}
	
	@Override
	public Label resolveLabel(Name name)
	{
		if (name == $forStart)
		{
			return this.startLabel;
		}
		if (name == $forUpdate)
		{
			return this.updateLabel;
		}
		if (name == $forEnd)
		{
			return this.endLabel;
		}
		
		return null;
	}
	
	@Override
	public ILoop getEnclosingLoop()
	{
		return this;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.variable != null)
		{
			this.variable.resolveTypes(markers, context);
		}
		
		if (this.action != null)
		{
			this.action.resolveTypes(markers, context);
		}
	}
	
	@Override
	public void resolveStatement(ILabelContext context, MarkerList markers)
	{
		if (this.action != null)
		{
			this.action.resolveStatement(new CombiningLabelContext(this, context), markers);
		}
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		IType varType = this.variable.getType();
		IValue value = this.variable.getValue().resolve(markers, context);
		this.variable.setValue(value);
		
		if (value.valueTag() == IValue.RANGE_OPERATOR)
		{
			RangeOperator ro = (RangeOperator) value;
			IValue value1 = ro.getFirstValue();
			IValue value2 = ro.getLastValue();
			IType rangeType = ro.getElementType();
			
			if (varType == Types.UNKNOWN)
			{
				if (rangeType == Types.UNKNOWN)
				{
					rangeType = Types.combine(value1.getType(), value2.getType());
				}
				
				this.variable.setType(varType = rangeType);
				if (varType == Types.UNKNOWN)
				{
					markers.add(Markers.semantic(this.variable.getPosition(), "for.variable.infer",
					                             this.variable.getName()));
				}
			}
			else if (rangeType != Types.UNKNOWN && !varType.isSuperTypeOf(rangeType))
			{
				Marker marker = Markers.semantic(value1.getPosition(), "for.range.type");
				marker.addInfo(Markers.getSemantic("range.type", rangeType));
				marker.addInfo(Markers.getSemantic("variable.type", varType));
				markers.add(marker);
			}
			
			RangeForStatement rfs = new RangeForStatement(this.position, this.variable, value1, value2,
			                                              ro.isHalfOpen());
			rfs.resolveAction(this.action, markers, context);
			return rfs;
		}
		
		IType valueType = value.getType();
		if (valueType.isArrayType())
		{
			if (varType == Types.UNKNOWN)
			{
				this.variable.setType(varType = valueType.getElementType());
				if (varType == Types.UNKNOWN)
				{
					markers.add(Markers.semantic(this.variable.getPosition(), "for.variable.infer",
					                             this.variable.getName()));
				}
			}
			else if (!varType.isSuperTypeOf(valueType.getElementType()))
			{
				Marker marker = Markers.semantic(value.getPosition(), "for.array.type");
				marker.addInfo(Markers.getSemantic("array.type", valueType));
				marker.addInfo(Markers.getSemantic("variable.type", varType));
				markers.add(marker);
			}
			
			ArrayForStatement afs = new ArrayForStatement(this.position, this.variable, valueType);
			afs.resolveAction(this.action, markers, context);
			return afs;
		}
		if (Types.ITERABLE.isSuperTypeOf(valueType))
		{
			final IType iterableType = valueType.resolveTypeSafely(IterableForStatement.ITERABLE_TYPE).getReturnType();
			if (varType == Types.UNKNOWN)
			{
				this.variable.setType(varType = iterableType);
				if (varType == Types.UNKNOWN)
				{
					markers.add(Markers.semantic(this.variable.getPosition(), "for.variable.infer",
					                             this.variable.getName()));
				}
			}
			else if (!varType.isSuperTypeOf(iterableType))
			{
				Marker m = Markers.semantic(value.getPosition(), "for.iterable.type");
				m.addInfo(Markers.getSemantic("iterable.type", iterableType));
				m.addInfo(Markers.getSemantic("variable.type", varType));
				markers.add(m);
			}
			
			IterableForStatement ifs = new IterableForStatement(this.position, this.variable);
			ifs.resolveAction(this.action, markers, context);
			return ifs;
		}
		if (Types.STRING.isSuperTypeOf(valueType))
		{
			if (varType == Types.UNKNOWN)
			{
				this.variable.setType(Types.CHAR);
			}
			else if (!varType.classEquals(Types.CHAR))
			{
				Marker marker = Markers.semantic(value.getPosition(), "for.string.type");
				marker.addInfo(Markers.getSemantic("variable.type", varType));
				markers.add(marker);
			}
			
			StringForStatement sfs = new StringForStatement(this.position, this.variable);
			sfs.resolveAction(this.action, markers, context);
			return sfs;
		}
		
		Marker marker = Markers.semantic(this.variable.getPosition(), "for.each.invalid");
		marker.addInfo(Markers.getSemantic("variable.type", varType));
		marker.addInfo(Markers.getSemantic("value.type", valueType));
		markers.add(marker);
		
		this.resolveAction(this.action, markers, context);
		
		return this;
	}
	
	protected void resolveAction(IValue action, MarkerList markers, IContext context)
	{
		if (action == null)
		{
			return;
		}
		
		IContext context1 = new CombiningContext(this, context);
		this.action = action.resolve(markers, context1);
		
		IValue typedAction = this.action.withType(Types.VOID, Types.VOID, markers, context1);
		if (typedAction == null || !typedAction.isUsableAsStatement())
		{
			Marker marker = Markers.semantic(this.action.getPosition(), "for.action.type");
			marker.addInfo(Markers.getSemantic("action.type", this.action.getType()));
			markers.add(marker);
		}
		else
		{
			this.action = typedAction;
		}
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		if (this.variable != null)
		{
			this.variable.getValue().checkTypes(markers, context);
		}
		if (this.action != null)
		{
			this.action.checkTypes(markers, new CombiningContext(this, context));
		}
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		if (this.variable != null)
		{
			this.variable.getValue().check(markers, context);
		}
		if (this.action != null)
		{
			this.action.check(markers, new CombiningContext(this, context));
		}
	}
	
	@Override
	public IValue foldConstants()
	{
		this.variable.foldConstants();
		if (this.action != null)
		{
			this.action = this.action.foldConstants();
		}
		return this;
	}
	
	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		this.variable.cleanup(this, compilableList);
		if (this.action != null)
		{
			this.action = this.action.cleanup(new CombiningContext(this, context), compilableList);
		}
		return this;
	}
	
	@Override
	public void writeStatement(MethodWriter writer) throws BytecodeException
	{
		throw new BytecodeException("Cannot compile invalid ForEach statement");
	}
	
	@Override
	public String toString()
	{
		return IASTNode.toString(this);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("for");
		Formatting.appendSeparator(buffer, "for.open_paren", '(');

		this.variable.getType().toString(prefix, buffer);
		buffer.append(' ').append(this.variable.getName());

		Formatting.appendSeparator(buffer, "for.each.separator", ':');

		this.variable.getValue().toString(prefix, buffer);

		if (Formatting.getBoolean("for.close_paren.space_before"))
		{
			buffer.append(' ');
		}
		buffer.append(')');
		
		if (this.action != null && !Util.formatStatementList(prefix, buffer, this.action))
		{
			String actionPrefix = Formatting.getIndent("for.indent", prefix);
			if (Formatting.getBoolean("for.close_paren.newline_after"))
			{
				buffer.append('\n').append(actionPrefix);
			}
			else if (Formatting.getBoolean("for.close_paren.space_after"))
			{
				buffer.append(' ');
			}

			this.action.toString(actionPrefix, buffer);
		}
	}
}
