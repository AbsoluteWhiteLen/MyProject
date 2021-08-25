package pack_and_encrypt;

/**
 * stack
 * 
 * @author son
 * 
 * @param <T>
 */
public class Stack<T> {

	Stack(int aSize) {
		size = aSize;
		obj = new Object[aSize];
	}

	// obj
	private Object[] obj;

	// top of the current stack
	private int top = -1;

	// size of the stack
	private int size;

	/**
	 * if the stack is empty
	 * 
	 * @return
	 */
	boolean isEmpty() {
		if (top == -1) {
			return true;
		}
		return false;
	}

	/**
	 * if the stack is full
	 * 
	 * @return
	 */
	boolean isFull() {
		if (top == size - 1) {
			return true;
		}
		return false;
	}

	/**
	 * clear
	 */
	public void clear() {
		for (int i = 0; i <= top; i++) {
			obj[i] = null;
		}
		top = -1;
	}

	/**
	 * push a obj to the stack
	 * 
	 * @param obj
	 * @return
	 */
	boolean push(T obj) {
		if (this.isFull()) {
			return false;
		}
		this.obj[++top] = obj;
		return true;
	}

	/**
	 * pop a obj to from stack
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	T pop() {
		if (this.isEmpty()) {
			return null;
		}
		return (T) this.obj[top--];
	}

	/**
	 * getter of top
	 * 
	 * @return
	 */
	public int getTop() {
		return this.top;
	}

	/**
	 * getter of size
	 * 
	 * @return
	 */
	public int getSize() {
		return this.size;
	}
}
