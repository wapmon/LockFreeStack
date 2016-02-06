package lockFreeStack;

import java.util.Random;

public class Main {
	
	public static class Cell{
		Cell next;
		char data;
		
		public Cell(Cell next, char data){
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
	
	public static class ThreadInfo extends Thread{
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
	private static int[] collision = new int[NUM_THREADS];
	private static SimpleStack S = new SimpleStack(null);
	private static int numOps = 0;
	private static Random random = new Random();
	
	public static void main(String[] args) {
		ThreadInfo currentThread;
		initStack();
//		for(int i = 0; i < NUM_THREADS; i++){
//			currentThread = new ThreadInfo(i, S.top.data, S.top, null);
//			currentThread.start();
//		}
	}

	public static void stackOp(ThreadInfo p){
		if(!tryPerformStackOp(p)){
			lesOp(p);
		}
	}

	private static void lesOp(ThreadInfo p) {
		int position, him = -5000;
		while(numOps < NUM_OPERATIONS){
			location[p.id] = p;
			position = random.nextInt(NUM_THREADS - 1);
			him = collision[position];
			while(!compareAndSwap(collision[position], him, p.id)){
				him = collision[position];
			}
			if(him != -5000){
				
			}
		}
	}

	private static boolean tryPerformStackOp(ThreadInfo p) {
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
	
	private static boolean compareAndSwap(Object V, Object A, Object B){
		if(A.equals(V)){
			V = B;
			return true;
		}
		else{
			return false;
		}
	}
	
	private static void initStack(){
		Cell currentCell = new Cell(null, 'c');
		Cell nextCell;
		for(int i = 0; i < 50; i++){
			nextCell = new Cell(null, 'c');
			currentCell.next = nextCell;
			currentCell = nextCell;
			S.top = currentCell;
		}
	}
}
