package dyvil.tools.compiler.ast.classes;

import dyvil.reflect.Modifiers;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.method.CodeMethod;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MethodMatchList;
import dyvil.tools.compiler.ast.modifiers.FlagModifierSet;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.IParameter;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.ClassWriter;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.MethodWriterImpl;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.transform.CaseClasses;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;

public final class CaseClassMetadata extends ClassMetadata
{
	protected IMethod applyMethod;
	
	public CaseClassMetadata(IClass iclass)
	{
		super(iclass);
	}
	
	@Override
	public void resolveTypesHeader(MarkerList markers, IContext context)
	{
		super.resolveTypesHeader(markers, context);
		
		if (!this.theClass.isSubTypeOf(Types.SERIALIZABLE))
		{
			this.theClass.addInterface(Types.SERIALIZABLE);
		}
	}
	
	@Override
	public void resolveTypesBody(MarkerList markers, IContext context)
	{
		super.resolveTypesBody(markers, context);
		
		this.checkMethods();
	}

	@Override
	public void resolveTypesGenerate(MarkerList markers, IContext context)
	{
		super.resolveTypesGenerate(markers, context);

		if ((this.members & APPLY) != 0)
		{
			return;
		}

		// Generate the apply method signature

		CodeMethod applyMethod = new CodeMethod(this.theClass, Names.apply, this.theClass.getType(),
		                                        new FlagModifierSet(Modifiers.PUBLIC | Modifiers.STATIC));
		IParameter[] parameters = this.theClass.getParameters();
		int parameterCount = this.theClass.parameterCount();

		applyMethod.setParameters(parameters, parameterCount);
		applyMethod.setTypeParameters(this.theClass.getTypeParameters(), this.theClass.typeParameterCount());

		if (parameterCount > 0 && parameters[parameterCount - 1].isVarargs())
		{
			applyMethod.setVariadic();
		}

		applyMethod.resolveTypes(markers, context);
		this.applyMethod = applyMethod;
	}
	
	@Override
	public void getMethodMatches(MethodMatchList list, IValue instance, Name name, IArguments arguments)
	{
		if (name == Names.apply && this.applyMethod != null)
		{
			float match = this.applyMethod.getSignatureMatch(name, instance, arguments);
			if (match > 0)
			{
				list.add(this.applyMethod, match);
			}
		}
	}
	
	@Override
	public void write(ClassWriter writer) throws BytecodeException
	{
		super.write(writer);
		MethodWriter mw;
		
		if ((this.members & APPLY) == 0)
		{
			mw = new MethodWriterImpl(writer, writer.visitMethod(this.applyMethod.getModifiers().toFlags(), "apply",
			                                                     this.applyMethod.getDescriptor(),
			                                                     this.applyMethod.getSignature(), null));
			mw.begin();
			mw.writeTypeInsn(Opcodes.NEW, this.theClass.getType().getInternalName());
			mw.writeInsn(Opcodes.DUP);
			int len = this.theClass.parameterCount();
			for (int i = 0; i < len; i++)
			{
				IParameter param = this.theClass.getParameter(i);
				param.writeInit(mw);
				mw.writeVarInsn(param.getType().getLoadOpcode(), param.getLocalIndex());
			}
			this.constructor.writeInvoke(mw, 0);
			mw.writeInsn(Opcodes.ARETURN);
			mw.end(this.theClass.getType());
		}
		
		String internal = this.theClass.getInternalName();
		if ((this.members & EQUALS) == 0)
		{
			mw = new MethodWriterImpl(writer, writer.visitMethod(Modifiers.PUBLIC | Modifiers.SYNTHETIC, "equals",
			                                                     "(Ljava/lang/Object;)Z", null, null));
			mw.setThisType(internal);
			mw.registerParameter(1, "obj", Types.OBJECT, 0);
			mw.begin();
			CaseClasses.writeEquals(mw, this.theClass);
			mw.end();
		}
		
		if ((this.members & HASHCODE) == 0)
		{
			mw = new MethodWriterImpl(writer,
			                          writer.visitMethod(Modifiers.PUBLIC | Modifiers.SYNTHETIC, "hashCode", "()I",
			                                             null, null));
			mw.setThisType(internal);
			mw.begin();
			CaseClasses.writeHashCode(mw, this.theClass);
			mw.end();
		}
		
		if ((this.members & TOSTRING) == 0)
		{
			mw = new MethodWriterImpl(writer, writer.visitMethod(Modifiers.PUBLIC | Modifiers.SYNTHETIC, "toString",
			                                                     "()Ljava/lang/String;", null, null));
			mw.setThisType(internal);
			mw.begin();
			CaseClasses.writeToString(mw, this.theClass);
			mw.end();
		}
	}
}
