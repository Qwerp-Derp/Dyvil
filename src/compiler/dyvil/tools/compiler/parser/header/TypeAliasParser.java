package dyvil.tools.compiler.parser.header;

import dyvil.tools.compiler.ast.type.alias.ITypeAlias;
import dyvil.tools.compiler.ast.type.alias.ITypeAliasMap;
import dyvil.tools.compiler.ast.type.alias.TypeAlias;
import dyvil.tools.compiler.parser.ParserUtil;
import dyvil.tools.compiler.parser.type.TypeParameterListParser;
import dyvil.tools.compiler.parser.type.TypeParser;
import dyvil.tools.compiler.transform.DyvilKeywords;
import dyvil.tools.parsing.IParserManager;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.Parser;
import dyvil.tools.parsing.lexer.BaseSymbols;
import dyvil.tools.parsing.token.IToken;

public class TypeAliasParser extends Parser
{
	private static final int TYPE                      = 0;
	private static final int NAME                      = 1;
	private static final int TYPE_PARAMETERS           = 1 << 1;
	private static final int TYPE_PARAMETERS_END       = 1 << 2;
	private static final int EQUAL                     = 1 << 3;

	protected ITypeAliasMap map;
	protected ITypeAlias    typeAlias;

	public TypeAliasParser(ITypeAliasMap map)
	{
		this.map = map;
		// this.mode = TYPE;
	}

	public TypeAliasParser(ITypeAliasMap map, ITypeAlias typeAlias)
	{
		this.map = map;
		this.typeAlias = typeAlias;
		this.mode = NAME;
	}

	@Override
	public void parse(IParserManager pm, IToken token)
	{
		int type = token.type();
		switch (this.mode)
		{
		case END:
			this.map.addTypeAlias(this.typeAlias);
			pm.popParser(true);
			return;
		case TYPE:
			this.mode = NAME;

			if (type != DyvilKeywords.TYPE)
			{
				pm.reparse();
				pm.report(token, "typealias.type");
			}
			return;
		case NAME:
			if (ParserUtil.isIdentifier(type))
			{
				Name name = token.nameValue();
				this.typeAlias = new TypeAlias(name, token.raw());
				this.mode = TYPE_PARAMETERS;
				return;
			}

			pm.popParser();
			pm.report(token, "typealias.identifier");
			return;
		case TYPE_PARAMETERS:
			if (TypeParser.isGenericStart(token, type))
			{
				this.typeAlias.setTypeParametric();
				this.mode = TYPE_PARAMETERS_END;
				pm.splitJump(token, 1);
				pm.pushParser(new TypeParameterListParser(this.typeAlias));
				return;
			}
			// Fallthrough
		case EQUAL:
			this.mode = END;
			pm.pushParser(new TypeParser(this.typeAlias));

			if (type != BaseSymbols.EQUALS)
			{
				pm.reparse();
				pm.report(token, "typealias.equals");
			}
			return;
		case TYPE_PARAMETERS_END:
			this.mode = EQUAL;
			if (TypeParser.isGenericEnd(token, type))
			{
				pm.splitJump(token, 1);
				return;
			}

			pm.reparse();
			pm.report(token, "generic.close_angle");
		}
	}

	@Override
	public boolean reportErrors()
	{
		return this.mode > NAME;
	}
}
