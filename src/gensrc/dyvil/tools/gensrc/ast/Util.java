package dyvil.tools.gensrc.ast;

import dyvil.collection.List;
import dyvil.collection.mutable.ArrayList;
import dyvil.tools.gensrc.GenSrc;
import dyvil.tools.gensrc.ast.scope.Scope;
import dyvil.tools.gensrc.lang.I18n;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.marker.SemanticError;
import dyvil.tools.parsing.position.ICodePosition;

import java.io.File;

public class Util
{
	public static String getArgument(String line, int start, int end)
	{
		return line.substring(skipWhitespace(line, start, end));
	}

	public static String getProcessedArgument(String line, int start, int end, Scope replacements)
	{
		return processLine(line, skipWhitespace(line, start, end), end, replacements);
	}

	public static String[] getProcessedArguments(String line, int start, int end, Scope replacements)
	{
		return getProcessedArgument(line, start, end, replacements).split("\\s*,\\s*");
	}

	private static String parseIdentifier(String line, int start, int end)
	{
		final int keyStart = skipWhitespace(line, start, end);
		final int keyEnd = findIdentifierEnd(line, keyStart, end);
		return line.substring(keyStart, keyEnd);
	}

	public static String processLine(String line, Scope replacements)
	{
		return processLine(line, 0, line.length(), replacements);
	}

	public static String processLine(String line, int start, int end, Scope replacements)
	{
		if (start == end)
		{
			return "";
		}

		final StringBuilder builder = new StringBuilder(end - start);

		int prev = start;

		for (int i = start; i < end; )
		{
			final char c = line.charAt(i);

			if (c == '#' && i + 1 < end && line.charAt(i + 1) == '#')
			{
				// two consecutive ## are stripped

				// append contents before this identifier
				builder.append(line, prev, i);
				i = prev = i + 2; // advance by two characters
				continue;
			}
			if (!Character.isJavaIdentifierStart(c))
			{
				// advance to the first identifier start character
				i++;
				continue;
			}

			// append contents before this identifier
			builder.append(line, prev, i);

			// index of the first character that is not part of this identifier
			final int nextIndex = findIdentifierEnd(line, i + 1, end);
			final String key = line.substring(i, nextIndex);
			final String replacement = replacements.getReplacement(key);

			if (replacement != null)
			{
				builder.append(replacement); // append the replacement instead of the identifier
			}
			else
			{
				builder.append(line, i, nextIndex); // append the original identifier
			}
			i = prev = nextIndex;
		}

		// append remaining characters on line
		builder.append(line, prev, end);
		return builder.toString();
	}

	public static int findIdentifierEnd(String line, int start, int end)
	{
		for (; start < end; start++)
		{
			if (!Character.isJavaIdentifierPart(line.charAt(start)))
			{
				return start;
			}
		}
		return end;
	}

	private static int findWhitespace(String line, int start, int end)
	{
		for (; start < end; start++)
		{
			if (Character.isWhitespace(line.charAt(start)))
			{
				return start;
			}
		}
		return end;
	}

	/**
	 * Returns the first index greater than or equal to {@code start} where the character in {@code line} is NOT
	 * whitespace. If no such index is found, {@code end} is returned.
	 *
	 * @param line
	 * 	the string to check
	 * @param start
	 * 	the first index (inclusive) to check
	 * @param end
	 * 	the last index (exclusive) to check
	 *
	 * @return the first index {@code >= start} and {@code < end} where the character in the {@code string} is
	 * non-whitespace, or {@code end}.
	 */
	public static int skipWhitespace(String line, int start, int end)
	{
		for (; start < end; start++)
		{
			if (!Character.isWhitespace(line.charAt(start)))
			{
				return start;
			}
		}
		return end;
	}

	public static List<Specialization> parseSpecs(String line, GenSrc gensrc, Scope scope, MarkerList markers,
		                                             ICodePosition position)
	{
		final String[] files = getProcessedArguments(line, 0, line.length(), scope);
		final List<Specialization> specs = new ArrayList<>(files.length);
		final File sourceFile = scope.getSourceFile();

		for (String file : files)
		{
			final Specialization spec = Specialization.resolveSpec(gensrc, file, sourceFile);
			if (spec != null)
			{
				specs.add(spec);
				continue;
			}

			markers.add(new SemanticError(position, I18n.get("spec.resolve", file)));
		}
		return specs;
	}
}
