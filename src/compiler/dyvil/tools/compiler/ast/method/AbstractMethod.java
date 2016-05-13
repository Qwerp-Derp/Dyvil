package dyvil.tools.compiler.ast.method;

import dyvil.annotation.Mutating;
import dyvil.reflect.Modifiers;
import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Handle;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.context.IDefaultContext;
import dyvil.tools.compiler.ast.context.ILabelContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.expression.ThisExpr;
import dyvil.tools.compiler.ast.external.ExternalMethod;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.field.IVariable;
import dyvil.tools.compiler.ast.generic.GenericData;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.member.Member;
import dyvil.tools.compiler.ast.method.intrinsic.IntrinsicData;
import dyvil.tools.compiler.ast.method.intrinsic.Intrinsics;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.modifiers.ModifierUtil;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.IParameter;
import dyvil.tools.compiler.ast.parameter.IParameterList;
import dyvil.tools.compiler.ast.parameter.ParameterList;
import dyvil.tools.compiler.ast.statement.loop.ILoop;
import dyvil.tools.compiler.ast.structure.IDyvilHeader;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Mutability;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.ClassFormat;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.transform.Deprecation;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.compiler.transform.TypeChecker;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.marker.SemanticError;
import dyvil.tools.parsing.position.ICodePosition;

import java.lang.annotation.ElementType;

import static dyvil.reflect.Opcodes.IFEQ;
import static dyvil.reflect.Opcodes.IFNE;

public abstract class AbstractMethod extends Member implements IMethod, ILabelContext, IDefaultContext
{
	static final Handle EXTENSION_BSM = new Handle(ClassFormat.H_INVOKESTATIC, "dyvil/runtime/DynamicLinker",
	                                               "linkExtension",
	                                               "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;)Ljava/lang/invoke/CallSite;");

	protected ITypeParameter[] typeParameters;
	protected int              typeParameterCount;

	protected IType receiverType;

	protected ParameterList parameters = new ParameterList();

	protected IType[] exceptions;
	protected int     exceptionCount;

	// Metadata
	protected IClass        enclosingClass;
	protected String        descriptor;
	protected IntrinsicData intrinsicData;

	public AbstractMethod(IClass enclosingClass)
	{
		this.enclosingClass = enclosingClass;
	}

	public AbstractMethod(IClass enclosingClass, Name name)
	{
		this.enclosingClass = enclosingClass;
		this.name = name;
	}

	public AbstractMethod(IClass enclosingClass, Name name, IType type)
	{
		this.enclosingClass = enclosingClass;
		this.type = type;
		this.name = name;
	}

	public AbstractMethod(IClass enclosingClass, Name name, IType type, ModifierSet modifiers)
	{
		super(name, type, modifiers);
		this.enclosingClass = enclosingClass;
	}

	public AbstractMethod(ICodePosition position, Name name, IType type, ModifierSet modifiers, AnnotationList annotations)
	{
		super(position, name, type, modifiers, annotations);
	}

	@Override
	public void setEnclosingClass(IClass enclosingClass)
	{
		this.enclosingClass = enclosingClass;
	}

	@Override
	public IClass getEnclosingClass()
	{
		return this.enclosingClass;
	}

	@Override
	public void setTypeParametric()
	{
		this.typeParameters = new ITypeParameter[2];
	}

	@Override
	public boolean isTypeParametric()
	{
		return this.typeParameterCount > 0;
	}

	@Override
	public int typeParameterCount()
	{
		return this.typeParameterCount;
	}

	@Override
	public void setTypeParameters(ITypeParameter[] typeParameters, int count)
	{
		this.typeParameters = typeParameters;
		this.typeParameterCount = count;
	}

	@Override
	public void setTypeParameter(int index, ITypeParameter typeParameter)
	{
		this.typeParameters[index] = typeParameter;
	}

	@Override
	public void addTypeParameter(ITypeParameter typeParameter)
	{
		if (this.typeParameters == null)
		{
			this.typeParameters = new ITypeParameter[3];
			this.typeParameters[0] = typeParameter;
			this.typeParameterCount = 1;
			return;
		}

		int index = this.typeParameterCount++;
		if (this.typeParameterCount > this.typeParameters.length)
		{
			ITypeParameter[] temp = new ITypeParameter[this.typeParameterCount];
			System.arraycopy(this.typeParameters, 0, temp, 0, index);
			this.typeParameters = temp;
		}
		this.typeParameters[index] = typeParameter;

		typeParameter.setIndex(index);
	}

	@Override
	public ITypeParameter[] getTypeParameters()
	{
		return this.typeParameters;
	}

	@Override
	public ITypeParameter getTypeParameter(int index)
	{
		return this.typeParameters[index];
	}

	@Override
	public void setVariadic()
	{
		this.modifiers.addIntModifier(Modifiers.VARARGS);
	}

	@Override
	public boolean isVariadic()
	{
		return this.modifiers.hasIntModifier(Modifiers.VARARGS);
	}

	@Override
	public boolean setReceiverType(IType receiverType)
	{
		this.receiverType = receiverType;
		return true;
	}

	@Override
	public IParameterList getParameterList()
	{
		return this.parameters;
	}

	@Override
	public boolean addRawAnnotation(String type, IAnnotation annotation)
	{
		switch (type)
		{
		case "dyvil/annotation/Native":
			this.modifiers.addIntModifier(Modifiers.NATIVE);
			return false;
		case "dyvil/annotation/Strict":
			this.modifiers.addIntModifier(Modifiers.STRICT);
			return false;
		case Deprecation.JAVA_INTERNAL:
		case Deprecation.DYVIL_INTERNAL:
			this.modifiers.addIntModifier(Modifiers.DEPRECATED);
			return true;
		case "java/lang/Override":
			this.modifiers.addIntModifier(Modifiers.OVERRIDE);
			return false;
		case "dyvil/annotation/Intrinsic":
			if (annotation != null)
			{
				this.intrinsicData = Intrinsics.readAnnotation(this, annotation);
				return this.getClass() != ExternalMethod.class;
			}
			return true;
		}
		return true;
	}

	@Override
	public ElementType getElementType()
	{
		return ElementType.METHOD;
	}

	@Override
	public int exceptionCount()
	{
		return this.exceptionCount;
	}

	@Override
	public void setException(int index, IType exception)
	{
		this.exceptions[index] = exception;
	}

	@Override
	public void addException(IType exception)
	{
		if (this.exceptions == null)
		{
			this.exceptions = new IType[3];
			this.exceptions[0] = exception;
			this.exceptionCount = 1;
			return;
		}

		int index = this.exceptionCount++;
		if (this.exceptionCount > this.exceptions.length)
		{
			IType[] temp = new IType[this.exceptionCount];
			System.arraycopy(this.exceptions, 0, temp, 0, index);
			this.exceptions = temp;
		}
		this.exceptions[index] = exception;
	}

	@Override
	public IType getException(int index)
	{
		return this.exceptions[index];
	}

	@Override
	public boolean isStatic()
	{
		return this.modifiers.hasIntModifier(Modifiers.STATIC);
	}

	@Override
	public byte checkStatic()
	{
		return this.isStatic() ? TRUE : PASS;
	}

	@Override
	public boolean isAbstract()
	{
		return this.modifiers.hasIntModifier(Modifiers.ABSTRACT) && !this.isObjectMethod();
	}

	@Override
	public boolean isObjectMethod()
	{
		switch (this.parameters.size())
		{
		case 0:
			return this.name == Names.toString || this.name == Names.hashCode;
		case 1:
			if (this.name == Names.equals
				    && this.parameters.get(0).getInternalType().getTheClass() == Types.OBJECT_CLASS)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public IDyvilHeader getHeader()
	{
		return this.enclosingClass.getHeader();
	}

	@Override
	public IClass getThisClass()
	{
		return this.enclosingClass;
	}

	@Override
	public IType getThisType()
	{
		return this.receiverType;
	}

	@Override
	public ITypeParameter resolveTypeParameter(Name name)
	{
		for (int i = 0; i < this.typeParameterCount; i++)
		{
			final ITypeParameter typeParameter = this.typeParameters[i];
			if (typeParameter.getName() == name)
			{
				return typeParameter;
			}
		}

		return null;
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		return this.parameters.resolveParameter(name);
	}

	@Override
	public dyvil.tools.compiler.ast.statement.control.Label resolveLabel(Name name)
	{
		return null;
	}

	@Override
	public ILoop getEnclosingLoop()
	{
		return null;
	}

	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		final float selfMatch = this.getSignatureMatch(name, instance, arguments);
		if (selfMatch > 0)
		{
			list.add(this, selfMatch);
		}
	}

	@Override
	public byte checkException(IType type)
	{
		for (int i = 0; i < this.exceptionCount; i++)
		{
			if (Types.isSuperType(this.exceptions[i], type))
			{
				return TRUE;
			}
		}
		return FALSE;
	}

	@Override
	public IType getReturnType()
	{
		return this.type;
	}

	@Override
	public boolean isMember(IVariable variable)
	{
		return this.parameters.isParameter(variable);
	}

	@Override
	public IDataMember capture(IVariable variable)
	{
		return variable;
	}

	@Override
	public float getSignatureMatch(Name name, IValue receiver, IArguments arguments)
	{
		if (name != this.name && name != null)
		{
			return 0;
		}

		int parameterStartIndex = 0;
		int totalMatch = 1;

		// infix modifier implementation
		if (receiver != null)
		{
			final int mod = this.modifiers.toFlags() & Modifiers.INFIX;
			if (mod == 0 || receiver.valueTag() != IValue.CLASS_ACCESS)
			{
				if (mod == Modifiers.INFIX)
				{
					final IType infixReceiverType = this.parameters.get(0).getInternalType();
					final float receiverMatch = receiver.getTypeMatch(infixReceiverType);
					if (receiverMatch == 0)
					{
						return 0;
					}

					totalMatch += receiverMatch;
					parameterStartIndex = 1;
				}
				else if (mod == Modifiers.STATIC && receiver.valueTag() != IValue.CLASS_ACCESS)
				{
					// Disallow non-static access to static method
					return 0;
				}
				else
				{
					final float receiverMatch = receiver.getTypeMatch(this.enclosingClass.getClassType());
					if (receiverMatch <= 0)
					{
						return 0;
					}
					totalMatch += receiverMatch;
				}
			}
		}

		// Only matching the name
		if (arguments == null)
		{
			return totalMatch;
		}

		final int argumentCount = arguments.size();
		final int parametersLeft = this.parameters.size() - parameterStartIndex;
		if (argumentCount > parametersLeft && !this.isVariadic())
		{
			return 0;
		}

		for (int argumentIndex = 0; argumentIndex < parametersLeft; argumentIndex++)
		{
			final IParameter parameter = this.parameters.get(argumentIndex + parameterStartIndex);
			final float valueMatch = arguments.getTypeMatch(argumentIndex, parameter);
			if (valueMatch <= 0)
			{
				return 0;
			}

			totalMatch += valueMatch;
		}

		return totalMatch;
	}

	@Override
	public GenericData getGenericData(GenericData genericData, IValue instance, IArguments arguments)
	{
		if (!this.hasTypeVariables())
		{
			return genericData;
		}

		if (genericData == null)
		{
			return new GenericData(this, this.typeParameterCount);
		}

		genericData.setTypeParametric(this);
		genericData.setTypeCount(this.typeParameterCount);

		return genericData;
	}

	@Override
	public IValue checkArguments(MarkerList markers, ICodePosition position, IContext context, IValue receiver, IArguments arguments, GenericData genericData)
	{
		if (this.modifiers.hasIntModifier(Modifiers.PREFIX) && !this.isStatic())
		{
			IValue argument = arguments.getFirstValue();
			arguments.setFirstValue(receiver);
			receiver = argument;
		}

		if (receiver != null)
		{
			final int mod = this.modifiers.toFlags() & Modifiers.INFIX;
			if (mod == Modifiers.INFIX && receiver.valueTag() != IValue.CLASS_ACCESS)
			{
				final IParameter parameter = this.parameters.get(0);
				final IType paramType = parameter.getInternalType();

				updateReceiverType(receiver, genericData);
				receiver = TypeChecker.convertValue(receiver, paramType, genericData, markers, context,
				                                    TypeChecker.markerSupplier("method.access.infix_type", this.name));

				updateReceiverType(receiver, genericData);

				for (int i = 1, count = this.parameters.size(); i < count; i++)
				{
					arguments.checkValue(i - 1, this.parameters.get(i), genericData, markers, context);
				}

				if (genericData != null)
				{
					this.checkTypeVarsInferred(markers, position, genericData);
				}
				return receiver;
			}

			if ((mod & Modifiers.STATIC) != 0)
			{
				if (receiver.valueTag() != IValue.CLASS_ACCESS)
				{
					markers.add(Markers.semantic(position, "method.access.static", this.name));
				}
				else if (receiver.getType().getTheClass() != this.enclosingClass)
				{
					markers.add(Markers.semantic(position, "method.access.static.type", this.name,
					                             this.enclosingClass.getFullName()));
				}
				receiver = null;
			}
			else if (receiver.valueTag() == IValue.CLASS_ACCESS)
			{
				if (!receiver.getType().getTheClass().isObject())
				{
					markers.add(Markers.semantic(position, "method.access.instance", this.name));
				}
			}
			else
			{
				updateReceiverType(receiver, genericData);
				receiver = TypeChecker.convertValue(receiver, this.receiverType, genericData, markers, context,
				                                    TypeChecker
					                                    .markerSupplier("method.access.receiver_type", this.name));
				updateReceiverType(receiver, genericData);
			}
		}
		else if (!this.modifiers.hasIntModifier(Modifiers.STATIC))
		{
			if (context.isStatic())
			{
				markers.add(Markers.semantic(position, "method.access.instance", this.name));
			}
			else
			{
				markers.add(Markers.semantic(position, "method.access.unqualified", this.name.unqualified));

				final IType receiverType = this.enclosingClass.getType();
				receiver = new ThisExpr(position, receiverType, context, markers);
				if (genericData != null)
				{
					genericData.setFallbackTypeContext(receiverType);
				}
			}
		}

		for (int i = 0, count = this.parameters.size(); i < count; i++)
		{
			arguments.checkValue(i, this.parameters.get(i), genericData, markers, context);
		}

		if (genericData != null)
		{
			this.checkTypeVarsInferred(markers, position, genericData);
		}
		return receiver;
	}

	private static void updateReceiverType(IValue receiver, GenericData genericData)
	{
		if (genericData != null)
		{
			genericData.lock(genericData.typeCount());
			genericData.setFallbackTypeContext(receiver.getType());
		}
	}

	private void checkTypeVarsInferred(MarkerList markers, ICodePosition position, GenericData genericData)
	{
		genericData.lock(this.typeParameterCount);

		for (int i = 0; i < this.typeParameterCount; i++)
		{
			final ITypeParameter typeParameter = this.typeParameters[i];
			final IType typeArgument = genericData.getType(typeParameter.getIndex());

			if (typeArgument == null)
			{
				final IType inferredType = typeParameter.getDefaultType();
				markers.add(Markers.semantic(position, "method.typevar.infer", this.name, typeParameter.getName(),
				                             inferredType));
				genericData.addMapping(typeParameter, inferredType);
			}
			else if (!typeParameter.isAssignableFrom(typeArgument, genericData))
			{
				final Marker marker = Markers.semanticError(position, "method.typevar.incompatible", this.name,
				                                            typeParameter.getName());
				marker.addInfo(Markers.getSemantic("generic.type", typeArgument));
				marker.addInfo(Markers.getSemantic("typeparameter.declaration", typeParameter));
				markers.add(marker);
			}
		}
	}

	@Override
	public void checkCall(MarkerList markers, ICodePosition position, IContext context, IValue instance, IArguments arguments, ITypeContext typeContext)
	{
		ModifierUtil.checkVisibility(this, position, markers, context);

		if (instance != null)
		{
			this.checkMutating(markers, instance);
		}

		for (int i = 0; i < this.exceptionCount; i++)
		{
			IType exceptionType = this.exceptions[i];
			if (IContext.isUnhandled(context, exceptionType))
			{
				markers.add(Markers.semantic(position, "exception.unhandled", exceptionType.toString()));
			}
		}
	}

	private void checkMutating(MarkerList markers, IValue receiver)
	{
		final IType receiverType = receiver.getType();
		if (receiverType.getMutability() != Mutability.IMMUTABLE)
		{
			return;
		}

		final IAnnotation mutatingAnnotation = this.getAnnotation(Types.MUTATING_CLASS);
		if (mutatingAnnotation == null)
		{
			return;
		}

		final IValue value = mutatingAnnotation.getArguments().getValue(0, Annotation.VALUE);
		final String stringValue = value != null ? value.stringValue() : Mutating.VALUE_DEFAULT;
		StringBuilder builder = new StringBuilder(stringValue);

		int index = builder.indexOf("{method}");
		if (index >= 0)
		{
			builder.replace(index, index + 8, this.name.unqualified);
		}

		index = builder.indexOf("{type}");
		if (index >= 0)
		{
			builder.replace(index, index + 6, receiverType.toString());
		}

		markers.add(new SemanticError(receiver.getPosition(), builder.toString()));
	}

	@Override
	public boolean checkOverride(IMethod candidate, ITypeContext typeContext)
	{
		// Check Name
		if (candidate.getName() != this.name)
		{
			return false;
		}

		final IParameterList candidateParameters = candidate.getParameterList();

		// Check Parameter Count
		if (candidateParameters.size() != this.parameters.size())
		{
			return false;
		}

		// The above checks can be made without checking the cache (CodeMethod) or resolving parameter types (ExternalMethod)
		if (this.checkOverride0(candidate))
		{
			return true;
		}

		// Check Parameter Types
		for (int i = 0, count = this.parameters.size(); i < count; i++)
		{
			final IType parType = this.parameters.get(i).getInternalType().getConcreteType(typeContext);
			final IType candidateParType = candidateParameters.get(i).getInternalType().getConcreteType(typeContext);
			if (!Types.isSameType(parType, candidateParType))
			{
				return false;
			}
		}

		return true;
	}

	protected boolean checkOverride0(IMethod candidate)
	{
		return false;
	}

	@Override
	public void addOverride(IMethod candidate)
	{
	}

	@Override
	public boolean hasTypeVariables()
	{
		return this.typeParameterCount > 0 || this.enclosingClass.isTypeParametric();
	}

	@Override
	public boolean isIntrinsic()
	{
		return this.intrinsicData != null;
	}

	@Override
	public int getInvokeOpcode()
	{
		int modifiers = this.modifiers.toFlags();
		if ((modifiers & Modifiers.STATIC) != 0)
		{
			return Opcodes.INVOKESTATIC;
		}
		if ((modifiers & Modifiers.PRIVATE) == Modifiers.PRIVATE)
		{
			return Opcodes.INVOKESPECIAL;
		}
		if (this.enclosingClass.isInterface())
		{
			return Opcodes.INVOKEINTERFACE;
		}
		return Opcodes.INVOKEVIRTUAL;
	}

	@Override
	public Handle toHandle()
	{
		return new Handle(ClassFormat.insnToHandle(this.getInvokeOpcode()), this.enclosingClass.getInternalName(),
		                  this.name.qualified, this.getDescriptor());
	}

	@Override
	public String getDescriptor()
	{
		if (this.descriptor != null)
		{
			return this.descriptor;
		}

		// Similar copy in NestedMethod.getDescriptor
		final StringBuilder buffer = new StringBuilder();
		buffer.append('(');

		this.parameters.appendDescriptor(buffer);
		for (int i = 0; i < this.typeParameterCount; i++)
		{
			this.typeParameters[i].appendParameterDescriptor(buffer);
		}

		buffer.append(')');
		this.type.appendExtendedName(buffer);

		return this.descriptor = buffer.toString();
	}

	private boolean needsSignature()
	{
		return this.typeParameterCount != 0 || this.type.isGenericType() || this.type.hasTypeVariables()
			       || this.parameters.needsSignature();
	}

	@Override
	public String getSignature()
	{
		if (!this.needsSignature())
		{
			return null;
		}

		StringBuilder buffer = new StringBuilder();
		if (this.typeParameterCount > 0)
		{
			buffer.append('<');
			for (int i = 0; i < this.typeParameterCount; i++)
			{
				this.typeParameters[i].appendSignature(buffer);
			}
			buffer.append('>');
		}

		buffer.append('(');
		this.parameters.appendSignature(buffer);
		for (int i = 0; i < this.typeParameterCount; i++)
		{
			this.typeParameters[i].appendParameterSignature(buffer);
		}
		buffer.append(')');
		this.type.appendSignature(buffer);
		return buffer.toString();
	}

	@Override
	public String[] getInternalExceptions()
	{
		if (this.exceptionCount == 0)
		{
			return null;
		}

		String[] array = new String[this.exceptionCount];
		for (int i = 0; i < this.exceptionCount; i++)
		{
			array[i] = this.exceptions[i].getInternalName();
		}
		return array;
	}

	@Override
	public void writeCall(MethodWriter writer, IValue instance, IArguments arguments, ITypeContext typeContext, IType targetType, int lineNumber)
		throws BytecodeException
	{
		if (this.intrinsicData != null)
		{
			this.intrinsicData.writeIntrinsic(writer, instance, arguments, lineNumber);
		}
		else
		{
			this.writeArgumentsAndInvoke(writer, instance, arguments, typeContext, lineNumber);
		}

		if (Types.isVoid(targetType))
		{
			if (!Types.isVoid(this.type))
			{
				writer.visitInsn(Opcodes.AUTO_POP);
			}
			return;
		}

		if (targetType != null)
		{
			this.type.writeCast(writer, targetType, lineNumber);
		}
	}

	@Override
	public void writeJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
		throws BytecodeException
	{
		if (this.intrinsicData != null)
		{
			this.intrinsicData.writeIntrinsic(writer, dest, instance, arguments, lineNumber);
			return;
		}

		this.writeArgumentsAndInvoke(writer, instance, arguments, typeContext, lineNumber);
		writer.visitJumpInsn(IFNE, dest);
	}

	@Override
	public void writeInvJump(MethodWriter writer, Label dest, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
		throws BytecodeException
	{
		if (this.intrinsicData != null)
		{
			this.intrinsicData.writeInvIntrinsic(writer, dest, instance, arguments, lineNumber);
			return;
		}

		this.writeArgumentsAndInvoke(writer, instance, arguments, typeContext, lineNumber);
		writer.visitJumpInsn(IFEQ, dest);
	}

	protected void writeReceiver(MethodWriter writer, IValue receiver) throws BytecodeException
	{
		if (receiver != null)
		{
			if (this.modifiers.hasIntModifier(Modifiers.INFIX))
			{
				receiver.writeExpression(writer, this.parameters.get(0).getType());
				return;
			}

			if (receiver.isPrimitive() && this.intrinsicData != null)
			{
				receiver.writeExpression(writer, null);
				return;
			}

			receiver.writeExpression(writer, this.enclosingClass.getType());
		}
	}

	protected void writeArguments(MethodWriter writer, IValue instance, IArguments arguments) throws BytecodeException
	{
		if (instance != null && this.hasModifier(Modifiers.INFIX))
		{
			for (int i = 0, count = this.parameters.size() - 1; i < count; i++)
			{
				arguments.writeValue(i, this.parameters.get(i + 1), writer);
			}
			return;
		}

		for (int i = 0, count = this.parameters.size(); i < count; i++)
		{
			arguments.writeValue(i, this.parameters.get(i), writer);
		}
	}

	private void writeArgumentsAndInvoke(MethodWriter writer, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
		throws BytecodeException
	{
		this.writeReceiver(writer, instance);
		this.writeArguments(writer, instance, arguments);
		this.writeInvoke(writer, instance, arguments, typeContext, lineNumber);
	}

	@Override
	public void writeInvoke(MethodWriter writer, IValue instance, IArguments arguments, ITypeContext typeContext, int lineNumber)
		throws BytecodeException
	{
		for (int i = 0; i < this.typeParameterCount; i++)
		{
			final ITypeParameter typeParameter = this.typeParameters[i];
			typeParameter.writeArgument(writer, typeContext.resolveType(typeParameter));
		}

		writer.visitLineNumber(lineNumber);

		int opcode;
		int modifiers = this.modifiers.toFlags();

		String owner = this.enclosingClass.getInternalName();
		if ((modifiers & Modifiers.EXTENSION) == Modifiers.EXTENSION)
		{
			writer.visitInvokeDynamicInsn(this.name.qualified, this.getDescriptor(), EXTENSION_BSM,
			                              new Handle(ClassFormat.H_INVOKESTATIC, owner, this.name.qualified,
			                                         this.getDescriptor()));
			return;
		}

		if (instance != null && instance.valueTag() == IValue.SUPER)
		{
			opcode = Opcodes.INVOKESPECIAL;
		}
		else
		{
			opcode = this.getInvokeOpcode();
		}

		String name = this.name.qualified;
		String desc = this.getDescriptor();
		writer.visitMethodInsn(opcode, owner, name, desc, this.enclosingClass.isInterface());
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		super.toString(prefix, buffer);

		// Type
		boolean typeAscription = false;
		boolean parameters = true;
		if (this.type != null && this.type != Types.UNKNOWN)
		{
			typeAscription = Formatting.typeAscription("method.type_ascription", this);

			if (!typeAscription)
			{
				this.type.toString("", buffer);
			}
			else
			{
				buffer.append("func");
				parameters = this.parameters.size() > 0 || Formatting.getBoolean("method.parameters.visible");
			}
		}
		else
		{
			buffer.append("func");
			parameters = this.parameters.size() > 0 || Formatting.getBoolean("method.parameters.visible");
		}

		// Name
		buffer.append(' ').append(this.name);

		// Type Parameters
		if (this.typeParameterCount > 0)
		{
			if (Util.endsWithSymbol(buffer))
			{
				buffer.append(' ');
			}

			Formatting.appendSeparator(buffer, "generics.open_bracket", '<');
			Util.astToString(prefix, this.typeParameters, this.typeParameterCount,
			                 Formatting.getSeparator("generics.separator", ','), buffer);
			Formatting.appendSeparator(buffer, "generics.close_bracket", '>');
		}

		// Parameters
		if (parameters)
		{
			this.parameters.toString(prefix, buffer);
		}

		// Exceptions
		if (this.exceptionCount > 0)
		{
			String throwsPrefix = prefix;
			if (Formatting.getBoolean("method.throws.newline"))
			{
				throwsPrefix = Formatting.getIndent("method.throws.indent", prefix);
				buffer.append('\n').append(throwsPrefix).append("throws ");
			}
			else
			{
				buffer.append(" throws ");
			}

			Util.astToString(throwsPrefix, this.exceptions, this.exceptionCount,
			                 Formatting.getSeparator("method.throws", ','), buffer);
		}

		// Type Ascription
		if (typeAscription)
		{
			Formatting.appendSeparator(buffer, "method.type_ascription", ':');
			this.type.toString(prefix, buffer);
		}

		// Implementation
		final IValue value = this.getValue();
		if (value != null)
		{
			if (Util.formatStatementList(prefix, buffer, value))
			{
				return;
			}

			if (Formatting.getBoolean("method.declaration.space_before"))
			{
				buffer.append(' ');
			}

			buffer.append('=');

			String valuePrefix = Formatting.getIndent("method.declaration.indent", prefix);
			if (Formatting.getBoolean("method.declaration.newline_after"))
			{
				buffer.append('\n').append(valuePrefix);
			}
			else if (Formatting.getBoolean("method.declaration.space_after"))
			{
				buffer.append(' ');
			}

			value.toString(prefix, buffer);
		}

		if (Formatting.getBoolean("method.semicolon"))
		{
			buffer.append(';');
		}
	}
}
