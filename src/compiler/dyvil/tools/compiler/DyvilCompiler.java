package dyvil.tools.compiler;

import dyvil.io.AppendablePrintStream;
import dyvil.io.FileUtils;
import dyvil.tools.asm.Opcodes;
import dyvil.tools.compiler.config.CompilerConfig;
import dyvil.tools.compiler.config.ConfigParser;
import dyvil.tools.compiler.library.Library;
import dyvil.tools.compiler.phase.ICompilerPhase;
import dyvil.tools.compiler.phase.PrintPhase;
import dyvil.tools.compiler.sources.DyvilFileType;
import dyvil.tools.compiler.sources.FileFinder;
import dyvil.tools.compiler.transform.Names;
import dyvil.tools.compiler.util.TestThread;
import dyvil.tools.compiler.util.Util;
import dyvil.tools.parsing.CodeFile;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.*;

public final class DyvilCompiler
{
	public static final String VERSION         = "$$compilerVersion$$";
	public static final String DYVIL_VERSION   = "$$version$$";
	public static final String LIBRARY_VERSION = "$$libraryVersion$$";
	
	public static boolean debug;
	public static int constantFolding = 2;
	
	
	public static boolean compilationFailed;
	
	private static Logger logger;
	private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static CompilerConfig      config     = new CompilerConfig();
	public static Set<ICompilerPhase> phases     = new TreeSet<>();
	public static FileFinder          fileFinder = new FileFinder();
	
	public static void log(String message)
	{
		System.out.println(message);
		if (logger != null)
		{
			logger.info(message);
		}
	}
	
	public static void warn(String message)
	{
		System.out.println(message);
		if (logger != null)
		{
			logger.warning(message);
		}
	}
	
	public static void error(String message)
	{
		compilationFailed = true;
		System.out.println(message);
		if (logger != null)
		{
			logger.severe(message);
		}
	}
	
	public static void error(String message, Throwable throwable)
	{
		compilationFailed = true;
		System.err.println(message);
		throwable.printStackTrace();
		if (logger != null)
		{
			logger.log(Level.SEVERE, message, throwable);
		}
	}
	
	public static void error(String className, String methodName, Throwable throwable)
	{
		compilationFailed = true;
		throwable.printStackTrace();
		if (logger != null)
		{
			logger.throwing(className, methodName, throwable);
		}
	}
	
	public static void main(String[] args)
	{
		long now = System.nanoTime();
		long totalTime = now;
		
		System.out.println("Dyvil Compiler v" + VERSION + " for Dyvil v" + DYVIL_VERSION);
		System.out.println();
		
		Names.init();
		
		// Sets up States from arguments
		for (String arg : args)
		{
			processArgument(arg);
		}
		
		// Sets up the logger
		initLogger();
		
		File sourceDir = config.getSourceDir();
		if (sourceDir == null)
		{
			log("No source path defined in Configuration File. Skipping Compilation.");
			System.exit(1);
			return;
		}
		
		if (!sourceDir.exists())
		{
			log("The specified source path '" + sourceDir + "' does not exist. Skipping Compilation.");
			System.exit(1);
			return;
		}
		
		File outputDir = config.getOutputDir();
		int phases = DyvilCompiler.phases.size();
		
		log("Loaded Config (" + Util.toTime(System.nanoTime() - now) + ")");
		
		int libs = 0;
		now = System.nanoTime();
		
		// Loads libraries
		for (Library library : DyvilCompiler.config.libraries)
		{
			library.loadLibrary();
			libs++;
		}
		
		long now1 = System.nanoTime();
		now = now1 - now;
		DyvilCompiler.log("Loaded " + libs + (libs == 1 ? " Library (" : " Libraries (") + Util.toTime(now) + ")");
		
		now = System.nanoTime();
		log("Compiling '" + sourceDir + "' to '" + outputDir + "'");
		
		// Scan for Packages and Compilation Units
		DyvilFileType.setupFileFinder(fileFinder);
		config.findUnits(fileFinder);
		
		int fileCount = fileFinder.files.size();
		int unitCount = fileFinder.units.size();
		now = System.nanoTime() - now;
		log("Found " + fileCount + (fileCount == 1 ? " File (" : " Files (") + unitCount + (unitCount == 1 ?
				" Compilation Unit)" :
				" Compilation Units)") + " (" + Util.toTime(now) + ")");
		log("");
		
		now = System.nanoTime();
		
		// Apply states
		if (debug)
		{
			log("Applying " + phases + (phases == 1 ? " Phase: " : " Phases: ") + DyvilCompiler.phases);
			for (ICompilerPhase phase : DyvilCompiler.phases)
			{
				now1 = System.nanoTime();
				log("Applying " + phase.getName());
				try
				{
					phase.apply(fileFinder.units);
					now1 = System.nanoTime() - now1;
					log(phase.getName() + " completed (" + Util.toTime(now1) + ")");
				}
				catch (Throwable t)
				{
					log(phase.getName() + " failed!");
					error(phase.getName(), "apply", t);
					log("");
					log("Compilation FAILED (" + Util.toTime(System.nanoTime() - now) + ")");
					System.exit(1);
					return;
				}
			}
		}
		else
		{
			for (ICompilerPhase phase : DyvilCompiler.phases)
			{
				try
				{
					phase.apply(fileFinder.units);
				}
				catch (Throwable t)
				{
					error(phase.getName(), "apply", t);
					log("");
					log("Compilation FAILED (" + Util.toTime(System.nanoTime() - now) + ")");
					System.exit(1);
					return;
				}
			}
		}
		
		now1 = System.nanoTime();
		
		log("");
		
		if (compilationFailed)
		{
			log("Compilation FAILED (" + Util.toTime(now1 - now) + ", Total Running Time: " + Util
					.toTime(now1 - totalTime) + ")");
			System.exit(1);
			return;
		}
		
		log("Compilation finished (" + Util.toTime(now1 - now) + ", Total Running Time: " + Util
				.toTime(now1 - totalTime) + ")");
	}
	
	public static void initLogger()
	{
		File logFile = config.getLogFile();
		if (logFile == null)
		{
			return;
		}

		logger = Logger.getLogger("DYVIL-COMPILER");
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);

		Formatter formatter = new Formatter()
		{
			@Override
			public String format(LogRecord record)
			{
				String message = record.getMessage();
				if (message == null || message.isEmpty())
				{
					return "\n";
				}

				Throwable thrown = record.getThrown();
				StringBuilder builder = new StringBuilder();

				if (DyvilCompiler.debug)
				{
					builder.append('[').append(format.format(new Date(record.getMillis()))).append("] [");
					builder.append(record.getLevel()).append("]: ");
				}
				builder.append(message).append('\n');

				if (thrown != null)
				{
					thrown.printStackTrace(new AppendablePrintStream(builder));
				}
				return builder.toString();
			}
		};

		FileHandler fh;
		try
		{
			fh = new FileHandler(logFile.getAbsolutePath(), true);
			fh.setLevel(Level.ALL);
			fh.setFormatter(formatter);
			logger.addHandler(fh);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private static void loadConfig(String source)
	{
		System.out.println("Loading Configuration File from '" + source + "'");
		CodeFile file = new CodeFile(source);
		config.setConfigFile(file);
		if (!file.exists())
		{
			System.err.println("Configuration File '" + source + "' does not exist");
			return;
		}
		ConfigParser.parse(file.getCode(), config);
	}
	
	public static void processArgument(String arg)
	{
		switch (arg)
		{
		case "compile":
			phases.add(ICompilerPhase.TOKENIZE);
			phases.add(ICompilerPhase.PARSE_HEADER);
			phases.add(ICompilerPhase.RESOLVE_HEADER);
			phases.add(ICompilerPhase.PARSE);
			phases.add(ICompilerPhase.RESOLVE_TYPES);
			phases.add(ICompilerPhase.RESOLVE);
			phases.add(ICompilerPhase.CHECK_TYPES);
			phases.add(ICompilerPhase.CHECK);
			phases.add(ICompilerPhase.COMPILE);
			phases.add(ICompilerPhase.CLEANUP);
			return;
		case "optimize":
			phases.add(ICompilerPhase.FOLD_CONSTANTS);
			constantFolding = 1;
			return;
		case "jar":
			phases.add(ICompilerPhase.CLEAN);
			phases.add(ICompilerPhase.JAR);
			return;
		case "format":
			phases.add(ICompilerPhase.TOKENIZE);
			phases.add(ICompilerPhase.PARSE);
			phases.add(ICompilerPhase.FORMAT);
			return;
		case "clean":
			phases.add(ICompilerPhase.CLEAN);
			return;
		case "print":
			phases.add(ICompilerPhase.PRINT);
			return;
		case "test":
			phases.add(ICompilerPhase.TEST);
			return;
		case "--debug":
			phases.add(ICompilerPhase.PRINT);
			phases.add(ICompilerPhase.TEST);
			debug = true;
			return;
		}
		
		if (arg.startsWith("-o"))
		{
			try
			{
				phases.add(ICompilerPhase.FOLD_CONSTANTS);
				constantFolding = Integer.parseInt(arg.substring(2));
				return;
			}
			catch (Exception ex)
			{
				// Ignore
			}
		}
		if (arg.charAt(0) == '@')
		{
			loadConfig(arg.substring(1));
			return;
		}
		if (arg.startsWith("print["))
		{
			int index = arg.lastIndexOf(']');
			String phase = arg.substring(6, index);
			
			for (ICompilerPhase compilerPhase : phases)
			{
				if (compilerPhase.getName().equalsIgnoreCase(phase))
				{
					phases.add(new PrintPhase(compilerPhase));
					return;
				}
			}
		}
		
		if (!ConfigParser.readProperty(config, arg))
		{
			System.err.println("Invalid Argument '" + arg + "'. Ignoring.");
		}
	}
	
	public static void test()
	{
		String mainType = config.getMainType();
		if (mainType == null)
		{
			return;
		}
		
		File file = new File(config.getOutputDir(), config.getMainType().replace('.', '/') + ".class");
		if (!file.exists())
		{
			DyvilCompiler.log("The Main Type '" + config.getMainType()
					                  + "' does not exist or was not compiled, skipping test.");
			return;
		}
		
		new TestThread().start();
	}
	
	public static void clean()
	{
		File[] files = config.getOutputDir().listFiles();
		if (files == null)
		{
			return;
		}
		
		for (File s : files)
		{
			FileUtils.delete(s);
		}
	}
}
