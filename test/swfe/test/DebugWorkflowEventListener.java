package swfe.test;

import java.util.Date;

import swfe.WorkflowEventListener;

public class DebugWorkflowEventListener implements WorkflowEventListener<Object, Object> {

	@Override
	public boolean canComplete(long id, Object payload) throws Exception {
		System.out.println("[" + new Date() + "] canComplete      " + id + "\t\t" + payload);
		return true;
	}

	@Override
	public void onEdgeComplete(long from, long to, Object payload) throws Exception {
		System.out.println("[" + new Date() + "] onEdgeComplete   " + from + "\t"+to+"\t" + payload);
	}

	@Override
	public void onVertexActivate(long id, Object payload) throws Exception {
		System.out.println("[" + new Date() + "] onVertexActivate " + id + "\t\t" + payload);
	}

	@Override
	public void onVertexComplete(long id, Object payload) throws Exception {
		System.out.println("[" + new Date() + "] onVertexComplete " + id + "\t\t" + payload);
	}
}
