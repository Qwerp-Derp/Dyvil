package dyvil.tools.compiler.ast.type.compound;

import dyvil.collection.Set;
import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Type;
import dyvil.tools.asm.TypeAnnotatableVisitor;
import dyvil.tools.asm.TypePath;
import dyvil.tools.compiler.ast.annotation.AnnotationUtil;
import dyvil.tools.compiler.ast.annotation.IAnnotation;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.constructor.IConstructor;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.generic.ITypeParameter;
import dyvil.tools.compiler.ast.header.IClassCompilableList;
import dyvil.tools.compiler.ast.header.ICompilableList;
import dyvil.tools.compiler.ast.method.IMethod;
import dyvil.tools.compiler.ast.method.MatchList;
import dyvil.tools.compiler.ast.parameter.IArguments;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.ast.type.raw.IObjectType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.ast.IASTNode;
import dyvil.tools.parsing.marker.MarkerList;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class IntersectionType implements IObjectType
{
	private static final IClass[] OBJECT_CLASS_ARRAY = { Types.OBJECT_CLASS };

	private IType left;
	private IType right;

	// Metadata
	private IClass[] commonClasses;

	public IntersectionType()
	{
	}

	public IntersectionType(IType left, IType right)
	{
		this.left = left;
		this.right = right;
	}

	@Override
	public int typeTag()
	{
		return INTERSECTION;
	}

	public IType getLeft()
	{
		return this.left;
	}

	public void setLeft(IType left)
	{
		this.left = left;
	}

	public IType getRight()
	{
		return this.right;
	}

	public void setRight(IType right)
	{
		this.right = right;
	}

	@Override
	public boolean isGenericType()
	{
		return this.left.isGenericType() || this.right.isGenericType();
	}

	@Override
	public Name getName()
	{
		return Names.Intersection;
	}

	@Override
	public IClass getTheClass()
	{
		if (this.commonClasses != null)
		{
			return this.commonClasses[0];
		}

		if (this.left.getTheClass() == Types.OBJECT_CLASS || this.right.getTheClass() == Types.OBJECT_CLASS)
		{
			this.commonClasses = OBJECT_CLASS_ARRAY;
			return Types.OBJECT_CLASS;
		}

		final Set<IClass> commonClassSet = Types.commonClasses(this.left, this.right);
		commonClassSet.remove(Types.OBJECT_CLASS);

		if (commonClassSet.isEmpty())
		{
			this.commonClasses = OBJECT_CLASS_ARRAY;
			return Types.OBJECT_CLASS;
		}

		this.commonClasses = new IClass[commonClassSet.size()];
		commonClassSet.toArray(this.commonClasses);

		return this.commonClasses[0];
	}

	@Override
	public IType asParameterType()
	{
		return combine(this.left.asParameterType(), this.right.asParameterType(), null);
	}

	@Override
	public int subTypeCheckLevel()
	{
		return SUBTYPE_UNION_INTERSECTION;
	}

	@Override
	public boolean isSuperTypeOf(IType subType)
	{
		return Types.isSuperType(this.left, subType) && Types.isSuperType(this.right, subType);
	}

	@Override
	public boolean isSuperClassOf(IType subType)
	{
		return Types.isSuperClass(this.left, subType) && Types.isSuperClass(this.right, subType);
	}

	@Override
	public boolean isSubTypeOf(IType superType)
	{
		return Types.isSuperType(superType, this.left) || Types.isSuperType(superType, this.right);
	}

	@Override
	public boolean isSubClassOf(IType superType)
	{
		return Types.isSuperClass(superType, this.left) || Types.isSuperClass(superType, this.right);
	}

	@Override
	public IType resolveType(ITypeParameter typeParameter)
	{
		final IType left = this.left.resolveType(typeParameter);
		final IType right = this.right.resolveType(typeParameter);
		if (left == null)
		{
			return right;
		}
		if (right == null)
		{
			return left;
		}

		return combine(left, right, null);
	}

	@Override
	public boolean hasTypeVariables()
	{
		return this.left.hasTypeVariables() || this.right.hasTypeVariables();
	}

	@Override
	public IType getConcreteType(ITypeContext context)
	{
		final IType left = this.left.getConcreteType(context);
		final IType right = this.right.getConcreteType(context);
		if (left == this.left && right == this.right)
		{
			return this;
		}

		return combine(left, right, null);
	}

	@Override
	public void inferTypes(IType concrete, ITypeContext typeContext)
	{
		this.left.inferTypes(concrete, typeContext);
		this.right.inferTypes(concrete, typeContext);
	}

	@Override
	public boolean isResolved()
	{
		return true;
	}

	@Override
	public IType resolveType(MarkerList markers, IContext context)
	{
		this.left = this.left.resolveType(markers, context);
		this.right = this.right.resolveType(markers, context);
		return combine(this.left, this.right, this);
	}

	public static IType combine(IType left, IType right, IntersectionType intersectionType)
	{
		if (Types.isVoid(left) || Types.isVoid(right))
		{
			// either type is void -> result void
			return Types.VOID;
		}
		if (left.isArrayType())
		{
			if (!right.isArrayType())
			{
				return Types.ANY;
			}
			return arrayElementCombine(left.getElementType(), right.getElementType());
		}
		if (right.isArrayType())
		{
			if (!left.isArrayType())
			{
				return Types.ANY;
			}
			return arrayElementCombine(left.getElementType(), right.getElementType());
		}

		if (left.getTypeVariable() == null)
		{
			IClass leftClass = left.getTheClass();
			if (leftClass == null)
			{
				// left type unresolved -> result right type
				return right;
			}
			if (leftClass == Types.NULL_CLASS || leftClass == Types.OBJECT_CLASS)
			{
				// left type is null or Object -> result reference right type
				return right.getObjectType();
			}
		}

		if (right.getTypeVariable() == null)
		{
			final IClass rightClass = right.getTheClass();
			if (rightClass == null)
			{
				// right type unresolved -> result left type
				return left;
			}
			if (rightClass == Types.NULL_CLASS || rightClass == Types.OBJECT_CLASS)
			{
				// right type is null or Object -> result reference left type
				return left.getObjectType();
			}
		}

		if (Types.isSameType(left, right) || Types.isSuperType(left, right))
		{
			// same type, or left type is a super type of right type -> result left type
			return right;
		}
		if (Types.isSuperType(right, left))
		{
			// right type is a super type of left type -> result right type
			return left;
		}

		return intersectionType != null ? intersectionType : new IntersectionType(left, right);
	}

	private static IType arrayElementCombine(IType left, IType right)
	{
		if (left.getTypecode() != right.getTypecode())
		{
			return Types.ANY;
		}
		return new ArrayType(combine(left, right, null));
	}

	@Override
	public void checkType(MarkerList markers, IContext context, int position)
	{
		this.left.checkType(markers, context, position);
		this.right.checkType(markers, context, position);
	}

	@Override
	public void check(MarkerList markers, IContext context)
	{
		this.left.check(markers, context);
		this.right.check(markers, context);
	}

	@Override
	public void foldConstants()
	{
		this.left.foldConstants();
		this.right.foldConstants();
	}

	@Override
	public void cleanup(ICompilableList compilableList, IClassCompilableList classCompilableList)
	{
		this.left.cleanup(compilableList, classCompilableList);
		this.right.cleanup(compilableList, classCompilableList);
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		final IDataMember field = this.left.resolveField(name);
		if (field != null)
		{
			return field;
		}
		return this.right.resolveField(name);
	}

	@Override
	public void getMethodMatches(MatchList<IMethod> list, IValue receiver, Name name, IArguments arguments)
	{
		this.left.getMethodMatches(list, receiver, name, arguments);
		this.right.getMethodMatches(list, receiver, name, arguments);
	}

	@Override
	public void getImplicitMatches(MatchList<IMethod> list, IValue value, IType targetType)
	{
		this.left.getImplicitMatches(list, value, targetType);
		this.right.getImplicitMatches(list, value, targetType);
	}

	@Override
	public void getConstructorMatches(MatchList<IConstructor> list, IArguments arguments)
	{
	}

	@Override
	public IMethod getFunctionalMethod()
	{
		return null;
	}

	@Override
	public String getInternalName()
	{
		return this.getTheClass().getInternalName();
	}

	@Override
	public void appendDescriptor(StringBuilder buffer, int type)
	{
		if (type == NAME_FULL)
		{
			buffer.append('|');
			this.left.appendDescriptor(buffer, NAME_FULL);
			this.right.appendDescriptor(buffer, NAME_FULL);
			return;
		}

		buffer.append('L').append(this.getInternalName()).append(';');
	}

	@Override
	public void writeTypeExpression(MethodWriter writer) throws BytecodeException
	{
		writer.visitLdcInsn(Type.getObjectType(this.getTheClass().getInternalName()));
		this.left.writeTypeExpression(writer);
		this.right.writeTypeExpression(writer);
		writer.visitMethodInsn(Opcodes.INVOKESTATIC, "dyvilx/lang/model/type/IntersectionType", "apply",
		                       "(Ljava/lang/Class;Ldyvilx/lang/model/type/Type;Ldyvilx/lang/model/type/Type;)Ldyvilx/lang/model/type/IntersectionType;",
		                       false);
	}

	@Override
	public void addAnnotation(IAnnotation annotation, TypePath typePath, int step, int steps)
	{
	}

	@Override
	public void writeAnnotations(TypeAnnotatableVisitor visitor, int typeRef, String typePath)
	{
		AnnotationUtil.visitDyvilName(this, visitor, typeRef, typePath);
	}

	@Override
	public void write(DataOutput out) throws IOException
	{
		IType.writeType(this.left, out);
		IType.writeType(this.right, out);
	}

	@Override
	public void read(DataInput in) throws IOException
	{
		this.left = IType.readType(in);
		this.right = IType.readType(in);
	}

	@Override
	public String toString()
	{
		return IASTNode.toString(this);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		this.left.toString(prefix, buffer);
		buffer.append(" & ");
		this.right.toString(prefix, buffer);
	}

	@Override
	public IType clone()
	{
		return new IntersectionType(this.left.clone(), this.right.clone());
	}
}
