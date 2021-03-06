package dyvil.tools.gensrc.ast;

import dyvil.tools.gensrc.GenSrc;
import dyvil.tools.gensrc.ast.scope.Scope;
import dyvil.tools.gensrc.lang.I18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

public class Specialization implements Scope
{
	public static final String FILE_NAME_PROPERTY    = "@fileName";
	public static final String ENABLED_PROPERTY      = "@enabled";
	public static final String INHERIT_FROM_PROPERTY = "@inheritFrom";
	public static final String BASE_PROPERTY         = "@base";

	private Properties substitutions = new Properties();

	private Specialization parent;

	private final File   sourceFile;
	private final String templateName;
	private final String name;

	private Template template;

	private boolean enabled = true;

	public Specialization(File sourceFile, String templateName)
	{
		this(sourceFile, templateName, "default");
	}

	public Specialization(File sourceFile, String templateName, String specName)
	{
		this.sourceFile = sourceFile;
		this.templateName = templateName;
		this.name = specName;
	}

	public static Specialization createDefault(String templateName)
	{
		Specialization spec = new Specialization(null, templateName);
		spec.substitutions.put(FILE_NAME_PROPERTY, templateName);
		return spec;
	}

	@Override
	public File getSourceFile()
	{
		return this.sourceFile;
	}

	@Override
	public Scope getGlobalParent()
	{
		return null;
	}

	public String getTemplateName()
	{
		return this.templateName;
	}

	public Template getTemplate()
	{
		return this.template;
	}

	public void setTemplate(Template template)
	{
		this.template = template;
	}

	public String getName()
	{
		return this.name;
	}

	public String getFileName()
	{
		return this.getReplacement(FILE_NAME_PROPERTY);
	}

	public boolean isEnabled()
	{
		return this.enabled;
	}

	private boolean isBase()
	{
		return this.getBoolean(BASE_PROPERTY);
	}

	@Override
	public String getReplacement(String key)
	{
		final String sub = this.substitutions.getProperty(key);
		if (sub != null || this.parent == null)
		{
			return sub;
		}
		return this.parent.getReplacement(key);
	}

	public void load(GenSrc gensrc, List<String> markers)
	{
		try (BufferedReader reader = Files.newBufferedReader(this.sourceFile.toPath()))
		{
			this.substitutions.load(reader);
		}
		catch (IOException ignored)
		{
			gensrc.error(I18n.get("spec.file.error"));
		}

		if (!this.isBase())
		{
			if (this.template == null)
			{
				markers.add(I18n.get("spec.unassociated", this.templateName));
				this.enabled = false;
			}
			if (this.getFileName() == null)
			{
				markers.add(I18n.get("spec.fileName.missing"));
				this.enabled = false;
			}
		}
		final String enabled = this.getReplacement(ENABLED_PROPERTY);
		if (enabled != null && !"true".equals(enabled))
		{
			this.enabled = false;
		}

		final String inherited = this.getReplacement(INHERIT_FROM_PROPERTY);
		if (inherited == null)
		{
			return;
		}

		final File specFile = resolveSpecFile(gensrc, inherited, this.getSourceFile());
		final Specialization spec = gensrc.getSpecialization(specFile);
		if (spec == null)
		{
			markers.add(I18n.get("spec.inheritFrom.unresolved", specFile));
			return;
		}

		this.parent = spec;
	}

	public static File resolveSpecFile(GenSrc gensrc, String reference, File sourceFile)
	{
		// If the referenced file starts with '.', it is relative to the parent directory of this spec file
		// Otherwise, it is relative to the source root

		return reference.startsWith(".") ?
			       new File(sourceFile.getParent(), reference) :
			       new File(gensrc.getSourceRoot(), reference);
	}

	public static Specialization resolveSpec(GenSrc gensrc, String reference, File sourceFile)
	{
		return gensrc.getSpecialization(resolveSpecFile(gensrc, reference, sourceFile));
	}
}
