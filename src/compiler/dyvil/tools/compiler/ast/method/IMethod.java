package dyvil.tools.compiler.ast.method;

import org.objectweb.asm.Label;

import dyvil.tools.compiler.ast.IASTNode;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.member.IClassCompilable;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.Parameter;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.ITypeList;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.marker.MarkerList;

public interface IMethod extends IASTNode, IMember, IBaseMethod, IMethodSignature, IContext, IClassCompilable
{
	public int getSignatureMatch(String name, IValue instance, IArguments arguments);
	
	public void checkArguments(MarkerList markers, IValue instance, IArguments arguments, ITypeContext typeContext);
	
	// Misc
	
	public void setParameters(Parameter[] parameters, int parameterCount);
	
	@Override
	public default void addType(IType type)
	{
		int index = this.parameterCount();
		this.addParameter(new Parameter(index, "par" + index, type));
	}
	
	// Generics
	
	public boolean hasTypeVariables();
	
	public IType resolveType(String name, IValue instance, IArguments arguments, ITypeList generics);
	
	// Compilation
	
	public boolean isIntrinsic();
	
	public String getDescriptor();
	
	public String getSignature();
	
	public String[] getExceptions();
	
	public void writeCall(MethodWriter writer, IValue instance, IArguments arguments, IType type);
	
	public void writeJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments);
	
	public void writeInvJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments);
}
