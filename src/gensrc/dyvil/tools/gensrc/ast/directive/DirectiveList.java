package dyvil.tools.gensrc.ast.directive;

import dyvil.tools.gensrc.GenSrc;
import dyvil.tools.gensrc.ast.scope.Scope;

import java.io.PrintStream;
import java.util.Arrays;

public class DirectiveList implements Directive
{
	private Directive[] directives = new Directive[10];
	private int directiveCount;

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
	public void specialize(GenSrc gensrc, Scope scope, PrintStream output)
	{
		for (int i = 0; i < this.directiveCount; i++)
		{
			this.directives[i].specialize(gensrc, scope, output);
		}
	}
}
