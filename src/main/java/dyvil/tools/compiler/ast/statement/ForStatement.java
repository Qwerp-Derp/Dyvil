package dyvil.tools.compiler.ast.statement;

import java.util.List;

import dyvil.tools.compiler.ast.field.Field;

public class ForStatement extends WhileStatement
{
	private List<Field>	variables;
	private IStatement		action;
	
	public ForStatement()
	{
	}
	
	public void setVariables(List<Field> variables)
	{
		this.variables = variables;
	}
	
	public void setAction(IStatement action)
	{
		this.action = action;
	}
	
	public List<Field> getVariables()
	{
		return this.variables;
	}
	
	public void addVariable(Field variable)
	{
		this.variables.add(variable);
	}
	
	public IStatement getAction()
	{
		return this.action;
	}
}