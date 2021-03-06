package dyvil.tools.compiler.ast.statement.loop;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.expression.access.MethodCall;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IVariable;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.PrimitiveType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.position.ICodePosition;

public class RangeForStatement extends ForEachStatement
{
	private static final int INT       = 0;
	private static final int LONG      = 1;
	private static final int FLOAT     = 2;
	private static final int DOUBLE    = 3;
	private static final int RANGEABLE = 4;

	private final IType elementType;

	public RangeForStatement(ICodePosition position, IVariable var, IType elementType)
	{
		super(position, var);

		this.elementType = elementType;
	}

	public static boolean isRangeOperator(MethodCall methodCall)
	{
		final Name name = methodCall.getName();
		return (name == Names.dotdot || name == Names.dotdotlt) // name is .. or ..<
			       && methodCall.getReceiver() != null // has receiver
			       && methodCall.getArguments().size() == 1 // has exactly one argument
			       ;//&& Types.isSuperType(LazyFields.RANGE, methodCall.getType()); // return type <: dyvil.collection.Range
	}

	public static boolean isHalfOpen(MethodCall rangeOperator)
	{
		return rangeOperator.getName() == Names.dotdotlt;
	}

	public static IValue getStartValue(MethodCall rangeOperator)
	{
		return rangeOperator.getReceiver();
	}

	public static IValue getEndValue(MethodCall rangeOperator)
	{
		return rangeOperator.getArguments().getFirstValue();
	}

	public static IType getElementType(MethodCall range)
	{
		return Types.combine(range.getReceiver().getType(), range.getArguments().getFirstValue().getType());
	}

	@Override
	public void writeStatement(MethodWriter writer) throws BytecodeException
	{
		// Determine the kind (int, long, float, double, Rangeable) of the range to fasten up compilation.
		byte kind = RANGEABLE;
		boolean boxed = false;

		final int lineNumber = this.getLineNumber();

		final IVariable var = this.variable;
		final IType varType = var.getType();

		final MethodCall rangeOperator = (MethodCall) var.getValue();
		final IValue startValue = getStartValue(rangeOperator);
		final IValue endValue = getEndValue(rangeOperator);
		final IType elementType = this.elementType;
		final boolean halfOpen = isHalfOpen(rangeOperator);

		if (elementType.isPrimitive())
		{
			switch (elementType.getTypecode())
			{
			case PrimitiveType.BYTE_CODE:
			case PrimitiveType.SHORT_CODE:
			case PrimitiveType.CHAR_CODE:
			case PrimitiveType.INT_CODE:
				kind = INT;
				break;
			case PrimitiveType.LONG_CODE:
				kind = LONG;
				break;
			case PrimitiveType.FLOAT_CODE:
				kind = FLOAT;
				break;
			case PrimitiveType.DOUBLE_CODE:
				kind = DOUBLE;
				break;
			}

			if (!varType.isPrimitive())
			{
				boxed = true;
			}
		}

		dyvil.tools.asm.Label startLabel = this.startLabel.getTarget();
		dyvil.tools.asm.Label updateLabel = this.updateLabel.getTarget();
		dyvil.tools.asm.Label endLabel = this.endLabel.getTarget();
		dyvil.tools.asm.Label scopeLabel = new dyvil.tools.asm.Label();
		writer.visitLabel(scopeLabel);

		// Write the start value and store it in the variable.
		startValue.writeExpression(writer, elementType);

		final int counterVarIndex, varIndex;

		if (boxed)
		{
			// Create two variables, the counter variable and the user-visible loop variable

			writer.visitInsn(Opcodes.AUTO_DUP);
			counterVarIndex = writer.localCount();
			writer.visitVarInsn(elementType.getStoreOpcode(), counterVarIndex);

			elementType.writeCast(writer, varType, lineNumber);
			var.writeInit(writer, null);

			varIndex = var.getLocalIndex();
		}
		else
		{
			// Use the loop variable as the counter variable

			var.writeInit(writer, null);

			varIndex = counterVarIndex = var.getLocalIndex();
		}

		endValue.writeExpression(writer, elementType);

		final int endVarIndex = writer.localCount();
		writer.visitVarInsn(elementType.getStoreOpcode(), endVarIndex);

		writer.visitTargetLabel(startLabel);

		// Check the condition
		switch (kind)
		{
		case INT:
			writer.visitVarInsn(Opcodes.ILOAD, counterVarIndex);
			writer.visitVarInsn(Opcodes.ILOAD, endVarIndex);
			writer.visitJumpInsn(halfOpen ? Opcodes.IF_ICMPGE : Opcodes.IF_ICMPGT, endLabel);
			break;
		case LONG:
			writer.visitVarInsn(Opcodes.LLOAD, counterVarIndex);
			writer.visitVarInsn(Opcodes.LLOAD, endVarIndex);
			writer.visitJumpInsn(halfOpen ? Opcodes.IF_LCMPGE : Opcodes.IF_LCMPGT, endLabel);
			break;
		case FLOAT:
			writer.visitVarInsn(Opcodes.FLOAD, counterVarIndex);
			writer.visitVarInsn(Opcodes.FLOAD, endVarIndex);
			writer.visitJumpInsn(halfOpen ? Opcodes.IF_FCMPGE : Opcodes.IF_FCMPGT, endLabel);
			break;
		case DOUBLE:
			writer.visitVarInsn(Opcodes.DLOAD, counterVarIndex);
			writer.visitVarInsn(Opcodes.DLOAD, endVarIndex);
			writer.visitJumpInsn(halfOpen ? Opcodes.IF_DCMPGE : Opcodes.IF_DCMPGT, endLabel);
			break;
		case RANGEABLE:
			writer.visitVarInsn(Opcodes.ALOAD, counterVarIndex);
			writer.visitVarInsn(Opcodes.ALOAD, endVarIndex);
			writer.visitLineNumber(lineNumber);
			writer.visitMethodInsn(Opcodes.INVOKEINTERFACE, "dyvil/collection/range/Rangeable", "compareTo",
			                       "(Ldyvil/collection/range/Rangeable;)I", true);
			writer.visitJumpInsn(halfOpen ? Opcodes.IFGE : Opcodes.IFGT, endLabel);
			break;
		}

		// Action
		if (this.action != null)
		{
			this.action.writeExpression(writer, Types.VOID);
		}

		// Increment
		writer.visitLabel(updateLabel);
		switch (kind)
		{
		case INT:
			writer.visitIincInsn(counterVarIndex, 1);

			if (boxed)
			{
				writer.visitVarInsn(Opcodes.ILOAD, counterVarIndex);
				elementType.writeCast(writer, varType, lineNumber);
				writer.visitVarInsn(varType.getStoreOpcode(), varIndex);
			}
			break;
		case LONG:
			writer.visitVarInsn(Opcodes.LLOAD, counterVarIndex);
			writer.visitInsn(Opcodes.LCONST_1);
			writer.visitInsn(Opcodes.LADD);

			if (boxed)
			{
				writer.visitInsn(Opcodes.DUP2);
				elementType.writeCast(writer, varType, lineNumber);
				writer.visitVarInsn(varType.getStoreOpcode(), varIndex);
			}

			writer.visitVarInsn(Opcodes.LSTORE, counterVarIndex);
			break;
		case FLOAT:
			writer.visitVarInsn(Opcodes.FLOAD, counterVarIndex);
			writer.visitInsn(Opcodes.FCONST_1);
			writer.visitInsn(Opcodes.FADD);

			if (boxed)
			{
				writer.visitInsn(Opcodes.DUP);
				elementType.writeCast(writer, varType, lineNumber);
				writer.visitVarInsn(varType.getStoreOpcode(), varIndex);
			}

			writer.visitVarInsn(Opcodes.FSTORE, counterVarIndex);
			break;
		case DOUBLE:
			writer.visitVarInsn(Opcodes.DLOAD, counterVarIndex);
			writer.visitInsn(Opcodes.DCONST_1);
			writer.visitInsn(Opcodes.DADD);

			if (boxed)
			{
				writer.visitInsn(Opcodes.DUP2);
				elementType.writeCast(writer, varType, lineNumber);
				writer.visitVarInsn(varType.getStoreOpcode(), varIndex);
			}

			writer.visitVarInsn(Opcodes.DSTORE, counterVarIndex);
			break;
		case RANGEABLE:
			writer.visitVarInsn(Opcodes.ALOAD, counterVarIndex);
			writer.visitLineNumber(lineNumber);
			writer.visitMethodInsn(Opcodes.INVOKEINTERFACE, "dyvil/collection/range/Rangeable", "next",
			                       "()Ldyvil/collection/range/Rangeable;", true);

			if (elementType.getTheClass() != LazyFields.RANGEABLE_CLASS)
			{
				LazyFields.RANGEABLE.writeCast(writer, elementType, lineNumber);
			}

			assert !boxed;

			writer.visitVarInsn(Opcodes.ASTORE, counterVarIndex);
			break;
		}

		writer.visitJumpInsn(Opcodes.GOTO, startLabel);

		// Local Variables
		writer.resetLocals(counterVarIndex);
		writer.visitLabel(endLabel);

		var.writeLocal(writer, scopeLabel, endLabel);
	}

	public static final class LazyFields
	{
		public static final IClass RANGEABLE_CLASS = Package.dyvilCollectionRange.resolveClass("Rangeable");
		public static final IType  RANGEABLE       = RANGEABLE_CLASS.getClassType();

		private static final IClass         RANGE_CLASS = Package.dyvilCollection.resolveClass("Range");
		public static final  IType          RANGE       = RANGE_CLASS.getClassType();
		public static final  ITypeParameter RANGE_TYPE  = RANGE_CLASS.getTypeParameter(0);
	}
}
