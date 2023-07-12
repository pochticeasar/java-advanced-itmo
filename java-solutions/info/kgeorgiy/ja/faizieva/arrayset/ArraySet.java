package info.kgeorgiy.ja.faizieva.arrayset;

import java.util.*;


@SuppressWarnings("unused")
public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> list;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this.list = new ArrayList<>();
        this.comparator = null;
    }

    public ArraySet(Collection<E> list) {
        this(list, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this.list = new ArrayList<>();
        this.comparator = comparator;
    }

    public ArraySet(Collection<E> collection, Comparator<? super E> comparator) {
        this.comparator = comparator;
        TreeSet<E> set = new TreeSet<>(comparator);
        set.addAll(collection);
        this.list = new ArrayList<>(set);
    }


    public ArraySet(List<E> collection, Comparator<? super E> comparator) {
        this.list = collection;
        this.comparator = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        int start = Collections.binarySearch(list, fromElement, comparator);
        int end = Collections.binarySearch(list, toElement, comparator);
        if (comparator != null && comparator.compare(fromElement, toElement) > 0 || comparator == null && start >= end) {
            throw new IllegalArgumentException();
        }
        return subSetImpl(start, end);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        int ind = Collections.binarySearch(list, toElement, comparator);
        return subSetImpl(0, ind);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        int ind = Collections.binarySearch(list, fromElement, comparator);
        return subSetImpl(ind, list.size());
    }

    private ArraySet<E> subSetImpl(int start, int end) {
        final int startBound = start >= 0 ? start : -start - 1;
        final int endBound = end >= 0 ? end : -end - 1;
        return new ArraySet<>(list.subList(startBound, endBound));
    }


    @Override
    public E first() {
        return getElement(0);
    }

    @Override
    public E last() {
        return getElement(list.size() - 1);
    }

    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(list, (E) o, comparator) >= 0;
    }

    private E getElement(int ind) {
        if (!list.isEmpty()) {
            return list.get(ind);
        } else {
            throw new NoSuchElementException();
        }
    }
}
