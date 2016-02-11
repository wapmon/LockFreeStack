package lockFreeStack;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

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
		double factor;
		
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
			this.cell = cell == null ? new Cell(null, 'T') : cell;
			this.op = op;
			this.adaptParams = adaptParams;
		}
		
		public void run(){
			while(numOps < NUM_OPERATIONS){
				stackOp(this);
				this.op = instructions.get(0);
				instructions.remove(0);
				numOps++;
			}
			System.out.println("Finished thread " + id);
		}
	}
	
	
	// TODO: Figure out the correct values for ADAPT_INIT, MAX_COUNT, MAX_FACTOR, and MIN_FACTOR
	//and figure our how the delay(spin) should work
	private static final int NUM_THREADS = 4;
	private static final int NUM_OPERATIONS = 50000;
	private static SimpleStack S = new SimpleStack(null);
	private static int numOps = 0;
	private static Random random = new Random();
	private static ArrayList<Character> instructions = new ArrayList<Character>();
	
	public static void main(String[] args) {
		ThreadInfo currentThread;
		initStack();
		initInstructions();
		for(int i = 0; i < NUM_THREADS; i++){
			currentThread = new ThreadInfo(i, instructions.get(i), null, null);
			currentThread.start();
		}
		System.out.println("Made it to the end of main()");
	}

	public static void stackOp(ThreadInfo p){
//		if(!tryPerformStackOp(p)){
//			lesOp(p);
//		}
		p.cell = S.top.next;
	}
//
//	private static void lesOp(ThreadInfo p) {
//		int position;
//		ThreadInfo q;
//		while(true){
//			location[p.id] = p;
//			position = random.nextInt(NUM_THREADS - 1);
//			him = collision[position];
//			while(!compareAndSwap(collision[position], him, p.id)){
//				him = collision[position];
//			}
//			if(him != -5000){
//				q = location[him];
//				if(q != null && q.id == him && q.op != p.op){
//					if(compareAndSwap(location[p.id], p, null)){
//						if(tryCollision(p, q)){
//							break;
//						}
//						else{
//							if(tryPerformStackOp(p)){
//								break;
//							}
//						}
//					} else{
//						finishCollision(p);
//						break;
//					}
//				}
//			}
//		}
//		delay(p);
//		adaptWidth(Direction.SHRINK, p);
//		if(!compareAndSwap(location[p.id], p, null)){
//			finishCollision(p);
//			return;
//		}
//		
//		if(tryPerformStackOp(p)){
//			return;
//		}
//	}
//
//	private static void adaptWidth(Direction dir, ThreadInfo p) {
//		if(dir.equals(Direction.SHRINK)){
//			if(p.adaptParams.count > 0){
//				p.adaptParams.count--;
//			} else{
//				p.adaptParams.count = ADAPT_INIT;
//				p.adaptParams.factor = (p.adaptParams.factor / 2) > MIN_FACTOR ? (p.adaptParams.factor / 2) : MIN_FACTOR;
//			}
//		}
//		else if(p.adaptParams.count < MAX_COUNT){
//			p.adaptParams.count++;
//		}
//		else{
//			p.adaptParams.count = ADAPT_INIT;
//			p.adaptParams.factor = (2 * p.adaptParams.factor) < MAX_FACTOR ? (2 * p.adaptParams.factor) : MAX_FACTOR;
//		}
//		
//	}
//
//	private static void finishCollision(ThreadInfo p) {
//		if(p.op == '+'){
//			p.cell = location[p.id].cell;
//			location[p.id] = null;
//		}
//		
//	}
//
//	private static boolean tryCollision(ThreadInfo p, ThreadInfo q) {
//		if(p.op == '+'){
//			if(compareAndSwap(location[him],q,p)){
//				return true;
//			} else{
//				adaptWidth(Direction.EXPAND, p);
//				System.out.println("tryCollision is false");
//				return false;
//			}
//		}
//		if(p.op == '-'){
//			if(compareAndSwap(location[him], q, null)){
//				p.cell = q.cell;
//				location[p.id] = null;
//				return true;
//			} else{
//				adaptWidth(Direction.EXPAND, p);
//				System.out.println("tryCollision is false");
//				return false;
//			}
//		}
//		return false;
//	}
//
//	private static boolean tryPerformStackOp(ThreadInfo p) {
//		Cell ptop, pnext;
//		if(p.op == '+'){
//			ptop = S.top;
//			p.cell.next = ptop;
//			return compareAndSwap(S.top, ptop, p.cell);
//		}
//		if(p.op == '-'){
//			ptop = S.top;
//			if(ptop == null){
//				p.cell = null;
//				return true;
//			}
//			pnext = ptop.next;
//			if(compareAndSwap(S.top, ptop, pnext)){
//				p.cell = ptop;
//				return true;
//			}
//			else{
//				System.out.println("tryPerformStackOp is false");
//				return false;
//			}
//		}
//		System.out.println("tryPerformStackOp is false");
//		return false;
//	}
//	
//	private static boolean compareAndSwap(Object V, Object A, Object B){
//		if(A.equals(V)){
//			V = B;
//			return true;
//		}
//		else{
//			System.out.println("compareAndSwap is false");
//			return false;
//		}
//	}
//	
//	public static boolean delay(ThreadInfo p){
//		boolean retry = true;
//		int retries = 0;
//		ThreadInfo temp = p;
//		while(retry && retries < MAX_RETRIES){
//			try {
//				Thread.sleep((long) Math.pow(2, retries));
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			if(p.equals(temp)){
//				return true;
//			}
//		}
//		return false;
//	}
	
	
	private static void initStack(){
		Cell currentCell = new Cell(null, '0');
		Cell nextCell;
		for(int i = 0; i < 50; i++){
			nextCell = new Cell(currentCell, (char) ('a' + i));
			currentCell = nextCell;
			S.top = currentCell;
		}
	}
	
	private static void initInstructions(){
		for(int i = 0; i < 520000; i++){
			if(random.nextBoolean()){
				instructions.add('+');
			}else {
				instructions.add('-');
			}
		}
	}
}
