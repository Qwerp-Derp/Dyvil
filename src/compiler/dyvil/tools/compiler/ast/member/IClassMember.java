package dyvil.tools.compiler.ast.member;

import dyvil.tools.compiler.ast.classes.IClass;
import dyvil.tools.compiler.ast.header.IClassCompilable;
import dyvil.tools.compiler.ast.header.IObjectCompilable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IClassMember extends IMember, IClassCompilable, IObjectCompilable
{
	IClass getEnclosingClass();
	
	void setEnclosingClass(IClass enclosingClass);
	
	@Override
	default void write(DataOutput out) throws IOException
	{
		this.writeSignature(out);
	}
	
	void writeSignature(DataOutput out) throws IOException;
	
	@Override
	default void read(DataInput in) throws IOException
	{
		this.readSignature(in);
	}
	
	void readSignature(DataInput in) throws IOException;
}
