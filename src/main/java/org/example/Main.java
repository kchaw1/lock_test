package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private final String FILE_PATH = "test.txt";

    public String read() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " read start");
            return Files.readString(Path.of(FILE_PATH));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            System.out.println(Thread.currentThread().getName() + " read end");
            readLock.unlock();
        }
    }

    public void write() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " write start");
            Files.writeString(Path.of(FILE_PATH), Thread.currentThread().getName(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + " write end");
            writeLock.unlock();
        }
    }

    /**
     * result
     * 1. writeLock 은 1개의 스레드에서만 획득가능하기 때문에 start-end,start-end .. 와 같이 짝이 맞는다.
     * 2. readLock 은 여러 스레드에서 동시 접근이 되기에 start 와 end 의 짝이 맞지 않는다. start, start, end, end ...
     * 3. 특정 스레드에서 writeLock 이 걸린 경우 read, write 를 할 수 없다.
     *
     * == result ==
     * pool-1-thread-2 write start
     * pool-1-thread-2 write end
     * pool-1-thread-1 write start
     * pool-1-thread-1 write end
     * pool-1-thread-3 read start
     * pool-1-thread-5 read start
     * pool-1-thread-4 read start
     * pool-1-thread-4 read end
     * pool-1-thread-3 read end
     * pool-1-thread-5 read end
     * pool-1-thread-6 write start
     * pool-1-thread-6 write end
     * pool-1-thread-7 write start
     * pool-1-thread-7 write end
     * pool-1-thread-8 read start
     * pool-1-thread-9 read start
     * pool-1-thread-8 read end
     * pool-1-thread-9 read end
     * pool-1-thread-10 write start
     * pool-1-thread-10 write end
     */
    public static void main(String[] args) {
        Main main = new Main();

        int THREAD_POOL_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_COUNT);

        for (int i = 0; i < THREAD_POOL_COUNT; i++) {
            Random random = new Random();

            int r = random.nextInt() % 2;

            if (r == 0) {
                executor.submit(() -> main.write());
            } else {
                executor.submit(main::read);
            }
        }
        executor.shutdown();
    }
}