package dyvil.tools.compiler.ast.operator;

import static dyvil.reflect.Opcodes.*;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.PrimitiveType;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.lexer.marker.MarkerList;

public final class CastOperator extends ASTNode implements IValue
{
	public IValue	value;
	public IType	type;
	
	public CastOperator(IValue value, IType type)
	{
		this.value = value;
		this.type = type;
	}
	
	@Override
	public int getValueType()
	{
		return CAST_OPERATOR;
	}
	
	@Override
	public boolean isPrimitive()
	{
		return this.type.isPrimitive();
	}
	
	@Override
	public IType getType()
	{
		return this.type;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return type.isSuperTypeOf(this.type);
	}
	
	@Override
	public int getTypeMatch(IType type)
	{
		if (this.type.equals(type))
		{
			return 3;
		}
		else if (type.isSuperTypeOf(this.type))
		{
			return 2;
		}
		return 0;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		this.value.resolveTypes(markers, context);
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		this.value = this.value.resolve(markers, context);
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		IValue value1 = this.value.withType(this.type);
		if (value1 != null)
		{
			this.value = value1;
		}
		
		this.value.checkTypes(markers, context);
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		this.value.check(markers, context);
		
		if (this.type == Type.VOID)
		{
			markers.add(this.position, "cast.void");
		}
		
		boolean primitiveType = this.type.isPrimitive();
		boolean primitiveValue = this.value.isPrimitive();
		if (primitiveType)
		{
			if (!primitiveValue)
			{
				markers.add(this.position, "cast.reference");
			}
		}
		else if (primitiveValue)
		{
			markers.add(this.position, "cast.primitive");
		}
		else if (this.value.isType(this.type))
		{
			markers.add(this.position, "cast.unnecessary");
		}
	}
	
	@Override
	public IValue foldConstants()
	{
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer)
	{
		this.value.writeExpression(writer);
		if (this.type.isPrimitive())
		{
			writePrimitiveCast(this.value.getType(), (PrimitiveType) this.type, writer);
		}
		else
		{
			writer.writeTypeInsn(Opcodes.CHECKCAST, this.type);
		}
	}
	
	@Override
	public void writeStatement(MethodWriter writer)
	{
		this.writeExpression(writer);
		writer.writeInsn(this.type.getReturnOpcode());
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.value.toString(prefix, buffer);
		buffer.append(" :> ");
		this.type.toString(prefix, buffer);
	}
	
	public static void writePrimitiveCast(IType value, PrimitiveType cast, MethodWriter writer)
	{
		IClass iclass = value.getTheClass();
		if (iclass == Type.BYTE_CLASS || iclass == Type.SHORT_CLASS || iclass == Type.CHAR_CLASS || iclass == Type.INT_CLASS)
		{
			writeIntCast(cast, writer);
			return;
		}
		if (iclass == Type.LONG_CLASS)
		{
			writeLongCast(cast, writer);
			return;
		}
		if (iclass == Type.FLOAT_CLASS)
		{
			writeFloatCast(cast, writer);
			return;
		}
		if (iclass == Type.DOUBLE_CLASS)
		{
			writeDoubleCast(cast, writer);
			return;
		}
	}
	
	private static void writeIntCast(PrimitiveType cast, MethodWriter writer)
	{
		switch (cast.typecode)
		{
		case MethodWriter.T_BOOLEAN:
		case MethodWriter.T_BYTE:
		case MethodWriter.T_SHORT:
		case MethodWriter.T_CHAR:
		case MethodWriter.T_INT:
			break;
		case MethodWriter.T_LONG:
			writer.writeInsn(I2L);
			break;
		case MethodWriter.T_FLOAT:
			writer.writeInsn(I2F);
			break;
		case MethodWriter.T_DOUBLE:
			writer.writeInsn(I2D);
			break;
		}
	}
	
	private static void writeLongCast(PrimitiveType cast, MethodWriter writer)
	{
		switch (cast.typecode)
		{
		case MethodWriter.T_BOOLEAN:
			writer.writeInsn(L2I);
			break;
		case MethodWriter.T_BYTE:
			writer.writeInsn(L2B);
			break;
		case MethodWriter.T_SHORT:
			writer.writeInsn(L2S);
			break;
		case MethodWriter.T_CHAR:
			writer.writeInsn(L2C);
			break;
		case MethodWriter.T_INT:
			writer.writeInsn(L2I);
			break;
		case MethodWriter.T_LONG:
			break;
		case MethodWriter.T_FLOAT:
			writer.writeInsn(L2F);
			break;
		case MethodWriter.T_DOUBLE:
			writer.writeInsn(L2D);
			break;
		}
	}
	
	private static void writeFloatCast(PrimitiveType cast, MethodWriter writer)
	{
		switch (cast.typecode)
		{
		case MethodWriter.T_BOOLEAN:
			writer.writeInsn(F2I);
			break;
		case MethodWriter.T_BYTE:
			writer.writeInsn(F2B);
			break;
		case MethodWriter.T_SHORT:
			writer.writeInsn(F2S);
			break;
		case MethodWriter.T_CHAR:
			writer.writeInsn(F2C);
			break;
		case MethodWriter.T_INT:
			writer.writeInsn(F2I);
			break;
		case MethodWriter.T_LONG:
			writer.writeInsn(F2L);
			break;
		case MethodWriter.T_FLOAT:
			break;
		case MethodWriter.T_DOUBLE:
			writer.writeInsn(F2D);
			break;
		}
	}
	
	private static void writeDoubleCast(PrimitiveType cast, MethodWriter writer)
	{
		switch (cast.typecode)
		{
		case MethodWriter.T_BOOLEAN:
			writer.writeInsn(D2I);
			break;
		case MethodWriter.T_BYTE:
			writer.writeInsn(D2B);
			break;
		case MethodWriter.T_SHORT:
			writer.writeInsn(D2S);
			break;
		case MethodWriter.T_CHAR:
			writer.writeInsn(D2C);
			break;
		case MethodWriter.T_INT:
			writer.writeInsn(D2I);
			break;
		case MethodWriter.T_LONG:
			writer.writeInsn(D2L);
			break;
		case MethodWriter.T_FLOAT:
			writer.writeInsn(D2F);
			break;
		case MethodWriter.T_DOUBLE:
			break;
		}
	}
}
