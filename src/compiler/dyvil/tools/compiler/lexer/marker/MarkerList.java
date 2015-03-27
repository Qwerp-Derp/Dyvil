package dyvil.tools.compiler.lexer.marker;

import java.util.*;

import dyvil.collections.ArrayIterator;
import dyvil.tools.compiler.lexer.position.ICodePosition;
import dyvil.tools.compiler.util.I18n;

public final class MarkerList implements Iterable<Marker>
{
	public static enum MarkerType
	{
		IGNORE, INFO, WARNING, ERROR;
		
		static Map<String, MarkerType>	map	= new HashMap();
	}
	
	public static class MarkerComparator implements Comparator<Marker>
	{
		public static final MarkerComparator	instance	= new MarkerComparator();
		
		private MarkerComparator()
		{
		}
		
		@Override
		public int compare(Marker o1, Marker o2)
		{
			int start1 = o1.position.startIndex();
			int start2 = o2.position.startIndex();
			
			return start1 == start2 ? 0 : start1 < start2 ? -1 : 0;
		}
	}
	
	private Marker[]	markers;
	private int			markerCount;
	
	private int			warnings;
	private int			errors;
	
	public MarkerList()
	{
		this.markers = new Marker[1];
	}
	
	public static MarkerType getMarkerType(String key)
	{
		MarkerType m = MarkerType.map.get(key);
		if (m == null)
		{
			switch (I18n.getString("marker." + key))
			{
			case "info":
				m = MarkerType.INFO;
				break;
			case "warning":
				m = MarkerType.WARNING;
				break;
			case "ignore":
				m = MarkerType.IGNORE;
				break;
			default:
				m = MarkerType.ERROR;
			}
			
			MarkerType.map.put(key, m);
		}
		return m;
	}
	
	public int size()
	{
		return this.markerCount;
	}
	
	public int getErrors()
	{
		return this.errors;
	}
	
	public int getWarnings()
	{
		return this.warnings;
	}
	
	public boolean isEmpty()
	{
		return this.markerCount == 0;
	}
	
	public void sort()
	{
		Arrays.sort(this.markers, MarkerComparator.instance);
	}
	
	public void add(Marker marker)
	{
		if (marker.isError())
		{
			this.errors++;
		}
		if (marker.isWarning())
		{
			this.warnings++;
		}
		
		this.add_(marker);
	}
	
	private void add_(Marker marker)
	{
		int index = this.markerCount++;
		if (index >= this.markers.length)
		{
			Marker[] temp = new Marker[this.markerCount];
			System.arraycopy(this.markers, 0, temp, 0, this.markers.length);
			this.markers = temp;
		}
		this.markers[index] = marker;
	}
	
	public void add(ICodePosition position, String key)
	{
		this.create(position, key);
	}
	
	public void add(ICodePosition position, String key, Object... args)
	{
		this.create(position, key, args);
	}
	
	public Marker create(ICodePosition position, String key)
	{
		MarkerType type = getMarkerType(key);
		Marker marker;
		switch (type)
		{
		case ERROR:
			this.errors++;
			marker = new SemanticError(position, I18n.getString(key));
			break;
		case INFO:
			marker = new Info(position, I18n.getString(key));
			break;
		case WARNING:
			this.warnings++;
			marker = new Warning(position, I18n.getString(key));
			break;
		case IGNORE:
			return IgnoredMarker.instance;
		default:
			marker = null;
		}
		this.add_(marker);
		return marker;
	}
	
	public Marker create(ICodePosition position, String key, Object... args)
	{
		MarkerType type = getMarkerType(key);
		Marker marker;
		switch (type)
		{
		case ERROR:
			this.errors++;
			marker = new SemanticError(position, I18n.getString(key, args));
			break;
		case INFO:
			marker = new Info(position, I18n.getString(key, args));
			break;
		case WARNING:
			this.warnings++;
			marker = new Warning(position, I18n.getString(key, args));
			break;
		default:
			marker = null;
		}
		this.add_(marker);
		return marker;
	}
	
	@Override
	public Iterator<Marker> iterator()
	{
		return new ArrayIterator<Marker>(this.markers, this.markerCount);
	}
}