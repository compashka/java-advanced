package info.kgeorgiy.ja.pleshanov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides methods for performing parallel computations on lists using multiple threads.
 *
 * @author Pleshanov Pavel
 */
public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        this.mapper = null;
    }

    private <T, U, R> R parallelFunc(int threads, List<? extends T> values,
                                     Function<Stream<? extends T>, U> func,
                                     Function<Stream<? extends U>, R> reducer) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("The number of threads must be greater than 0.");
        }

        int blockCapacity = Math.max(1, values.size() / threads);
        int index = 0;
        List<Stream<? extends T>> blocks = new ArrayList<>();

        while (index < values.size()) {
            blocks.add(values.subList(index,
                            index += blockCapacity + (((values.size() - index) % blockCapacity == 0) ? 0 : 1))
                    .stream());
        }
        List<U> result;
        if (mapper != null) {
            result = mapper.map(func, blocks);
        } else {
            try (ParallelMapper temporaryMapper = new ParallelMapperImpl(blocks.size())) {
                result = temporaryMapper.map(func, blocks);
            }
        }
        return reducer.apply(result.stream());
    }

    /**
     * Concatenates the string representations of the given values using the specified number of threads.
     *
     * @param threads the number of threads to use for the parallel execution
     * @param values  the list of values to join
     * @return the concatenated string representation of the values
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelFunc(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Returns a list containing only the elements from the input list that satisfy the given predicate.
     *
     * @param threads   the number of threads to use for the parallel execution
     * @param values    the list of values to filter
     * @param predicate the predicate to apply to each element
     * @return a list containing only the elements from the input list that satisfy the given predicate
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelFunc(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Returns a new list containing the results of applying the given function to each element of the original list.
     *
     * @param threads the number of threads to use for the parallel execution
     * @param values  the list of values to filter
     * @param f       the function to apply to each element of the input list
     * @return a new list containing the results of applying the given function to each element of the input list
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelFunc(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Finds the maximum element in the list using the given comparator in parallel.
     *
     * @param threads    the number of threads to use for the parallel execution
     * @param values     the list of values to filter
     * @param comparator the comparator to be used for finding the maximum element
     * @return the maximum element in the list according to the given comparator
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelFunc(threads, values,
                stream -> stream.max(comparator).orElseThrow(),
                stream -> stream.max(comparator).orElseThrow());
    }

    /**
     * Finds the minimum element in the list using the given comparator in parallel.
     *
     * @param threads    the number of threads to use for the parallel execution
     * @param values     the list of values to filter
     * @param comparator the comparator to be used for finding the minimum element
     * @return the minimum element in the list according to the given comparator
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Returns {@code true} if the predicate matches all elements.
     *
     * @param threads   the number of threads to use for the parallel execution
     * @param values    the list of values to filter
     * @param predicate the predicate to apply to each element
     * @return {@code true} if the predicate matches all elements, {@code false} otherwise
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelFunc(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::valueOf));
    }

    /**
     * Returns {@code true} if the predicate matches any elements.
     *
     * @param threads   the number of threads to use for the parallel execution
     * @param values    the list of values to filter
     * @param predicate the predicate to apply to each element
     * @return {@code true} if the predicate matches any elements, {@code false} otherwise
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    /**
     * Returns the number of elements in the specified list that satisfy the given predicate.
     *
     * @param threads   the number of threads to use for the parallel execution
     * @param values    the list of values to filter
     * @param predicate the predicate to apply to each element
     * @return the number of elements in the list that satisfy the predicate
     * @throws InterruptedException if any thread is interrupted during the execution of this method
     */
    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelFunc(threads, values,
                stream -> (int) stream.filter(predicate).count(),
                stream -> stream.mapToInt(Integer::valueOf).sum());
    }
}
