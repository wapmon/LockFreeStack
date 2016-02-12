package lockFreeStack;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
			while(numOps.incrementAndGet() < NUM_OPERATIONS){
				stackOp(this);
				this.op = instructions.remove(0);
			}
			System.out.println("Finished thread " + id);
		}
	}
	
	
	// TODO: Figure out the correct values for ADAPT_INIT, MAX_COUNT, MAX_FACTOR, and MIN_FACTOR
	//and figure our how the delay(spin) should work
	private static final int NUM_THREADS = 8;
	private static final int NUM_OPERATIONS = 500000;
	private static char currentData = 'a';
	private static SimpleStack S = new SimpleStack(null);
	private static AtomicInteger numOps = new AtomicInteger(0), stackSize = new AtomicInteger(0);
	private static Random random = new Random();
	private static ArrayList<Character> instructions = new ArrayList<Character>();
	private static Object lock = new Object();
	
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
		Cell newCell;
		synchronized(lock){
			if(p.op == '+'){
				newCell = p.cell;
				newCell.next = S.top;
				S.top = newCell;
				p.cell = new Cell(null, ++currentData);
				stackSize.incrementAndGet();
			} else{
				if (S.top != null) {
					S.top = S.top.next;
					stackSize.decrementAndGet();
				}
			}
			System.out.println("stack size = " + stackSize);
		}
		
		
	}
	
	
	private static void initStack(){
		Cell currentCell = new Cell(null, '0');
		Cell nextCell;
		int i;
		for(i = 0; i < 50; i++){
			nextCell = new Cell(currentCell, (char) ('a' + i));
			currentCell = nextCell;
			S.top = currentCell;
		}
		stackSize.set(i + 1);
	}
	
	private static void initInstructions(){
		System.out.println("Started init instructions");
		for(int i = 0; i < NUM_OPERATIONS; i++){
			if(random.nextBoolean()){
				instructions.add('+');
			}else {
				instructions.add('-');
			}
		}
		System.out.println("Finished init instructions");
	}
}
