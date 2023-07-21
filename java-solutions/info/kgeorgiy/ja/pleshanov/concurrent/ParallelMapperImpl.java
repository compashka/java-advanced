package info.kgeorgiy.ja.pleshanov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * This class implements of {@link ParallelMapper} interface that allows to apply a function to elements
 * of a given list in parallel.
 *
 * @author Pleshanov Pavel
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> tasks;
    private final List<Thread> threadsList;

    /**
     * Constructs a new instance of {@code ParallelMapperImpl} with a given number of threads.
     *
     * @param threads the number of threads to be used
     * @throws IllegalArgumentException if the number of threads is less than 1
     */
    public ParallelMapperImpl(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("The number of threads must be greater than 0.");
        }
        tasks = new ArrayDeque<>();
        threadsList = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        getNextTask().run();
                    }
                } catch (InterruptedException ignored) {
                }
            });
            threadsList.add(thread);
            thread.start();
        }
    }

    private Runnable getNextTask() throws InterruptedException {
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }

            return tasks.poll();
        }
    }

    /**
     * Applies the specified function to the elements of the given list in parallel.
     *
     * @param f    the function to be applied to the elements
     * @param args the list of arguments to which the function is to be applied
     * @return a list containing the results of applying the function to the elements of the input list
     * @throws InterruptedException if the thread is interrupted while waiting for the tasks to be completed
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        var notCompletedTasks = new NotCompletedTasks(args.size());
        for (int i = 0; i < args.size(); ++i) {
            final int ind = i;
            tasks.add(() -> {
                var r = f.apply(args.get(ind));
                synchronized (result) {
                    result.set(ind, r);
                }
                notCompletedTasks.decrease();
            });
            synchronized (tasks) {
                tasks.notify();
            }
        }
        notCompletedTasks.waits();
        return result;
    }

    /**
     * Interrupts all threads associated with this instance of {@code ParallelMapperImpl}.
     */
    @Override
    public void close() {
        threadsList.forEach(Thread::interrupt);
    }

    private static class NotCompletedTasks {
        private int number;

        private NotCompletedTasks(int number) {
            this.number = number;
        }

        private synchronized void decrease() {
            if (--number == 0) {
                notify();
            }
        }

        private synchronized void waits() throws InterruptedException {
            while (number != 0) {
                wait();
            }
        }
    }
}
