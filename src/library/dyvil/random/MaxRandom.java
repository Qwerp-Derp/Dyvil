package dyvil.random;

import dyvil.math.MathUtils;

/**
 * A {@link Random} implementation that always returns the maximum value.
 *
 * @author Clashsoft
 */
public final class MaxRandom implements Random
{
	public static final MaxRandom instance = new MaxRandom();
	
	private MaxRandom()
	{
	}
	
	@Override
	public int next(int bits)
	{
		return (1 << bits) - 1;
	}
	
	@Override
	public boolean nextBoolean()
	{
		return true;
	}
	
	@Override
	public boolean nextBoolean(float f)
	{
		return true;
	}
	
	@Override
	public byte nextByte()
	{
		return Byte.MAX_VALUE;
	}
	
	@Override
	public byte nextByte(byte max)
	{
		return (byte) (max - 1);
	}
	
	@Override
	public byte nextByte(byte min, byte max)
	{
		return max;
	}
	
	@Override
	public short nextShort()
	{
		return Short.MAX_VALUE;
	}
	
	@Override
	public short nextShort(short max)
	{
		return (short) (max - 1);
	}
	
	@Override
	public short nextShort(short min, short max)
	{
		return max;
	}
	
	@Override
	public char nextChar()
	{
		return Character.MAX_VALUE;
	}
	
	@Override
	public char nextChar(char max)
	{
		return (char) (max - 1);
	}
	
	@Override
	public char nextChar(char min, char max)
	{
		return max;
	}
	
	@Override
	public int nextInt()
	{
		return Integer.MAX_VALUE;
	}
	
	@Override
	public int nextInt(int max)
	{
		return max - 1;
	}
	
	@Override
	public int nextInt(float f)
	{
		return MathUtils.ceil(f);
	}
	
	@Override
	public int nextInt(int min, int max)
	{
		return max;
	}
	
	@Override
	public long nextLong()
	{
		return 1L;
	}
	
	@Override
	public long nextLong(long max)
	{
		return max - 1;
	}
	
	@Override
	public long nextLong(long min, long max)
	{
		return max - 1;
	}
	
	@Override
	public float nextFloat()
	{
		return 1F;
	}
	
	@Override
	public float nextFloat(float max)
	{
		return max;
	}
	
	@Override
	public float nextFloat(float min, float max)
	{
		return max;
	}
	
	@Override
	public double nextDouble()
	{
		return 1D;
	}
	
	@Override
	public double nextDouble(double max)
	{
		return max;
	}
	
	@Override
	public double nextDouble(double min, double max)
	{
		return max;
	}
	
	@Override
	public synchronized double nextGaussian()
	{
		return 1D;
	}
}
