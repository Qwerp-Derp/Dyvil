package dyvil.tools.compiler.parser.annotation;

import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.field.Field;
import dyvil.tools.compiler.lexer.marker.SyntaxError;
import dyvil.tools.compiler.lexer.token.IToken;
import dyvil.tools.compiler.parser.Parser;
import dyvil.tools.compiler.parser.ParserManager;
import dyvil.tools.compiler.parser.expression.ExpressionParser;

public class AnnotationParametersParser extends Parser
{
	protected IContext context;
	protected Annotation annotation;
	
	private Field parameter;
	
	public AnnotationParametersParser(IContext context, Annotation annotation)
	{
		this.context = context;
		this.annotation = annotation;
	}
	
	@Override
	public boolean parse(ParserManager pm, String value, IToken token) throws SyntaxError
	{
		if (",".equals(value))
		{
			this.annotation.addParameter(this.parameter);
			return true;
		}
		else if (")".equals(value))
		{
			pm.popParser(token);
			return true;
		}
		else if ("=".equals(value))
		{
			this.parameter = new Field(token.prev().value());
			pm.pushParser(new ExpressionParser(this.context, this.parameter));
			return true;
		}
		return false;
	}
}
