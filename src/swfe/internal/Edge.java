package swfe.internal;

import java.io.Serializable;

public class Edge<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	public final long from, to;

	public final T payload;

	public Edge(long from, long to, T payload) {
		this.from = from;
		this.to = to;
		this.payload = payload;
	}
}
