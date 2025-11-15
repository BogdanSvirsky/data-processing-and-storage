import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

class Sorter {
    private static int count = 0;
    private int sorterIndex = ++count;
    final long delay;
    private final Thread executor = new Thread(() -> {
        try {
            sort();
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }
    });
    final SynchronizedList<String> list;
    private boolean isRunning = true;

    public Sorter(long delay, SynchronizedList<String> list) {
        this.delay = delay;
        this.list = list;
    }

    public void start() {
        isRunning = true;
        executor.start();
    }

    public void stopAndWait() throws InterruptedException {
        isRunning = false;
        executor.interrupt();
        executor.join();
    }

    private void sort() throws InterruptedException {
        while (isRunning) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                if (!isRunning)
                    break;
            }
            System.out.println("=== " + sorterIndex + " getting head");

            SynchronizedList<String>.Node<String> currNode = list
                    .getHead(), nextNode = null, tmp;

            if (currNode == null)
                continue;

            System.out.println("=== " + sorterIndex + " start sorting");
            currNode.lock.lock();
            System.out.println("=== " + sorterIndex + " currNode locked");
            try {
                for (; currNode.next != null; currNode = currNode.next) {
                    Thread.sleep(delay);
                    nextNode = currNode.next;
                    ReentrantLock nextLock = nextNode.lock;
                    nextLock.lock();
                    System.out.println("=== " + sorterIndex + " next locked");
                    if (currNode.element.compareTo(nextNode.element) > 0) {
                        list.swapWithNext(currNode);
                        tmp = currNode;
                        currNode = nextNode;
                        nextNode = tmp;
                    }
                    currNode.lock.unlock();
                }
            } finally {
                currNode.lock.unlock();
            }
            System.out.println("=== " + sorterIndex + " end sorting step");
        }

    }
}

public class Main {
    public static void main(String[] args) {
        // System.out.println(func());

        SynchronizedList<String> list = new SynchronizedList<>();

        Sorter sorter = new Sorter(1000, list);
        Sorter sorter1 = new Sorter(2000, list);
        Sorter sorter2 = new Sorter(3000, list);
        sorter.start();
        sorter1.start();
        sorter2.start();

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String newString = scanner.nextLine();
            if (newString.isEmpty()) {
                printList(list);
            } else {
                list.add(newString);
            }
        }
        try {
            sorter.stopAndWait();
            sorter1.stopAndWait();
            sorter2.stopAndWait();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        scanner.close();
    }

    public static void printList(SynchronizedList<String> list) {
        System.out.println("=== Current list state ===");
        int index = 1;
        for (String elem : list) {
            System.out.println(index++ + ": " + elem);
        }
        System.out.println("=====       End      =====");
    }
}
