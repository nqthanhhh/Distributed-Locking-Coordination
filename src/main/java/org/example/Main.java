package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    private static final String LOCK_ROOT = "/locks";
    private static final String LOCK_NODE_PREFIX = LOCK_ROOT + "/lock-";

    private ZooKeeper zooKeeper;
    private String currentLockNode;

    public static void main(String[] args) throws Exception {
        Main demo = new Main();

        demo.connect();
        demo.createLockRootIfNeeded();

        System.out.println("Dang yeu cau khoa phan tan...");
        demo.acquireLock();

        System.out.println("Da lay duoc khoa. Dang xu ly tac vu...");
        Thread.sleep(5000);

        demo.releaseLock();
        System.out.println("Da nha khoa.");

        demo.close();
    }

    public void connect() throws Exception {
        CountDownLatch connectedSignal = new CountDownLatch(1);

        zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
            }
        });

        connectedSignal.await();
        System.out.println("Ket noi ZooKeeper thanh cong.");
    }

    public void createLockRootIfNeeded() throws Exception {
        if (zooKeeper.exists(LOCK_ROOT, false) == null) {
            zooKeeper.create(
                    LOCK_ROOT,
                    "Thu muc chua cac lock".getBytes(StandardCharsets.UTF_8),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT
            );
            System.out.println("Da tao node goc: " + LOCK_ROOT);
        }
    }

    public void acquireLock() throws Exception {
        currentLockNode = zooKeeper.create(
                LOCK_NODE_PREFIX,
                "client dang giu lock".getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );

        while (true) {
            List<String> children = zooKeeper.getChildren(LOCK_ROOT, false);
            children.sort(String::compareTo);

            String smallestNode = LOCK_ROOT + "/" + children.get(0);

            if (currentLockNode.equals(smallestNode)) {
                System.out.println("Lock node hien tai: " + currentLockNode);
                return;
            }

            System.out.println("Chua den luot lay lock, dang cho...");
            Thread.sleep(1000);
        }
    }

    public void releaseLock() throws Exception {
        if (currentLockNode != null && zooKeeper.exists(currentLockNode, false) != null) {
            zooKeeper.delete(currentLockNode, -1);
        }
    }

    public void close() throws Exception {
        if (zooKeeper != null) {
            zooKeeper.close();
        }
    }
}