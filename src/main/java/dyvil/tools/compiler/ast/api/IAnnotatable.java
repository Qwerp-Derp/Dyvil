package dyvil.tools.compiler.ast.api;

import java.util.List;

import dyvil.tools.compiler.ast.annotation.Annotation;
import dyvil.tools.compiler.ast.type.IType;

public interface IAnnotatable
{
	public void setAnnotations(List<Annotation> annotations);
	
	public List<Annotation> getAnnotations();
	
	public Annotation getAnnotation(IType type);
	
	public default void addAnnotation(Annotation annotation)
	{
		this.getAnnotations().add(annotation);
	}
}
