package lockFreeStack;

import java.util.ArrayList;
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
			this.cell = cell;
			this.op = op;
			this.adaptParams = adaptParams;
		}
		
		public void run(){
			stackOp(this);
			System.out.println("Finished thread " + id);
		}
	}
	
	private enum Direction{
		SHRINK, EXPAND
	}
	
	// TODO: Figure out the correct values for ADAPT_INIT, MAX_COUNT, MAX_FACTOR, and MIN_FACTOR
	//and figure our how the delay(spin) should work
	private static final int NUM_THREADS = 4;
	private static final int NUM_OPERATIONS = 500000, ADAPT_INIT = 1, MAX_COUNT = 5, MAX_RETRIES = 3;;
	private static final double MIN_FACTOR = 1.0, MAX_FACTOR = 2.0;
	private static ThreadInfo[] location = new ThreadInfo[NUM_THREADS];
	private static int[] collision = new int[NUM_THREADS];
	private static SimpleStack S = new SimpleStack(null);
	private static int numOps = 0, him = -5000;
	private static Random random = new Random();
	private static Character[] instructions = new Character[NUM_OPERATIONS];
	
	public static void main(String[] args) {
		ThreadInfo currentThread;
		initStack();
		initInstructions();
		for(int i = 0; i < NUM_THREADS; i++){
			currentThread = new ThreadInfo(i, instructions[i], S.top, null);
			currentThread.start();
		}
		System.out.println("Made it to the end of main()");
	}

	public static void stackOp(ThreadInfo p){
		if(!tryPerformStackOp(p)){
			lesOp(p);
		}
	}

	private static void lesOp(ThreadInfo p) {
		int position;
		ThreadInfo q;
		while(true){
			location[p.id] = p;
			position = random.nextInt(NUM_THREADS - 1);
			him = collision[position];
			while(!compareAndSwap(collision[position], him, p.id)){
				him = collision[position];
			}
			if(him != -5000){
				q = location[him];
				if(q != null && q.id == him && q.op != p.op){
					if(compareAndSwap(location[p.id], p, null)){
						if(tryCollision(p, q)){
							break;
						}
						else{
							if(tryPerformStackOp(p)){
								break;
							}
						}
					} else{
						finishCollision(p);
						break;
					}
				}
			}
		}
		delay(p);
		adaptWidth(Direction.SHRINK, p);
		if(!compareAndSwap(location[p.id], p, null)){
			finishCollision(p);
			return;
		}
		
		if(tryPerformStackOp(p)){
			return;
		}
	}

	private static void adaptWidth(Direction dir, ThreadInfo p) {
		if(dir.equals(Direction.SHRINK)){
			if(p.adaptParams.count > 0){
				p.adaptParams.count--;
			} else{
				p.adaptParams.count = ADAPT_INIT;
				p.adaptParams.factor = (p.adaptParams.factor / 2) > MIN_FACTOR ? (p.adaptParams.factor / 2) : MIN_FACTOR;
			}
		}
		else if(p.adaptParams.count < MAX_COUNT){
			p.adaptParams.count++;
		}
		else{
			p.adaptParams.count = ADAPT_INIT;
			p.adaptParams.factor = (2 * p.adaptParams.factor) < MAX_FACTOR ? (2 * p.adaptParams.factor) : MAX_FACTOR;
		}
		
	}

	private static void finishCollision(ThreadInfo p) {
		if(p.op == '+'){
			p.cell = location[p.id].cell;
			location[p.id] = null;
		}
		
	}

	private static boolean tryCollision(ThreadInfo p, ThreadInfo q) {
		if(p.op == '+'){
			if(compareAndSwap(location[him],q,p)){
				return true;
			} else{
				adaptWidth(Direction.EXPAND, p);
				return false;
			}
		}
		if(p.op == '-'){
			if(compareAndSwap(location[him], q, null)){
				p.cell = q.cell;
				location[p.id] = null;
				return true;
			} else{
				adaptWidth(Direction.EXPAND, p);
				return false;
			}
		}
		return false;
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
	
	public static boolean delay(ThreadInfo p){
		boolean retry = true;
		int retries = 0;
		ThreadInfo temp = p;
		while(retry && retries < MAX_RETRIES){
			try {
				Thread.sleep((long) Math.pow(2, retries));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(p.equals(temp)){
				return true;
			}
		}
		return false;
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
	
	private static Character[] initInstructions(){
		for(int i = 0; i < 500000; i++){
			if(i < 250000){
				instructions[i] = '+';
			}else {
				instructions[i] = '-';
			}
		}
		return instructions;
	}
}
