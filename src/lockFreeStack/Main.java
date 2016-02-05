package lockFreeStack;

public class Main {
	private static final int NUM_THREADS = 4;
	private static final int NUM_OPERATIONS = 500000;
	private static Object[] location = new Object[NUM_THREADS];
	private static int[] collision = new int[NUM_OPERATIONS];
	
	
	public class Cell{
		Cell next;
		Object data;
		
		public Cell(Cell next, Object data){
			this.next = next;
			this.data = data;
		}
	}
	
	public class SimpleStack{
		Cell top;
		
		public SimpleStack(Cell top){
			this.top = top;
		}
	}
	
	public class AdaptParams{
		int count;
		float factor;
		
		public AdaptParams(int count, float factor){
			this.count = count;
			this.factor = factor;
		}
	}
	
	public class ThreadInfo{
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
