package dyvil.tools.repl;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.lexer.Dlex;
import dyvil.tools.compiler.lexer.TokenIterator;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.MarkerList;
import dyvil.tools.compiler.library.Library;
import dyvil.tools.compiler.parser.classes.ClassBodyParser;
import dyvil.tools.compiler.parser.classes.DyvilUnitParser;
import dyvil.tools.compiler.parser.expression.ExpressionParser;

public class DyvilREPL
{
	public static final String VERSION = "1.0.0";
	
	private static BufferedReader	reader;
	protected static REPLContext	context	= new REPLContext();
	protected static REPLParser		parser	= new REPLParser();
	protected static String			currentCode;
	
	public static void main(String[] args) throws Exception
	{
		System.err.println("Dyvil REPL " + VERSION);
		
		Library.javaLibrary.loadLibrary();
		Library.dyvilLibrary.loadLibrary();
		if (Library.dyvilBinLibrary != null)
		{
			Library.dyvilBinLibrary.loadLibrary();
		}
		
		Package.init();
		Types.init();
		
		reader = new BufferedReader(new InputStreamReader(System.in));
		
		do
		{
			loop();
		}
		while (currentCode != null);
	}
	
	public static synchronized void loop()
	{
		System.out.print("> ");
		
		try
		{
			currentCode = reader.readLine();
			TokenIterator tokens = Dlex.tokenIterator(currentCode + ";");
			REPLContext.newClassName();
			
			if (parser.parse(null, tokens, new DyvilUnitParser(context)))
			{
				return;
			}
			if (parser.parse(null, tokens, new ClassBodyParser(context)))
			{
				return;
			}
			
			MarkerList markers = new MarkerList();
			parser.parse(markers, tokens, new ExpressionParser(context));
			
			StringBuilder buf = new StringBuilder();
			for (Marker m : markers)
			{
				m.log(currentCode, buf);
			}
			System.out.println(buf.toString());
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}
}
