package dyvil.tools.compiler.ast.classes;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import dyvil.tools.compiler.CompilerState;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.api.IField;
import dyvil.tools.compiler.ast.api.IMember;
import dyvil.tools.compiler.ast.api.IMethod;
import dyvil.tools.compiler.ast.api.IProperty;
import dyvil.tools.compiler.ast.expression.MethodCall;
import dyvil.tools.compiler.ast.field.FieldMatch;
import dyvil.tools.compiler.ast.method.Method;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.statement.FieldAssign;
import dyvil.tools.compiler.ast.statement.StatementList;
import dyvil.tools.compiler.ast.structure.CompilationUnit;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.ast.value.SuperValue;
import dyvil.tools.compiler.ast.value.ThisValue;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.position.ICodePosition;
import dyvil.tools.compiler.util.Modifiers;
import dyvil.tools.compiler.util.Util;

public class CodeClass extends ASTNode implements IClass
{
	protected CompilationUnit	unit;
	
	protected int				type;
	
	protected int				modifiers;
	
	protected String			name;
	protected String			qualifiedName;
	protected String			internalName;
	
	protected List<Annotation>	annotations	= new ArrayList(1);
	
	protected IType				superClass	= Type.OBJECT;
	protected List<IType>		interfaces	= new ArrayList(1);
	
	protected ClassBody			body;
	
	public CodeClass()
	{
		this.body = new ClassBody(null, this);
	}
	
	public CodeClass(ICodePosition position, CompilationUnit unit, int type)
	{
		this.position = position;
		this.unit = unit;
		this.type = type;
		this.body = new ClassBody(null, this);
	}
	
	public void setClassType(int type)
	{
		this.type = type;
	}
	
	public int getClassType()
	{
		return this.type;
	}
	
	@Override
	public CompilationUnit getUnit()
	{
		return this.unit;
	}
	
	@Override
	public Package getPackage()
	{
		return this.unit.pack;
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
	public boolean addModifier(int mod)
	{
		boolean flag = (this.modifiers & mod) == mod;
		this.modifiers |= mod;
		return flag;
	}
	
	@Override
	public void removeModifier(int mod)
	{
		this.modifiers &= ~mod;
	}
	
	@Override
	public boolean hasModifier(int mod)
	{
		return (this.modifiers & mod) != 0;
	}
	
	@Override
	public void setName(String name)
	{
		this.name = name;
		this.internalName = this.unit.getInternalName(this.name);
		this.qualifiedName = this.unit.getQualifiedName(this.name);
	}
	
	@Override
	public String getName()
	{
		return this.name;
	}
	
	@Override
	public void setQualifiedName(String name)
	{
		this.qualifiedName = name;
	}
	
	@Override
	public String getQualifiedName()
	{
		return this.qualifiedName;
	}
	
	@Override
	public boolean isName(String name)
	{
		return this.qualifiedName.equals(name);
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
	public void addAnnotation(Annotation annotation)
	{
		annotation.target = ElementType.TYPE;
		this.annotations.add(annotation);
	}
	
	@Override
	public Annotation getAnnotation(IType type)
	{
		for (Annotation a : this.annotations)
		{
			if (a.type.equals(type))
			{
				return a;
			}
		}
		return null;
	}
	
	@Override
	public void setType(IType type)
	{
		this.superClass = type;
	}
	
	@Override
	public IType getType()
	{
		return this.toType();
	}
	
	@Override
	public void setTypes(List<IType> types)
	{
		this.interfaces = types;
	}
	
	@Override
	public List<IType> getTypes()
	{
		return this.interfaces;
	}
	
	@Override
	public void addType(IType type)
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
	public Type toType()
	{
		return new Type(this.name, this);
	}
	
	@Override
	public IType getSuperType()
	{
		return this.superClass;
	}
	
	@Override
	public List<IType> getInterfaces()
	{
		return this.interfaces;
	}
	
	@Override
	public boolean isSuperType(IType t)
	{
		if (this.interfaces.contains(t))
		{
			return true;
		}
		else if (this.superClass != null)
		{
			return this.superClass.isAssignableFrom(t);
		}
		return false;
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
	public String[] getInterfaceArray()
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
	public CodeClass applyState(CompilerState state, IContext context)
	{
		if (state == CompilerState.RESOLVE_TYPES)
		{
			if (this.superClass != null)
			{
				if (this.superClass.isName("void"))
				{
					this.superClass = null;
				}
				else
				{
					this.superClass = this.superClass.resolve(context);
				}
			}
			Util.applyState(this.interfaces, state, context);
		}
		
		Util.applyState(this.annotations, state, this);
		this.body.applyState(state, this);
		return this;
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
	public FieldMatch resolveField(IContext context, String name)
	{
		// Own properties
		IField field = this.body.getProperty(name);
		if (field != null)
		{
			return new FieldMatch(field, 1);
		}
		
		// Own fields
		field = this.body.getField(name);
		if (field != null)
		{
			return new FieldMatch(field, 1);
		}
		
		FieldMatch match;
		IClass predef = Type.PREDEF.theClass;
		
		// Inherited Fields
		if (this.superClass != null && this != predef)
		{
			match = this.superClass.resolveField(context, name);
			if (match != null)
			{
				return match;
			}
		}
		
		for (IType i : this.interfaces)
		{
			match = i.resolveField(context, name);
			if (match != null)
			{
				return match;
			}
		}
		
		// Predef
		if (this != predef)
		{
			match = predef.resolveField(context, name);
			if (match != null)
			{
				return match;
			}
		}
		
		return null;
	}
	
	@Override
	public MethodMatch resolveMethod(IContext context, String name, IType... argumentTypes)
	{
		if (argumentTypes == null)
		{
			return null;
		}
		
		List<MethodMatch> list = new ArrayList();
		this.getMethodMatches(list, null, name, argumentTypes);
		
		if (!list.isEmpty())
		{
			Collections.sort(list);
			return list.get(0);
		}
		
		return null;
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, IType type, String name, IType... argumentTypes)
	{
		this.body.getMethodMatches(list, type, name, argumentTypes);
		
		if (!list.isEmpty())
		{
			return;
		}
		
		IClass predef = Type.PREDEF.theClass;
		
		if (this.superClass != null && this != predef)
		{
			this.superClass.getMethodMatches(list, type, name, argumentTypes);
		}
		for (IType i : this.interfaces)
		{
			i.getMethodMatches(list, type, name, argumentTypes);
		}
		
		if (list.isEmpty() && this != predef)
		{
			predef.getMethodMatches(list, type, name, argumentTypes);
		}
	}
	
	@Override
	public boolean isMember(IMember member)
	{
		return this == member.getTheClass();
	}
	
	@Override
	public byte getAccessibility(IMember member)
	{
		IClass iclass = member.getTheClass();
		if (iclass == this)
		{
			return member.getAccessibility();
		}
		
		int level = member.getAccessLevel();
		if (level == Modifiers.PUBLIC)
		{
			return member.getAccessibility();
		}
		if (level == Modifiers.PROTECTED || level == Modifiers.DERIVED)
		{
			if (this.superClass != null && this.superClass.getTheClass() == iclass)
			{
				return member.getAccessibility();
			}
			
			for (IType i : this.interfaces)
			{
				if (i.getTheClass() == iclass)
				{
					return member.getAccessibility();
				}
			}
		}
		if (level == Modifiers.PROTECTED || level == Modifiers.PACKAGE)
		{
			if (iclass.getPackage() == this.unit.pack)
			{
				return member.getAccessibility();
			}
		}
		
		return INVISIBLE;
	}
	
	@Override
	public void write(ClassWriter writer)
	{
		String internalName = this.getInternalName();
		String signature = this.getSignature();
		String superClass = this.superClass == null ? null : this.superClass.getInternalName();
		String[] interfaces = this.getInterfaceArray();
		writer.visit(Opcodes.V1_8, this.modifiers & 0xFFFF, internalName, signature, superClass, interfaces);
		
		List<IField> fields = this.body.fields;
		List<IMethod> methods = this.body.methods;
		
		ThisValue thisValue = new ThisValue(null, this.toType());
		StatementList instanceFields = new StatementList(null);
		StatementList staticFields = new StatementList(null);
		boolean instanceFieldsAdded = false;
		boolean staticFieldsAdded = false;
		
		if ((this.modifiers & Modifiers.OBJECT_CLASS) != 0)
		{
			writer.visitAnnotation("Ldyvil/lang/annotation/object;", true);
		}
		if ((this.modifiers & Modifiers.MODULE) != 0)
		{
			writer.visitAnnotation("Ldyvil/lang/annotation/module;", true);
		}
		
		for (Annotation a : this.annotations)
		{
			a.write(writer);
		}
		
		for (IField f : fields)
		{
			f.write(writer);
			
			IValue v = f.getValue();
			if (v != null)
			{
				if (f.hasModifier(Modifiers.STATIC))
				{
					FieldAssign assign = new FieldAssign(null, f.getName(), null);
					assign.value = v;
					assign.field = f;
					staticFields.addValue(assign);
				}
				else
				{
					FieldAssign assign = new FieldAssign(null, f.getName(), thisValue);
					assign.value = v;
					assign.field = f;
					instanceFields.addValue(assign);
				}
			}
		}
		
		for (IProperty p : this.body.properties)
		{
			p.write(writer);
		}
		
		for (IMethod m : methods)
		{
			String name = m.getName();
			if (name.equals("<init>"))
			{
				Util.prependValue(m, instanceFields);
				instanceFieldsAdded = true;
			}
			else if (name.equals("<clinit>"))
			{
				Util.prependValue(m, staticFields);
				staticFieldsAdded = true;
			}
			m.write(writer);
		}
		
		if (!instanceFieldsAdded && !instanceFields.isEmpty())
		{
			// Create the default constructor
			Method m = new Method(this);
			m.setQualifiedName("<init>");
			m.setType(Type.VOID);
			m.setModifiers(Modifiers.PUBLIC | Modifiers.MANDATED);
			
			// If this class has a superclass...
			if (this.superClass != null)
			{
				IClass iclass = this.superClass.getTheClass();
				if (iclass != null)
				{
					IMethod m1 = iclass.getBody().getMethod("<init>");
					// ... and the superclass has a default constructor
					if (m1 != null)
					{
						// Create the call to the super constructor
						MethodCall superConstructor = new MethodCall(null, new SuperValue(null, this.superClass), "<init>");
						superConstructor.method = m1;
						instanceFields.getValues().add(0, superConstructor);
					}
				}
			}
			m.setValue(instanceFields);
			m.write(writer);
		}
		if (!staticFieldsAdded && !staticFields.isEmpty())
		{
			// Create the classinit method
			Method m = new Method(this);
			m.setQualifiedName("<clinit>");
			m.setType(Type.VOID);
			m.setModifiers(Modifiers.STATIC | Modifiers.MANDATED);
			m.setValue(staticFields);
			m.write(writer);
		}
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		for (Annotation annotation : this.annotations)
		{
			buffer.append(prefix);
			annotation.toString(prefix, buffer);
			buffer.append('\n');
		}
		
		buffer.append(prefix).append(Modifiers.CLASS.toString(this.modifiers));
		buffer.append(Modifiers.CLASS_TYPE.toString(this.type)).append(this.name);
		
		if (this.superClass != null)
		{
			buffer.append(" extends ");
			this.superClass.toString("", buffer);
		}
		else
		{
			buffer.append(" extends void");
		}
		if (!this.interfaces.isEmpty())
		{
			buffer.append(" implements ");
			Util.astToString(this.interfaces, Formatting.Class.superClassesSeperator, buffer);
		}
		
		this.body.toString(prefix, buffer);
	}
}
