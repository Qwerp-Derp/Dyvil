package dyvil.random;

/**
 * A {@link Random} implementation that always returns the minimum value.
 *
 * @author Clashsoft
 */
public final class MinRandom implements Random
{
	public static final MinRandom instance = new MinRandom();
	
	private MinRandom()
	{
	}
	
	@Override
	public int next(int bits)
	{
		return 0;
	}
	
	@Override
	public boolean nextBoolean()
	{
		return false;
	}
	
	@Override
	public boolean nextBoolean(float f)
	{
		return false;
	}
	
	@Override
	public byte nextByte()
	{
		return 0;
	}
	
	@Override
	public byte nextByte(byte max)
	{
		return 0;
	}
	
	@Override
	public byte nextByte(byte min, byte max)
	{
		return 0;
	}
	
	@Override
	public short nextShort()
	{
		return 0;
	}
	
	@Override
	public short nextShort(short max)
	{
		return 0;
	}
	
	@Override
	public short nextShort(short min, short max)
	{
		return 0;
	}
	
	@Override
	public char nextChar()
	{
		return 0;
	}
	
	@Override
	public char nextChar(char max)
	{
		return 0;
	}
	
	@Override
	public char nextChar(char min, char max)
	{
		return 0;
	}
	
	@Override
	public int nextInt()
	{
		return 0;
	}
	
	@Override
	public int nextInt(int max)
	{
		return 0;
	}
	
	@Override
	public int nextInt(float f)
	{
		return 0;
	}
	
	@Override
	public int nextInt(int min, int max)
	{
		return min;
	}
	
	@Override
	public long nextLong()
	{
		return 0L;
	}
	
	@Override
	public long nextLong(long max)
	{
		return 0;
	}
	
	@Override
	public long nextLong(long min, long max)
	{
		return min;
	}
	
	@Override
	public float nextFloat()
	{
		return 0F;
	}
	
	@Override
	public float nextFloat(float max)
	{
		return 0F;
	}
	
	@Override
	public float nextFloat(float min, float max)
	{
		return min;
	}
	
	@Override
	public double nextDouble()
	{
		return 0D;
	}
	
	@Override
	public double nextDouble(double max)
	{
		return 0D;
	}
	
	@Override
	public double nextDouble(double min, double max)
	{
		return min;
	}
	
	@Override
	public synchronized double nextGaussian()
	{
		return 0D;
	}
}
