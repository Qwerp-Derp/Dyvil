package dyvil.tools.compiler.parser.header;

import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.header.HeaderDeclaration;
import dyvil.tools.compiler.ast.header.ImportDeclaration;
import dyvil.tools.compiler.ast.header.IncludeDeclaration;
import dyvil.tools.compiler.ast.header.PackageDeclaration;
import dyvil.tools.compiler.ast.modifiers.Modifier;
import dyvil.tools.compiler.ast.modifiers.ModifierList;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.modifiers.ModifierUtil;
import dyvil.tools.compiler.ast.operator.Operator;
import dyvil.tools.compiler.ast.structure.IDyvilHeader;
import dyvil.tools.compiler.ast.type.alias.TypeAlias;
import dyvil.tools.compiler.parser.ParserUtil;
import dyvil.tools.compiler.parser.annotation.AnnotationParser;
import dyvil.tools.compiler.transform.DyvilKeywords;
import dyvil.tools.compiler.transform.DyvilSymbols;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.IParserManager;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.Parser;
import dyvil.tools.parsing.lexer.BaseSymbols;
import dyvil.tools.parsing.lexer.Tokens;
import dyvil.tools.parsing.position.ICodePosition;
import dyvil.tools.parsing.token.IToken;

public class DyvilHeaderParser extends Parser
{
	protected static final int PACKAGE  = 1;
	protected static final int IMPORT   = 2;
	protected static final int METADATA = 4;

	protected IDyvilHeader unit;

	protected ModifierSet    modifiers;
	protected AnnotationList annotations;

	protected IToken lastToken;

	public DyvilHeaderParser(IDyvilHeader unit)
	{
		this.unit = unit;
		this.mode = PACKAGE;
	}

	protected boolean parsePackage(IParserManager pm, IToken token, int type)
	{
		if (type == DyvilKeywords.PACKAGE)
		{
			PackageDeclaration pack = new PackageDeclaration(token.raw());
			this.unit.setPackageDeclaration(pack);
			pm.pushParser(new PackageParser(pack));
			return true;
		}
		return false;
	}

	protected boolean parseImport(IParserManager pm, IToken token, int type)
	{
		switch (type)
		{
		case DyvilKeywords.IMPORT:
		{
			final IToken next = token.next();
			if (next.type() == DyvilKeywords.STATIC)
			{
				pm.report(Markers.syntaxWarning(next, "import.using.static"));
				pm.skip();
				this.createUsingDeclaration(pm, token.to(next));
				return true;
			}

			ImportDeclaration i = new ImportDeclaration(token.raw());
			pm.pushParser(new ImportParser(im -> {
				i.setImport(im);
				this.unit.addImport(i);
			}));
			return true;
		}
		case DyvilKeywords.USING:
		{
			this.createUsingDeclaration(pm, token);
			return true;
		}
		case DyvilKeywords.OPERATOR:
			pm.pushParser(new OperatorParser(this.unit, Operator.INFIX), true);
			return true;
		case DyvilKeywords.PREFIX:
		case DyvilKeywords.POSTFIX:
		case DyvilKeywords.INFIX:
			if (token.next().type() == DyvilKeywords.OPERATOR)
			{
				final OperatorParser operatorParser = new OperatorParser(this.unit);
				// Parse this token so the OperatorParser correctly detects the type (prefix, postfix, infix)
				operatorParser.parse(pm, token);
				pm.pushParser(operatorParser);
				return true;
			}

			return false;
		case DyvilKeywords.INCLUDE:
		{
			IncludeDeclaration i = new IncludeDeclaration(token.raw());
			pm.pushParser(new IncludeParser(this.unit, i));
			return true;
		}
		case DyvilKeywords.TYPE:
		{
			TypeAlias typeAlias = new TypeAlias();
			pm.pushParser(new TypeAliasParser(this.unit, typeAlias));
			return true;
		}
		}
		return false;
	}

	private void createUsingDeclaration(IParserManager pm, ICodePosition position)
	{
		ImportDeclaration i = new ImportDeclaration(position, true);
		pm.pushParser(new ImportParser(im -> {
			i.setImport(im);
			this.unit.addUsing(i);
		}));
	}

	protected boolean parseMetadata(IParserManager pm, IToken token, int type)
	{
		Modifier modifier;
		if ((modifier = ModifierUtil.parseModifier(token, pm)) != null)
		{
			if (this.modifiers == null)
			{
				this.modifiers = new ModifierList();
			}

			this.modifiers.addModifier(modifier);
			return true;
		}
		if (type == DyvilSymbols.AT && token.next().type() != DyvilKeywords.INTERFACE)
		{
			this.parseAnnotation(pm, token);
			return true;
		}
		if (type == DyvilKeywords.HEADER)
		{
			IToken next = token.next();
			if (ParserUtil.isIdentifier(next.type()))
			{
				pm.skip();
				if (this.unit.getHeaderDeclaration() != null)
				{
					pm.report(token, "header.declaration.duplicate");
					return true;
				}

				Name name = next.nameValue();
				this.unit.setHeaderDeclaration(
					new HeaderDeclaration(this.unit, next.raw(), name, this.modifiers, this.annotations));
				this.modifiers = null;
				this.annotations = null;
				this.lastToken = null;
				this.mode = IMPORT;
				return true;
			}
		}
		return false;
	}

	@Override
	public void parse(IParserManager pm, IToken token)
	{
		final int type = token.type();
		switch (type)
		{
		case Tokens.EOF:
			if (hasModifiers(this.modifiers, this.annotations))
			{
				pm.report(token, "header.element");
			}
			pm.popParser();
			// Fallthrough
		case BaseSymbols.SEMICOLON:
			return;
		}

		switch (this.mode)
		{
		case PACKAGE:
			if (this.parsePackage(pm, token, type))
			{
				this.mode = IMPORT;
				return;
			}
			// Fallthrough
		case IMPORT:
			this.mode = IMPORT;
			if (this.parseImport(pm, token, type))
			{
				return;
			}
			// Fallthrough
		case METADATA:
			if (this.mode != METADATA)
			{
				this.lastToken = token;
				this.mode = METADATA;
			}
			if (this.parseMetadata(pm, token, type))
			{
				return;
			}
		}

		reportInvalidElement(pm, token);
	}

	public static boolean hasModifiers(ModifierSet modifiers, AnnotationList annotations)
	{
		return modifiers != null && !modifiers.isEmpty() || annotations != null && annotations.annotationCount() != 0;
	}

	protected static void reportInvalidElement(IParserManager pm, IToken token)
	{
		pm.report(Markers.syntaxError(token, "header.element.invalid", token.toString()));
	}

	private void parseAnnotation(IParserManager pm, IToken token)
	{
		if (this.annotations == null)
		{
			this.annotations = new AnnotationList();
		}

		final Annotation annotation = new Annotation(token.raw());
		this.annotations.addAnnotation(annotation);
		pm.pushParser(new AnnotationParser(annotation));
	}

	@Override
	public boolean reportErrors()
	{
		return this.mode > PACKAGE && this.lastToken == null;
	}
}
