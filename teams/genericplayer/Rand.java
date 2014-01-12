package genericplayer;

/**
 * 
 * @author Rene
 * 
 * Class for handling random integer generation
 * Initialize with a seed using the constructor
 * 
 * next() returns a random integer
 * next(int max) returns a nonnegative integer less than max
 * nextAnd(int n) returns a nonnegative integer bitwise anded with n
 *
 */
public class Rand {
	static final long a = 0xffffda61L;
	private long x;
	
	public Rand(long seed) {
		x = seed & 0xffffffffL;
	}
	
	public int next() {
		x = (a * (x & 0xffffffffL)) + (x >>> 32);
		return (int) x;
	}
	
	public int next(int max) {
		return (next() & 0x7fffffff) % max;
	}
	
	public int nextAnd(int n) {
		return next() & n;
	}
	
}
