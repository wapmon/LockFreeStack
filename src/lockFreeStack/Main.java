//William Pearigen
//Team 21 - Scalable Lock Free Stack Algorithm - Sequential Implementation
//15 February, 2016
//COP 4520 Parallel Processes 

package lockFreeStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
		Cell top;

		public SimpleStack(Cell top){
			this.top = top;
		}
	}

	/** This class is not used in the sequential implementation,
	  	and will be set to null for now **/
	public static class AdaptParams{
		int count;
		double factor;

		public AdaptParams(int count, float factor){
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
			List<Character> instructions = initInstructions();
			long startTime = System.currentTimeMillis();
			for(char instruction : instructions){
				this.op = instruction;
				stackOp(this);
			}
			System.out.println("Finished thread " + id + " in " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds");
		}
	}


	//////////////////////////////////////////////////////////////
	////////////////// VARIABLE DECLARATIONS /////////////////////
	//////////////////////////////////////////////////////////////

	
	//final integers for number of threads to use and the number of operations
	//for EACH thread to perform.
	private static final int NUM_THREADS = 4;
	private static final int NUM_OPERATIONS = 500000;
	//data to put in the cells that are pushed to stack (not important in this case,
	//just used to represent a type of data the stack is holding)
	private static char currentData = 'a';
	private static SimpleStack S = new SimpleStack(null);
	//used to generate list of instructions, randomizing push and pops
	private static Random random = new Random();
	//shared lock object used in synchronized block to make stack operations thread safe
	private static Object lock = new Object();

	
	//////////////////////////////////////////////////////////////
	//////////////////// METHOD DECLARATIONS /////////////////////
	//////////////////////////////////////////////////////////////

	
	public static void main(String[] args) {
		ThreadInfo currentThread;
		initStack();
		for(int i = 0; i < NUM_THREADS; i++){
			currentThread = new ThreadInfo(i, ' ', null, null);
			currentThread.start();
		}
	}

	/**
	 * Performs an operation on the shared SimpleStack S. Does this by entering synchronized
	 * block on Object lock. if its a pop, add the thread's cell to the top of the stack
	 * and point its next cell to the old top. Then a new cell must be created for the thread.
	 * If it is a pop, we simply set the top of the stack to the old top's next cell. 
	 * There is no need to create a new cell for p in this case. This must be in a synch block
	 * so the threads can safely operate on the shared stack object sequentially. 
	 * 
	 * @param p - the current thread that is trying to perform a stack operation
	 */
	public static void stackOp(ThreadInfo p){
		Cell newCell;
		synchronized(lock){
			if(p.op == '+'){
				newCell = p.cell;
				newCell.next = S.top;
				S.top = newCell;
				p.cell = new Cell(null, ++currentData);
			} else{
				if (S.top != null) {
					S.top = S.top.next;
				}
			}
		}
	}

	/**
	 * Adds some cells to the simple stack S so it will not be empty
	 * before we start performing operations on it. This is optional
	 * and can be skipped to start with an empty stack
	 */
	private static void initStack(){
		Cell currentCell = new Cell(null, '0');
		Cell nextCell;
		int i;
		for(i = 0; i < 50; i++){
			nextCell = new Cell(currentCell, (char) ('a' + i));
			currentCell = nextCell;
			S.top = currentCell;
		}
	}

	/**
	 * Creates an array list of char instructions, either '+' or '-' for 
	 * push or pop, respectively. Each thread will call this method to get
	 * an individual and randomly generated unique array, each the length of the desired number
	 * of operations, in this case 500,000.
	 * 
	 * @return a character array of instructions
	 */
	private static List<Character> initInstructions(){
		List<Character> instructions = new ArrayList<Character>();
		for(int i = 0; i < NUM_OPERATIONS; i++){
			if(random.nextDouble() < 0.50){
				instructions.add('+');
			}else {
				instructions.add('-');
			}
		}
		return instructions;
	}
}
