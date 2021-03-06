package dyvil.tools.parsing.token;

import dyvil.tools.parsing.Name;

public class IdentifierToken implements IToken
{
	public IToken prev;
	public IToken next;

	public final int  type;
	public final Name name;

	public final int lineNumber;
	public final int startColumn;
	public final int endColumn;

	public IdentifierToken(IToken prev, Name name, int type, int lineNumber, int startColumn, int endColumn)
	{
		this.prev = prev;
		prev.setNext(this);
		this.name = name;
		this.type = type;

		this.lineNumber = lineNumber;
		this.startColumn = startColumn;
		this.endColumn = endColumn;
	}

	public IdentifierToken(Name name, int type, int lineNumber, int startColumn, int endColumn)
	{
		this.name = name;
		this.type = type;

		this.lineNumber = lineNumber;
		this.startColumn = startColumn;
		this.endColumn = endColumn;
	}

	@Override
	public int type()
	{
		return this.type;
	}

	@Override
	public Name nameValue()
	{
		return this.name;
	}

	@Override
	public String stringValue()
	{
		return this.name.unqualified;
	}

	@Override
	public int startColumn()
	{
		return this.startColumn;
	}

	@Override
	public int endColumn()
	{
		return this.endColumn;
	}

	@Override
	public int startLine()
	{
		return this.lineNumber;
	}

	@Override
	public int endLine()
	{
		return this.lineNumber;
	}

	@Override
	public void setPrev(IToken prev)
	{
		this.prev = prev;
	}

	@Override
	public void setNext(IToken next)
	{
		this.next = next;
	}

	@Override
	public IToken prev()
	{
		return this.prev;
	}

	@Override
	public IToken next()
	{
		return this.next;
	}

	@Override
	public boolean hasNext()
	{
		return this.next.type() != 0;
	}

	@Override
	public boolean hasPrev()
	{
		return this.prev.type() != 0;
	}

	@Override
	public String toString()
	{
		return "Identifier '" + this.name + "\'";
	}
}
