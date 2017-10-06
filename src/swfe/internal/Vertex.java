package swfe.internal;

import static swfe.internal.Flags.fromBin;

import java.io.Serializable;

public class Vertex<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	public final long id;

	public final T payload;

	public Vertex(long id, T payload) {
		this.id = id;
		this.payload = payload;
	}

	public int flags = 0;

	public Vertex(long id, T payload, int flags) {
		this(id, payload);
		this.flags = flags;
	}

	public static int ACTIVE   = fromBin("0001");
	public static int COMPLETE = fromBin("0010");
	public static int START    = fromBin("0100");
	public static int END      = fromBin("1000");

}
