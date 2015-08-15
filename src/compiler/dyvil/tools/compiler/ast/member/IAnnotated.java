package dyvil.tools.compiler.ast.member;

import java.lang.annotation.ElementType;

import dyvil.tools.compiler.ast.annotation.AnnotationList;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;

public interface IAnnotated
{
	public AnnotationList getAnnotations();
	
	public void setAnnotations(AnnotationList annotations);
	
	public void addAnnotation(IAnnotation annotation);
	
	public default boolean addRawAnnotation(String type, IAnnotation annotation)
	{
		return true;
	}
	
	public IAnnotation getAnnotation(IClass type);
	
	public ElementType getElementType();
}
