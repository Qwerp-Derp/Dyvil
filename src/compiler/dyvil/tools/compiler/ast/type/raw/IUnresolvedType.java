package dyvil.tools.compiler.ast.type.raw;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constructor.IConstructor;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;

public interface IUnresolvedType extends IRawType
{
	@Override
	default IClass getTheClass()
	{
		return Types.OBJECT_CLASS;
	}

	@Override
	default boolean isResolved() {
		return false;
	}

	@Override
	default IDataMember resolveField(Name name)
	{
		return null;
	}

	@Override
	default void getMethodMatches(MatchList<IMethod> list, IValue receiver, Name name, IArguments arguments)
	{
	}

	@Override
	default void getImplicitMatches(MatchList<IMethod> list, IValue value, IType targetType)
	{
	}

	@Override
	default void getConstructorMatches(MatchList<IConstructor> list, IArguments arguments)
	{
	}

	@Override
	default IMethod getFunctionalMethod()
	{
		return null;
	}

	// Compilation

	@Override
	default String getInternalName()
	{
		return null;
	}

	@Override
	default void appendDescriptor(StringBuilder buffer, int type)
	{
	}

	@Override
	default void writeTypeExpression(MethodWriter writer) throws BytecodeException
	{
	}
}
