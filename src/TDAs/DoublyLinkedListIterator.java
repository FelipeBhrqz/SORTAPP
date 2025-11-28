package TDAs;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.lang.reflect.Field;

public class DoublyLinkedListIterator<E> implements Iterator<E> {
    private NodeDLL<E> current;
    private NodeDLL<E> trailer;

    public DoublyLinkedListIterator(DoublyLinkedList<E> list) {
        this.current = getHeader(list).getNext();
        this.trailer = getTrailer(list);
    }
    @SuppressWarnings("unchecked")
    private NodeDLL<E> getHeader(DoublyLinkedList<E> list) {
        try {
            Field field = DoublyLinkedList.class.getDeclaredField("header");
            field.setAccessible(true);
            return (NodeDLL<E>) field.get(list);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("No se pudo acceder al header", e);
        }
    }
    @SuppressWarnings("unchecked")
    private NodeDLL<E> getTrailer(DoublyLinkedList<E> list) {
        try {
            Field field = DoublyLinkedList.class.getDeclaredField("trailer");
            field.setAccessible(true);
            return (NodeDLL<E>) field.get(list);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("No se pudo acceder al trailer", e);
        }
    }
    @Override
    public boolean hasNext() {
        return current != trailer;
    }
    @Override
    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        E element = current.getElement();
        current = current.getNext();
        return element;
    }
}
