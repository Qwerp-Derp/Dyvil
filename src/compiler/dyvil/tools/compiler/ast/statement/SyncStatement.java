package dyvil.tools.compiler.ast.statement;

import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.context.IContext;
import dyvil.tools.compiler.ast.context.IImplicitContext;
import dyvil.tools.compiler.ast.expression.AbstractValue;
import dyvil.tools.compiler.ast.expression.IValue;
import dyvil.tools.compiler.ast.generic.ITypeContext;
import dyvil.tools.compiler.ast.header.IClassCompilableList;
import dyvil.tools.compiler.ast.header.ICompilableList;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.builtin.Types;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.backend.exception.BytecodeException;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.marker.MarkerList;
import dyvil.tools.parsing.position.ICodePosition;

public final class SyncStatement extends AbstractValue implements IStatement
{
	protected IValue lock;
	protected IValue action;
	
	public SyncStatement(ICodePosition position)
	{
		this.position = position;
	}
	
	public SyncStatement(ICodePosition position, IValue lock, IValue block)
	{
		this.position = position;
		this.lock = lock;
		this.action = block;
	}
	
	@Override
	public int valueTag()
	{
		return SYNCHRONIZED;
	}
	
	public IValue getLock()
	{
		return this.lock;
	}
	
	public void setLock(IValue lock)
	{
		this.lock = lock;
	}
	
	public IValue getAction()
	{
		return this.action;
	}
	
	public void setAction(IValue action)
	{
		this.action = action;
	}
	
	@Override
	public IType getType()
	{
		return this.action.getType();
	}
	
	@Override
	public IValue withType(IType type, ITypeContext typeContext, MarkerList markers, IContext context)
	{
		this.action = this.action.withType(type, typeContext, markers, context);
		return this;
	}
	
	@Override
	public boolean isType(IType type)
	{
		return this.action.isType(type);
	}
	
	@Override
	public int getTypeMatch(IType type, IImplicitContext implicitContext)
	{
		return this.action.getTypeMatch(type, implicitContext);
	}
	
	@Override
	public void resolveTypes(MarkerList markers, IContext context)
	{
		this.lock.resolveTypes(markers, context);
		this.action.resolveTypes(markers, context);
	}
	
	@Override
	public IValue resolve(MarkerList markers, IContext context)
	{
		this.lock.resolve(markers, context);
		this.action.resolve(markers, context);
		return this;
	}
	
	@Override
	public void checkTypes(MarkerList markers, IContext context)
	{
		this.lock.checkTypes(markers, context);
		this.action.checkTypes(markers, context);
	}
	
	@Override
	public void check(MarkerList markers, IContext context)
	{
		this.lock.check(markers, context);
		this.action.check(markers, context);
	}
	
	@Override
	public IValue foldConstants()
	{
		this.lock = this.lock.foldConstants();
		this.action = this.action.foldConstants();
		return this;
	}
	
	@Override
	public IValue cleanup(ICompilableList compilableList, IClassCompilableList classCompilableList)
	{
		this.lock = this.lock.cleanup(compilableList, classCompilableList);
		this.action = this.action.cleanup(compilableList, classCompilableList);
		return this;
	}
	
	@Override
	public void writeExpression(MethodWriter writer, IType type) throws BytecodeException
	{
		/*
			synchonized (obj) {
				statements...
			}
			->
			Object lock = obj
			_monitorEnter(lock)
			try {
				statements...
			}
			finally {
				_monitorExit(lock)
			}
		 */

		dyvil.tools.asm.Label start = new dyvil.tools.asm.Label();
		dyvil.tools.asm.Label end = new dyvil.tools.asm.Label();
		dyvil.tools.asm.Label handlerStart = new dyvil.tools.asm.Label();
		dyvil.tools.asm.Label throwLabel = new dyvil.tools.asm.Label();
		dyvil.tools.asm.Label handlerEnd = new dyvil.tools.asm.Label();
		
		this.lock.writeExpression(writer, Types.OBJECT);
		writer.visitInsn(Opcodes.DUP);
		
		int varIndex = writer.startSync();
		writer.visitVarInsn(Opcodes.ASTORE, varIndex);
		writer.visitInsn(Opcodes.MONITORENTER);
		
		writer.visitLabel(start);
		this.action.writeExpression(writer, type);
		writer.endSync();
		
		writer.visitVarInsn(Opcodes.ALOAD, varIndex);
		writer.visitInsn(Opcodes.MONITOREXIT);
		writer.visitLabel(end);
		
		writer.visitJumpInsn(Opcodes.GOTO, handlerEnd);
		
		writer.visitLabel(handlerStart);
		writer.visitVarInsn(Opcodes.ALOAD, varIndex);
		writer.visitInsn(Opcodes.MONITOREXIT);
		writer.visitLabel(throwLabel);
		writer.visitInsn(Opcodes.ATHROW);
		if (type != Types.VOID)
		{
			this.action.getType().writeDefaultValue(writer);
		}
		
		writer.resetLocals(varIndex);
		writer.visitLabel(handlerEnd);
		
		writer.visitFinallyBlock(start, end, handlerStart);
		writer.visitFinallyBlock(handlerStart, throwLabel, handlerStart);
	}

	@Override
	public void writeStatement(MethodWriter writer) throws BytecodeException
	{
		this.writeExpression(writer, Types.VOID);
	}

	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append("synchronized");

		Formatting.appendSeparator(buffer, "sync.open_paren", '(');
		if (this.lock != null)
		{
			this.lock.toString(prefix, buffer);
		}

		if (Formatting.getBoolean("sync.close_paren.space_before"))
		{
			buffer.append(' ');
		}
		buffer.append(')');

		if (this.action == null)
		{
			return;
		}

		if (Util.formatStatementList(prefix, buffer, this.action))
		{
			return;
		}

		String valuePrefix = Formatting.getIndent("sync.indent", prefix);

		if (Formatting.getBoolean("sync.close_paren.newline_after"))
		{
			buffer.append('\n').append(valuePrefix);
		}
		else if (Formatting.getBoolean("sync.close_paren.space_after"))
		{
			buffer.append(' ');
		}

		this.action.toString(prefix, buffer);
	}
}
