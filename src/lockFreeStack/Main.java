package lockFreeStack;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class Main {
	
	//////////////////////////////////////////////////////////////
	//////////////////// CLASS DECLARATIONS //////////////////////
	//////////////////////////////////////////////////////////////
	
	
	/**
	* Cells make up the stack, with the "next" cell being the one
	* under the current cell, so the bottom cell in the stack will
	* have a null next cell
	*/
	public static class Cell{
		Cell next;
		char data;
		
		public Cell(Cell next, char data){
			this.next = next;
			this.data = data;
		}
	}
	
	/**
	 * Shared stack object. Very simple, consists only of the top cell,
	 * which points down the stack, similar to a linked list. 
	 */
	public static class SimpleStack{
		AtomicReference<Cell> top = new AtomicReference<Cell>();
		
		public SimpleStack(Cell top){
			this.top.set(top);
		}
	}
	
	/** 
	* Parameters for each thread to
	* determine the size of the collision
	* layer when attempting to collide
	* with another thread
	*/
	public static class AdaptParams{
		int count;
		double factor;
		
		public AdaptParams(int count, double factor){
			this.count = count;
			this.factor = factor;
		}
	}
	
	/**
	 * This class extends Thread class in order to add an id, stack operation, current cell to use
	 * and adapt parameters (not used in sequential implementation). The run method will create
	 * an array of instructions for each thread, then go through each instruction on the stack until the
	 * entire array has been performed
	 */
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
			for(char instruction : instructions){
				this.op = instruction;
				stackOp(this);
			}
		}
	}
	
	private enum Direction{
		SHRINK, EXPAND
	}
	
	
	//////////////////////////////////////////////////////////////
	////////////////// VARIABLE DECLARATIONS /////////////////////
	//////////////////////////////////////////////////////////////
	
	
	//final integers for number of threads to use and the number of operations
	//for EACH thread to perform, along with initial value for adaptParameters
	private static final int NUM_THREADS = 8;
	private static final int NUM_OPERATIONS = 500000, ADAPT_INIT = NUM_THREADS / 2, MAX_COUNT = NUM_THREADS, MAX_RETRIES = 3;
	private static final double MIN_FACTOR = 0.0, MAX_FACTOR = 1.0;
	
	//Atomic references and variables to ensure correctness where compareAndSwaps are used
	private static AtomicReferenceArray<ThreadInfo> location = new AtomicReferenceArray<ThreadInfo>(NUM_THREADS);
	private static AtomicIntegerArray collision = new AtomicIntegerArray(NUM_THREADS);
	
	//Create an empty stack to begin
	private static SimpleStack S = new SimpleStack(null);
	
	//Random generator for operation distribution and position allocation
	private static Random random = new Random();
	
	
	public static void main(String[] args) {
		ThreadInfo currentThread;
		//create and start the threads, each with 500,000 operations
		for(int i = 0; i < NUM_THREADS; i++){
			collision.set(i, -5000);
			currentThread = new ThreadInfo(i, ' ', null, new AdaptParams(0,0.0));
			currentThread.start();
		}
	}
	
	/**
	 * attempts to perform a stack op normally and upon failure
	 * will send the thread to the collision layer.
	 * 
	 * @param p The thread to performa an operation
	 */
	public static void stackOp(ThreadInfo p){
		if(!tryPerformStackOp(p)){
			lesOp(p);
		}
	}
	
	/**
	 * Repeatedly tries to collide with another thread or perform the
	 * stack op normally until one of them is successful
	 * @param p
	 */
	private static void lesOp(ThreadInfo p) {
		int position, him;
		ThreadInfo q;
		while(true){
			location.set(p.id, p);
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
			if(!delay(p)){
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

	/**
	 * dynamically updates the size of a threads collision layer
	 * while attempting to make a collision with another thread
	 * 
	 * @param dir whether to make the factor larger or smaller
	 * @param p the thread who's adaptParameters are being adjusted
	 */
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

	/**
	 * finishes a collision by clearing entry in location array
	 * and updating the thread's cell.
	 * 
	 * @param p
	 */
	private static void finishCollision(ThreadInfo p) {
		if(p.op == '-'){
			if(location.get(p.id) != null){
				p.cell = location.get(p.id).cell;
			}
			location.set(p.id, null);
		}
		
	}

	/**
	 * makes an attempt to collide 2 threads by checking the collision layer,
	 * and adapts the collision size if it fails. 
	 * @param p
	 * @param q
	 * @param him
	 * @return
	 */
	private static boolean tryCollision(ThreadInfo p, ThreadInfo q, int him) {
		if(p.op == '+'){
			for(int i = (int) ((NUM_THREADS / 2) - ((p.adaptParams.factor * NUM_THREADS) / 2));
					i < ((NUM_THREADS / 2) + ((p.adaptParams.factor * NUM_THREADS) / 2)); i++){
				if(location.compareAndSet(i, q, p)){	
					return true;
				} 
			}
			adaptWidth(Direction.EXPAND, p);
			return false;
		}
		if(p.op == '-'){
			for(int i = (int) ((NUM_THREADS / 2) - ((p.adaptParams.factor * NUM_THREADS) / 2));
					i < ((NUM_THREADS / 2) + ((p.adaptParams.factor * NUM_THREADS) / 2)); i++){
				if(location.compareAndSet(i, q, null)){
					p.cell = q.cell;
					location.set(p.id, null);;
					return true;
				}		
			}
				adaptWidth(Direction.EXPAND, p);
				return false;
		}
		return false;
	}

	/**
	 * Makes an attempt to perform an operation without interacting
	 * with any other threads. Will return if it is a success or failure,
	 * dictating where the thread will go next
	 * @param p
	 * @return
	 */
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
				return false;
			}
		}
		else if(p.op == '-'){
			ptop = S.top.get();
			
			//stack is empty
			if(ptop == null){
				return true;
			}
			
			pnext = ptop.next;
			if(S.top.compareAndSet(ptop, pnext)){
				p.cell = ptop;
				return true;
			}
			else{
				return false;
			}
		}
		return false;
	}
	
	/**
	 * exponential backoff delay used to increase chances
	 * of a successful collision
	 * @param p
	 * @return
	 */
	private static boolean delay(ThreadInfo p){
		boolean retry = true;
		int retries = 0;
		ThreadInfo temp = p;
		while(retry && retries < MAX_RETRIES){
			long startTime = System.currentTimeMillis();
			while(System.currentTimeMillis() - startTime < Math.pow(2, retries)){
			}
			if(!p.equals(temp)){
				return true;
			}
			retries++;
		}
		return false;
	}
	
	/**
	 * creates a stack and adds 50 cells to it, in order
	 * to not start with an empty stack. Optional method
	 */
	private static void initStack(){
		Cell currentCell = new Cell(null, '0');
		Cell nextCell;
		for(int i = 0; i < 50; i++){
			nextCell = new Cell(currentCell, (char) ('a' + i));
			currentCell = nextCell;
		}
		S = new SimpleStack(currentCell);
	}
	
	/**
	 * creates a list of 500,000 operations, called once for each thread.
	 * Operations will be distributed based on random generator, and approximate
	 * distribution can be controlled. 
	 * 
	 * @return
	 */
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
