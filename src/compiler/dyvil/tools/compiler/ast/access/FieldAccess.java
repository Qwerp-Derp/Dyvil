package dyvil.tools.compiler.ast.access;

import dyvil.reflect.Modifiers;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constant.EnumValue;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.context.IMemberContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.IField;
import dyvil.tools.compiler.ast.field.IVariable;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.member.INamed;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.parameter.EmptyArguments;
import dyvil.tools.compiler.ast.reference.IReference;
import dyvil.tools.compiler.ast.reference.InstanceFieldReference;
import dyvil.tools.compiler.ast.reference.StaticFieldReference;
import dyvil.tools.compiler.ast.reference.VariableReference;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.structure.RootPackage;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.ast.type.raw.PackageType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class FieldAccess implements IValue, INamed, IReceiverAccess
{
	protected ICodePosition position;
	protected IValue        receiver;
	protected Name          name;

	// Metadata
	protected IDataMember field;
	protected IType       type;

	public FieldAccess()
	{
	}

	public FieldAccess(ICodePosition position)
	{
		this.position = position;
	}

	public FieldAccess(IDataMember field)
	{
		this.field = field;
		this.name = field.getName();
	}

	public FieldAccess(ICodePosition position, IValue instance, Name name)
	{
		this.position = position;
		this.receiver = instance;
		this.name = name;
	}

	public FieldAccess(ICodePosition position, IValue instance, IDataMember field)
	{
		this.position = position;
		this.receiver = instance;
		this.field = field;
		this.name = field.getName();
	}

	@Override
	public ICodePosition getPosition()
	{
		return this.position;
	}

	@Override
	public void setPosition(ICodePosition position)
	{
		this.position = position;
	}

	public MethodCall toMethodCall(IMethod method)
	{
		MethodCall call = new MethodCall(this.position);
		call.receiver = this.receiver;
		call.name = this.name;
		call.method = method;
		call.arguments = EmptyArguments.INSTANCE;
		return call;
	}

	@Override
	public int valueTag()
	{
		return FIELD_ACCESS;
	}

	public IValue getInstance()
	{
		return this.receiver;
	}

	public IDataMember getField()
	{
		return this.field;
	}

	@Override
	public boolean isConstantOrField()
	{
		return this.field != null && this.field.hasModifier(Modifiers.CONST);
	}

	@Override
	public boolean hasSideEffects()
	{
		return this.receiver != null && this.receiver.hasSideEffects();
	}

	@Override
	public boolean isResolved()
	{
		return this.field != null && this.field.getType().isResolved();
	}

	@Override
	public IValue toAssignment(IValue rhs, ICodePosition position)
	{
		return new FieldAssignment(this.position.to(position), this.receiver, this.name, rhs);
	}

	@Override
	public IReference toReference()
	{
		if (this.field == null)
		{
			return null;
		}

		if (this.field.isField())
		{
			if (this.field.hasModifier(Modifiers.STATIC))
			{
				return new StaticFieldReference((IField) this.field);
			}
			else
			{
				return new InstanceFieldReference(this.receiver, (IField) this.field);
			}
		}
		if (this.field.isVariable() && this.field instanceof IVariable)
		{
			// We have to pass the actual FieldAccess here because variable access are sometimes replaced with captures
			return new VariableReference(this);
		}
		return null;
	}

	@Override
	public IType getType()
	{
		if (this.type == null)
		{
			if (this.field == null)
			{
				return Types.UNKNOWN;
			}

			if (this.receiver == null)
			{
				return this.field.getType().asReturnType();
			}

			final ITypeContext typeContext = this.receiver.getType();
			return this.type = this.field.getType().getConcreteType(typeContext).asReturnType();
		}
		return this.type;
	}

	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (this.field == null)
		{
			return this; // don't create an extra type error
		}

		return Types.isSuperType(type, this.getType()) ? this : null;
	}

	@Override
	public boolean isType(IType type)
	{
		return this.field != null && Types.isSuperType(type, this.getType());
	}

	@Override
	public int getTypeMatch(IType type)
	{
		if (this.field == null)
		{
			return 0;
		}

		return Types.getDistance(type, this.getType());
	}

	@Override
	public void setName(Name name)
	{
		this.name = name;
	}

	@Override
	public Name getName()
	{
		return this.name;
	}

	@Override
	public void setReceiver(IValue value)
	{
		this.receiver = value;
	}

	@Override
	public IValue getReceiver()
	{
		return this.receiver;
	}

	@Override
	public IValue toAnnotationConstant(MarkerList markers, IContext context, int depth)
	{
		if (this.field == null)
		{
			return this; // do not create an extra error
		}

		if (depth == 0 || !this.isConstantOrField())
		{
			return null;
		}

		IValue value = this.field.getValue();
		if (value == null)
		{
			return null;
		}

		return value.toAnnotationConstant(markers, context, depth - 1);
	}

	@Override
	public Marker getAnnotationError()
	{
		return Markers.semantic(this.getPosition(), "annotation.field.not_constant", this.name);
	}

	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		if (this.receiver != null)
		{
			this.receiver.resolveTypes(markers, context);
		}
	}

	@Override
	public void resolveReceiver(MarkerList markers, IContext context)
	{
		if (this.receiver != null)
		{
			this.receiver = this.receiver.resolve(markers, context);
		}
	}

	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		this.resolveReceiver(markers, context);

		IValue v = this.resolveFieldAccess(markers, context);
		if (v != null)
		{
			return v;
		}

		// Don't report an error if the receiver is not resolved
		if (this.receiver != null && !this.receiver.isResolved())
		{
			return this;
		}

		ICall.addResolveMarker(markers, this.position, this.receiver, this.name, EmptyArguments.INSTANCE);
		return this;
	}

	protected IValue resolveFieldAccess(MarkerList markers, IContext context)
	{
		if (ICall.privateAccess(context, this.receiver))
		{
			// Also true when receiver == null

			IValue value = this.resolveField(this.receiver, context);
			if (value != null)
			{
				return value;
			}

			// Duplicate in FieldAssignment
			if (this.receiver == null)
			{
				final IValue implicit = context.getImplicit();
				if (implicit != null)
				{
					value = this.resolveMethod(implicit, markers, context);
					if (value != null)
					{
						return value;
					}

					value = this.resolveField(implicit, context);
					if (value != null)
					{
						return value;
					}
				}
			}

			value = this.resolveMethod(this.receiver, markers, context);
			if (value != null)
			{
				return value;
			}
		}
		else
		{
			IValue value = this.resolveMethod(this.receiver, markers, context);
			if (value != null)
			{
				return value;
			}

			value = this.resolveField(this.receiver, context);
			if (value != null)
			{
				return value;
			}
		}

		// Qualified Type Name Resolution

		return this.resolveType(context);
	}

	private IValue resolveType(IContext context)
	{
		final IMemberContext typeContext;
		final IMemberContext packageContext;

		if (this.receiver == null)
		{
			typeContext = context;
			packageContext = RootPackage.rootPackage;
		}
		else if (this.receiver.valueTag() == IValue.CLASS_ACCESS)
		{
			packageContext = typeContext = this.receiver.getType();
		}
		else
		{
			return null;
		}

		final IClass iclass = IContext.resolveClass(typeContext, this.name);
		if (iclass != null)
		{
			return new ClassAccess(this.position, iclass.getType());
		}

		final Package thePackage = packageContext.resolvePackage(this.name);
		if (thePackage != null)
		{
			return new ClassAccess(this.position, new PackageType(thePackage));
		}

		return null;
	}

	private IValue resolveField(IValue receiver, IContext context)
	{
		IDataMember field = ICall.resolveField(context, receiver, this.name);
		if (field != null)
		{
			if (field.isEnumConstant())
			{
				return new EnumValue(field.getType(), this.name);
			}

			this.field = field;
			this.receiver = receiver;
			return this;
		}
		return null;
	}

	private IValue resolveMethod(IValue receiver, MarkerList markers, IContext context)
	{
		final IMethod method = ICall.resolveMethod(context, receiver, this.name, EmptyArguments.INSTANCE);
		if (method != null)
		{
			final AbstractCall mc = this.toMethodCall(method);
			mc.setReceiver(receiver);
			mc.checkArguments(markers, context);
			return mc;
		}
		return null;
	}

	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		if (this.receiver != null)
		{
			this.receiver.checkTypes(markers, context);
		}

		if (this.field != null)
		{
			this.field = this.field.capture(context);
			this.receiver = this.field.checkAccess(markers, this.position, this.receiver, context);
		}
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
		if (this.receiver != null)
		{
			this.receiver.check(markers, context);
		}
	}

	@Override
	public IValue foldConstants()
	{
		if (this.field != null && this.field.hasModifier(Modifiers.CONST))
		{
			final IValue value = this.field.getValue();
			return value != null && value.isConstantOrField() ? value : this;
		}
		if (this.receiver != null)
		{
			this.receiver = this.receiver.foldConstants();
		}
		return this;
	}

	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		if (this.receiver != null)
		{
			this.receiver = this.receiver.cleanup(context, compilableList);
		}
		return this;
	}

	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		final int lineNumber = this.getLineNumber();
		this.field.writeGet(writer, this.receiver, lineNumber);

		if (type == null)
		{
			type = this.getType();
		}
		else if (Types.isVoid(type))
		{
			type = this.getType();
			this.field.getType().writeCast(writer, type, lineNumber);

			writer.visitInsn(type.getReturnOpcode());
			return;
		}

		this.field.getType().writeCast(writer, type, lineNumber);
	}

	@Override
	public String toString()
	{
		return IASTNode.toString(this);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.receiver != null)
		{
			this.receiver.toString(prefix, buffer);
			buffer.append('.');
		}

		buffer.append(this.name);
	}
}
