package swfe;

import static swfe.internal.Flags.fromBin;
import static swfe.internal.Flags.isset;
import static swfe.internal.Flags.set;
import static swfe.internal.Flags.unset;
import static swfe.internal.Vertex.ACTIVE;
import static swfe.internal.Vertex.COMPLETE;
import static swfe.internal.Vertex.END;
import static swfe.internal.Vertex.START;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import swfe.internal.Edge;
import swfe.internal.RuntimeWrapException;
import swfe.internal.Vertex;

public class Engine<E,V> implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<Vertex<V>> vertexes = new LinkedList<>();
	private List<Edge<E>> edges = new LinkedList<>();
	private int flags = 0;

	private static int VALIDATED = fromBin("01");
	private static int STRICT    = fromBin("10");

	// strict mode: only one start and one end
	public Engine<E,V> setStrict() {
		flags = set(flags, STRICT);
		return this;
	}

	private Vertex<V> lookupVertex(final long id) {
		return vertexes.stream().filter((v) -> v.id == id).findFirst().orElse(null);
	}

	private List<Vertex<V>> collect(final int flag) {
		return vertexes.stream().filter((v) -> isset(v.flags, flag)).collect(Collectors.toList());
	}

	private List<Edge<E>> edgesFrom(final long id) {
		return edges.stream().filter((e) -> e.from == id).collect(Collectors.toList());
	}

	private List<Edge<E>> edgesTo(final long id) {
		return edges.stream().filter((e) -> e.to == id).collect(Collectors.toList());
	}

	private Edge<E> edge(final long from, final long to) {
		return edges.stream().filter((e) -> e.from == from && e.to == to).findFirst().orElse(null);
	}

	public Engine<E,V> start(final long id, final V payload) {
		return define(id, payload, START);
	}

	public Engine<E,V> end(final long id, final V payload) {
		return define(id, payload, END);
	}

	private Engine<E,V> define(final long id, final V payload, int vflags) {
		if (lookupVertex(id) != null) {
			throw new IllegalArgumentException("vertex " + id + " exists");
		}
		flags = unset(flags, VALIDATED);
		vertexes.add(new Vertex<V>(id, payload, vflags));
		return this;
	}

	public Engine<E,V> define(final long id, final V payload) {
		return define(id, payload, 0);
	}

	public Engine<E,V> link(final long from, final long to, final E payload) {
		if (edge(from, to) != null) {
			throw new IllegalArgumentException("edge from=" + from + " to=" + to + " already exists");
		}
		flags = unset(flags, VALIDATED);
		edges.add(new Edge<E>(from, to, payload));
		return this;
	}

	public Engine<E, V> unlink(final long from, final long to) {
		for (Iterator<Edge<E>> it = edges.iterator(); it.hasNext(); ) {
			Edge<E> e = it.next();
			if (e.from == from && e.to == to) {
				flags = unset(flags, VALIDATED);
				it.remove();
				return this;
			}
		}
		throw new IllegalArgumentException("edge from=" + from + " to=" + to + " not found");
	}

	public Engine<E, V> undefine(final long id) {
		if (!(edgesFrom(id).isEmpty() && edgesTo(id).isEmpty())) {
			throw new IllegalArgumentException("edge from or to "+id+" exists");
		}
		for (Iterator<Vertex<V>> it = vertexes.iterator(); it.hasNext();) {
			Vertex<V> v = it.next();
			if (v.id == id) {
				flags = unset(flags, VALIDATED);
				it.remove();
				return this;
			}
		}
		return this;
	}

	public boolean canComplete() {
		try {
			validate();
			return true;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	private void validate() {
		if (isset(flags, VALIDATED)) {
			return;
		}
		if (isset(flags, STRICT)) {
			// only one start and one end
			if (collect(START).size() != 1) {
				throw new IllegalStateException("Only one start is allowed in strict mode");
			}
			if (collect(END).size() != 1) {
				throw new IllegalStateException("Only one end is allowed in strict mode");
			}
		}
		// no dead vertexes
		if (vertexes.stream().filter((v) -> edgesFrom(v.id).isEmpty() && edgesTo(v.id).isEmpty()).count() != 0) {
			throw new IllegalStateException("Vertexes exists that are not linked to others");
		}
		// every vertex from start does reach end
		Queue<Long> toAnalyzeQueue = new LinkedList<>(collect(START).stream().map((v) -> v.id).collect(Collectors.toList()));
		HashSet<Long> analyzed = new HashSet<>();
		while (!toAnalyzeQueue.isEmpty()) {
			long id = toAnalyzeQueue.poll();
			if (isset(lookupVertex(id).flags, END)) {
				continue;
			}
			if (analyzed.contains(id)) {
				continue;
			}
			List<Edge<E>> edges = edgesFrom(id);
			if (edges.isEmpty()) {
				throw new IllegalStateException("dead vertex " + id + " is not an end vertex");
			}
			for (Edge<E> e : edges) {
				toAnalyzeQueue.offer(lookupVertex(e.to).id);
			}
			analyzed.add(id);
		}
		flags = set(flags, VALIDATED);
	}

	public boolean hasWorkToDo() {
		return vertexes.stream().filter((v) -> isset(v.flags, ACTIVE) || isset(v.flags, START) && !isset(v.flags, ACTIVE) && !isset(v.flags, COMPLETE)).count() > 0;
	}

	private boolean completeStartingNodes(WorkflowEventListener<V, E> listener) {
		boolean stateChanged = false;
		for (Vertex<V> v : collect(START)) {
			if (!isset(v.flags, ACTIVE) && !isset(v.flags, COMPLETE)) {
				try {
					listener.onVertexActivate(v.id, v.payload);
				} catch (Exception e) {
					throw new RuntimeWrapException(e);
				}
				v.flags = set(v.flags, ACTIVE);
				stateChanged = true;
			}
		}
		return stateChanged;
	}

	private boolean completeActiveVertexes(WorkflowEventListener<V, E> listener) {
		boolean stateChanged = false;
		for (Vertex<V> v : collect(ACTIVE)) {
			boolean canCompleteVertex = false;
			try {
				canCompleteVertex = listener.canComplete(v.id, v.payload);
			} catch (Exception ex) {
				throw new RuntimeWrapException(ex);
			}
			if (canCompleteVertex) {
				try {
					listener.onVertexComplete(v.id, v.payload);
				} catch (Exception ex) {
					throw new RuntimeWrapException(ex);
				}
				v.flags = unset(v.flags, ACTIVE);
				v.flags = set(v.flags, COMPLETE);
				stateChanged = true;
				for (Edge<E> edge : edgesFrom(v.id)) {
					try {
						listener.onEdgeComplete(edge.from, edge.to, edge.payload);
					} catch (Exception ex) {
						throw new RuntimeWrapException(ex);
					}
				}
			}
		}
		return stateChanged;
	}

	private boolean activateVertexes(WorkflowEventListener<V, E> listener) {
		boolean stateChanged = false;
		for (Vertex<V> v : collect(COMPLETE)) {
			for (Edge<E> ptr : edgesFrom(v.id)) {
				Vertex<V> target = lookupVertex(ptr.to);
				if (!isset(target.flags, ACTIVE) && !isset(target.flags, COMPLETE)) {
					boolean canBeActivated = true;
					for (Edge<E> ptrEdge : edgesTo(target.id)) {
						if (!isset(lookupVertex(ptrEdge.from).flags, COMPLETE)) {
							canBeActivated = false;
							break;
						}
					}
					if (canBeActivated) {
						try {
							listener.onVertexActivate(target.id, target.payload);
						} catch (Exception ex) {
							throw new RuntimeWrapException(ex);
						}
						target.flags = set(target.flags, ACTIVE);
						stateChanged = true;
					}
				}
			}
		}
		return stateChanged;
	}

	public Engine<E,V> complete(WorkflowEventListener<V, E> listener) {
		validate();
		// starting nodes
		completeStartingNodes(listener);
		boolean stateChanged = false;
		do {
			stateChanged = false;
			stateChanged = completeActiveVertexes(listener) || stateChanged;
			stateChanged = activateVertexes(listener) || stateChanged;
		} while (stateChanged == true);
		return this;
	}

}
