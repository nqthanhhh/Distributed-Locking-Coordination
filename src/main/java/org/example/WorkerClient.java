package org.example;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WorkerClient implements Runnable {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT_MS = 5_000;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int WORK_DURATION_MS = 3_000;

    private final String clientName;
    private final CountDownLatch startSignal;

    public WorkerClient(String clientName, CountDownLatch startSignal) {
        this.clientName = clientName;
        this.startSignal = startSignal;
    }

    @Override
    public void run() {
        ZooKeeper zooKeeper = null;
        DistributedLock lock = null;

        try {
            startSignal.await();

            System.out.println(clientName + " dang ket noi ZooKeeper...");
            zooKeeper = connect();
            System.out.println(clientName + " ket noi ZooKeeper thanh cong.");

            lock = new DistributedLock(zooKeeper, clientName);

            System.out.println(clientName + " dang yeu cau khoa...");
            lock.acquire();
            System.out.println(clientName + " da lay duoc khoa.");

            // Mô phỏng vùng xử lý quan trọng chỉ một client được chạy tại một thời điểm.
            System.out.println(clientName + " dang xu ly tac vu quan trong...");
            Thread.sleep(WORK_DURATION_MS);
            System.out.println(clientName + " xu ly xong tac vu.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(clientName + " bi gian doan.");
        } catch (Exception e) {
            System.err.println(clientName + " gap loi: " + e.getMessage());
        } finally {
            releaseLock(lock);
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

    private void releaseLock(DistributedLock lock) {
        if (lock == null) {
            return;
        }

        try {
            String releasedLockPath = lock.release();
            if (releasedLockPath != null) {
                System.out.println(clientName + " da nha khoa: " + releasedLockPath);
            }
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(clientName + " bi gian doan khi dong ket noi ZooKeeper.");
        }
    }
}
