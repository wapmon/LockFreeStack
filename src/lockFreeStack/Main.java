package lockFreeStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
		AtomicReference<Cell> top = new AtomicReference<Cell>();
		
		public SimpleStack(Cell top){
			this.top.set(top);
		}
	}
	
	public static class AdaptParams{
		int count;
		double factor;
		
		public AdaptParams(int count, double factor){
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
			ArrayList<Character> instructions = initInstructions();
			long startTime = System.currentTimeMillis();
			for(char instruction : instructions){
				this.op = instruction;
				stackOp(this);
			}
			System.out.println("Finished thread " + id + " in " + (System.currentTimeMillis() - startTime));
		}
	}
	
	private enum Direction{
		SHRINK, EXPAND
	}
	
	// TODO: Figure out the correct values for ADAPT_INIT, MAX_COUNT, and how to init AdaptParams
	//and figure collision layer is applied to threads.
	private static final int NUM_THREADS = 8;
	private static final int NUM_OPERATIONS = 5, ADAPT_INIT = NUM_THREADS / 2, MAX_COUNT = NUM_THREADS, MAX_RETRIES = 10 ;
	private static final double MIN_FACTOR = 0.0, MAX_FACTOR = 1.0;
	private static AtomicReferenceArray<ThreadInfo> location = new AtomicReferenceArray<ThreadInfo>(NUM_THREADS);
	private static AtomicIntegerArray collision = new AtomicIntegerArray(NUM_THREADS);
	private static SimpleStack S;
	private static Random random = new Random();
//	private static final Logger logger = Logger.getLogger("MyLog");
	
	
	public static void main(String[] args) {
//		FileHandler fh = null;
//		try {
//			fh = new FileHandler("C:\\Users\\Austin\\workspace\\LockFreeStack\\src\\lockFreeStack\\log.txt");
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}  
//        logger.addHandler(fh);
//        SimpleFormatter formatter = new SimpleFormatter();  
//        fh.setFormatter(formatter);
		ThreadInfo currentThread;
		initStack();
		for(int i = 0; i < NUM_THREADS; i++){
			collision.set(i, -5000);
			currentThread = new ThreadInfo(i, ' ', null, new AdaptParams(0,0.0));
			currentThread.start();
		}
		System.out.println("Made it to the end of main()");
	}

	public static void stackOp(ThreadInfo p){
		if(!tryPerformStackOp(p)){
//			logger.log(Level.FINE,"Stack op failed");
			lesOp(p);
		}
	}

	private static void lesOp(ThreadInfo p) {
		int position, him;
		ThreadInfo q;
		while(true){
			location.set(p.id, p);
			//TODO: This needs to be a getPostion() method that uses Threadinfo.factor to
			//create a collision layer for collision array
			position = random.nextInt(NUM_THREADS - 1);
			him = collision.get(position);
			while(!collision.compareAndSet(position, him, p.id)){
				him = collision.get(position);
			}
			if(him != -5000){
				q = location.get(him);
				if(q != null && q.id == him && q.op != p.op){
					if(location.compareAndSet(p.id, p, null)){
						if(tryCollision(p, q, him)){
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
//			System.out.println("Calling delay");
			if(!delay(p)){
//				logger.log(Level.INFO, "Delay has failed, calling adaptWith()");
				adaptWidth(Direction.SHRINK, p);
			}
			if(!location.compareAndSet(p.id, p, null)){
				finishCollision(p);
				break;
			}
			
			if(tryPerformStackOp(p)){
				break;
			}
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
		if(p.op == '-'){
//			if(p.cell == null || location.get(p.id) == null){
//				System.out.println();
//			}
			p.cell = location.get(p.id).cell;
			location.set(p.id, null);
		}
		
	}

	private static boolean tryCollision(ThreadInfo p, ThreadInfo q, int him) {
		if(p.op == '+'){
			for(int i = (int) ((NUM_THREADS / 2) - ((p.adaptParams.factor * NUM_THREADS) / 2));
					i < ((NUM_THREADS / 2) + ((p.adaptParams.factor * NUM_THREADS) / 2)); i++){
				if(location.compareAndSet(him, q, p)){	
					return true;
				} 
			}
//				logger.log(Level.WARNING, "tryCollision is false");
			adaptWidth(Direction.EXPAND, p);
			return false;
		}
		if(p.op == '-'){
			if(location.compareAndSet(him, q, null)){
				p.cell = q.cell;
				location.set(p.id, null);;
				return true;
			} else{
//				logger.log(Level.WARNING, "tryCollision is false");
				adaptWidth(Direction.EXPAND, p);
				return false;
			}
		}
		return false;
	}

	private static boolean tryPerformStackOp(ThreadInfo p) {
		Cell ptop;
		Cell pnext;
		if(p.op == '+'){
			ptop = S.top.get();
			if(p.cell == null){
				System.out.println("null");
				p.cell = new Cell(null, 'p');
			}
			p.cell.next = ptop;
			if(S.top.compareAndSet(ptop, p.cell)){
				p.cell = new Cell(null, 'p');
				return true;
			}
			else{
//				logger.log(Level.WARNING, "tryPerformStackOp is false");
				return false;
			}
		}
		else if(p.op == '-'){
			ptop = S.top.get();
			
			//stack is empty
			if(ptop == null){
//				p.cell = null;
				return true;
			}
			
			pnext = ptop;
			if(S.top.compareAndSet(ptop, pnext)){
				p.cell = ptop;
				return true;
			}
			else{
//				logger.log(Level.WARNING, "tryPerformStackOp is false");
				return false;
			}
		}
		return false;
	}
	
//	private static boolean compareAndSwap(Cell A, Cell B) {
////		if(A == null || B == null){
////			System.out.println("null");
////		}
//		if((A == null && S.top == null) || S.top.equals(A)){
//			S.top.set(B);;
//			return true;
//		}
//		else{
//			System.out.println("compareAndSwap is false");
//			return false;
//		}
//	}

	
	//TODO: Needs to use Shavit and Zemach technique for diffracting trees
	private static boolean delay(ThreadInfo p){
		boolean retry = true;
		int retries = 0;
		ThreadInfo temp = p;
		while(retry && retries < MAX_RETRIES){
			long startTime = System.currentTimeMillis();
			while(System.currentTimeMillis() - startTime < Math.pow(2, retries)){
			}
			if(!p.equals(temp)){
//				System.out.println("delay Worked");
				return true;
			}
			retries++;
		}
//		System.out.println("delay did not Work");
		return false;
	}
	private static void initStack(){
		Cell currentCell = new Cell(null, '0');
		Cell nextCell;
		for(int i = 0; i < 50; i++){
			nextCell = new Cell(currentCell, (char) ('a' + i));
			currentCell = nextCell;
		}
		S = new SimpleStack(currentCell);
	}
	
	private static ArrayList<Character> initInstructions(){
		ArrayList<Character> instructions = new ArrayList<Character>(NUM_OPERATIONS);
		for(int i = 0; i < NUM_OPERATIONS; i++){
			if(random.nextBoolean()){
				instructions.add('+');
			}else {
				instructions.add('-');
			}
		}
		return instructions;
	}
}
