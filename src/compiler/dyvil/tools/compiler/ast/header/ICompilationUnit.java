package dyvil.tools.compiler.ast.header;

import dyvil.io.Console;
import dyvil.tools.compiler.DyvilCompiler;
import dyvil.tools.compiler.lang.I18n;
import dyvil.tools.compiler.sources.IFileType;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.source.FileSource;

import java.io.File;

public interface ICompilationUnit extends IASTNode
{
	FileSource getSourceFile();

	File getOutputFile();

	MarkerList getMarkers();

	void tokenize();

	void parse();

	void resolveHeaders();

	void resolveTypes();

	void resolve();

	void checkTypes();

	void check();

	void foldConstants();

	void cleanup();

	void compile();

	static boolean printMarkers(DyvilCompiler compiler, MarkerList markers, IFileType fileType, Name name, FileSource source)
	{
		final int size = markers.size();
		if (size <= 0)
		{
			return false;
		}

		final StringBuilder builder = new StringBuilder(I18n
			                                                .get("unit.problems", fileType.getLocalizedName(), name,
			                                                     source)).append("\n\n");

		final int warnings = markers.getWarnings();
		final int errors = markers.getErrors();
		final boolean colors = compiler.config.useAnsiColors();

		markers.log(source, builder, colors);

		builder.append('\n');

		if (errors > 0)
		{
			if (colors)
			{
				builder.append(Console.ANSI_RED);
			}

			builder.append(errors == 1 ? I18n.get("unit.errors.1") : I18n.get("unit.errors.n", errors));

			if (colors)
			{
				builder.append(Console.ANSI_RESET);
			}
		}
		if (warnings > 0)
		{
			if (errors > 0)
			{
				builder.append(", ");
			}

			if (colors)
			{
				builder.append(Console.ANSI_YELLOW);
			}
			builder.append(
				warnings == 1 ? I18n.get("unit.warnings.1") : I18n.get("unit.warnings.n", warnings));

			if (colors)
			{
				builder.append(Console.ANSI_RESET);
			}
		}

		builder.append('\n');

		compiler.log(builder.toString());
		if (errors > 0)
		{
			compiler.fail();
			compiler.warn(I18n.get("unit.problems.not_compiled", name));
			compiler.warn("");
			return true;
		}
		return false;
	}
}
