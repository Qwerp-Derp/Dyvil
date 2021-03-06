package dyvil.tools.gensrc.ast.directive;

import dyvil.tools.gensrc.GenSrc;
import dyvil.tools.gensrc.ast.scope.Scope;
import dyvil.tools.parsing.marker.MarkerList;

import java.io.PrintStream;
import java.util.Arrays;

public class DirectiveList implements Directive
{
	private Directive[] directives;
	private int         directiveCount;

	public DirectiveList()
	{
		this.directives = new Directive[8];
	}

	public DirectiveList(int capacity)
	{
		this.directives = new Directive[capacity];
	}

	public int size()
	{
		return this.directiveCount;
	}

	public void add(Directive dir)
	{
		final int index = this.directiveCount++;
		if (index >= this.directives.length)
		{
			this.directives = Arrays.copyOf(this.directives, index << 1);
		}
		this.directives[index] = dir;
	}

	@Override
	public void specialize(GenSrc gensrc, Scope scope, MarkerList markers, PrintStream output)
	{
		for (int i = 0; i < this.directiveCount; i++)
		{
			this.directives[i].specialize(gensrc, scope, markers, output);
		}
	}

	@Override
	public String toString()
	{
		return Directive.toString(this);
	}

	@Override
	public void toString(String indent, StringBuilder builder)
	{
		for (int i = 0; i < this.directiveCount; i++)
		{
			this.directives[i].toString(indent, builder);
		}
	}
}
