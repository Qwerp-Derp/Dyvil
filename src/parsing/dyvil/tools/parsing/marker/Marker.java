package dyvil.tools.parsing.marker;

import dyvil.collection.List;
import dyvil.collection.mutable.ArrayList;
import dyvil.io.AppendablePrintStream;
import dyvil.io.Console;
import dyvil.tools.parsing.position.ICodePosition;

public abstract class Marker implements Comparable<Marker>
{
	protected final ICodePosition position;

	private final String       message;
	private       List<String> info;

	public Marker(ICodePosition position, String message)
	{
		if (position == null)
		{
			position = ICodePosition.ORIGIN;
		}

		this.message = message;
		this.position = position;
	}

	public ICodePosition getPosition()
	{
		return this.position;
	}

	public String getMessage()
	{
		return this.message;
	}

	public void addInfo(String info)
	{
		if (this.info == null)
		{
			this.info = new ArrayList<>(2);
		}
		this.info.add(info);
	}

	public void addError(Throwable throwable)
	{
		final StringBuilder builder = new StringBuilder();
		throwable.printStackTrace(new AppendablePrintStream(builder));
		this.addInfo(builder.toString());
	}

	public abstract String getMarkerType();

	public abstract String getColor();

	public abstract boolean isError();

	public abstract boolean isWarning();

	@Override
	public int compareTo(Marker o)
	{
		final int start1 = this.position.startIndex();
		final int start2 = o.position.startIndex();
		return start1 == start2 ? 0 : start1 < start2 ? -1 : 0;
	}

	@Override
	@SuppressWarnings("SimplifiableIfStatement")
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof Marker))
			return false;

		Marker marker = (Marker) o;

		if (this.position != null ? !this.position.equals(marker.position) : marker.position != null)
			return false;
		if (this.message != null ? !this.message.equals(marker.message) : marker.message != null)
			return false;
		return this.info != null ? this.info.equals(marker.info) : marker.info == null;
	}

	@Override
	public int hashCode()
	{
		int result = this.position != null ? this.position.hashCode() : 0;
		result = 31 * result + (this.message != null ? this.message.hashCode() : 0);
		result = 31 * result + (this.info != null ? this.info.hashCode() : 0);
		return result;
	}

	public void log(String code, StringBuilder buf, boolean colors)
	{
		final String type = this.getMarkerType();
		final String message = this.message;

		buf.append("line ").append(this.position.startLine()).append(": ");

		final String colorString;
		if (colors)
		{
			colorString = this.getColor();
			buf.append(colorString);
		}
		else
		{
			colorString = null;
		}

		buf.append(type);
		if (message != null)
		{
			buf.append(": ").append(message);
		}

		if (colors)
		{
			buf.append(Console.ANSI_RESET);
		}

		// Append Info (if any)
		if (this.info != null)
		{
			for (String s : this.info)
			{
				buf.append("\n\t").append(s);
			}
			buf.append('\n');
		}

		if (code == null)
		{
			buf.append('\n');
			return;
		}

		int startIndex = this.position.startIndex();
		int endIndex = this.position.endIndex();
		final int codeLength = code.length();

		if (startIndex >= codeLength)
		{
			startIndex = codeLength - 1;
		}
		if (endIndex > codeLength)
		{
			endIndex = codeLength;
		}

		final int prevNL = lineStart(code, startIndex);
		final int nextNL = lineEnd(code, startIndex);

		buf.append('\n');
		buf.append(code, prevNL, nextNL);
		buf.append('\n');

		// Append Spaces
		for (int i = prevNL; i < startIndex; i++)
		{
			if (code.charAt(i) == '\t')
			{
				buf.append('\t');
			}
			else
			{
				buf.append(' ');
			}
		}

		if (colors)
		{
			buf.append(colorString);
		}
		for (int i = startIndex; i < endIndex; i++)
		{
			buf.append('¯');
		}
		if (colors)
		{
			buf.append(Console.ANSI_RESET);
		}

		buf.append('\n');
	}

	private static int lineStart(String code, int start)
	{
		if (code.charAt(start) == '\n')
		{
			--start;
		}

		final int i = code.lastIndexOf('\n', start);
		if (i < 0)
		{
			return 0;
		}
		return i + 1;
	}

	private static int lineEnd(String code, int end)
	{
		final int i = code.indexOf('\n', end);
		if (i < 0)
		{
			return code.length();
		}
		return i;
	}
}
