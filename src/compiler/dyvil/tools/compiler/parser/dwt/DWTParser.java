package dyvil.tools.compiler.parser.dwt;

import dyvil.tools.compiler.ast.dwt.DWTNode;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.ast.value.IValued;
import dyvil.tools.compiler.lexer.marker.SyntaxError;
import dyvil.tools.compiler.lexer.token.IToken;
import dyvil.tools.compiler.parser.Parser;
import dyvil.tools.compiler.parser.ParserManager;
import dyvil.tools.compiler.util.ParserUtil;
import dyvil.tools.compiler.util.Tokens;

public class DWTParser extends Parser implements IValued
{
	public static final int	NAME			= 1;
	public static final int	BODY			= 2;
	public static final int	PROPERTY_NAME	= 4;
	public static final int	EQUALS			= 8;
	public static final int	BODY_END		= 64;
	
	public DWTNode			node;
	
	private String	name;
	
	public DWTParser(DWTNode node)
	{
		this.node = node;
		this.mode = NAME;
	}
	
	@Override
	public void parse(ParserManager pm, IToken token) throws SyntaxError
	{
		int type = token.type();
		if (this.mode == NAME)
		{
			this.mode = BODY;
			if (ParserUtil.isIdentifier(type))
			{
				this.node.setPosition(token.raw());
				this.node.name = this.node.fullName = token.value();
				return;
			}
			throw new SyntaxError(token, "Invalid DWT File - Name expected");
		}
		if (this.mode == BODY)
		{
			if (type == Tokens.OPEN_CURLY_BRACKET)
			{
				this.mode = PROPERTY_NAME;
				return;
			}
			throw new SyntaxError(token, "Invalid Body - '{' expected");
		}
		if (type == Tokens.CLOSE_CURLY_BRACKET)
		{
			pm.popParser();
			return;
		}
		if (this.mode == PROPERTY_NAME)
		{
			if (ParserUtil.isIdentifier(type))
			{
				this.mode = EQUALS;
				this.name = token.value();
				return;
			}
			this.mode = PROPERTY_NAME;
			throw new SyntaxError(token, "Invalid Property - Name expected");
		}
		if (this.mode == EQUALS)
		{
			if (type == Tokens.EQUALS || type == Tokens.COLON)
			{
				pm.pushParser(new DWTValueParser(this));
				return;
			}
			this.mode = PROPERTY_NAME;
			throw new SyntaxError(token, "Invalid Property - '=' expected");
		}
		if (this.mode == BODY_END)
		{
			// type == Tokens.CLOSE_CURLY_BRACKET was already handled above, so
			// throw a SyntaxError
			throw new SyntaxError(token, "Invalid Body - '}' expected");
		}
	}
	
	@Override
	public void setValue(IValue value)
	{
		this.mode = PROPERTY_NAME;
		this.node.addValue(this.name, value);
		this.name = null;
	}
	
	@Override
	public IValue getValue()
	{
		return null;
	}
}