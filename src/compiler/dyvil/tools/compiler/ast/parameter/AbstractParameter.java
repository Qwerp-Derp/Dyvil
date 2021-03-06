package dyvil.tools.compiler.ast.parameter;

import dyvil.annotation.internal.NonNull;
import dyvil.reflect.Modifiers;
import dyvil.tools.asm.AnnotationVisitor;
import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.field.Variable;
import dyvil.tools.compiler.ast.member.MemberKind;
import dyvil.tools.compiler.ast.method.ICallableMember;
import dyvil.tools.compiler.ast.modifiers.FlagModifierSet;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.compound.ArrayType;
import dyvil.tools.compiler.ast.type.compound.LambdaType;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.backend.visitor.AnnotationValueReader;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.lang.annotation.ElementType;

public abstract class AbstractParameter extends Variable implements IParameter
{
	// Metadata
	protected ICallableMember method;
	protected int             index;
	private   IType           covariantType;

	public AbstractParameter()
	{
	}

	public AbstractParameter(Name name)
	{
		super(name, null);
	}

	public AbstractParameter(Name name, IType type)
	{
		super(name, type);
	}

	public AbstractParameter(ICallableMember callable, ICodePosition position, Name name, IType type)
	{
		super(position, name, type);
		this.method = callable;
	}

	public AbstractParameter(ICallableMember callable, ICodePosition position, Name name, IType type,
		                        ModifierSet modifiers, AnnotationList annotations)
	{
		super(position, name, type, modifiers, annotations);
		this.method = callable;
	}

	@Override
	public MemberKind getKind()
	{
		return MemberKind.METHOD_PARAMETER;
	}

	@Override
	public ElementType getElementType()
	{
		return ElementType.PARAMETER;
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public ICallableMember getMethod()
	{
		return this.method;
	}

	@Override
	public void setMethod(ICallableMember method)
	{
		this.method = method;
	}

	@Override
	public ModifierSet getModifiers()
	{
		if (this.modifiers == null)
		{
			this.modifiers = new FlagModifierSet();
		}

		return this.modifiers;
	}

	@Override
	public void setType(IType type)
	{
		this.type = type;
		this.covariantType = null;
	}

	@Override
	public String getInternalName()
	{
		return this.name == null ? null : this.name.qualified;
	}

	@Override
	public IType getCovariantType()
	{
		if (this.covariantType != null)
		{
			return this.covariantType;
		}

		return this.covariantType = this.type.asParameterType();
	}

	@Override
	public int getIndex()
	{
		return this.index;
	}

	@Override
	public void setIndex(int index)
	{
		this.index = index;
	}

	@Override
	public boolean isVarargs()
	{
		return this.hasModifier(Modifiers.VARARGS);
	}

	@Override
	public void setVarargs(boolean varargs)
	{
		if (varargs)
		{
			this.getModifiers().addIntModifier(Modifiers.VARARGS);
		}
	}

	@Override
	public boolean addRawAnnotation(String type, IAnnotation annotation)
	{
		return true;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String internalType)
	{
		switch (internalType)
		{
		case "dyvil/annotation/internal/DefaultValue":
			return new AnnotationValueReader(this);
		case "dyvil/annotation/internal/DefaultArrayValue":
			return new AnnotationValueReader(value -> this.value = value.withType(this.type, this.type, null, null));
		}

		return IParameter.super.visitAnnotation(internalType);
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		super.resolveTypes(markers, context);

		if (this.value != null)
		{
			this.value.resolveTypes(markers, context);
		}

		this.covariantType = null;

		final LambdaType functionType;
		if (this.type != null && (functionType = this.type.extract(LambdaType.class)) != null)
		{
			if (functionType.isExtension())
			{
				this.getModifiers().addIntModifier(Modifiers.INFIX_FLAG);
			}
			else if (this.modifiers != null && this.modifiers.hasIntModifier(Modifiers.INFIX_FLAG))
			{
				functionType.setExtension(true);
			}
		}
	}

	@Override
	public void write(ClassWriter writer) throws BytecodeException
	{
	}

	@Override
	public void writeInit(MethodWriter writer)
	{
		IParameter.super.writeInit(writer);
	}

	@Override
	public void toString(@NonNull String indent, @NonNull StringBuilder buffer)
	{
		if (this.annotations != null)
		{
			int count = this.annotations.annotationCount();
			for (int i = 0; i < count; i++)
			{
				this.annotations.getAnnotation(i).toString(indent, buffer);
				buffer.append(' ');
			}
		}

		if (this.modifiers != null)
		{
			this.modifiers.toString(this.getKind(), buffer);
		}

		// The type of varargs syntax for this parameter
		// 0 = not variadic
		// 1 = array
		// 2 = other type
		final byte varargs = !this.isVarargs() ? (byte) 0 : this.type.canExtract(ArrayType.class) ? (byte) 1 : (byte) 2;

		boolean typeAscription = false;
		if (this.type != null)
		{
			typeAscription = Formatting.typeAscription("parameter.type_ascription", this);

			if (!typeAscription)
			{
				this.appendType(indent, buffer, varargs);
				buffer.append(' ');
			}
		}

		if (this.name != null)
		{
			buffer.append(this.name);
		}
		else
		{
			buffer.append('_');
		}
		if (varargs == 2)
		{
			buffer.append("...");
		}

		if (typeAscription)
		{
			Formatting.appendSeparator(buffer, "parameter.type_ascription", ':');
			this.appendType(indent, buffer, varargs);
		}

		if (this.value != null)
		{
			Formatting.appendSeparator(buffer, "field.assignment", '=');
			this.value.toString(indent, buffer);
		}
	}

	private void appendType(String prefix, StringBuilder buffer, byte varargs)
	{
		if (varargs == 1)
		{
			this.type.extract(ArrayType.class).getElementType().toString(prefix, buffer);
			buffer.append("...");
		}
		else
		{
			this.type.toString(prefix, buffer);
		}
	}
}
