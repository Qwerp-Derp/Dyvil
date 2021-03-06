package dyvil.tools.compiler.ast.method.intrinsic;

import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.parameter.IParameter;
import dyvil.tools.compiler.ast.parameter.IParameterList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;

import static dyvil.reflect.Modifiers.INFIX;

public interface IntrinsicData
{
	default int getCompilerCode()
	{
		return 0;
	}

	void writeIntrinsic(MethodWriter writer, IValue receiver, IArguments arguments, int lineNumber)
		throws BytecodeException;

	void writeIntrinsic(MethodWriter writer, Label dest, IValue receiver, IArguments arguments, int lineNumber)
		throws BytecodeException;

	void writeInvIntrinsic(MethodWriter writer, Label dest, IValue receiver, IArguments arguments, int lineNumber)
		throws BytecodeException;

	static void writeInsn(MethodWriter writer, IMethod method, int insn, IValue receiver, IArguments arguments,
		                     int lineNumber) throws BytecodeException
	{
		if (insn < 0)
		{
			IntrinsicData.writeArgument(writer, method, ~insn, // = -insn+1
			                            receiver, arguments);
			return;
		}

		writer.visitInsnAtLine(insn, lineNumber);
	}

	static IType writeArgument(MethodWriter writer, IMethod method, int index, IValue receiver, IArguments arguments)
		throws BytecodeException
	{
		final IParameterList params = method.getParameterList();

		if (receiver == null || receiver.isIgnoredClassAccess())
		{
			final IParameter parameter = params.get(index);
			arguments.writeValue(index, parameter, writer);
			return parameter.getCovariantType();
		}

		if (index == 0)
		{
			final IType type = method.hasModifier(INFIX) ? params.get(0).getCovariantType() : method.getReceiverType();
			receiver.writeExpression(writer, type);
			return type;
		}

		final IParameter parameter = params.get(method.hasModifier(INFIX) ? index : index - 1);
		arguments.writeValue(index - 1, parameter, writer);
		return parameter.getCovariantType();
	}
}
