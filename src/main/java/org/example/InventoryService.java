package org.example;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

public class InventoryService {

    public static final String INVENTORY_ROOT = "/inventory";

    private final ZooKeeper zooKeeper;
    private final OrderStats orderStats;

    public InventoryService(ZooKeeper zooKeeper, OrderStats orderStats) {
        this.zooKeeper = zooKeeper;
        this.orderStats = orderStats;
    }

    public static String createProductSlug(String productName) {
        String normalizedName = Normalizer.normalize(productName, Normalizer.Form.NFD)
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        String slug = normalizedName
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        // Tên đã hợp lệ nhưng không có ký tự Latin/số vẫn cần một ZooKeeper path hợp lệ.
        return slug.isEmpty() ? "product" : slug;
    }

    public void initializeInventory(String productPath, int initialStock)
            throws KeeperException, InterruptedException {
        if (initialStock < 0) {
            throw new IllegalArgumentException("Ton kho ban dau khong duoc am.");
        }

        createInventoryRootIfNeeded();
        byte[] stockData = toBytes(initialStock);

        try {
            zooKeeper.create(
                    productPath,
                    stockData,
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT
            );
        } catch (KeeperException.NodeExistsException ignored) {
            // Reset node đã tồn tại bằng tồn kho mới do người dùng nhập.
            zooKeeper.setData(productPath, stockData, -1);
        }
    }

    public int getStock(String productPath) throws KeeperException, InterruptedException {
        byte[] data = zooKeeper.getData(productPath, false, null);

        try {
            return Integer.parseInt(new String(data, StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Du lieu ton kho khong hop le tai " + productPath,
                    e
            );
        }
    }

    public void updateStock(String productPath, int newStock)
            throws KeeperException, InterruptedException {
        if (newStock < 0) {
            throw new IllegalArgumentException("Ton kho moi khong duoc am.");
        }

        zooKeeper.setData(productPath, toBytes(newStock), -1);
    }

    public void processOrder(String customerName, String productName, String productPath)
            throws KeeperException, InterruptedException {
        int currentStock = getStock(productPath);
        System.out.println(customerName + " kiem tra ton kho: " + currentStock);

        // Mô phỏng xử lý đơn hàng trong vùng được distributed lock bảo vệ.
        Thread.sleep(1_000);

        if (currentStock > 0) {
            int newStock = currentStock - 1;
            updateStock(productPath, newStock);
            orderStats.incrementSuccess();
            System.out.println(customerName + " DAT HANG THANH CONG.");
            System.out.println("Ton kho con lai: " + newStock);
        } else {
            orderStats.incrementFail();
            System.out.println(customerName + " DAT HANG THAT BAI VI HET HANG.");
        }
    }

    private void createInventoryRootIfNeeded()
            throws KeeperException, InterruptedException {
        try {
            zooKeeper.create(
                    INVENTORY_ROOT,
                    new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT
            );
        } catch (KeeperException.NodeExistsException ignored) {
            // Node gốc đã sẵn sàng cho dữ liệu tồn kho.
        }
    }

    private byte[] toBytes(int stock) {
        return Integer.toString(stock).getBytes(StandardCharsets.UTF_8);
    }
}
