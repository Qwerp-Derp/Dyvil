package dyvil.tools.compiler.ast.annotation;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.consumer.IAnnotationConsumer;

import java.lang.annotation.ElementType;

public interface IAnnotated extends IAnnotationConsumer
{
	AnnotationList getAnnotations();
	
	void setAnnotations(AnnotationList annotations);
	
	void addAnnotation(IAnnotation annotation);
	
	default boolean addRawAnnotation(String type, IAnnotation annotation)
	{
		return true;
	}
	
	IAnnotation getAnnotation(IClass type);
	
	ElementType getElementType();
	
	@Override
	default void setAnnotation(IAnnotation annotation)
	{
		this.addAnnotation(annotation);
	}
}
