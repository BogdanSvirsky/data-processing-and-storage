import java.util.concurrent.locks.ReentrantLock;

public class SynchronizedList<T> implements Iterable<T> {
    public class Node<K> {
        public final K element;
        public Node<K> next = null, prev = null;
        public final ReentrantLock lock = new ReentrantLock();

        public Node(K element) {
            this.element = element;
        }
    }

    public class Iterator<L> implements java.util.Iterator<L> {
        private Node<L> currNode;

        public Iterator(Node<L> head) {
            this.currNode = head;
            currNode.lock.lock();
        }

        @Override
        public boolean hasNext() {
            return currNode != null;
        }

        @Override
        public L next() {
            if (currNode == null) {
                return null;
            }
            L result = currNode.element;
            ReentrantLock currLock = currNode.lock;
            try {
                if (currNode.next != null) {
                    currNode = currNode.next;
                    currNode.lock.lock();
                } else {
                    currNode = null;
                }
            } finally {
                currLock.unlock();
            }
            return result;
        }
    }

    private final Object headLock = new Object();
    private Node<T> head = null;

    @Override
    public Iterator<T> iterator() {
        synchronized (headLock) {
            return new Iterator<T>(head);
        }
    }

    public void add(T element) {
        Node<T> newNode = new Node<T>(element);
        synchronized (headLock) {
            if (head != null) {
                newNode.next = head;
                head.lock.lock();
                try {
                    head.prev = newNode;
                } finally {
                    head.lock.unlock();
                }
            }
            head = newNode;
        }
    }

    public void swapWithNext(Node<T> currNode) {
        ReentrantLock currLock = currNode.lock;
        currLock.lock();
        try {
            Node<T> nextNode = currNode.next;
            ReentrantLock nextLock = nextNode.lock;
            nextLock.lock();
            try {
                if (currNode.prev != null)
                    currNode.prev.next = nextNode;
                if (nextNode.next != null)
                    nextNode.next.prev = currNode;

                currNode.next = nextNode.next;
                nextNode.prev = currNode.prev;

                currNode.prev = nextNode;
                nextNode.next = currNode;

                synchronized (headLock) {
                    if (head == currNode)
                        head = nextNode;
                }

                System.out.println("Swapped " + currNode.element + " " + nextNode.element);
            } finally {
                nextLock.unlock();
            }
        } finally {
            currLock.unlock();
        }
    }

    public Node<T> getHead() {
        synchronized (headLock) {
            return head;
        }
    }
}
