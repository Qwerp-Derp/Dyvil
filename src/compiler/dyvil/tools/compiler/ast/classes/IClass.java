package dyvil.tools.compiler.ast.classes;

import dyvil.collection.Collection;
import dyvil.collection.Set;
import dyvil.reflect.Modifiers;
import dyvil.tools.compiler.ast.annotation.AnnotationMetadata;
import dyvil.tools.compiler.ast.classes.metadata.*;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.generic.ITypeParametric;
import dyvil.tools.compiler.ast.header.IClassCompilable;
import dyvil.tools.compiler.ast.header.IClassCompilableList;
import dyvil.tools.compiler.ast.header.ICompilable;
import dyvil.tools.compiler.ast.header.IHeaderUnit;
import dyvil.tools.compiler.ast.member.IClassMember;
import dyvil.tools.compiler.ast.member.MemberKind;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.IParametric;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

public interface IClass extends IClassMember, ICompilable, ITypeParametric, IContext, IParametric, IClassCompilableList
{
	@Override
	default MemberKind getKind()
	{
		return MemberKind.CLASS;
	}

	void setHeader(IHeaderUnit unit);

	@Override
	IHeaderUnit getHeader();

	@Override
	void setEnclosingClass(IClass enclosingClass);

	@Override
	IClass getEnclosingClass();

	// Modifiers

	default boolean isAnonymous()
	{
		return false;
	}

	boolean isAbstract();

	boolean isInterface();

	boolean isAnnotation();

	boolean isObject();

	// Full Name

	void setFullName(String name);

	String getFullName();

	// Super Types

	@Override
	@Deprecated
	@dyvil.annotation.Deprecated(replacements = { "getThisType", "getReceiverType", "getClassType" })
	default IType getType()
	{
		return this.getThisType();
	}

	@Override
	@Deprecated
	default void setType(IType type)
	{
	}

	default IType getReceiverType()
	{
		return this.getThisType().asParameterType();
	}

	IType getClassType();

	void setSuperType(IType type);

	IType getSuperType();

	boolean isSubClassOf(IType type);

	default IArguments getSuperConstructorArguments()
	{
		return null;
	}

	default void setSuperConstructorArguments(IArguments arguments)
	{
	}

	// Interfaces

	int interfaceCount();

	void setInterface(int index, IType type);

	void addInterface(IType type);

	IType getInterface(int index);

	// Generics

	IType resolveType(ITypeParameter typeVar, IType concrete);

	// Body

	void setBody(IClassBody body);

	IClassBody getBody();

	void setMetadata(IClassMetadata metadata);

	IClassMetadata getMetadata();

	IMethod getFunctionalMethod();

	IDataMember getSuperField(Name name);

	boolean isMember(IClassMember member);

	byte getVisibility(IClassMember member);

	Collection<IMethod> getMethods(Name name);

	boolean checkImplements(IMethod candidate, ITypeContext typeContext);

	void checkMethods(MarkerList markers, IClass checkedClass, ITypeContext typeContext, Set<IClass> checkedClasses);

	// Other Compilables (Lambda Expressions, ...)

	@Override
	int classCompilableCount();

	@Override
	void addClassCompilable(IClassCompilable compilable);

	// Compilation

	@Override
	String getInternalName();

	String getSignature();

	String[] getInterfaceArray();

	@Override
	void write(ClassWriter writer) throws BytecodeException;

	@Override
	void writeClassInit(MethodWriter writer) throws BytecodeException;

	@Override
	void writeStaticInit(MethodWriter writer) throws BytecodeException;

	void writeInnerClassInfo(ClassWriter writer);

	static IClassMetadata getClassMetadata(IClass iclass, int modifiers)
	{
		if ((modifiers & Modifiers.OBJECT_CLASS) != 0)
		{
			return new ObjectClassMetadata(iclass);
		}
		if ((modifiers & Modifiers.CASE_CLASS) != 0)
		{
			return new CaseClassMetadata(iclass);
		}
		if ((modifiers & Modifiers.ANNOTATION) == Modifiers.ANNOTATION)
		{
			return new AnnotationMetadata(iclass);
		}
		final int interfaceModifiers = modifiers & Modifiers.TRAIT_CLASS;
		if (interfaceModifiers == Modifiers.TRAIT_CLASS)
		{
			return new TraitMetadata(iclass);
		}
		else if (interfaceModifiers == Modifiers.INTERFACE_CLASS)
		{
			return new InterfaceMetadata(iclass);
		}
		return new ClassMetadata(iclass);
	}
}
