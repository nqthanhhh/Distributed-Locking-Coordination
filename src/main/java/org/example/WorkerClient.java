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

    private final String customerName;
    private final String productName;
    private final String productPath;
    private final OrderStats orderStats;
    private final CountDownLatch startSignal;
    private InventoryService inventoryService;

    public WorkerClient(
            String customerName,
            String productName,
            String productPath,
            OrderStats orderStats,
            CountDownLatch startSignal
    ) {
        this.customerName = customerName;
        this.productName = productName;
        this.productPath = productPath;
        this.orderStats = orderStats;
        this.startSignal = startSignal;
    }

    @Override
    public void run() {
        ZooKeeper zooKeeper = null;
        DistributedLock distributedLock = null;

        try {
            startSignal.await();

            System.out.println(customerName + " dang ket noi ZooKeeper...");
            zooKeeper = connect();
            System.out.println(customerName + " ket noi ZooKeeper thanh cong.");

            inventoryService = new InventoryService(zooKeeper, orderStats);
            distributedLock = new DistributedLock(zooKeeper);

            System.out.println(customerName + " gui yeu cau dat mua " + productName + ".");
            System.out.println(customerName + " dang yeu cau khoa...");
            distributedLock.acquireLock(customerName);

            // Chỉ đọc và cập nhật tồn kho sau khi đã giữ distributed lock.
            inventoryService.processOrder(customerName, productName, productPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(customerName + " bi gian doan.");
        } catch (Exception e) {
            System.err.println(customerName + " gap loi: " + e.getMessage());
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
            distributedLock.releaseLock(customerName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(customerName + " bi gian doan khi nha khoa.");
        } catch (Exception e) {
            System.err.println(customerName + " khong the nha khoa: " + e.getMessage());
        }
    }

    private void closeConnection(ZooKeeper zooKeeper) {
        if (zooKeeper == null) {
            return;
        }

        try {
            zooKeeper.close();
            System.out.println(customerName + " da dong ket noi ZooKeeper.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(customerName + " bi gian doan khi dong ket noi ZooKeeper.");
        }
    }
}
