package dyvil.tools.compiler.ast.field;

import dyvil.reflect.Modifiers;
import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.member.Member;
import dyvil.tools.compiler.ast.method.IConstructor;
import dyvil.tools.compiler.ast.reference.ReferenceType;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.MarkerMessages;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.lang.annotation.ElementType;

public final class Variable extends Member implements IVariable
{
	protected int    localIndex;
	protected IValue value;
	
	// Metadata
	private IType refType;

	/**
	 * Marks if this variable is assigned anywhere. This is used to check if it is effectively final.
	 */
	private boolean assigned;
	
	public Variable()
	{
	}
	
	public Variable(ICodePosition position)
	{
		this.position = position;
	}
	
	public Variable(ICodePosition position, IType type)
	{
		this.position = position;
		this.type = type;
	}
	
	public Variable(ICodePosition position, Name name, IType type)
	{
		this.position = position;
		this.name = name;
		this.type = type;
	}
	
	public Variable(Name name, IType type)
	{
		this.name = name;
		this.type = type;
	}
	
	@Override
	public void setValue(IValue value)
	{
		this.value = value;
	}
	
	@Override
	public IValue getValue()
	{
		return this.value;
	}
	
	@Override
	public void setLocalIndex(int index)
	{
		this.localIndex = index;
	}
	
	@Override
	public int getLocalIndex()
	{
		return this.localIndex;
	}

	@Override
	public boolean isAssigned()
	{
		return this.assigned;
	}

	@Override
	public boolean addRawAnnotation(String type, IAnnotation annotation)
	{
		return true;
	}
	
	@Override
	public ElementType getElementType()
	{
		return ElementType.LOCAL_VARIABLE;
	}
	
	@Override
	public IValue checkAccess(MarkerList markers, ICodePosition position, IValue instance, IContext context)
	{
		return instance;
	}
	
	@Override
	public IValue checkAssign(MarkerList markers, IContext context, ICodePosition position, IValue instance, IValue newValue)
	{
		if (this.modifiers != null && this.modifiers.hasIntModifier(Modifiers.FINAL))
		{
			markers.add(MarkerMessages.createMarker(position, "variable.assign.final", this.name.unqualified));
		}
		else
		{
			this.assigned = true;
		}
		
		final IValue typedValue = this.type.convertValue(newValue, this.type, markers, context);
		if (typedValue == null)
		{
			Marker marker = MarkerMessages
					.createMarker(newValue.getPosition(), "variable.assign.type", this.name.unqualified);
			marker.addInfo(MarkerMessages.getMarker("variable.type", this.type));
			marker.addInfo(MarkerMessages.getMarker("value.type", newValue.getType()));
			markers.add(marker);
		}
		else
		{
			newValue = typedValue;
		}
		
		return newValue;
	}
	
	@Override
	public boolean isReferenceCapturable()
	{
		return true;
	}
	
	@Override
	public boolean isReferenceType()
	{
		return this.refType != null;
	}
	
	@Override
	public void setReferenceType()
	{
		if (this.refType == null)
		{
			this.refType = this.type.getSimpleRefType();
		}
	}
	
	@Override
	public IType getActualType()
	{
		return this.refType == null ? this.type : this.refType;
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		super.resolveTypes(markers, context);
		
		if (this.value != null)
		{
			this.value.resolveTypes(markers, context);
		}
	}
	
	@Override
	public void resolve(MarkerList markers, IContext context)
	{
		super.resolve(markers, context);

		this.value = this.value.resolve(markers, context);

		boolean inferType = false;
		if (this.type == Types.UNKNOWN)
		{
			inferType = true;
			this.type = this.value.getType();
			if (this.type == Types.UNKNOWN)
			{
				markers.add(MarkerMessages.createMarker(this.position, "variable.type.infer", this.name.unqualified));
				this.type = Types.ANY;
			}
		}
		
		IValue value1 = this.type.convertValue(this.value, this.type, markers, context);
		if (value1 == null)
		{
			Marker marker = MarkerMessages
					.createMarker(this.position, "variable.type.incompatible", this.name.unqualified);
			marker.addInfo(MarkerMessages.getMarker("variable.type", this.type));
			marker.addInfo(MarkerMessages.getMarker("value.type", this.value.getType()));
			markers.add(marker);
		}
		else
		{
			this.value = value1;
			if (inferType)
			{
				this.type = value1.getType();
			}
		}
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		super.checkTypes(markers, context);
		
		this.value.checkTypes(markers, context);
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		super.check(markers, context);
		
		this.value.check(markers, context);
		
		if (this.type == Types.VOID)
		{
			markers.add(MarkerMessages.createMarker(this.position, "variable.type.void"));
		}
	}
	
	@Override
	public void foldConstants()
	{
		super.foldConstants();
		
		this.value = this.value.foldConstants();
	}
	
	@Override
	public void cleanup(IContext context, IClassCompilableList compilableList)
	{
		super.cleanup(context, compilableList);
		
		this.value = this.value.cleanup(context, compilableList);
	}
	
	@Override
	public String getDescription()
	{
		return this.type.getExtendedName();
	}
	
	@Override
	public String getSignature()
	{
		return this.type.getSignature();
	}
	
	public void writeLocal(MethodWriter writer, Label start, Label end)
	{
		IType type = this.refType != null ? this.refType : this.type;
		writer.writeLocal(this.localIndex, this.name.qualified, type.getExtendedName(), type.getSignature(), start,
		                  end);
	}
	
	public void writeInit(MethodWriter writer) throws BytecodeException
	{
		this.writeInit(writer, this.value);
	}
	
	public void writeInit(MethodWriter writer, IValue value) throws BytecodeException
	{
		if (this.refType != null)
		{
			IConstructor c = this.refType.getTheClass().getBody().getConstructor(0);
			writer.writeTypeInsn(Opcodes.NEW, this.refType.getInternalName());
			writer.writeInsn(Opcodes.DUP);
			
			if (value != null)
			{
				value.writeExpression(writer, this.type);
			}
			else
			{
				writer.writeInsn(Opcodes.AUTO_DUP_X1);
			}
			c.writeInvoke(writer, this.getLineNumber());
			
			this.localIndex = writer.localCount();
			
			writer.setLocalType(this.localIndex, this.refType.getInternalName());
			writer.writeVarInsn(Opcodes.ASTORE, this.localIndex);
			return;
		}
		
		if (value != null)
		{
			value.writeExpression(writer, this.type);
		}
		this.localIndex = writer.localCount();
		writer.writeVarInsn(this.type.getStoreOpcode(), this.localIndex);
		writer.setLocalType(this.localIndex, this.type.getFrameType());
	}
	
	@Override
	public void writeGet(MethodWriter writer, IValue instance, int lineNumber) throws BytecodeException
	{
		if (this.refType != null)
		{
			writer.writeVarInsn(Opcodes.ALOAD, this.localIndex);
			
			final IClass refClass = this.refType.getTheClass();
			final IDataMember refField = refClass.getBody().getField(0);
			refField.writeGet(writer, null, lineNumber);
			
			if (refClass == Types.getObjectSimpleRefClass())
			{
				Types.OBJECT.writeCast(writer, this.type, lineNumber);
			}
			return;
		}
		
		writer.writeVarInsn(this.type.getLoadOpcode(), this.localIndex);
	}
	
	@Override
	public void writeSet(MethodWriter writer, IValue instance, IValue value, int lineNumber) throws BytecodeException
	{
		if (this.refType != null)
		{
			ReferenceType.writeGetRef(writer, value, this.localIndex);
			
			IDataMember f = this.refType.getTheClass().getBody().getField(0);
			f.writeSet(writer, null, null, lineNumber);
			return;
		}
		
		if (value != null)
		{
			value.writeExpression(writer, this.type);
		}
		
		writer.writeVarInsn(this.type.getStoreOpcode(), this.localIndex);
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.type.toString("", buffer);
		buffer.append(' ').append(this.name);
		
		if (this.value != null)
		{
			Formatting.appendSeparator(buffer, "field.assignment", '=');
			this.value.toString(prefix, buffer);
		}
	}
}
