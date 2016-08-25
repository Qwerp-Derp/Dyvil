package dyvil.tools.compiler.ast.context;

import dyvil.tools.compiler.DyvilCompiler;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.header.IHeaderUnit;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.alias.ITypeAlias;
import dyvil.tools.parsing.Name;

public interface IDefaultContext extends IStaticContext
{
	IDefaultContext DEFAULT = new IDefaultContext() {};

	@Override
	default byte checkStatic()
	{
		return PASS;
	}

	@Override
	default DyvilCompiler getCompilationContext()
	{
		return null;
	}

	@Override
	default IHeaderUnit getHeader()
	{
		return null;
	}

	@Override
	default Package resolvePackage(Name name)
	{
		return null;
	}

	@Override
	default IHeaderUnit resolveHeader(Name name)
	{
		return null;
	}

	@Override
	default IClass resolveClass(Name name)
	{
		return null;
	}

	@Override
	default ITypeAlias resolveTypeAlias(Name name, int arity)
	{
		return null;
	}

	@Override
	default IDataMember resolveField(Name name)
	{
		return null;
	}

	@Override
	default IType getReturnType()
	{
		return null;
	}

	@Override
	default byte checkException(IType type)
	{
		return PASS;
	}

	@Override
	default void getMethodMatches(MatchList<IMethod> list, IValue receiver, Name name, IArguments arguments)
	{
	}

	@Override
	default void getImplicitMatches(MatchList<IMethod> list, IValue value, IType targetType)
	{
	}
}
