package dyvil.tools.compiler.ast.pattern;

import dyvil.annotation.internal.NonNull;
import dyvil.reflect.Opcodes;
import dyvil.tools.asm.Label;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.field.IDataMember;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.ast.type.compound.TupleType;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.Markers;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.Name;
import dyvil.tools.parsing.marker.Marker;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class TuplePattern extends Pattern implements IPatternList
{
	private IPattern[] patterns;
	private int        patternCount;

	// Metadata
	private IType tupleType;

	public TuplePattern(ICodePosition position)
	{
		this.position = position;
		this.patterns = new IPattern[3];
	}

	@Override
	public int getPatternType()
	{
		return TUPLE;
	}

	@Override
	public int patternCount()
	{
		return this.patternCount;
	}

	@Override
	public void setPattern(int index, IPattern pattern)
	{
		this.patterns[index] = pattern;
	}

	@Override
	public void addPattern(IPattern pattern)
	{
		int index = this.patternCount++;
		if (this.patternCount > this.patterns.length)
		{
			IPattern[] temp = new IPattern[this.patternCount];
			System.arraycopy(this.patterns, 0, temp, 0, index);
			this.patterns = temp;
		}
		this.patterns[index] = pattern;
	}

	@Override
	public IPattern getPattern(int index)
	{
		return this.patterns[index];
	}

	@Override
	public boolean isExhaustive()
	{
		for (int i = 0; i < this.patternCount; i++)
		{
			if (!this.patterns[i].isExhaustive())
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public IType getType()
	{
		if (this.tupleType != null)
		{
			return this.tupleType;
		}

		final TupleType tupleType = new TupleType(this.patternCount);
		for (int i = 0; i < this.patternCount; i++)
		{
			tupleType.addType(this.patterns[i].getType());
		}
		return this.tupleType = tupleType;
	}

	@Override
	public IPattern withType(IType type, MarkerList markers)
	{
		final IClass tupleClass = TupleType.getTupleClass(this.patternCount);
		if (tupleClass == null)
		{
			return null;
		}

		final IType tupleType = tupleClass.getClassType();
		if (!Types.isSuperClass(type, tupleType))
		{
			return null;
		}

		this.tupleType = null; // reset type to recompute it later

		for (int i = 0; i < this.patternCount; i++)
		{
			final IType elementType = Types.resolveTypeSafely(type, tupleClass.getTypeParameter(i));
			final IPattern pattern = this.patterns[i];
			final IPattern typedPattern = pattern.withType(elementType, markers);

			if (typedPattern == null)
			{
				final Marker marker = Markers.semanticError(pattern.getPosition(), "pattern.tuple.element.type");
				marker.addInfo(Markers.getSemantic("pattern.type", pattern.getType()));
				marker.addInfo(Markers.getSemantic("tuple.element.type", elementType));
				markers.add(marker);
			}
			else
			{
				this.patterns[i] = typedPattern;
			}
		}

		if (!Types.isSuperClass(tupleType, type))
		{
			return new TypeCheckPattern(this, type, tupleType);
		}
		return this;
	}

	@Override
	public IDataMember resolveField(Name name)
	{
		for (int i = 0; i < this.patternCount; i++)
		{
			final IDataMember field = this.patterns[i].resolveField(name);
			if (field != null)
			{
				return field;
			}
		}

		return null;
	}

	@Override
	public IPattern resolve(MarkerList markers, IContext context)
	{
		if (this.patternCount == 1)
		{
			return this.patterns[0].resolve(markers, context);
		}

		for (int i = 0; i < this.patternCount; i++)
		{
			this.patterns[i] = this.patterns[i].resolve(markers, context);
		}

		return this;
	}

	@Override
	public void writeInvJump(MethodWriter writer, int varIndex, IType matchedType, Label elseLabel)
		throws BytecodeException
	{
		varIndex = IPattern.ensureVar(writer, varIndex, matchedType);

		final int lineNumber = this.getLineNumber();
		final IType tupleType = this.getType();
		final IClass tupleClass = tupleType.getTheClass();
		final String internalTupleClassName = tupleType.getInternalName();

		for (int i = 0; i < this.patternCount; i++)
		{
			if (this.patterns[i].isWildcard())
			{
				// Skip wildcard patterns
				continue;
			}

			writer.visitVarInsn(Opcodes.ALOAD, varIndex);
			writer.visitFieldInsn(Opcodes.GETFIELD, internalTupleClassName, "_" + (i + 1), "Ljava/lang/Object;");

			final IType targetType = Types.resolveTypeSafely(tupleType, tupleClass.getTypeParameter(i));

			Types.OBJECT.writeCast(writer, targetType, lineNumber);
			this.patterns[i].writeInvJump(writer, -1, targetType, elseLabel);
		}
	}

	@Override
	public void toString(@NonNull String indent, @NonNull StringBuilder buffer)
	{
		if (this.patternCount == 0)
		{
			if (Formatting.getBoolean("tuple.empty.space_between"))
			{
				buffer.append("( )");
			}
			else
			{
				buffer.append("()");
			}
			return;
		}

		buffer.append('(');
		if (Formatting.getBoolean("tuple.open_paren.space_after"))
		{
			buffer.append(' ');
		}

		Util.astToString(indent, this.patterns, this.patternCount, Formatting.getSeparator("tuple.separator", ','),
		                 buffer);

		if (Formatting.getBoolean("tuple.close_paren.space_before"))
		{
			buffer.append(' ');
		}
		buffer.append(')');
	}
}
