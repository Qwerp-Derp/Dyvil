package dyvil.tools.compiler.library;

import dyvil.collection.ImmutableMap;
import dyvil.collection.Map;
import dyvil.collection.mutable.HashMap;
import dyvil.reflect.ReflectUtils;
import dyvil.tools.compiler.DyvilCompiler;
import dyvil.tools.compiler.ast.structure.Package;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.LinkOption;

public abstract class Library
{
	public static final File javaLibraryLocation;
	public static final File dyvilLibraryLocation;
	
	public static final Library dyvilLibrary;
	public static final Library dyvilBinLibrary;
	public static final Library javaLibrary;

	private static File getFileLocation(Class<?> klass)
	{
		try
		{
			return ReflectUtils.getFileLocation(klass);
		}
		catch (ClassNotFoundException ex)
		{
			return null;
		}
	}
	
	static
	{
		javaLibraryLocation = getFileLocation(java.lang.String.class);
		dyvilLibraryLocation = getFileLocation(dyvil.lang.Void.class);
		
		if ((dyvilLibrary = load(dyvilLibraryLocation)) == null)
		{
			DyvilCompiler.error("Could not load Dyvil Runtime Library");
		}
		if ((javaLibrary = load(javaLibraryLocation)) == null)
		{
			DyvilCompiler.error("Could not load Java Runtime Library");
		}
		
		File bin = new File("build/dyvilbin");
		if (bin.exists())
		{
			dyvilBinLibrary = load(bin);
		}
		else
		{
			dyvilBinLibrary = null;
		}
	}
	
	protected static final Map<String, String> env = ImmutableMap.apply("create", "true");
	
	protected static final String[]     emptyStrings     = {};
	protected static final LinkOption[] emptyLinkOptions = {};
	
	protected final File file;
	protected final Map<String, Package> packages = new HashMap<>();
	
	protected Library(File file)
	{
		this.file = file;
	}
	
	public static Library load(File file)
	{
		if (file == null)
		{
			return null;
		}
		
		if (file.isDirectory())
		{
			return new FileLibrary(file);
		}
		else if (file.getPath().endsWith(".jar"))
		{
			return new JarLibrary(file);
		}
		String error = "Invalid Library File: " + file.getAbsolutePath();
		if (!file.exists())
		{
			error += " (File does not exist)";
		}
		DyvilCompiler.error(error);
		return null;
	}
	
	public abstract void loadLibrary();
	
	public abstract void unloadLibrary();
	
	public URL getURL() throws MalformedURLException
	{
		return this.file.toURI().toURL();
	}
	
	public abstract boolean isSubPackage(String internal);
	
	public Package resolvePackage(String internal)
	{
		Package pack = this.packages.get(internal);
		if (pack != null)
		{
			return pack;
		}
		
		int index = internal.indexOf('/');
		if (index < 0)
		{
			if (this.isSubPackage(internal))
			{
				return Package.rootPackage.createSubPackage(internal);
			}
			return null;
		}
		
		if (this.isSubPackage(internal))
		{
			String s = internal.substring(0, index);
			pack = Package.rootPackage.createSubPackage(s);
			
			if (pack == null)
			{
				return null;
			}
			
			do
			{
				int index1 = internal.indexOf('/', index + 1);
				int index2 = index1 >= 0 ? index1 : internal.length();
				s = internal.substring(index + 1, index2);
				pack = pack.createSubPackage(s);
				if (pack == null)
				{
					return null;
				}
				index = index1;
			}
			while (index >= 0);
			return pack;
		}
		
		return null;
	}
	
	public abstract InputStream getInputStream(String fileName);
	
	@Override
	public String toString()
	{
		return this.file.getAbsolutePath();
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		this.unloadLibrary();
	}
}
