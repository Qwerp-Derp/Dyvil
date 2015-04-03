package dyvil.tools.compiler.ast.generic;

import dyvil.tools.compiler.ast.IASTNode;
import dyvil.tools.compiler.ast.member.INamed;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.lexer.marker.MarkerList;

public interface ITypeVariable extends IASTNode, INamed, IBounded
{
	public IGeneric getGeneric();
	
	// Super Types
	
	public boolean isSuperTypeOf(IType type);
	
	// Resolve Types
	
	public void resolveTypes(MarkerList markers, IContext context);
	
	// Compilation
	
	public void appendSignature(StringBuilder buffer);
}
