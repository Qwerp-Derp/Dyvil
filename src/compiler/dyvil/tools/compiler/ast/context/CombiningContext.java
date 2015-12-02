package dyvil.tools.compiler.ast.context;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IAccessible;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.IVariable;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.method.ConstructorMatchList;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.structure.IDyvilHeader;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.parsing.Name;

public class CombiningContext implements IContext
{
	private final IContext inner;
	private final IContext outer;
	
	public CombiningContext(IContext context1, IContext context2)
	{
		this.inner = context1;
		this.outer = context2;
	}
	
	@Override
	public boolean isStatic()
	{
		return this.inner.isStatic() && this.outer.isStatic();
	}
	
	@Override
	public IDyvilHeader getHeader()
	{
		IDyvilHeader header = this.inner.getHeader();
		return header == null ? this.outer.getHeader() : header;
	}
	
	@Override
	public IClass getThisClass()
	{
		IClass iclass = this.inner.getThisClass();
		return iclass == null ? this.outer.getThisClass() : iclass;
	}
	
	@Override
	public Package resolvePackage(Name name)
	{
		Package pack = this.inner.resolvePackage(name);
		return pack == null ? this.outer.resolvePackage(name) : pack;
	}
	
	@Override
	public IClass resolveClass(Name name)
	{
		IClass iclass = this.inner.resolveClass(name);
		return iclass == null ? this.outer.resolveClass(name) : iclass;
	}
	
	@Override
	public IType resolveType(Name name)
	{
		IType type = this.inner.resolveType(name);
		return type == null ? this.outer.resolveType(name) : type;
	}
	
	@Override
	public ITypeVariable resolveTypeVariable(Name name)
	{
		ITypeVariable typeVar = this.inner.resolveTypeVariable(name);
		return typeVar == null ? this.outer.resolveTypeVariable(name) : typeVar;
	}
	
	@Override
	public IDataMember resolveField(Name name)
	{
		IDataMember field = this.inner.resolveField(name);
		return field == null ? this.outer.resolveField(name) : field;
	}
	
	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		this.inner.getMethodMatches(list, instance, name, arguments);
		this.outer.getMethodMatches(list, instance, name, arguments);
	}
	
	@Override
	public void getConstructorMatches(ConstructorMatchList list, IArguments arguments)
	{
		this.inner.getConstructorMatches(list, arguments);
		this.outer.getConstructorMatches(list, arguments);
	}
	
	@Override
	public boolean handleException(IType type)
	{
		return this.inner.handleException(type) || this.outer.handleException(type);
	}
	
	@Override
	public IAccessible getAccessibleThis(IClass type)
	{
		IAccessible i = this.inner.getAccessibleThis(type);
		return i == null ? this.outer.getAccessibleThis(type) : i;
	}
	
	@Override
	public IAccessible getAccessibleImplicit()
	{
		IAccessible i = this.inner.getAccessibleImplicit();
		return i == null ? this.outer.getAccessibleImplicit() : i;
	}
	
	@Override
	public boolean isMember(IVariable variable)
	{
		return this.inner.isMember(variable);
	}
	
	@Override
	public IDataMember capture(IVariable variable)
	{
		if (this.inner.isMember(variable))
		{
			return variable;
		}
		if (this.outer.isMember(variable))
		{
			return this.inner.capture(variable);
		}
		
		IDataMember dm = this.outer.capture(variable);
		return dm.capture(this.inner);
	}
}
