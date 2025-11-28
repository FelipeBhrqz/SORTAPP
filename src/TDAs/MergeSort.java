package TDAs;

public class MergeSort {

  public static <E extends Comparable<E>> void mergeSort(DoublyLinkedList<E> list) {
    mergeSort(list, false);
  }

  /**
   * Ordena la lista usando el algoritmo Merge Sort.
   *
   * @param list La lista doblemente enlazada a ordenar.
   * @param descending Si es verdadero, ordena en orden descendente; de lo contrario, en orden ascendente.
   */
  public static <E extends Comparable<E>> void mergeSort(DoublyLinkedList<E> list, boolean descending) {
    int n = list.getSize();
    if (n < 2) return;
    @SuppressWarnings("unchecked")
    E[] arr = (E[]) new Comparable[n];
    int i = 0;
    for (E e : list) arr[i++] = e;
    @SuppressWarnings("unchecked")
    E[] temp = (E[]) new Comparable[n];
    mergeSortArray(arr, temp, 0, n - 1, descending);
    // escribir de vuelta en la lista
    NodeDLL<E> header = ReflectAccess.getHeader(list);
    NodeDLL<E> trailer = ReflectAccess.getTrailer(list);
    NodeDLL<E> cur = header.getNext();
    i = 0;
    while (cur != trailer) {
      cur.setElement(arr[i++]);
      cur = cur.getNext();
    }
  }

  private static <E extends Comparable<E>> void mergeSortArray(E[] arr, E[] temp, int left, int right, boolean desc) {
    if (left >= right) return;
    int mid = left + (right - left) / 2;
    mergeSortArray(arr, temp, left, mid, desc);
    mergeSortArray(arr, temp, mid + 1, right, desc);
    merge(arr, temp, left, mid, right, desc);
  }

  private static <E extends Comparable<E>> void merge(E[] arr, E[] temp, int left, int mid, int right, boolean desc) {
    int i = left, j = mid + 1, k = left;
    while (i <= mid && j <= right) {
      int cmp = arr[i].compareTo(arr[j]);
      if (desc) cmp = -cmp; // invertir para descendente
      if (cmp <= 0) temp[k++] = arr[i++];
      else temp[k++] = arr[j++];
    }
    while (i <= mid) temp[k++] = arr[i++];
    while (j <= right) temp[k++] = arr[j++];
    for (int p = left; p <= right; p++) arr[p] = temp[p];
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

// Nota: mergeSort(list) ordena ascendentemente. Usar mergeSort(list, true) para descendente.