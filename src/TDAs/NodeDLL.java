package TDAs;

public class NodeDLL<E> {
    private E element;
    private NodeDLL<E> prev;
    private NodeDLL<E> next;
    public NodeDLL(E e, NodeDLL<E> p, NodeDLL<E> n) {
        element = e;
        prev = p;
        next = n;
    }
    public E getElement(){
        return element;
    }
    public void setElement(E element) {
        this.element = element;
    }
    public NodeDLL<E> getPrevious(){
        return prev;
    }
    public NodeDLL<E> getNext(){
        return next;
    }
    public void setPrevious(NodeDLL<E> p){
        prev = p;
    }
    public void setNext(NodeDLL<E> n){
        next = n;
    }
}
