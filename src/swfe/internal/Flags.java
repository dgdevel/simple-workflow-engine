package swfe.internal;

public class Flags {

	public static boolean isset(int flags, int mask) {
		return (flags & mask) == mask;
	}

	public static int set(int flags, int mask) {
		return flags | mask;
	}

	public static int unset(int flags, int mask) {
		return flags & ~mask;
	}

	public static int toggle(int flags, int mask) {
		return flags ^ mask;
	}

	public static int fromBin(String spec) {
		return Integer.parseInt(spec, 2);
	}

}
