package swfe.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;
import swfe.Engine;

public class EngineTest extends TestCase {

	public void testDoubleId() {
		try {
			Engine<Object, Object> e =
				new Engine<>()
				.start(1, null)
				.start(1, null);
			throw new RuntimeException();
		} catch (IllegalArgumentException e) {}
	}

	public void testStrictMode() {
		assertFalse(
			new Engine<>()
				.setStrict()
				.start(1, null)
				.start(2, null)
				.canComplete());
	}

	public void testStraightPath() {
		new Engine<Object, Object>()
		.start(1, null)
		.define(2, null)
		.end(3, null)
		.link(1, 2, null)
		.link(2, 3, null)
		.complete(new DebugWorkflowEventListener());
	}

	public void testMultiPath() {
		new Engine<Object, Object>()
		.start(1, null)
		.define(2, null)
		.define(3, null)
		.end(4, null)
		.link(1, 2, null)
		.link(1, 3, null)
		.link(2, 4, null)
		.link(3, 4, null)
		.complete(new DebugWorkflowEventListener());
	}

	public void testMultiStart() {
		new Engine<Object, Object>()
		.start(1, "v1")
		.start(2, "v2")
		.define(3, "v3")
		.link(1, 3, "e13")
		.link(2, 3, "e23")
		.end(4, "v4")
		.link(3, 4, "e34")
		.define(5, "v5")
		.link(3, 5, "e35")
		.end(6, "v6")
		.link(5, 6, "e56")
		.start(7, "v7")
		.end(8, "v8")
		.link(7, 8, "e78")
		.complete(new DebugWorkflowEventListener());
	}

	public void testSerialization() throws Exception {
		Engine<Object,Object> e = new Engine<>()
		.start(1, null)
		.end(2, null)
		.link(1, 2, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new ObjectOutputStream(out).writeObject(e);
		byte[] data = out.toByteArray();
		Engine<Object, Object> e2 = (Engine<Object, Object>) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
		e2.complete(new DebugWorkflowEventListener());
	}
}
