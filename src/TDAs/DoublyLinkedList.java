package TDAs;

import java.util.Iterator;

public class DoublyLinkedList<E> implements Iterable<E> {
    private NodeDLL<E> header;
    private NodeDLL<E> trailer;
    private int size = 0;
    public DoublyLinkedList() {
        header = new NodeDLL<>(null,null,null);
        trailer = new NodeDLL<>(null, header, null);
        header.setNext(trailer);
        trailer.setPrevious(header);
    }
    public int getSize() {
        return size;
    }
    public boolean isEmpty() {
        return size == 0;
    }
    public E first() {
        if(isEmpty()) return null;
        return header.getNext().getElement();
    }
    public E last() {
        if(isEmpty()) return null;
        return trailer.getPrevious().getElement();
    }
    public void addFirst(E e) {
        addBetween(e, header, header.getNext());
    }
    public void addLast(E e) {
        addBetween(e, trailer.getPrevious(), trailer);
    }
    public E removeFirst(){
        if(isEmpty()) return null;
        return remove(header.getNext());
    }
    public E removeLast(){
        if(isEmpty()) return null;
        return remove(trailer.getPrevious());
    }
    public void addBetween(E e, NodeDLL<E> predecessor, NodeDLL<E> successor) {
        NodeDLL<E> newest = new NodeDLL<>(e, predecessor, successor);
        predecessor.setNext(newest);
        successor.setPrevious(newest);
        size++;
    }
    public E remove(NodeDLL<E> node) {
        NodeDLL<E> predecessor = node.getPrevious();
        NodeDLL<E> successor = node.getNext();
        predecessor.setNext(successor);
        successor.setPrevious(predecessor);
        size--;
        return node.getElement();
    }
    public NodeDLL<E> find(E e) {
        NodeDLL<E> node = header.getNext();
        while(node != trailer && !e.equals(node.getElement())) {
            node = node.getNext();
        }
        return (node != trailer) ? node : null;
    }
    public Iterator<E> iterator() {
        return new DoublyLinkedListIterator<>(this);
    }
}
