package dyvil.tools.parsing;

import dyvil.collection.List;
import dyvil.collection.mutable.ArrayList;
import dyvil.tools.parsing.lexer.Symbols;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.token.IToken;

public class TryParserManager extends ParserManager
{
	private boolean hasSyntaxErrors;
	private boolean reportErrors;

	private List<IToken> splitTokens;

	public static final int REPORT_ERRORS = 1;
	public static final int EXIT_ON_ROOT  = 2;

	public TryParserManager(Symbols symbols)
	{
		super(symbols);
	}

	public TryParserManager(Symbols symbols, TokenIterator tokens)
	{
		super(symbols, tokens, null);
	}

	@Deprecated
	public TryParserManager(Symbols symbols, TokenIterator tokens, MarkerList markers)
	{
		super(symbols, tokens, markers);
	}

	@Override
	public void report(Marker error)
	{
		final boolean isError = error.isError();
		if (!this.hasSyntaxErrors && isError)
		{
			this.hasSyntaxErrors = true;
		}

		if (this.reportErrors || !isError)
		{
			super.report(error);
		}
	}

	public void resetTo(IToken token)
	{
		this.tokens.jump(token);

		if (this.splitTokens == null)
		{
			return;
		}

		// Restore all tokens that have been split
		for (IToken splitToken : this.splitTokens)
		{
			// The original tokens prev and next fields have not been updated by the split method

			splitToken.prev().setNext(splitToken);
			splitToken.next().setPrev(splitToken);
		}

		this.splitTokens.clear();
	}

	@Override
	public IToken split(IToken token, int length)
	{
		final IToken split = super.split(token, length);
		if (split == token)
		{
			return token;
		}

		if (this.splitTokens == null)
		{
			this.splitTokens = new ArrayList<>();
		}
		this.splitTokens.add(token);

		return split;
	}

	@Deprecated
	public boolean parse(Parser parser, boolean reportErrors)
	{
		return this.parse(parser, this.markers, reportErrors ? REPORT_ERRORS : 0);
	}

	@Deprecated
	public boolean parse(Parser parser, int flags)
	{
		return this.parse(parser, this.markers, flags);
	}

	public boolean parse(Parser parser, MarkerList markers, int flags)
	{
		this.parser = parser;
		this.hasSyntaxErrors = false;
		this.markers = new MarkerList(markers.getI18n());
		this.reportErrors = (flags & REPORT_ERRORS) != 0;

		IToken token = null;

		while (true)
		{
			if (this.reparse)
			{
				this.reparse = false;
			}
			else
			{
				if (!this.tokens.hasNext())
				{
					break;
				}

				token = this.tokens.next();
			}

			if (this.skip > 0)
			{
				this.skip--;
				continue;
			}

			if (this.parser == null)
			{
				if ((flags & EXIT_ON_ROOT) != 0)
				{
					return this.success(markers);
				}
				this.reportUnparsed(token);
				continue;
			}
			if (!this.reportErrors && this.parser.reportErrors())
			{
				if (this.hasSyntaxErrors)
				{
					return this.success(markers);
				}

				this.reportErrors = true;
			}

			try
			{
				this.parser.parse(this, token);
			}
			catch (Exception ex)
			{
				this.reportError(token, ex);
				return this.success(markers);
			}

			if (this.hasSyntaxErrors && !this.reportErrors)
			{
				return this.success(markers);
			}
		}

		this.parseRemaining(token);

		return this.success(markers);
	}

	private boolean success(MarkerList markers)
	{
		if (!this.hasSyntaxErrors || this.reportErrors)
		{
			markers.addAll(this.markers);
			return true;
		}
		return false;
	}
}
