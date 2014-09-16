package dyvil.tools.compiler.parser.expression;

import dyvil.tools.compiler.ast.api.IValueList;
import dyvil.tools.compiler.ast.api.IValued;
import dyvil.tools.compiler.ast.context.IClassContext;
import dyvil.tools.compiler.ast.expression.ConstructorCall;
import dyvil.tools.compiler.ast.expression.FieldAccess;
import dyvil.tools.compiler.ast.expression.MethodCall;
import dyvil.tools.compiler.ast.statement.IStatement;
import dyvil.tools.compiler.ast.statement.IfStatement;
import dyvil.tools.compiler.ast.statement.ReturnStatement;
import dyvil.tools.compiler.ast.statement.StatementList;
import dyvil.tools.compiler.ast.value.*;
import dyvil.tools.compiler.lexer.marker.SyntaxError;
import dyvil.tools.compiler.lexer.token.IToken;
import dyvil.tools.compiler.lexer.token.Token;
import dyvil.tools.compiler.parser.Parser;
import dyvil.tools.compiler.parser.ParserManager;
import dyvil.tools.compiler.parser.statement.IfStatementParser;
import dyvil.tools.compiler.parser.type.TypeParser;

public class ExpressionParser extends Parser
{
	public static final int	VALUE			= 1;
	public static final int	VALUE_2			= 2;
	public static final int	TUPLE_END		= 4;
	
	public static final int	ACCESS			= 8;
	public static final int	DOT_ACCESS		= 16;
	public static final int	SUGARACCESS		= 32;
	
	public static final int	STATEMENT		= 64;
	public static final int	TYPE			= 128;
	public static final int	PARAMETERS		= 256;
	public static final int	PARAMETERS_2	= 512;
	
	protected IClassContext	context;
	protected IValued		field;
	protected boolean		statements;
	
	private IValue			value;
	
	public ExpressionParser(IClassContext context, IValued field)
	{
		this.mode = VALUE;
		this.context = context;
		this.field = field;
	}
	
	public ExpressionParser(IClassContext context, IValued field, boolean statements)
	{
		this.mode = VALUE | (statements ? STATEMENT : 0);
		this.context = context;
		this.field = field;
		this.statements = statements;
	}
	
	@Override
	public boolean parse(ParserManager pm, String value, IToken token) throws SyntaxError
	{
		if (this.mode == 0)
		{
			pm.popParser(token);
			return true;
		}
		if (this.isInMode(VALUE))
		{
			if (this.parsePrimitive(value, token))
			{
				return true;
			}
			else if ("(".equals(value))
			{
				this.mode = TUPLE_END;
				this.value = new TupleValue();
				
				if (!token.next().equals(")"))
				{
					pm.pushParser(new ExpressionListParser(this.context, (IValueList) this.value));
				}
				return true;
			}
			else if ("{".equals(value))
			{
				this.mode = VALUE_2;
				this.value = new StatementList();
				
				if (!token.next().equals("}"))
				{
					pm.pushParser(new ExpressionListParser(this.context, (IValueList) this.value, true));
				}
				return true;
			}
			else if ("new".equals(value))
			{
				ConstructorCall call = new ConstructorCall();
				this.mode = PARAMETERS;
				this.value = call;
				pm.pushParser(new TypeParser(call));
				return true;
			}
			this.mode = ACCESS;
		}
		if (this.isInMode(VALUE_2))
		{
			if ("}".equals(value))
			{
				return true;
			}
		}
		if (this.isInMode(TUPLE_END))
		{
			if (")".equals(value))
			{
				this.mode = ACCESS;
				return true;
			}
		}
		if (this.isInMode(STATEMENT))
		{
			if ("return".equals(value))
			{
				ReturnStatement statement = new ReturnStatement();
				this.addStatement(statement);
				pm.pushParser(new ExpressionParser(this.context, statement));
				return true;
			}
			else if ("if".equals(value))
			{
				IfStatement statement = new IfStatement();
				this.addStatement(statement);
				pm.pushParser(new IfStatementParser(this.context, statement));
				return true;
			}
		}
		if (this.isInMode(ACCESS))
		{
			if (".".equals(value))
			{
				this.mode = DOT_ACCESS;
				return true;
			}
			else if (token.isType(Token.TYPE_IDENTIFIER))
			{
				this.mode = SUGARACCESS;
			}
		}
		if (this.isInMode(DOT_ACCESS))
		{
			if (token.next().equals("("))
			{
				MethodCall call = new MethodCall(this.value, value);
				this.value = call;
				this.mode = PARAMETERS;
				return true;
			}
			else
			{
				FieldAccess access = new FieldAccess(this.value, value);
				this.value = access;
				this.mode = VALUE;
				return true;
			}
		}
		if (this.isInMode(SUGARACCESS))
		{
			MethodCall call = new MethodCall(this.value, value);
			call.setSugarCall(true);
			this.value = call;
			this.mode = 0;
			pm.pushTryParser(new ExpressionParser(this.context, call), token.next());
			return true;
		}
		if (this.isInMode(PARAMETERS))
		{
			if ("(".equals(value))
			{
				pm.pushParser(new ExpressionListParser(this.context, (IValueList) this.value));
				this.mode = PARAMETERS_2;
				return true;
			}
		}
		if (this.isInMode(PARAMETERS_2))
		{
			if (")".equals(value))
			{
				this.mode = ACCESS;
				return true;
			}
		}
		
		if (this.value != null)
		{
			pm.popParser(token);
			return true;
		}
		return false;
	}
	
	@Override
	public void end(ParserManager pm)
	{
		if (this.value != null)
		{
			this.field.setValue(this.value);
		}
	}
	
	public boolean parsePrimitive(String value, IToken token) throws SyntaxError
	{
		if ("null".equals(value))
		{
			this.value = new NullValue();
			return true;
		}
		else if ("this".equals(value))
		{
			if (this.context.isStatic())
			{
				throw new SyntaxError(token, "Cannot access 'this' in a static context");
			}
			this.value = new ThisValue();
			return true;
		}
		// Boolean
		else if ("true".equals(value))
		{
			this.value = BooleanValue.of(true);
			return true;
		}
		else if ("false".equals(value))
		{
			this.value = BooleanValue.of(false);
			return true;
		}
		// String
		else if (token.isType(Token.TYPE_STRING))
		{
			this.value = new StringValue((String) token.object());
			return true;
		}
		// Char
		else if (token.isType(Token.TYPE_CHAR))
		{
			this.value = new CharValue((Character) token.object());
			return true;
		}
		// Int
		else if (token.isType(Token.TYPE_INT))
		{
			this.value = new IntValue((Integer) token.object());
			return true;
		}
		else if (token.isType(Token.TYPE_LONG))
		{
			this.value = new LongValue((Long) token.object());
			return true;
		}
		// Float
		else if (token.isType(Token.TYPE_FLOAT))
		{
			this.value = new FloatValue((Float) token.object());
			return true;
		}
		else if (token.isType(Token.TYPE_DOUBLE))
		{
			this.value = new DoubleValue((Double) token.object());
			return true;
		}
		return false;
	}
	
	public void addStatement(IStatement statement)
	{
		if (this.value == null)
		{
			this.value = statement;
		}
		else
		{
			StatementList list = new StatementList();
			list.addValue(this.value);
			list.addStatement(statement);
			this.value = list;
		}
	}
}
