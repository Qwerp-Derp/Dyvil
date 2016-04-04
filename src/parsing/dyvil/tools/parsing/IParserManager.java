package dyvil.tools.parsing;

import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.token.IToken;

public interface IParserManager
{
	TokenIterator getTokens();

	MarkerList getMarkers();

	IToken split(IToken token, int length);

	void report(IToken token, String message);

	void report(Marker error);

	void stop();

	void skip();

	void skip(int n);

	void reparse();

	void jump(IToken token);

	void setParser(Parser parser);
	
	Parser getParser();
	
	void pushParser(Parser parser);
	
	void pushParser(Parser parser, boolean reparse);
	
	void popParser();
	
	void popParser(boolean reparse);
}
