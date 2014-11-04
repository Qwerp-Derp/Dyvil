package dyvil.tools.compiler.ast.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import dyvil.tools.compiler.CompilerState;
import dyvil.tools.compiler.ast.ASTObject;
import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.api.IField;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.structure.CompilationUnit;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.position.ICodePosition;
import dyvil.tools.compiler.util.Modifiers;
import dyvil.tools.compiler.util.Util;

public class CodeClass extends ASTObject implements IClass
{
	protected CompilationUnit	unit;
	
	protected int				type;
	
	protected int				modifiers;
	
	protected String			name;
	protected String			internalName;
	
	protected List<Annotation>	annotations	= new ArrayList(1);
	
	protected Type				superClass	= Type.OBJECT;
	protected List<Type>		interfaces	= new ArrayList(1);
	
	protected ClassBody			body;
	
	public CodeClass()
	{}
	
	public CodeClass(ICodePosition position, CompilationUnit unit, int type, ClassBody body)
	{
		this.position = position;
		this.unit = unit;
		this.type = type;
		this.body = body;
	}
	
	public int getClassType()
	{
		return this.type;
	}
	
	@Override
	public void setModifiers(int modifiers)
	{
		this.modifiers = modifiers;
	}
	
	@Override
	public int getModifiers()
	{
		return this.modifiers;
	}
	
	@Override
	public void setName(String name)
	{
		this.name = name;
		this.internalName = this.unit.getInternalName(this.name);
	}
	
	@Override
	public String getName()
	{
		return this.name;
	}
	
	@Override
	public void setAnnotations(List<Annotation> annotations)
	{
		this.annotations = annotations;
	}
	
	@Override
	public List<Annotation> getAnnotations()
	{
		return this.annotations;
	}
	
	@Override
	public void setType(Type type)
	{
		this.superClass = type;
	}
	
	@Override
	public Type getType()
	{
		return this.superClass;
	}
	
	@Override
	public void setTypes(List<Type> types)
	{
		this.interfaces = types;
	}
	
	@Override
	public List<Type> getTypes()
	{
		return this.interfaces;
	}
	
	@Override
	public void addType(Type type)
	{
		this.interfaces.add(type);
	}
	
	@Override
	public void setBody(ClassBody body)
	{
		this.body = body;
	}
	
	@Override
	public ClassBody getBody()
	{
		return this.body;
	}
	
	@Override
	public boolean isSuperType(Type t)
	{
		return t.equals(this.superClass) || this.interfaces.contains(t);
	}
	
	@Override
	public String getInternalName()
	{
		return this.internalName;
	}
	
	@Override
	public String getSignature()
	{
		return null;
	}
	
	@Override
	public String[] getInterfaces()
	{
		int len = this.interfaces.size();
		String[] interfaces = new String[len];
		for (int i = 0; i < len; i++)
		{
			interfaces[i] = this.interfaces.get(i).getInternalName();
		}
		return interfaces;
	}
	
	@Override
	public void write(ClassWriter writer)
	{
		String internalName = this.getInternalName();
		String signature = this.getSignature();
		String superClass = this.superClass.getInternalName();
		String[] interfaces = this.getInterfaces();
		writer.visit(Opcodes.V1_8, this.modifiers, internalName, signature, superClass, interfaces);
		
		List<IField> fields = this.body.fields;
		for (IField f : fields)
		{
			f.write(writer);
		}
		
		List<IMethod> methods = this.body.methods;
		for (IMethod m : methods)
		{
			m.write(writer);
		}
	}
	
	@Override
	public boolean isStatic()
	{
		return false;
	}
	
	@Override
	public Type getThisType()
	{
		return new Type(this.name, this);
	}
	
	@Override
	public IClass resolveClass(String name)
	{
		if (this.name.equals(name))
		{
			return this;
		}
		
		return this.unit.resolveClass(name);
	}
	
	@Override
	public IField resolveField(String name)
	{
		// Own fields
		IField field = this.body.getField(name);
		if (field != null)
		{
			return field;
		}
		
		IClass predef = Type.PREDEF.theClass;
		
		// Inherited Fields
		if (this.superClass != null && this.superClass.theClass != null && this != predef)
		{
			field = this.superClass.resolveField(name);
			if (field != null)
			{
				return field;
			}
		}
		
		for (Type type : this.interfaces)
		{
			field = type.resolveField(name);
			if (field != null)
			{
				return field;
			}
		}
		
		// Predef
		if (this != predef)
		{
			field = predef.resolveField(name);
			if (field != null)
			{
				return field;
			}
		}
		
		return null;
	}
	
	@Override
	public IMethod resolveMethod(String name, Type... args)
	{
		if (args == null)
		{
			return null;
		}
		
		List<MethodMatch> list = new ArrayList();
		this.getMethodMatches(list, name, args);
		Collections.sort(list);
		
		// TODO Static, Accessibility, Ambiguity
		
		return list.isEmpty() ? null : list.get(0).theMethod;
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, String name, Type... types)
	{
		this.body.getMethodMatches(list, name, types);
		
		if (!list.isEmpty())
		{
			return;
		}
		
		IClass predef = Type.PREDEF.theClass;
		
		if (this.superClass != null && this.superClass.theClass != null && this != predef)
		{
			this.superClass.theClass.getMethodMatches(list, name, types);
		}
		for (Type type : this.interfaces)
		{
			type.theClass.getMethodMatches(list, name, types);
		}
		
		if (list.isEmpty() && this != predef)
		{
			predef.getMethodMatches(list, name, types);
		}
	}
	
	@Override
	public CodeClass applyState(CompilerState state, IContext context)
	{
		if (state == CompilerState.RESOLVE_TYPES)
		{
			if (this.superClass == Type.VOID)
			{
				return null;
			}
			else if (this.superClass != null)
			{
				this.superClass = this.superClass.resolve(context);
			}
			this.interfaces.replaceAll(t -> t.applyState(state, context));
		}
		
		this.body = this.body.applyState(state, this);
		return this;
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(prefix);
		buffer.append(Modifiers.CLASS.toString(this.modifiers));
		buffer.append(Modifiers.CLASS_TYPE.toString(this.type));
		buffer.append(this.name);
		
		if (this.superClass != null)
		{
			buffer.append(" extends ");
			this.superClass.toString("", buffer);
		}
		if (!this.interfaces.isEmpty())
		{
			buffer.append(" implements ");
			Util.astToString(this.interfaces, Formatting.Class.superClassesSeperator, buffer);
		}
		
		buffer.append(Formatting.Class.bodyStart);
		this.body.toString(Formatting.Class.bodyIndent, buffer);
		buffer.append(Formatting.Class.bodyEnd);
	}
}
