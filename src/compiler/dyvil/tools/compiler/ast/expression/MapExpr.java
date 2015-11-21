package dyvil.tools.compiler.ast.expression;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeVariable;
import dyvil.tools.compiler.ast.parameter.ArgumentList;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.MapType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.I18n;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public class MapExpr implements IValue
{
	public static final class Types
	{
		public static final IClass	MAP_CLASS				= Package.dyvilCollection.resolveClass("Map");
		public static final IClass	MAP_CONVERTIBLE_CLASS	= Package.dyvilLangLiteral.resolveClass("MapConvertible");
		
		public static final ITypeVariable	KEY_VARIABLE	= Types.MAP_CLASS.getTypeVariable(0);
		public static final ITypeVariable	VALUE_VARIABLE	= Types.MAP_CLASS.getTypeVariable(1);
	}
	
	protected ICodePosition position;
	
	protected IValue[]	keys;
	protected IValue[]	values;
	
	protected int count;
	
	// Metadata
	private IType	type;
	private IType	keyType;
	private IType	valueType;
	
	public MapExpr(ICodePosition position)
	{
		this.position = position;
	}
	
	public MapExpr(ICodePosition position, IValue[] keys, IValue[] values, int count)
	{
		this.position = position;
		this.keys = keys;
		this.values = values;
		this.count = count;
	}
	
	@Override
	public int valueTag()
	{
		return MAP;
	}
	
	@Override
	public void setPosition(ICodePosition position)
	{
		this.position = position;
	}
	
	@Override
	public ICodePosition getPosition()
	{
		return this.position;
	}
	
	public IType getKeyType()
	{
		if (this.keyType != null)
		{
			return this.keyType;
		}
		return this.keyType = ArrayExpr.getCommonType(this.keys, this.count);
	}
	
	public IType getValueType()
	{
		if (this.valueType != null)
		{
			return this.valueType;
		}
		return this.valueType = ArrayExpr.getCommonType(this.values, this.count);
	}
	
	@Override
	public boolean isResolved()
	{
		return this.type != null && this.type.isResolved();
	}
	
	@Override
	public IType getType()
	{
		if (this.type == null)
		{
			return this.type = new MapType(this.getKeyType(), this.getValueType());
		}
		return this.type;
	}
	
	@Override
	public void setType(IType type)
	{
		this.type = type;
	}
	
	@Override
	public IValue withType(IType mapType, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		if (!Types.MAP_CLASS.isSubTypeOf(mapType))
		{
			IAnnotation annotation = mapType.getTheClass().getAnnotation(Types.MAP_CONVERTIBLE_CLASS);
			if (annotation != null)
			{
				ArgumentList arguments = new ArgumentList(new IValue[] { new ArrayExpr(this.keys, this.count), new ArrayExpr(this.values, this.count) }, 2);
				return new LiteralConversion(this, annotation, arguments).withType(mapType, typeContext, markers, context);
			}
			return null;
		}
		
		IType keyType = this.keyType = mapType.resolveTypeSafely(Types.KEY_VARIABLE);
		IType valueType = this.valueType = mapType.resolveTypeSafely(Types.VALUE_VARIABLE);
		
		for (int i = 0; i < this.count; i++)
		{
			IValue value = this.keys[i];
			IValue value1 = IType.convertValue(value, keyType, typeContext, markers, context);
			
			if (value1 == null)
			{
				Marker marker = I18n.createMarker(value.getPosition(), "map.key.type.incompatible");
				marker.addInfo(I18n.getString("type.expected", keyType.getConcreteType(typeContext)));
				marker.addInfo(I18n.getString("map.key.type", value.getType()));
				markers.add(marker);
			}
			else
			{
				value = value1;
				this.keys[i] = value1;
			}
			
			value = this.values[i];
			value1 = IType.convertValue(value, valueType, typeContext, markers, context);
			
			if (value1 == null)
			{
				Marker marker = I18n.createMarker(value.getPosition(), "map.value.type.incompatible");
				marker.addInfo(I18n.getString("type.expected", valueType.getConcreteType(typeContext)));
				marker.addInfo(I18n.getString("map.value.type", value.getType()));
				markers.add(marker);
			}
			else
			{
				value = value1;
				this.values[i] = value1;
			}
		}
		
		return this;
	}
	
	@Override
	public boolean isType(IType type)
	{
		if (!Types.MAP_CLASS.isSubTypeOf(type))
		{
			return this.isConvertibleFrom(type);
		}
		
		IType keyType = type.resolveTypeSafely(Types.KEY_VARIABLE);
		IType valueType = type.resolveTypeSafely(Types.VALUE_VARIABLE);
		
		for (int i = 0; i < this.count; i++)
		{
			if (!this.keys[i].isType(keyType))
			{
				return false;
			}
			if (!this.values[i].isType(valueType))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private boolean isConvertibleFrom(IType type)
	{
		return type.getTheClass().getAnnotation(Types.MAP_CONVERTIBLE_CLASS) != null;
	}
	
	@Override
	public float getTypeMatch(IType type)
	{
		if (!Types.MAP_CLASS.isSubTypeOf(type))
		{
			return this.isConvertibleFrom(type) ? CONVERSION_MATCH : 0;
		}
		
		if (this.count == 0)
		{
			return 1;
		}
		
		IType keyType = type.resolveTypeSafely(Types.KEY_VARIABLE);
		IType valueType = type.resolveTypeSafely(Types.VALUE_VARIABLE);
		
		float total = 0;
		for (int i = 0; i < this.count; i++)
		{
			float f = this.keys[i].getTypeMatch(keyType);
			if (f <= 0F)
			{
				return 0F;
			}
			
			total += f;
			f = this.values[i].getTypeMatch(valueType);
			if (f <= 0F)
			{
				return 0F;
			}
			total += f;
		}
		
		return 1F + total / (this.count * 2F);
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.count; i++)
		{
			this.keys[i].resolveTypes(markers, context);
			this.values[i].resolveTypes(markers, context);
		}
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.count; i++)
		{
			this.keys[i] = this.keys[i].resolve(markers, context);
			this.values[i] = this.values[i].resolve(markers, context);
		}
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.count; i++)
		{
			this.keys[i].checkTypes(markers, context);
			this.values[i].checkTypes(markers, context);
		}
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		for (int i = 0; i < this.count; i++)
		{
			this.keys[i].check(markers, context);
			this.values[i].check(markers, context);
		}
	}
	
	@Override
	public IValue foldConstants()
	{
		for (int i = 0; i < this.count; i++)
		{
			this.keys[i] = this.keys[i].foldConstants();
			this.values[i] = this.values[i].foldConstants();
		}
		return this;
	}
	
	@Override
	public IValue cleanup(IContext context, IClassCompilableList compilableList)
	{
		for (int i = 0; i < this.count; i++)
		{
			this.keys[i] = this.keys[i].cleanup(context, compilableList);
			this.values[i] = this.values[i].cleanup(context, compilableList);
		}
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer) throws BytecodeException
	{
		if (this.count == 0)
		{
			writer.writeFieldInsn(Opcodes.GETSTATIC, "dyvil/collection/immutable/EmptyMap", "instance", "Ldyvil/collection/immutable/EmptyMap;");
			return;
		}
		
		IType keyObject = this.keyType.getObjectType();
		IType valueObject = this.valueType.getObjectType();
		
		writer.writeLDC(this.count);
		writer.writeNewArray("java/lang/Object", 1);
		
		for (int i = 0; i < this.count; i++)
		{
			writer.writeInsn(Opcodes.DUP);
			writer.writeLDC(i);
			this.keys[i].writeExpression(writer, keyObject);
			writer.writeInsn(Opcodes.AASTORE);
		}
		
		writer.writeLDC(this.count);
		writer.writeNewArray("java/lang/Object", 1);
		
		for (int i = 0; i < this.count; i++)
		{
			writer.writeInsn(Opcodes.DUP);
			writer.writeLDC(i);
			this.values[i].writeExpression(writer, valueObject);
			writer.writeInsn(Opcodes.AASTORE);
		}
		
		writer.writeInvokeInsn(Opcodes.INVOKESTATIC, "dyvil/collection/ImmutableMap", "apply",
				"([Ljava/lang/Object;[Ljava/lang/Object;)Ldyvil/collection/ImmutableMap;", true);
	}
	
	@Override
	public void writeStatement(MethodWriter writer) throws BytecodeException
	{
		this.writeExpression(writer);
		writer.writeInsn(Opcodes.ARETURN);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		if (this.count <= 0)
		{
			buffer.append(Formatting.Expression.emptyArray);
			return;
		}
		
		buffer.append(Formatting.Expression.arrayStart);
		
		this.keys[0].toString(prefix, buffer);
		buffer.append(Formatting.Expression.mapSeparator);
		this.values[0].toString(prefix, buffer);
		for (int i = 1; i < this.count; i++)
		{
			buffer.append(Formatting.Expression.arraySeperator);
			this.keys[i].toString(prefix, buffer);
			buffer.append(Formatting.Expression.mapSeparator);
			this.values[i].toString(prefix, buffer);
		}
		
		buffer.append(Formatting.Expression.arrayEnd);
	}
}