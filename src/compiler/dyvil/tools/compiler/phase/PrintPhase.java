package dyvil.tools.compiler.phase;

import dyvil.tools.compiler.DyvilCompiler;
import dyvil.tools.compiler.ast.header.ICompilationUnit;
import dyvil.tools.compiler.lang.I18n;

public class PrintPhase implements ICompilerPhase
{
	private final ICompilerPhase predecessor;
	
	public PrintPhase(ICompilerPhase predecessor)
	{
		this.predecessor = predecessor;
	}
	
	@Override
	public String getName()
	{
		return "PRINT";
	}
	
	@Override
	public int getID()
	{
		return this.predecessor.getID() + 1;
	}
	
	@Override
	public void apply(DyvilCompiler compiler)
	{
		compiler.log(I18n.get("phase.syntax_trees", this.predecessor.getName()));
		for (ICompilationUnit unit : compiler.fileFinder.units)
		{
			try
			{
				compiler.log(unit.getSourceFile() + ":\n" + unit.toString());
			}
			catch (Throwable throwable)
			{
				compiler.error(I18n.get("phase.syntax_trees.error", unit.getSourceFile()), throwable);
			}
		}
	}
	
	@Override
	public String toString()
	{
		return "PRINT[" + this.predecessor.getName() + "]";
	}
}
