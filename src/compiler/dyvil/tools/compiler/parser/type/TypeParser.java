package dyvil.tools.compiler.parser.type;

import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.consumer.ITypeConsumer;
import dyvil.tools.compiler.ast.generic.Variance;
import dyvil.tools.compiler.ast.reference.ImplicitReferenceType;
import dyvil.tools.compiler.ast.reference.ReferenceType;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.ITyped;
import dyvil.tools.compiler.ast.type.Mutability;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.ast.type.compound.*;
import dyvil.tools.compiler.ast.type.generic.GenericType;
import dyvil.tools.compiler.ast.type.generic.NamedGenericType;
import dyvil.tools.compiler.ast.type.raw.NamedType;
import dyvil.tools.compiler.parser.ParserUtil;
import dyvil.tools.compiler.parser.annotation.AnnotationParser;
import dyvil.tools.compiler.transform.DyvilKeywords;
import dyvil.tools.compiler.transform.DyvilSymbols;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.IParserManager;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.Parser;
import dyvil.tools.parsing.lexer.BaseSymbols;
import dyvil.tools.parsing.position.CodePosition;
import dyvil.tools.parsing.token.IToken;

public final class TypeParser extends Parser implements ITypeConsumer
{
	protected static final int NAME           = 0;
	protected static final int GENERICS       = 1;
	protected static final int GENERICS_END   = 1 << 1;
	protected static final int ARRAY_COLON    = 1 << 2;
	protected static final int ARRAY_END      = 1 << 3;
	protected static final int TUPLE_END      = 1 << 5;
	protected static final int LAMBDA_END     = 1 << 6;
	protected static final int ANNOTATION_END = 1 << 7;

	// Flags

	public static final int NAMED_ONLY    = 1;
	public static final int IGNORE_LAMBDA = 2;

	protected ITypeConsumer consumer;

	private IType parentType;
	private IType type;

	private int flags;

	public TypeParser(ITypeConsumer consumer)
	{
		this.consumer = consumer;
		this.mode = NAME;
	}

	public TypeParser(ITypeConsumer consumer, IType parentType)
	{
		this.consumer = consumer;
		this.parentType = parentType;
	}

	public TypeParser withFlags(int flags)
	{
		this.flags |= flags;
		return this;
	}

	public TypeParser namedOnly()
	{
		this.flags |= NAMED_ONLY;
		return this;
	}

	@Override
	public void parse(IParserManager pm, IToken token)
	{
		final int type = token.type();
		switch (this.mode)
		{
		case END:
		{
			if ((this.flags & NAMED_ONLY) == 0)
			{
				if (ParserUtil.isIdentifier(type))
				{
					switch (token.stringValue().charAt(0))
					{
					case '?':
						this.type = new OptionType(this.type);
						pm.splitJump(token, 1);
						return;
					case '!':
						this.type = new ImplicitOptionType(this.type);
						pm.splitJump(token, 1);
						return;
					case '*':
						this.type = new ReferenceType(this.type);
						pm.splitJump(token, 1);
						return;
					case '^':
						this.type = new ImplicitReferenceType(this.type);
						pm.splitJump(token, 1);
						return;
					}
				}
				else if (type == DyvilSymbols.ARROW_RIGHT && this.parentType == null
					         && (this.flags & IGNORE_LAMBDA) == 0)
				{
					final LambdaType lambdaType = new LambdaType(token.raw(), this.type);
					this.type = lambdaType;
					this.mode = LAMBDA_END;
					pm.pushParser(new TypeParser(lambdaType));
					return;
				}
			}
			if (type == BaseSymbols.DOT)
			{
				pm.pushParser(new TypeParser(this, this.type));
				return;
			}

			if (this.type != null)
			{
				this.consumer.setType(this.type);
			}
			pm.popParser(true);
			return;
		}
		case NAME:
			if ((this.flags & NAMED_ONLY) == 0)
			{
				switch (type)
				{
				case DyvilSymbols.AT:
					Annotation a = new Annotation();
					pm.pushParser(new AnnotationParser(a));
					this.type = new AnnotatedType(a);
					this.mode = ANNOTATION_END;
					return;
				case BaseSymbols.OPEN_PARENTHESIS:
					TupleType tupleType = new TupleType();
					pm.pushParser(new TypeListParser(tupleType));
					this.type = tupleType;
					this.mode = TUPLE_END;
					return;
				case BaseSymbols.OPEN_SQUARE_BRACKET:
				{
					final ArrayType arrayType = new ArrayType();

					switch (token.next().type())
					{
					case DyvilKeywords.FINAL:
						arrayType.setMutability(Mutability.IMMUTABLE);
						pm.skip();
						break;
					case DyvilKeywords.VAR:
						arrayType.setMutability(Mutability.MUTABLE);
						pm.skip();
						break;
					}

					this.mode = ARRAY_COLON;
					this.type = arrayType;
					pm.pushParser(new TypeParser(arrayType));
					return;
				}
				case DyvilSymbols.ARROW_RIGHT:
				{
					if ((this.flags & IGNORE_LAMBDA) != 0)
					{
						pm.popParser(true);
						return;
					}

					final LambdaType lambdaType = new LambdaType(token.raw(), this.parentType);
					pm.pushParser(new TypeParser(lambdaType));
					this.type = lambdaType;
					this.mode = LAMBDA_END;
					return;
				}
				case DyvilKeywords.NULL:
					this.consumer.setType(Types.NULL);
					pm.popParser();
					return;
				case DyvilSymbols.UNDERSCORE:
					this.type = new WildcardType(token.raw());
					this.mode = END;
					return;
				}

				if (ParserUtil.isIdentifier(type))
				{
					final String identifier = token.stringValue();
					switch (identifier.charAt(0))
					{
					case '_':
						if (identifier.length() == 1 || identifier.charAt(1) == '>')
						{
							// _
							// Special Case: _> as in Class<_>
							this.type = new WildcardType(new CodePosition(token.startLine(), token.startIndex(),
							                                              token.startIndex() + 1));
							this.mode = END;
							pm.splitJump(token, 1);
							return;
						}
						break; // Parse as Identifier
					case '+':
					{
						final WildcardType wildcardType = new WildcardType(Variance.COVARIANT);
						pm.pushParser(new TypeParser(wildcardType));
						this.type = wildcardType;
						this.mode = END;
						return;
					}
					case '-':
					{
						final WildcardType wildcardType = new WildcardType(Variance.CONTRAVARIANT);
						pm.pushParser(new TypeParser(wildcardType));
						this.type = wildcardType;
						this.mode = END;
						return;
					}
					case '>':
						// Special Case: > as in List<>
						pm.split(token, 1);
						pm.popParser(); // no reparse
						return;
					}
				}
			}
			if (ParserUtil.isIdentifier(type))
			{
				final Name name = token.nameValue();
				final IToken next = token.next();

				if (isGenericStart(next, next.type()))
				{
					this.type = new NamedGenericType(token.raw(), name, this.parentType);
					this.mode = GENERICS;
					return;
				}

				this.type = new NamedType(token.raw(), name, this.parentType);
				this.mode = END;
				return;
			}
			if (ParserUtil.isTerminator(type))
			{
				pm.popParser(true);
				return;
			}
			pm.report(Markers.syntaxError(token, "type.invalid", token.toString()));
			return;
		case TUPLE_END:
		{
			if (type != BaseSymbols.CLOSE_PARENTHESIS)
			{
				pm.reparse();
				pm.report(token, "type.tuple.close_paren");
			}

			final IToken nextToken = token.next();
			if (nextToken.type() == DyvilSymbols.ARROW_RIGHT)
			{
				final LambdaType lambdaType = new LambdaType(nextToken.raw(), this.parentType, (TupleType) this.type);
				this.type = lambdaType;
				this.mode = LAMBDA_END;

				pm.skip();
				pm.pushParser(new TypeParser(lambdaType));
				return;
			}

			if (this.parentType != null)
			{
				pm.report(nextToken, "type.tuple.lambda_arrow");
			}
			this.type.expandPosition(token);
			this.mode = END;
			return;
		}
		case LAMBDA_END:
			this.type.expandPosition(token.prev());
			this.consumer.setType(this.type);
			pm.popParser(true);
			return;
		case ARRAY_COLON:
			if (type == BaseSymbols.COLON)
			{
				final MapType mapType = new MapType(this.type.getElementType(), null, this.type.getMutability());
				this.type = mapType;
				this.mode = ARRAY_END;
				pm.pushParser(new TypeParser(mapType::setValueType));
				return;
			}
			if (type == DyvilSymbols.ELLIPSIS)
			{
				this.type = new ListType(this.type.getElementType(), this.type.getMutability());
				this.mode = ARRAY_END;
				return;
			}
			// Fallthrough
		case ARRAY_END:
			this.type.expandPosition(token);
			this.mode = END;
			if (type != BaseSymbols.CLOSE_SQUARE_BRACKET)
			{
				pm.reparse();
				pm.report(token, "type.array.close_bracket");
			}
			return;
		case GENERICS:
			if (isGenericStart(token, type))
			{
				pm.splitJump(token, 1);
				pm.pushParser(new TypeListParser((GenericType) this.type));
				this.mode = GENERICS_END;
				return;
			}
			return;
		case GENERICS_END:
			this.mode = END;
			if (isGenericEnd(token, type))
			{
				pm.splitJump(token, 1);
				return;
			}

			pm.reparse();
			pm.report(token, "type.generic.close_angle");
			return;
		case ANNOTATION_END:
			this.mode = END;
			pm.pushParser(new TypeParser((ITyped) this.type), true);
		}
	}

	public static boolean isGenericStart(IToken token, int type)
	{
		return type == DyvilSymbols.ARROW_LEFT
			       || ParserUtil.isIdentifier(type) && token.nameValue().unqualified.charAt(0) == '<';
	}

	public static boolean isGenericEnd(IToken token, int type)
	{
		return ParserUtil.isIdentifier(type) && token.nameValue().unqualified.charAt(0) == '>';
	}

	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
}
