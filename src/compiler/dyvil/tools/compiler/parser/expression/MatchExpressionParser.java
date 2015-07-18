package dyvil.tools.compiler.parser.expression;

import dyvil.tools.compiler.ast.consumer.IValueConsumer;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.MatchCase;
import dyvil.tools.compiler.ast.expression.MatchExpression;
import dyvil.tools.compiler.lexer.marker.SyntaxError;
import dyvil.tools.compiler.lexer.token.IToken;
import dyvil.tools.compiler.parser.IParserManager;
import dyvil.tools.compiler.parser.Parser;
import dyvil.tools.compiler.parser.pattern.PatternParser;
import dyvil.tools.compiler.transform.Keywords;
import dyvil.tools.compiler.transform.Symbols;

public class MatchExpressionParser extends Parser implements IValueConsumer
{
	private static final int	OPEN_BRACKET	= 1;
	private static final int	CASE			= 2;
	private static final int	CONDITION		= 4;
	private static final int	ACTION			= 8;
	private static final int	SEPARATOR		= 16;
	
	protected MatchExpression	matchExpression;
	
	private MatchCase			currentCase;
	private boolean				singleCase;
	
	public MatchExpressionParser(MatchExpression matchExpression)
	{
		this.matchExpression = matchExpression;
		this.mode = OPEN_BRACKET;
	}
	
	@Override
	public void reset()
	{
		this.mode = OPEN_BRACKET;
	}
	
	@Override
	public void parse(IParserManager pm, IToken token) throws SyntaxError
	{
		int type = token.type();
		
		switch (this.mode)
		{
		case OPEN_BRACKET:
			if (type == Symbols.OPEN_CURLY_BRACKET)
			{
				this.mode = CASE;
				return;
			}
			if (type != Keywords.CASE)
			{
				throw new SyntaxError(token, "Invalid Match Expression - '{' or 'case' expected");
			}
			this.singleCase = true;
		case CASE:
			if (type == Keywords.CASE)
			{
				this.currentCase = new MatchCase();
				this.mode = CONDITION;
				pm.pushParser(new PatternParser(this.currentCase));
				return;
			}
			if (type == Symbols.CLOSE_CURLY_BRACKET)
			{
				pm.popParser();
				return;
			}
			throw new SyntaxError(token, "Invalid Match Expression - 'case' or '}' expected expected");
		case CONDITION:
			if (type == Keywords.IF)
			{
				this.mode = ACTION;
				pm.pushParser(pm.newExpressionParser(this));
				return;
			}
		case ACTION:
			this.mode = SEPARATOR;
			pm.pushParser(pm.newExpressionParser(this));
			if (type == Symbols.COLON || type == Symbols.ARROW_OPERATOR)
			{
				return;
			}
			throw new SyntaxError(token, "Invalid Match Case - ':' or '=>' expected");
		case SEPARATOR:
			this.matchExpression.addCase(this.currentCase);
			if (this.singleCase)
			{
				pm.popParser(true);
				return;
			}
			
			this.currentCase = null;
			if (type == Symbols.CLOSE_CURLY_BRACKET)
			{
				pm.popParser();
				return;
			}
			this.mode = CASE;
			if (type == Symbols.SEMICOLON)
			{
				return;
			}
			throw new SyntaxError(token, "Invalid Match Case - ';' expected", true);
		}
	}
	
	@Override
	public void setValue(IValue value)
	{
		switch (this.mode)
		{
		case ACTION:
			this.currentCase.setCondition(value);
			return;
		case SEPARATOR:
			this.currentCase.setAction(value);
			return;
		}
	}
}