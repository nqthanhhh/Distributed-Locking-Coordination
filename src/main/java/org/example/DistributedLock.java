package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributedLock {

    private static final String LOCK_ROOT = "/locks";
    private static final String LOCK_PREFIX = "lock-";
    private static final String LOCK_PATH_PREFIX = LOCK_ROOT + "/" + LOCK_PREFIX;

    private final ZooKeeper zooKeeper;
    private final String clientName;
    private String currentLockPath;

    public DistributedLock(ZooKeeper zooKeeper, String clientName) {
        this.zooKeeper = zooKeeper;
        this.clientName = clientName;
    }

    public void acquire() throws KeeperException, InterruptedException {
        createLockRootIfNeeded();

        // EPHEMERAL giúp ZooKeeper tự xóa node nếu client mất kết nối.
        // SEQUENTIAL tạo số thứ tự để xác định client được giữ khóa trước.
        currentLockPath = zooKeeper.create(
                LOCK_PATH_PREFIX,
                clientName.getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
        System.out.println(clientName + " tao lock node: " + currentLockPath);

        String currentNodeName = currentLockPath.substring(LOCK_ROOT.length() + 1);

        while (true) {
            List<String> lockNodes = zooKeeper.getChildren(LOCK_ROOT, false)
                    .stream()
                    .filter(node -> node.startsWith(LOCK_PREFIX))
                    .sorted(Comparator.naturalOrder())
                    .toList();

            int currentNodeIndex = lockNodes.indexOf(currentNodeName);
            if (currentNodeIndex < 0) {
                throw new KeeperException.NoNodeException(
                        "Lock node khong con ton tai: " + currentLockPath
                );
            }

            if (currentNodeIndex == 0) {
                return;
            }

            String predecessorPath = LOCK_ROOT + "/" + lockNodes.get(currentNodeIndex - 1);
            CountDownLatch predecessorDeleted = new CountDownLatch(1);
            System.out.println(clientName + " dang cho node truoc do: " + predecessorPath);

            // Chỉ theo dõi node ngay trước để tránh mọi client cùng bị đánh thức.
            if (zooKeeper.exists(predecessorPath, event -> {
                if (event.getType() == org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted) {
                    predecessorDeleted.countDown();
                }
            }) != null) {
                predecessorDeleted.await();
            }
            // Nếu node đã biến mất trước khi watcher được đăng ký, kiểm tra lại thứ tự ngay.
        }
    }

    public String release() throws KeeperException, InterruptedException {
        if (currentLockPath == null) {
            return null;
        }

        String releasedLockPath = currentLockPath;
        try {
            zooKeeper.delete(currentLockPath, -1);
        } catch (KeeperException.NoNodeException ignored) {
            // Node ephemeral có thể đã bị ZooKeeper xóa khi session kết thúc.
            return null;
        } finally {
            currentLockPath = null;
        }

        return releasedLockPath;
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
            // Client khác đã tạo /locks trước, có thể tiếp tục sử dụng.
        }
    }
}
