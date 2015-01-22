package dyvil.tools.compiler.ast.method;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.ValueList;
import dyvil.tools.compiler.ast.field.FieldMatch;
import dyvil.tools.compiler.ast.field.Parameter;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.member.Member;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.ITyped;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IntValue;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.SemanticError;
import dyvil.tools.compiler.transform.Symbols;
import dyvil.tools.compiler.util.Modifiers;
import dyvil.tools.compiler.util.OpcodeUtil;
import dyvil.tools.compiler.util.Util;

public class Method extends Member implements IMethod
{
	private List<ITypeVariable>	generics;
	
	private List<Parameter>		parameters			= new ArrayList(3);
	private List<IType>			throwsDeclarations	= new ArrayList(1);
	
	private IValue				value;
	
	protected boolean			isConstructor;
	
	protected IMethod			overrideMethod;
	protected int[]				prefixBytecode;
	protected int[]				infixBytecode;
	protected int[]				postfixBytecode;
	
	public Method(IClass iclass)
	{
		super(iclass);
	}
	
	public Method(IClass iclass, String name)
	{
		super(iclass, name);
		if (name.equals("new"))
		{
			this.isConstructor = true;
		}
	}
	
	public Method(IClass iclass, String name, IType type)
	{
		super(iclass, name, type);
		if (name.equals("new"))
		{
			this.isConstructor = true;
		}
	}
	
	public Method(IClass iclass, String name, IType type, int modifiers, List<Annotation> annotations)
	{
		super(iclass, name, type, modifiers, annotations);
		if (name.equals("new"))
		{
			this.isConstructor = true;
			this.qualifiedName = "<init>";
		}
	}
	
	@Override
	public void setName(String name)
	{
		if (name.equals("new"))
		{
			this.qualifiedName = "<init>";
			this.name = "new";
			this.isConstructor = true;
		}
		else
		{
			this.qualifiedName = Symbols.qualify(name);
			this.name = name;
		}
	}
	
	@Override
	public void setQualifiedName(String name)
	{
		if (name.equals("<init>"))
		{
			this.qualifiedName = "<init>";
			this.name = "new";
			this.isConstructor = true;
		}
		else
		{
			this.qualifiedName = name;
			this.name = Symbols.unqualify(name);
		}
	}
	
	@Override
	public void setGeneric()
	{
		this.generics = new ArrayList(2);
	}
	
	@Override
	public boolean isGeneric()
	{
		return this.generics != null;
	}
	
	@Override
	public void addType(IType type)
	{
		this.generics.add((ITypeVariable) type);
	}
	
	@Override
	public void setVarargs()
	{
		this.modifiers |= Modifiers.VARARGS;
	}
	
	@Override
	public boolean isVarargs()
	{
		return (this.modifiers & Modifiers.VARARGS) != 0;
	}
	
	@Override
	public void setParameters(List<Parameter> parameters)
	{
		this.parameters = parameters;
	}
	
	@Override
	public List<Parameter> getParameters()
	{
		return this.parameters;
	}
	
	@Override
	public void addParameter(Parameter parameter)
	{
		this.parameters.add(parameter);
	}
	
	@Override
	public int getVariableCount()
	{
		return this.parameters.size();
	}
	
	@Override
	public void addAnnotation(Annotation annotation)
	{
		if (!this.processAnnotation(annotation))
		{
			annotation.target = ElementType.METHOD;
			this.annotations.add(annotation);
		}
	}
	
	private boolean processAnnotation(Annotation annotation)
	{
		String name = annotation.type.fullName;
		if ("dyvil.lang.annotation.inline".equals(name))
		{
			this.modifiers |= Modifiers.INLINE;
			return true;
		}
		if ("dyvil.lang.annotation.implicit".equals(name))
		{
			this.modifiers |= Modifiers.IMPLICIT;
			return true;
		}
		if ("dyvil.lang.annotation.prefix".equals(name))
		{
			this.modifiers |= Modifiers.PREFIX;
			return true;
		}
		if ("dyvil.lang.annotation.sealed".equals(name))
		{
			this.modifiers |= Modifiers.SEALED;
			return true;
		}
		if ("dyvil.lang.annotation.Bytecode".equals(name))
		{
			this.prefixBytecode = readOpcodes(annotation, "prefixOpcode", "prefixOpcodes");
			this.infixBytecode = readOpcodes(annotation, "infixOpcode", "infixOpcodes");
			this.postfixBytecode = readOpcodes(annotation, "postfixOpcode", "postfixOpcodes");
			return false;
		}
		if ("java.lang.Override".equals(name))
		{
			this.modifiers |= Modifiers.OVERRIDE;
			return true;
		}
		return false;
	}
	
	@Override
	public void checkArguments(List<Marker> markers, IValue instance, List<IValue> arguments)
	{
		int pOff = 0;
		int len = arguments.size();
		List<Parameter> params = this.parameters;
		Parameter par;
		IType parType;
		
		if (instance != null && (this.modifiers & Modifiers.IMPLICIT) == Modifiers.IMPLICIT)
		{
			par = params.get(0);
			parType = par.type;
			if (!instance.requireType(parType))
			{
				SemanticError error = new SemanticError(instance.getPosition(), "The implicit method argument for '" + par.name + "' is incompatible with the required type");
				error.addInfo("Required Type: " + parType);
				IType vtype = instance.getType();
				error.addInfo("Value Type: " + (vtype == null ? "unknown" : vtype));
				markers.add(error);
			}
			pOff = 1;
		}
		
		for (int i = 0; i < len; i++)
		{
			par = params.get(i + pOff);
			parType = par.type;
			IValue value = arguments.get(i);
			if (!value.requireType(parType))
			{
				SemanticError error = new SemanticError(value.getPosition(), "The method argument for '" + par.name + "' is incompatible with the required type");
				error.addInfo("Required Type: " + parType);
				IType vtype = value.getType();
				error.addInfo("Value Type: " + (vtype == null ? "unknown" : vtype));
				markers.add(error);
			}
		}
	}
	
	@Override
	public int getSignatureMatch(String name, ITyped instance, List<? extends ITyped> arguments)
	{
		if (name == null)
		{
			return 1;
		}
		
		if (!name.equals(this.qualifiedName))
		{
			return 0;
		}
		
		// Only matching the name
		if (arguments == null)
		{
			return 1;
		}
		
		int pOff = 0;
		int match = 1;
		int len = arguments.size();
		List<Parameter> params = this.parameters;
		
		// implicit modifier implementation
		if (instance != null && (this.modifiers & Modifiers.IMPLICIT) == Modifiers.IMPLICIT)
		{
			if (len != params.size() - 1)
			{
				return 0;
			}
			
			IType t2 = params.get(0).type;
			int m = instance.getTypeMatch(t2);
			if (m == 0)
			{
				return 0;
			}
			match += m;
			
			pOff = 1;
		}
		else if (len != this.parameters.size())
		{
			return 0;
		}
		
		for (int i = 0; i < len; i++)
		{
			IType t1 = params.get(i + pOff).type;
			ITyped typed = arguments.get(i);
			int m = typed.getTypeMatch(t1);
			if (m == 0)
			{
				return 0;
			}
			match += m;
		}
		
		return match;
	}
	
	@Override
	public void setThrows(List<IType> throwsDecls)
	{
		this.throwsDeclarations = throwsDecls;
	}
	
	@Override
	public List<IType> getThrows()
	{
		return this.throwsDeclarations;
	}
	
	@Override
	public void addThrows(IType throwsDecl)
	{
		this.throwsDeclarations.add(throwsDecl);
	}
	
	@Override
	public void setValue(IValue statement)
	{
		this.value = statement;
	}
	
	@Override
	public IValue getValue()
	{
		return this.value;
	}
	
	@Override
	public String getDescriptor()
	{
		StringBuilder buf = new StringBuilder();
		buf.append('(');
		for (Parameter par : this.parameters)
		{
			buf.append(par.type.getExtendedName());
		}
		buf.append(')');
		buf.append(this.isConstructor ? "V" : this.type.getExtendedName());
		return buf.toString();
	}
	
	@Override
	public String getSignature()
	{
		// TODO Generic Signature
		return null;
	}
	
	@Override
	public String[] getExceptions()
	{
		int len = this.throwsDeclarations.size();
		if (len == 0)
		{
			return null;
		}
		
		String[] array = new String[len];
		for (int i = 0; i < len; i++)
		{
			array[i] = this.throwsDeclarations.get(i).getInternalName();
		}
		return array;
	}
	
	@Override
	public void resolveTypes(List<Marker> markers, IContext context)
	{
		this.type = this.type.resolve(context);
		if (!this.type.isResolved())
		{
			markers.add(new SemanticError(this.type.getPosition(), "'" + this.type + "' could not be resolved to a type"));
		}
		
		int len = this.throwsDeclarations.size();
		for (int i = 0; i < len; i++)
		{
			IType t1 = this.throwsDeclarations.get(i);
			IType t2 = t1.resolve(context);
			if (t1 != t2)
			{
				this.throwsDeclarations.set(i, t2);
			}
			if (!t2.isResolved())
			{
				markers.add(new SemanticError(t2.getPosition(), "'" + t2 + "' could not be resolved to a type"));
			}
		}
		
		for (Annotation a : this.annotations)
		{
			a.resolveTypes(markers, context);
		}
		
		if (this.generics != null)
		{
			for (ITypeVariable v : this.generics)
			{
				v.resolveTypes(markers, context);
			}
		}
		
		for (Parameter p : this.parameters)
		{
			p.resolveTypes(markers, context);
		}
		
		if (this.value != null)
		{
			this.value.resolveTypes(markers, this);
		}
	}
	
	@Override
	public void resolve(List<Marker> markers, IContext context)
	{
		if ((this.modifiers & Modifiers.STATIC) == 0)
		{
			IType t = this.theClass.getSuperType();
			if (t != null)
			{
				IClass iclass = t.getTheClass();
				if (iclass != null)
				{
					this.overrideMethod = iclass.getBody().getMethod(this.name, this.parameters);
				}
			}
		}
		
		Iterator<Annotation> iterator = this.annotations.iterator();
		while (iterator.hasNext())
		{
			Annotation a = iterator.next();
			if (this.processAnnotation(a))
			{
				iterator.remove();
				continue;
			}
			a.resolve(markers, context);
		}
		
		int index = this.hasModifier(Modifiers.STATIC) ? 0 : 1;
		
		for (Parameter p : this.parameters)
		{
			p.index = index++;
			p.resolve(markers, context);
		}
		
		if (this.value != null)
		{
			this.value = this.value.resolve(markers, this);
		}
	}
	
	@Override
	public void check(List<Marker> markers, IContext context)
	{
		if ((this.modifiers & Modifiers.STATIC) == 0)
		{
			if (this.overrideMethod == null)
			{
				if ((this.modifiers & Modifiers.OVERRIDE) != 0)
				{
					markers.add(new SemanticError(this.position, "The method '" + this.name + "' must override or implement a supertype method"));
				}
			}
			else if (!this.isConstructor)
			{
				if ((this.modifiers & Modifiers.OVERRIDE) == 0)
				{
					markers.add(new SemanticError(this.position, "The method '" + this.name + "' overrides a method, but does not have an 'override' modifier"));
				}
				else if (this.overrideMethod.hasModifier(Modifiers.FINAL))
				{
					markers.add(new SemanticError(this.position, "The method '" + this.name + "' cannot override a final method"));
				}
				else
				{
					IType type = this.overrideMethod.getType();
					if (!Type.isSuperType(type, this.type))
					{
						SemanticError error = new SemanticError(this.position, "The return type of '" + this.name + "' is incompatible with the overriden method type");
						error.addInfo("Return Type: " + this.type);
						error.addInfo("Overriden Type: " + type);
						markers.add(error);
					}
				}
			}
		}
		
		for (Annotation a : this.annotations)
		{
			a.check(markers, context);
		}
		
		for (Parameter p : this.parameters)
		{
			p.check(markers, context);
		}
		
		if (this.value != null)
		{
			if (this.isConstructor)
			{
				if (!this.value.requireType(Type.VOID))
				{
					SemanticError error = new SemanticError(this.position, "The constructor must not return a result");
					error.addInfo("Expression Type: " + this.value.getType());
					markers.add(error);
				}
			}
			else if (!this.value.requireType(this.type))
			{
				SemanticError error = new SemanticError(this.position, "The expression type of '" + this.name + "' is incompatible with the return type");
				error.addInfo("Return Type: " + this.type);
				IType vtype = this.value.getType();
				error.addInfo("Value Type: " + (vtype == null ? "unknown" : vtype));
				markers.add(error);
			}
			this.value.check(markers, context);
		}
		// If the method does not have an implementation and is static
		else if (this.isStatic())
		{
			markers.add(new SemanticError(this.position, "The method '" + this.name + "' is declared static, but does not have an implementation"));
		}
		// Or not declared abstract and a member of a non-abstract class
		else if ((this.modifiers & Modifiers.ABSTRACT) == 0)
		{
			if (this.theClass.isAbstract())
			{
				this.modifiers |= Modifiers.ABSTRACT;
			}
			else
			{
				markers.add(new SemanticError(this.position, "The method '" + this.name + "' is not implemented, but does not have an abstract modifier"));
			}
		}
	}
	
	@Override
	public void foldConstants()
	{
		for (Annotation a : this.annotations)
		{
			a.foldConstants();
		}
		
		for (Parameter p : this.parameters)
		{
			p.foldConstants();
		}
		
		if (this.value != null)
		{
			this.value = this.value.foldConstants();
		}
	}
	
	private static int[] readOpcodes(Annotation annotation, String key1, String key2)
	{
		int[] opcodes = null;
		ValueList array = (ValueList) annotation.getValue(key2);
		if (array != null)
		{
			List<IValue> values = array.values;
			int len = values.size();
			opcodes = new int[len];
			for (int i = 0; i < len; i++)
			{
				IntValue v = (IntValue) values.get(i).foldConstants();
				opcodes[i] = v.value;
			}
		}
		else
		{
			IValue i = annotation.getValue(key1);
			if (i != null)
			{
				IntValue v = (IntValue) i.foldConstants();
				opcodes = new int[] { v.value };
			}
		}
		return opcodes;
	}
	
	@Override
	public boolean isStatic()
	{
		return (this.modifiers & Modifiers.STATIC) != 0;
	}
	
	@Override
	public IType getThisType()
	{
		return this.theClass.getThisType();
	}
	
	@Override
	public Package resolvePackage(String name)
	{
		return this.theClass.resolvePackage(name);
	}
	
	@Override
	public IClass resolveClass(String name)
	{
		if (this.generics != null)
		{
			for (ITypeVariable var : this.generics)
			{
				if (var.isName(name))
				{
					return var.getTheClass();
				}
			}
		}
		
		return this.theClass.resolveClass(name);
	}
	
	@Override
	public FieldMatch resolveField(String name)
	{
		for (Parameter param : this.parameters)
		{
			if (param.isName(name))
			{
				return new FieldMatch(param, 1);
			}
		}
		
		return this.theClass.resolveField(name);
	}
	
	@Override
	public MethodMatch resolveMethod(ITyped instance, String name, List<? extends ITyped> arguments)
	{
		return this.theClass.resolveMethod(instance, name, arguments);
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, ITyped instance, String name, List<? extends ITyped> arguments)
	{
		this.theClass.getMethodMatches(list, instance, name, arguments);
	}
	
	@Override
	public byte getAccessibility(IMember member)
	{
		IClass iclass = member.getTheClass();
		if (iclass == null)
		{
			return READ_WRITE_ACCESS;
		}
		if ((this.modifiers & Modifiers.STATIC) != 0 && iclass == this.theClass && !member.hasModifier(Modifiers.STATIC))
		{
			return STATIC;
		}
		return this.theClass.getAccessibility(member);
	}
	
	@Override
	public void writePrefixBytecode(MethodWriter writer)
	{
		if (this.prefixBytecode != null)
		{
			for (int i : this.prefixBytecode)
			{
				writer.visitInsn(i);
			}
		}
	}
	
	@Override
	public void writeInfixBytecode(MethodWriter writer)
	{
		if (this.infixBytecode != null)
		{
			for (int i : this.infixBytecode)
			{
				writer.visitInsn(i);
			}
		}
	}
	
	@Override
	public boolean writePostfixBytecode(MethodWriter writer)
	{
		if (this.postfixBytecode != null)
		{
			for (int i : this.postfixBytecode)
			{
				writer.visitInsn(i);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void writePrefixBytecode(MethodWriter writer, Label dest)
	{
		if (this.prefixBytecode != null)
		{
			for (int i : this.prefixBytecode)
			{
				if (OpcodeUtil.isJumpOpcode(i))
				{
					writer.visitJumpInsn(i, dest);
				}
				else
				{
					writer.visitInsn(i);
				}
			}
		}
	}
	
	@Override
	public void writeInfixBytecode(MethodWriter writer, Label dest)
	{
		if (this.infixBytecode != null)
		{
			for (int i : this.infixBytecode)
			{
				if (OpcodeUtil.isJumpOpcode(i))
				{
					writer.visitJumpInsn(i, dest);
				}
				else
				{
					writer.visitInsn(i);
				}
			}
		}
	}
	
	@Override
	public boolean writePostfixBytecode(MethodWriter writer, Label dest)
	{
		if (this.postfixBytecode != null)
		{
			for (int i : this.postfixBytecode)
			{
				if (OpcodeUtil.isJumpOpcode(i))
				{
					writer.visitJumpInsn(i, dest);
				}
				else
				{
					writer.visitInsn(i);
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void write(ClassWriter writer)
	{
		int modifiers = this.modifiers & 0xFFFF;
		if (this.value == null)
		{
			modifiers |= Modifiers.ABSTRACT;
		}
		MethodVisitor visitor = writer.visitMethod(modifiers, this.qualifiedName, this.getDescriptor(), this.getSignature(), this.getExceptions());
		MethodWriter mw = new MethodWriter(visitor);
		
		if (this.isConstructor)
		{
			mw.setConstructor(this.type);
		}
		if ((this.modifiers & Modifiers.STATIC) == 0)
		{
			mw.addLocal(this.type);
		}
		
		for (Annotation annotation : this.annotations)
		{
			annotation.write(mw);
		}
		
		if ((this.modifiers & Modifiers.INLINE) == Modifiers.INLINE)
		{
			mw.visitAnnotation("Ldyvil/lang/annotation/inline;", false);
		}
		if ((this.modifiers & Modifiers.IMPLICIT) == Modifiers.IMPLICIT)
		{
			mw.visitAnnotation("Ldyvil/lang/annotation/implicit;", false);
		}
		if ((this.modifiers & Modifiers.PREFIX) == Modifiers.PREFIX)
		{
			mw.visitAnnotation("Ldyvil/lang/annotation/prefix;", false);
		}
		if ((this.modifiers & Modifiers.SEALED) == Modifiers.SEALED)
		{
			mw.visitAnnotation("Ldyvil/lang/annotation/sealed;", false);
		}
		
		for (Parameter param : this.parameters)
		{
			param.write(mw);
		}
		
		if (this.value != null)
		{
			mw.visitCode();
			this.value.writeExpression(mw);
			mw.visitEnd(this.isConstructor ? Type.VOID : this.type);
		}
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		super.toString(prefix, buffer);
		
		buffer.append(Modifiers.METHOD.toString(this.modifiers));
		if (this.type != null)
		{
			this.type.toString("", buffer);
			buffer.append(' ');
		}
		
		if (Formatting.Method.convertQualifiedNames)
		{
			buffer.append(this.qualifiedName);
		}
		else
		{
			buffer.append(this.name);
		}
		
		if (this.generics != null)
		{
			buffer.append('<');
			Util.astToString(this.generics, Formatting.Type.genericSeperator, buffer);
			buffer.append('>');
		}
		
		Util.parametersToString(this.parameters, buffer, true);
		
		if (!this.throwsDeclarations.isEmpty())
		{
			buffer.append(Formatting.Method.signatureThrowsSeperator);
			Util.astToString(this.throwsDeclarations, Formatting.Method.throwsSeperator, buffer);
		}
		
		IValue value = this.getValue();
		if (value != null)
		{
			buffer.append(Formatting.Method.signatureBodySeperator);
			Formatting.appendValue(value, prefix, buffer);
		}
		buffer.append(';');
	}
}
