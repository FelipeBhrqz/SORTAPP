package TDAs;

public class InsertionSort {
    // Método sobre arreglos de chars (puede mantenerse, pero no interfiere con la versión genérica)
    public static void insertionSort(char[] data) {
        int n = data.length;
        for (int k = 1; k < n; k++) {
            char cur = data[k];
            int j = k;
            while (j > 0 && data[j - 1] > cur) {
                data[j] = data[j - 1];
                j--;
            }
            data[j] = cur;
        }
    }

    // Versión genérica para DoublyLinkedList (ascendente por defecto)
    public static <E extends Comparable<E>> void insertionSort(DoublyLinkedList<E> list) {
        insertionSort(list, false);
    }

    // Versión con parámetro descending (false = ascendente)
    public static <E extends Comparable<E>> void insertionSort(DoublyLinkedList<E> list, boolean descending) {
        if (list.getSize() < 2) return;
        NodeDLL<E> header = ReflectAccess.getHeader(list);
        NodeDLL<E> trailer = ReflectAccess.getTrailer(list);
        NodeDLL<E> marker = header.getNext().getNext(); // segundo nodo real
        while (marker != trailer) {
            E cur = marker.getElement();
            NodeDLL<E> walk = marker.getPrevious();
            while (walk != header) {
                int cmp = walk.getElement().compareTo(cur);
                if (descending) cmp = -cmp;
                if (cmp > 0) { // desplazar elemento hacia la derecha
                    walk.getNext().setElement(walk.getElement());
                    walk = walk.getPrevious();
                } else break;
            }
            walk.getNext().setElement(cur);
            marker = marker.getNext();
        }
    }

    // Acceso por reflexión a nodos internos como en otros algoritmos
    private static class ReflectAccess {
        @SuppressWarnings("unchecked")
        static <E> NodeDLL<E> getHeader(DoublyLinkedList<E> list) {
            try {
                java.lang.reflect.Field f = DoublyLinkedList.class.getDeclaredField("header");
                f.setAccessible(true);
                return (NodeDLL<E>) f.get(list);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        @SuppressWarnings("unchecked")
        static <E> NodeDLL<E> getTrailer(DoublyLinkedList<E> list) {
            try {
                java.lang.reflect.Field f = DoublyLinkedList.class.getDeclaredField("trailer");
                f.setAccessible(true);
                return (NodeDLL<E>) f.get(list);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }
}