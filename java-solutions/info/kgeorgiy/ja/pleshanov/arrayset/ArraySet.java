package info.kgeorgiy.ja.pleshanov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final List<E> elements;
    private final Comparator<? super E> comparator;

    // :NOTE: List.of()
    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(final Collection<? extends E> elements) {
        this(elements, null);
    }

    // :NOTE: not safe
    private ArraySet(final ReversedList<E> listView, final Comparator<? super E> comparator) {
        this.elements = listView;
        this.comparator = comparator;
    }

    public ArraySet(final Collection<? extends E> elements, final Comparator<? super E> comparator) {
        final TreeSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(elements);
        this.elements = new ArrayList<>(treeSet);
        this.comparator = comparator;
    }

    // :NOTE: mutable
    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    // :NOTE: copy-paste
    @Override
    public E lower(final E e) {
        return getElement(e, false, false);
    }

    @Override
    public E floor(final E e) {
        return getElement(e, true, false);
    }

    @Override
    public E ceiling(final E e) {
        return getElement(e, true, true);
    }

    @Override
    public E higher(final E e) {
        return getElement(e, false, true);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }

        return getSubSet(first(), true, toElement, inclusive);
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        // :NOTE: copy-paste
        if (isEmpty()) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }

        return getSubSet(fromElement, inclusive, last(), true);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Invalid range: fromElement should be <= toElement.");
        }
        if (compare(fromElement, toElement) == 0 && !(fromInclusive && toInclusive)) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }

        final int indexFromElement = binarySearch(fromElement, fromInclusive, true);
        final int indexToElement = binarySearch(toElement, toInclusive, false);
        // :NOTE: (indexFromElement > indexToElement) ?
        return new ArraySet<>(elements.subList(indexFromElement, indexToElement + 1), comparator);
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedList<>(elements), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        assertNotEmpty();
        return elements.get(0);
    }

    @Override
    public E last() {
        assertNotEmpty();
        return elements.get(elements.size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return Collections.binarySearch(elements, (E) o, comparator) >= 0;
    }

    @SuppressWarnings("unchecked")
    private int compare(final E a, final E b) {
        return (comparator == null) ? ((Comparable<E>) a).compareTo(b) : comparator.compare(a, b);
    }

    private int binarySearch(final E e, final boolean inclusive, final boolean greater) {
        final int index = Collections.binarySearch(elements, e, comparator);
        final int shift = (index >= 0) ? (inclusive ? 0 : (greater ? 1 : -1)) : (greater ? -1 : -2);
        // :NOTE: abs
        return Math.abs(index) + shift;
    }

    private E getElement(final E e, final boolean inclusive, final boolean greater) {
        final int index = binarySearch(e, inclusive, greater);
        return (0 <= index && index < elements.size()) ? elements.get(index) : null;
    }

    private void assertNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException("ArraySet has not elements.");
        }
    }

    private NavigableSet<E> getSubSet(final E fromElement, final boolean fromInclusive,
                                      final E toElement, final boolean toInclusive) {
        try {
            return subSet(fromElement, fromInclusive, toElement, toInclusive);
        } catch (final IllegalArgumentException e) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }
    }

    public static void main(final String... args) {
        final ArraySet<String> set = new ArraySet<>(List.of("hello", "world"));
        final Iterator<String> it = set.iterator();
        it.next();
        it.remove();
        System.out.println(set);
    }
}
