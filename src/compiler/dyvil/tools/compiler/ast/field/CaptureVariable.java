package dyvil.tools.compiler.ast.field;

import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.modifiers.ModifierSet;
import dyvil.tools.compiler.ast.structure.IClassCompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.util.MarkerMessages;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

import java.lang.annotation.ElementType;

public class CaptureVariable implements IVariable
{
	public int       index;
	public IVariable variable;
	public IType     type;
	
	public CaptureVariable()
	{
	}
	
	public CaptureVariable(IVariable variable)
	{
		this.variable = variable;
		this.type = variable.getType();
	}
	
	@Override
	public ICodePosition getPosition()
	{
		return this.variable.getPosition();
	}
	
	@Override
	public void setPosition(ICodePosition position)
	{
	}
	
	@Override
	public int getAccessLevel()
	{
		return this.variable.getAccessLevel();
	}
	
	@Override
	public void setName(Name name)
	{
		this.variable.setName(name);
	}
	
	@Override
	public Name getName()
	{
		return this.variable.getName();
	}
	
	@Override
	public void setType(IType type)
	{
		this.variable.setType(type);
	}
	
	@Override
	public IType getType()
	{
		return this.variable.getType();
	}
	
	@Override
	public void setModifiers(ModifierSet modifiers)
	{
		this.variable.setModifiers(modifiers);
	}
	
	@Override
	public ModifierSet getModifiers()
	{
		return this.variable.getModifiers();
	}
	
	@Override
	public boolean hasModifier(int mod)
	{
		return this.variable.hasModifier(mod);
	}
	
	@Override
	public AnnotationList getAnnotations()
	{
		return this.variable.getAnnotations();
	}
	
	@Override
	public void setAnnotations(AnnotationList annotations)
	{
	}
	
	@Override
	public IAnnotation getAnnotation(IClass type)
	{
		return this.variable.getAnnotation(type);
	}
	
	@Override
	public void addAnnotation(IAnnotation annotation)
	{
		this.variable.addAnnotation(annotation);
	}
	
	@Override
	public ElementType getElementType()
	{
		return ElementType.LOCAL_VARIABLE;
	}
	
	@Override
	public void setValue(IValue value)
	{
		this.variable.setValue(value);
	}
	
	@Override
	public IValue getValue()
	{
		return this.variable.getValue();
	}
	
	@Override
	public void setLocalIndex(int index)
	{
		this.index = index;
	}
	
	@Override
	public int getLocalIndex()
	{
		return this.index;
	}
	
	@Override
	public boolean isReferenceType()
	{
		return this.variable.isReferenceType();
	}
	
	@Override
	public void setReferenceType()
	{
		this.variable.setReferenceType();
	}
	
	@Override
	public IType getActualType()
	{
		return this.variable.getActualType();
	}
	
	@Override
	public boolean isReferenceCapturable()
	{
		return true;
	}
	
	@Override
	public IValue checkAccess(MarkerList markers, ICodePosition position, IValue instance, IContext context)
	{
		return this.variable.checkAccess(markers, position, instance, context);
	}
	
	@Override
	public IValue checkAssign(MarkerList markers, IContext context, ICodePosition position, IValue instance, IValue newValue)
	{
		if (!this.variable.isReferenceCapturable())
		{
			markers.add(MarkerMessages.createMarker(position, "variable.capture", this.variable.getName()));
		}
		else
		{
			this.variable.setReferenceType();
			this.type = this.variable.getActualType();
		}
		
		return this.variable.checkAssign(markers, context, position, instance, newValue);
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void resolve(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
	}
	
	@Override
	public void foldConstants()
	{
	}
	
	@Override
	public void cleanup(IContext context, IClassCompilableList compilableList)
	{
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
	
	@Override
	public void writeGet(MethodWriter writer, IValue instance, int lineNumber) throws BytecodeException
	{
		int index = this.variable.getLocalIndex();
		this.variable.setLocalIndex(this.index);
		this.variable.writeGet(writer, instance, lineNumber);
		this.variable.setLocalIndex(index);
	}
	
	@Override
	public void writeSet(MethodWriter writer, IValue instance, IValue value, int lineNumber) throws BytecodeException
	{
		int index = this.variable.getLocalIndex();
		this.variable.setLocalIndex(this.index);
		this.variable.writeSet(writer, instance, value, lineNumber);
		this.variable.setLocalIndex(index);
	}
	
	@Override
	public String toString()
	{
		return "#" + this.variable.toString();
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.variable.toString(prefix, buffer);
	}
}
