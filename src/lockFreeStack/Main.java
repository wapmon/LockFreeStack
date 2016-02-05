package lockFreeStack;

public class Main {
	
	public static class Cell{
		Cell next;
		Object data;
		
		public Cell(Cell next, Object data){
			this.next = next;
			this.data = data;
		}
	}
	
	public static class SimpleStack{
		Cell top;
		
		public SimpleStack(Cell top){
			this.top = top;
		}
	}
	
	public static class AdaptParams{
		int count;
		float factor;
		
		public AdaptParams(int count, float factor){
			this.count = count;
			this.factor = factor;
		}
	}
	
	public static class ThreadInfo{
		int id;
		char op;
		Cell cell;
		AdaptParams adaptParams;
		
		public ThreadInfo(int id, char op, Cell cell, AdaptParams adaptParams){
			this.id = id;
			this.cell = cell;
			this.op = op;
			this.adaptParams = adaptParams;
		}
	}
	
	private static final int NUM_THREADS = 4;
	private static final int NUM_OPERATIONS = 500000;
	private static Object[] location = new Object[NUM_THREADS];
	private static int[] collision = new int[NUM_OPERATIONS];
	private static SimpleStack S = new SimpleStack(null);
	private static int numOps = 0;
	
	public static void main(String[] args) {
		
	}

	public void stackOp(ThreadInfo p){
		if(!tryPerformStackOp(p)){
			lesOp(p);
		}
	}

	private void lesOp(ThreadInfo p) {
		while(numOps < NUM_OPERATIONS){
			location[p.id] = p;
		}
	}

	private boolean tryPerformStackOp(ThreadInfo p) {
		Cell ptop, pnext;
		if(p.op == '+'){
			ptop = S.top;
			p.cell.next = ptop;
			return compareAndSwap(S.top, ptop, p.cell);
		}
		if(p.op == '-'){
			ptop = S.top;
			if(ptop == null){
				p.cell = null;
				return true;
			}
			pnext = ptop.next;
			if(compareAndSwap(S.top, ptop, pnext)){
				p.cell = ptop;
				return true;
			}
			else{
				return false;
			}
		}
		return false;
	}
	
	private boolean compareAndSwap(Cell V, Cell A, Cell B){
		if(A.equals(V)){
			V = B;
			return true;
		}
		else{
			return false;
		}
	}
}
