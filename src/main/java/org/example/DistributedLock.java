package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributedLock {

    private static final String LOCK_ROOT = "/locks";
    private static final String LOCK_NODE_PREFIX = "order-lock-";
    private static final String LOCK_PATH_PREFIX = LOCK_ROOT + "/" + LOCK_NODE_PREFIX;

    private final ZooKeeper zooKeeper;
    private String currentLockNode;

    public DistributedLock(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public void acquireLock(String clientName) throws KeeperException, InterruptedException {
        createLockRootIfNeeded();

        // EPHEMERAL tự xóa khi session đóng; SEQUENTIAL cung cấp thứ tự tranh khóa.
        currentLockNode = zooKeeper.create(
                LOCK_PATH_PREFIX,
                clientName.getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
        System.out.println(clientName + " tao lock node: " + currentLockNode);

        String currentNodeName = currentLockNode.substring(LOCK_ROOT.length() + 1);

        while (true) {
            List<String> lockNodes = zooKeeper.getChildren(LOCK_ROOT, false)
                    .stream()
                    .filter(node -> node.startsWith(LOCK_NODE_PREFIX))
                    .sorted(Comparator.naturalOrder())
                    .toList();

            int currentNodeIndex = lockNodes.indexOf(currentNodeName);
            if (currentNodeIndex < 0) {
                throw new KeeperException.NoNodeException(
                        "Lock node khong con ton tai: " + currentLockNode
                );
            }

            if (currentNodeIndex == 0) {
                System.out.println(clientName + " da lay duoc khoa xu ly don hang.");
                return;
            }

            String previousNodePath = LOCK_ROOT + "/" + lockNodes.get(currentNodeIndex - 1);
            CountDownLatch previousNodeDeleted = new CountDownLatch(1);

            System.out.println(clientName + " dang cho node truoc do: " + previousNodePath);

            // Chỉ watch node ngay trước để tránh đánh thức đồng loạt tất cả client.
            if (zooKeeper.exists(previousNodePath, event -> {
                if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
                    previousNodeDeleted.countDown();
                }
            }) != null) {
                previousNodeDeleted.await();
            }
            // Nếu node đã bị xóa trước khi đăng ký watcher, vòng lặp kiểm tra lại ngay.
        }
    }

    public void releaseLock(String clientName) throws KeeperException, InterruptedException {
        if (currentLockNode == null) {
            return;
        }

        String lockNodeToDelete = currentLockNode;
        try {
            if (zooKeeper.exists(lockNodeToDelete, false) != null) {
                zooKeeper.delete(lockNodeToDelete, -1);
                System.out.println(clientName + " da nha khoa: " + lockNodeToDelete);
            }
        } catch (KeeperException.NoNodeException ignored) {
            // Node ephemeral có thể đã được ZooKeeper xóa nếu session kết thúc.
        } finally {
            currentLockNode = null;
        }
    }

    private void createLockRootIfNeeded() throws KeeperException, InterruptedException {
        try {
            zooKeeper.create(
                    LOCK_ROOT,
                    "Thu muc chua distributed lock".getBytes(StandardCharsets.UTF_8),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT
            );
        } catch (KeeperException.NodeExistsException ignored) {
            // Một client khác đã tạo /locks trước.
        }
    }
}
