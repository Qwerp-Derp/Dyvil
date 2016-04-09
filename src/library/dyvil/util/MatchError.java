package dyvil.util;

/**
 * A <b>MatchError</b> is a {@link RuntimeException} thrown when a non-exhaustive {@code match} expression receives a
 * value that none of it's patterns can handle. The MatchError contains the value that was attempted to be matched, and
 * it's {@link #getMessage()} method returns a string describing the value as well as it's type.
 *
 * @author Clashsoft
 * @version 1.0
 */
public class MatchError extends RuntimeException
{
	private static final long serialVersionUID = 2882649299151786454L;

	public MatchError(Object match)
	{
		super(getMessage(match), null, false, true);
	}

	protected static String getMessage(Object match)
	{
		if (match == null)
		{
			return "null";
		}
		try
		{
			return match.toString() + " (of class " + match.getClass().getName() + ")";
		}
		catch (Throwable t)
		{
			return "An instance of class " + match.getClass().getCanonicalName();
		}
	}
}
