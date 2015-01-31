package dyvil.tools.compiler.ast.statement;

import java.util.List;

import jdk.internal.org.objectweb.asm.Label;
import dyvil.reflect.Opcodes;
import dyvil.tools.compiler.ast.ASTNode;
import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.field.FieldMatch;
import dyvil.tools.compiler.ast.field.Variable;
import dyvil.tools.compiler.ast.member.IMember;
import dyvil.tools.compiler.ast.method.MethodMatch;
import dyvil.tools.compiler.ast.structure.IContext;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.ast.type.IType;
import dyvil.tools.compiler.ast.type.Type;
import dyvil.tools.compiler.ast.value.IValue;
import dyvil.tools.compiler.backend.MethodWriter;
import dyvil.tools.compiler.config.Formatting;
import dyvil.tools.compiler.lexer.marker.Marker;
import dyvil.tools.compiler.lexer.marker.Markers;
import dyvil.tools.compiler.lexer.position.ICodePosition;

public class ForStatement extends ASTNode implements IStatement, IContext, ILoop
{
	public static final int	DEFAULT		= 0;
	public static final int	ITERATOR	= 1;
	public static final int	ARRAY		= 2;
	
	private IContext		context;
	private IStatement		parent;
	
	public Variable			variable;
	
	public IValue			condition;
	public IValue			update;
	
	public byte				type;
	
	public IValue			then;
	
	protected Label			startLabel;
	protected Label			updateLabel;
	protected Label			endLabel;
	
	protected Variable		indexVar;
	protected Variable		lengthVar;
	protected Variable		arrayVar;
	
	public ForStatement(ICodePosition position)
	{
		this.position = position;
		
		this.startLabel = new Label();
		this.updateLabel = new Label();
		this.endLabel = new Label();
	}
	
	@Override
	public int getValueType()
	{
		return FOR;
	}
	
	@Override
	public void setType(IType type)
	{
		this.variable = new Variable(type.getPosition());
		this.variable.type = type;
	}
	
	@Override
	public Label getContinueLabel()
	{
		return this.updateLabel;
	}
	
	@Override
	public Label getBreakLabel()
	{
		return this.endLabel;
	}
	
	@Override
	public void setParent(IStatement parent)
	{
		this.parent = parent;
	}
	
	@Override
	public IStatement getParent()
	{
		return this.parent;
	}
	
	@Override
	public void resolveTypes(List<Marker> markers, IContext context)
	{
		if (this.variable != null)
		{
			this.variable.resolveTypes(markers, context);
		}
		if (this.type == 0)
		{
			if (this.condition != null)
			{
				this.condition.resolveTypes(markers, context);
			}
			if (this.update != null)
			{
				this.update.resolveTypes(markers, context);
			}
		}
		
		if (this.then != null)
		{
			this.then.resolveTypes(markers, context);
		}
	}
	
	@Override
	public IValue resolve(List<Marker> markers, IContext context)
	{
		this.context = context;
		
		if (this.variable != null)
		{
			this.variable.resolve(markers, context);
		}
		if (this.type == 0)
		{
			if (this.condition != null)
			{
				this.condition = this.condition.resolve(markers, this);
			}
			if (this.update != null)
			{
				this.update = this.update.resolve(markers, this);
			}
		}
		
		if (this.then != null)
		{
			this.then = this.then.resolve(markers, this);
		}
		return this;
	}
	
	@Override
	public void check(List<Marker> markers, IContext context)
	{
		if (this.type != 0)
		{
			IType type = this.variable.type;
			IValue value = this.variable.value;
			value.check(markers, context);
			
			IType valueType = value.getType();
			int valueTypeDims = valueType.getArrayDimensions();
			if (valueTypeDims != 0)
			{
				this.type = ARRAY;
				if (!valueType.classEquals(type) || type.getArrayDimensions() != valueTypeDims - 1)
				{
					Marker marker = Markers.create(value.getPosition(), "for.array.type");
					marker.addInfo("Array Type: " + valueType);
					marker.addInfo("Variable Type: " + type);
					markers.add(marker);
				}
				else if (this.then == null)
				{
					markers.add(Markers.create(this.position, "for.array.statement"));
				}
				else
				{
					int index = context.getVariableCount();
					this.variable.index = index;
					
					Variable var = new Variable(null);
					var.type = Type.INT;
					var.name = "$index";
					var.qualifiedName = "$index";
					var.index = index + 1;
					this.indexVar = var;
					
					var = new Variable(null);
					var.type = Type.INT;
					var.name = "$length";
					var.qualifiedName = "$length";
					var.index = index + 2;
					this.lengthVar = var;
					
					var = new Variable(null);
					var.type = valueType;
					var.name = "$array";
					var.qualifiedName = "$array";
					var.index = index + 3;
					this.arrayVar = var;
				}
			}
			
			// TODO Iterator
		}
		else
		{
			this.context = context;
			
			if (this.variable != null)
			{
				this.variable.check(markers, context);
				this.variable.index = context.getVariableCount();
			}
			
			if (this.condition != null)
			{
				this.condition.check(markers, this);
				
				if (!this.condition.isType(Type.BOOLEAN))
				{
					Marker marker = Markers.create(this.condition.getPosition(), "for.condition.type");
					marker.addInfo("Condition Type: " + this.condition.getType());
					markers.add(marker);
				}
			}
			if (this.update != null)
			{
				this.update.check(markers, this);
			}
		}
		
		if (this.then != null)
		{
			this.then.check(markers, this);
		}
	}
	
	@Override
	public IValue foldConstants()
	{
		return this;
	}
	
	@Override
	public boolean isStatic()
	{
		return this.context.isStatic();
	}
	
	@Override
	public IType getThisType()
	{
		return this.context.getThisType();
	}
	
	@Override
	public int getVariableCount()
	{
		if (this.type == ARRAY)
		{
			return 3;
		}
		return this.variable != null ? 1 : 0;
	}
	
	@Override
	public Package resolvePackage(String name)
	{
		return this.context.resolvePackage(name);
	}
	
	@Override
	public IClass resolveClass(String name)
	{
		return this.context.resolveClass(name);
	}
	
	@Override
	public FieldMatch resolveField(String name)
	{
		if (this.variable.isName(name))
		{
			return new FieldMatch(this.variable, 1);
		}
		else if (this.type == ARRAY)
		{
			if ("$index".equals(name))
			{
				return new FieldMatch(this.indexVar, 1);
			}
			else if ("$length".equals(name))
			{
				return new FieldMatch(this.lengthVar, 1);
			}
			else if ("$array".equals(name))
			{
				return new FieldMatch(this.arrayVar, 1);
			}
		}
		
		return this.context.resolveField(name);
	}
	
	@Override
	public MethodMatch resolveMethod(IValue instance, String name, List<IValue> arguments)
	{
		return this.context.resolveMethod(instance, name, arguments);
	}
	
	@Override
	public void getMethodMatches(List<MethodMatch> list, IValue instance, String name, List<IValue> arguments)
	{
		this.context.getMethodMatches(list, instance, name, arguments);
	}
	
	@Override
	public byte getAccessibility(IMember member)
	{
		return this.context.getAccessibility(member);
	}
	
	@Override
	public Label resolveLabel(String name)
	{
		if ("$forStart".equals(name))
		{
			return this.startLabel;
		}
		else if ("$forUpdate".equals(name))
		{
			return this.updateLabel;
		}
		else if ("$forEnd".equals(name))
		{
			return this.endLabel;
		}
		
		return this.parent == null ? null : this.parent.resolveLabel(name);
	}
	
	@Override
	public void writeExpression(MethodWriter writer)
	{
		this.writeStatement(writer);
		writer.visitInsn(Opcodes.ACONST_NULL);
	}
	
	@Override
	public void writeStatement(MethodWriter writer)
	{
		Variable var = this.variable;
		if (this.type == DEFAULT)
		{
			// Variable
			if (var != null)
			{
				writer.addLocal(var.index, var.type);
				var.value.writeExpression(writer);
				var.writeSet(writer);
			}
			
			writer.visitLabel(this.startLabel);
			// Condition
			if (this.condition != null)
			{
				this.condition.writeJump(writer, this.endLabel);
			}
			// Then
			if (this.then != null)
			{
				this.then.writeStatement(writer);
			}
			// Update
			writer.visitLabel(this.updateLabel);
			if (this.update != null)
			{
				this.update.writeStatement(writer);
			}
			// Go back to Condition
			writer.visitJumpInsn(Opcodes.GOTO, this.startLabel);
			writer.visitLabel(this.endLabel);
			
			// Variable
			if (var != null)
			{
				writer.visitLocalVariable(var.qualifiedName, var.type.getExtendedName(), var.type.getSignature(), this.startLabel, this.endLabel, var.index);
				writer.removeLocals(1);
			}
			return;
		}
		if (this.type == ARRAY)
		{
			Variable arrayVar = this.arrayVar;
			Variable indexVar = this.indexVar;
			Variable lengthVar = this.lengthVar;
			
			Label scopeLabel = new Label();
			writer.visitLabel2(scopeLabel);
			
			// Local Variables
			writer.addLocal(var.index, MethodWriter.TOP);
			writer.addLocal(indexVar.index, MethodWriter.INT);
			writer.addLocal(lengthVar.index, MethodWriter.INT);
			writer.addLocal(arrayVar.index, arrayVar.type);
			
			// Load the array
			var.value.writeExpression(writer);
			writer.visitInsn(Opcodes.DUP);
			arrayVar.writeSet(writer);
			// Load the length
			writer.visitInsn(Opcodes.ARRAYLENGTH);
			lengthVar.writeSet(writer);
			// Set index to 0
			writer.visitLdcInsn(0);
			indexVar.writeSet(writer);
			
			// Jump to boundary check
			writer.visitJumpInsn(Opcodes.GOTO, this.updateLabel);
			writer.visitLabel(this.startLabel);
			
			// Load the element
			arrayVar.writeGet(writer);
			indexVar.writeGet(writer);
			writer.visitInsn(arrayVar.type.getArrayLoadOpcode());
			var.writeSet(writer);
			
			// Then
			this.then.writeStatement(writer);
			
			// Increase index
			writer.visitIincInsn(indexVar.index, 1);
			// Boundary Check
			writer.visitLabel(this.updateLabel);
			indexVar.writeGet(writer);
			lengthVar.writeGet(writer);
			writer.visitJumpInsn2(Opcodes.IF_ICMPLT, this.startLabel);
			
			// Local Variables
			writer.removeLocals(4);
			writer.visitLabel(this.endLabel);
			
			writer.visitLocalVariable(var.qualifiedName, var.type.getExtendedName(), var.type.getSignature(), scopeLabel, this.endLabel, var.index);
			writer.visitLocalVariable("$index", "I", null, scopeLabel, this.endLabel, indexVar.index);
			writer.visitLocalVariable("$length", "I", null, scopeLabel, this.endLabel, lengthVar.index);
			writer.visitLocalVariable("$array", arrayVar.type.getExtendedName(), arrayVar.type.getSignature(), scopeLabel, this.endLabel, arrayVar.index);
			return;
		}
	}
	
	@Override
	public void toString(String prefix, StringBuilder buffer)
	{
		buffer.append(Formatting.Statements.forStart);
		if (this.type != 0)
		{
			this.variable.type.toString(prefix, buffer);
			buffer.append(' ').append(this.variable.name).append(Formatting.Statements.forEachSeperator);
			this.variable.value.toString(prefix, buffer);
		}
		else
		{
			if (this.variable != null)
			{
				this.variable.toString(prefix, buffer);
			}
			buffer.append(';');
			if (this.condition != null)
			{
				buffer.append(' ');
				this.condition.toString(prefix, buffer);
			}
			buffer.append(';');
			if (this.update != null)
			{
				buffer.append(' ');
				this.update.toString(prefix, buffer);
			}
		}
		buffer.append(Formatting.Statements.forEnd);
		
		if (this.then != null)
		{
			if (this.then.isStatement())
			{
				buffer.append('\n').append(prefix);
				this.then.toString(prefix, buffer);
			}
			else
			{
				buffer.append(' ');
				this.then.toString(prefix, buffer);
			}
		}
	}
}
