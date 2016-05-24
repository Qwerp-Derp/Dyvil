package dyvil.tools.compiler.sources;

import dyvil.tools.compiler.DyvilCompiler;
import dyvil.tools.compiler.ast.structure.DyvilHeader;
import dyvil.tools.compiler.ast.structure.DyvilUnit;
import dyvil.tools.compiler.ast.structure.ICompilationUnit;
import dyvil.tools.compiler.ast.structure.Package;
import dyvil.tools.compiler.util.Markers;

import java.io.File;

public class DyvilFileType implements IFileType
{
	@FunctionalInterface
	interface HeaderSupplier
	{
		DyvilHeader newHeader(DyvilCompiler compiler, Package pack, File inputFile, File outputFile);
	}
	
	public static final String CLASS_EXTENSION  = ".class";
	public static final String OBJECT_EXTENSION = ".dyo";
	
	public static final IFileType DYVIL_UNIT   = new DyvilFileType("unit", "dyv", DyvilUnit::new);
	public static final IFileType DYVIL_HEADER = new DyvilFileType("header", "dyh", DyvilHeader::new);

	public static void setupFileFinder(FileFinder fileFinder)
	{
		fileFinder.registerFileType("dyv", DYVIL_UNIT);
		fileFinder.registerFileType("dyvil", DYVIL_UNIT);
		fileFinder.registerFileType("dyh", DYVIL_HEADER);
		fileFinder.registerFileType("dyvilh", DYVIL_HEADER);
	}
	
	protected String         extension;
	protected String identifier;
	protected HeaderSupplier headerSupplier;
	
	public DyvilFileType(String identifier, String extension, HeaderSupplier headerSupplier)
	{
		this.identifier = identifier;
		this.extension = extension;
		this.headerSupplier = headerSupplier;
	}
	
	@Override
	public String getExtension()
	{
		return this.extension;
	}

	@Override
	public String getLocalizedName()
	{
		return Markers.getInfo("unit.filetype." + this.identifier);
	}

	@Override
	public ICompilationUnit createUnit(DyvilCompiler compiler, Package pack, File inputFile, File outputFile)
	{
		DyvilHeader header = this.headerSupplier.newHeader(compiler, pack, inputFile, outputFile);
		pack.addHeader(header);
		return header;
	}
}
