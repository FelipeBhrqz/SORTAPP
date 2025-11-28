package TDAs;

public class BubbleSort {
    public static <E extends Comparable<E>> void bubbleSort(DoublyLinkedList<E> list) {
        bubbleSort(list, false);
    }

    public static <E extends Comparable<E>> void bubbleSort(DoublyLinkedList<E> list, boolean descending) {
        if (list.getSize() < 2) return;
        boolean swapped;
        do {
            swapped = false;
            // Obtener header y trailer por reflección como en DoublyLinkedListIterator
            NodeDLL<E> header = ReflectAccess.getHeader(list);
            NodeDLL<E> trailer = ReflectAccess.getTrailer(list);
            NodeDLL<E> current = header.getNext();
            while (current.getNext() != trailer) {
                NodeDLL<E> next = current.getNext();
                int cmp = current.getElement().compareTo(next.getElement());
                if (descending) cmp = -cmp;
                if (cmp > 0) {
                    E temp = current.getElement();
                    current.setElement(next.getElement());
                    next.setElement(temp);
                    swapped = true;
                }
                current = next;
            }
        } while (swapped);
    }

    // Utilidad interna para acceder a header/trailer sin exponerlos públicamente
    private static class ReflectAccess {
        @SuppressWarnings("unchecked")
        static <E> NodeDLL<E> getHeader(DoublyLinkedList<E> list) {
            try {
                java.lang.reflect.Field f = DoublyLinkedList.class.getDeclaredField("header");
                f.setAccessible(true);
                return (NodeDLL<E>) f.get(list);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        @SuppressWarnings("unchecked")
        static <E> NodeDLL<E> getTrailer(DoublyLinkedList<E> list) {
            try {
                java.lang.reflect.Field f = DoublyLinkedList.class.getDeclaredField("trailer");
                f.setAccessible(true);
                return (NodeDLL<E>) f.get(list);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

// Nota: El método con parámetro 'descending' permite invertir el orden. Por defecto se usará ascendente (descending=false).