package dyvil.tools.compiler.ast.type;

import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;

public interface IRawType extends IObjectType
{
	@Override
	public default IType getConcreteType(ITypeContext context)
	{
		return this;
	}
	
	@Override
	public default boolean hasTypeVariables()
	{
		return false;
	}
	
	@Override
	public default void inferTypes(IType concrete, ITypeContext typeContext)
	{
	}
	
	@Override
	public default IType resolveType(ITypeVariable typeVar)
	{
		return null;
	}
	
	@Override
	public default String getSignature()
	{
		return null;
	}
}
