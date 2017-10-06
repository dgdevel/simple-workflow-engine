package swfe;

public interface WorkflowEventListener<V,E> {

	public void onVertexActivate(long id, V payload) throws Exception;

	public boolean canComplete(long id, V payload) throws Exception;

	public void onVertexComplete(long id, V payload) throws Exception;

	public void onEdgeComplete(long from, long to, E payload) throws Exception;

}
