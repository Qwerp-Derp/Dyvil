package dyvil.tools.compiler.ast.classes;

import dyvil.reflect.Modifiers;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.classes.metadata.IClassMetadata;
import dyvil.tools.compiler.ast.constructor.IConstructor;
import dyvil.tools.compiler.ast.field.*;
import dyvil.tools.compiler.ast.modifiers.EmptyModifiers;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.IParameterList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.MethodWriterImpl;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.transform.CaptureHelper;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.position.ICodePosition;

public class AnonymousClass extends CodeClass
{
	protected CaptureHelper captureHelper = new CaptureHelper(CaptureField.factory(this));

	protected FieldThis    thisField;
	protected IConstructor constructor;
	protected String       constructorDesc;

	public AnonymousClass(ICodePosition position)
	{
		this.metadata = new AnonymousClassMetadata(this);
		this.interfaces = new IType[1];
		this.body = new ClassBody(this);
		this.position = position;
		this.modifiers = EmptyModifiers.INSTANCE;
	}

	public IConstructor getConstructor()
	{
		return this.constructor;
	}

	public void setConstructor(IConstructor constructor)
	{
		this.constructor = constructor;
	}

	@Override
	public void setInnerIndex(String internalName, int index)
	{
		String outerName = this.enclosingClass.getName().qualified;
		String indexString = Integer.toString(index);

		this.name = Name.getQualified(outerName + '$' + indexString);
		this.fullName = this.enclosingClass.getFullName() + '$' + indexString;
		this.internalName = this.enclosingClass.getInternalName() + '$' + indexString;
	}

	@Override
	public IDataMember capture(IVariable variable)
	{
		if (this.isMember(variable))
		{
			return variable;
		}

		return this.captureHelper.capture(variable);
	}

	@Override
	public IAccessible getAccessibleThis(IClass type)
	{
		if (type == this)
		{
			return VariableThis.DEFAULT;
		}

		IAccessible outer = this.enclosingClass.getAccessibleThis(type);
		if (outer == null)
		{
			return null;
		}

		if (this.thisField == null)
		{
			return this.thisField = new FieldThis(this, outer, type);
		}
		return this.thisField;
	}

	protected String getConstructorDesc()
	{
		if (this.constructorDesc != null)
		{
			return this.constructorDesc;
		}

		final IParameterList parameterList = this.constructor.getParameterList();
		final StringBuilder buf = new StringBuilder();

		buf.append('(');
		for (int i = 0, count = parameterList.size(); i < count; i++)
		{
			parameterList.get(i).getType().appendExtendedName(buf);
		}

		FieldThis thisField = this.thisField;
		if (thisField != null)
		{
			buf.append(thisField.getDescription());
		}

		this.captureHelper.appendCaptureTypes(buf);

		return this.constructorDesc = buf.append(")V").toString();
	}

	public void writeConstructorCall(MethodWriter writer, IArguments arguments) throws BytecodeException
	{
		String owner = this.getInternalName();
		String name = "<init>";
		writer.visitTypeInsn(Opcodes.NEW, owner);
		writer.visitInsn(Opcodes.DUP);

		this.constructor.writeArguments(writer, arguments);

		final FieldThis thisField = this.thisField;
		if (thisField != null)
		{
			thisField.getOuter().writeGet(writer);
		}

		this.captureHelper.writeCaptures(writer);

		writer.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, name, this.getConstructorDesc(), false);
	}
}

class AnonymousClassMetadata implements IClassMetadata
{
	private final AnonymousClass theClass;

	public AnonymousClassMetadata(AnonymousClass theClass)
	{
		this.theClass = theClass;
	}

	@Override
	public void write(ClassWriter writer) throws BytecodeException
	{
		final CaptureHelper captureHelper = this.theClass.captureHelper;
		final FieldThis thisField = this.theClass.thisField;
		final IConstructor constructor = this.theClass.constructor;

		captureHelper.writeCaptureFields(writer);

		final MethodWriter initWriter = new MethodWriterImpl(writer, writer.visitMethod(Modifiers.MANDATED, "<init>",
		                                                                                this.theClass
			                                                                                .getConstructorDesc(), null,
		                                                                                null));
		final IParameterList parameterList = constructor.getParameterList();
		final int parameterCount = parameterList.size();

		// Signature & Parameter Data

		initWriter.setThisType(this.theClass.getInternalName());

		for (int i = 0; i < parameterCount; i++)
		{
			parameterList.get(i).writeInit(initWriter);
		}

		int index = initWriter.localCount();
		int thisIndex = index;

		if (thisField != null)
		{
			thisField.writeField(writer);
			index = initWriter.visitParameter(index, thisField.getName(), thisField.getTheClass().getType(),
			                                  Modifiers.MANDATED);
		}

		captureHelper.writeCaptureParameters(initWriter, index);

		// Constructor Body

		initWriter.visitCode();
		initWriter.visitVarInsn(Opcodes.ALOAD, 0);
		for (int i = 0; i < parameterCount; i++)
		{
			parameterList.get(i).writeGet(initWriter);
		}
		constructor.writeInvoke(initWriter, 0);

		if (thisField != null)
		{
			initWriter.visitVarInsn(Opcodes.ALOAD, 0);
			initWriter.visitVarInsn(Opcodes.ALOAD, thisIndex);
			initWriter.visitFieldInsn(Opcodes.PUTFIELD, this.theClass.getInternalName(), thisField.getName(),
			                          thisField.getDescription());
		}

		captureHelper.writeFieldAssignments(initWriter);

		this.theClass.writeClassInit(initWriter);

		initWriter.visitEnd(Types.VOID);
	}
}

