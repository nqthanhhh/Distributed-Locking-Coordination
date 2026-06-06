package org.example;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WorkerClient implements Runnable {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT_MS = 3_000;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;

    private final String clientName;
    private final InventoryService inventoryService;

    public WorkerClient(String clientName, InventoryService inventoryService) {
        this.clientName = clientName;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run() {
        ZooKeeper zooKeeper = null;
        DistributedLock distributedLock = null;

        try {
            System.out.println(clientName + " dang ket noi ZooKeeper...");
            zooKeeper = connect();
            System.out.println(clientName + " ket noi ZooKeeper thanh cong.");

            distributedLock = new DistributedLock(zooKeeper);

            System.out.println(clientName + " dang yeu cau mua san pham...");
            System.out.println(clientName + " dang yeu cau khoa...");
            distributedLock.acquireLock(clientName);

            // Chỉ client đang giữ khóa mới được kiểm tra và trừ tồn kho.
            inventoryService.buyProduct(clientName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(clientName + " bi gian doan.");
        } catch (Exception e) {
            System.err.println(clientName + " gap loi: " + e.getMessage());
        } finally {
            releaseLock(distributedLock);
            closeConnection(zooKeeper);
        }
    }

    private ZooKeeper connect() throws IOException, InterruptedException {
        CountDownLatch connectedSignal = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper(
                ZOOKEEPER_ADDRESS,
                SESSION_TIMEOUT_MS,
                event -> {
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        connectedSignal.countDown();
                    }
                }
        );

        if (!connectedSignal.await(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            zooKeeper.close();
            throw new IOException("Khong the ket noi ZooKeeper tai " + ZOOKEEPER_ADDRESS);
        }

        return zooKeeper;
    }

    private void releaseLock(DistributedLock distributedLock) {
        if (distributedLock == null) {
            return;
        }

        try {
            distributedLock.releaseLock(clientName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(clientName + " bi gian doan khi nha khoa.");
        } catch (Exception e) {
            System.err.println(clientName + " khong the nha khoa: " + e.getMessage());
        }
    }

    private void closeConnection(ZooKeeper zooKeeper) {
        if (zooKeeper == null) {
            return;
        }

        try {
            zooKeeper.close();
            System.out.println(clientName + " da dong ket noi ZooKeeper.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(clientName + " bi gian doan khi dong ket noi ZooKeeper.");
        }
    }
}
