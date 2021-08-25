package pack_and_encrypt;

//栈结�?
public class Stack<T> {

    Stack(int aSize){
        size = aSize;
        obj = new Object[aSize];
    }

    //栈数�?
    private Object[] obj;

    //指向栈顶�?-1表示为空
    private int top = -1;

    //栈可以容纳的数量
    private int size;

    //判断栈是否为�?
    boolean isEmpty(){
        if(top == -1){
            return true;
        }
        return false;
    }

    //判断栈是�?
    boolean isFull(){
        if(top == size - 1){
            return true;
        }
        return false;
    }

    //清空�?
    public void clear(){
        for (int i = 0;i <= top;i++) {
            obj[i] = null;
        }
        top = -1;
    }

    //数据入栈
    boolean push(T data){
        if(this.isFull()){
            return false;
        }
        this.obj[++top] = data;
        return true;
    }

    //数据出栈
    @SuppressWarnings("unchecked")
    T pop(){
        if(this.isEmpty()){
            return null;
        }
        return (T)this.obj[top--];
    }

    public int getTop(){
        return this.top;
    }

    public int getSize(){
        return this.size;
    }
}
