package info.kgeorgiy.ja.pleshanov.arrayset;

import java.util.*;


public class ReversedList<E> extends AbstractList<E> implements List<E>, RandomAccess {
    private final List<E> elements;

    public ReversedList(final List<E> elements) {
        this.elements = elements;
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public E get(final int index) {
//        index = size() - index - 1;
//        Objects.checkIndex(index, size());
        return elements.get(size() - index - 1);
    }
}
