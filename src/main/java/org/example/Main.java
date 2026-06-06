package org.example;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT_MS = 3_000;
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        Scanner scanner = new Scanner(System.in);
        String productName = inputNonEmptyString(scanner, "Nhap ten san pham: ");
        int initialStock = inputIntegerMin(scanner, "Nhap ton kho ban dau: ", 0);
        int customerCount = inputIntegerMin(scanner, "Nhap so luong khach hang: ", 1);

        String productSlug = InventoryService.createProductSlug(productName);
        String productPath = InventoryService.INVENTORY_ROOT + "/" + productSlug;
        OrderStats orderStats = new OrderStats();
        ZooKeeper adminZooKeeper = null;

        System.out.println();
        System.out.println("BAT DAU DEMO FLASH SALE VOI APACHE ZOOKEEPER DISTRIBUTED LOCK");
        System.out.println("San pham: " + productName);
        System.out.println("Ton kho ban dau: " + initialStock);
        System.out.println("So khach hang tham gia: " + customerCount);
        System.out.println();

        try {
            adminZooKeeper = connectZooKeeper();
            InventoryService inventoryService = new InventoryService(adminZooKeeper, orderStats);

            // Tạo hoặc reset node sản phẩm bằng tồn kho vừa nhập.
            inventoryService.initializeInventory(productPath, initialStock);

            CountDownLatch startSignal = new CountDownLatch(1);
            List<Thread> customerThreads = createCustomerThreads(
                    productName,
                    productPath,
                    customerCount,
                    orderStats,
                    startSignal
            );

            customerThreads.forEach(Thread::start);
            startSignal.countDown();
            waitForCustomers(customerThreads);

            System.out.println();
            System.out.println("Ton kho cuoi cung: " + inventoryService.getStock(productPath));
            System.out.println("So don hang thanh cong: " + orderStats.getSuccessCount());
            System.out.println("So don hang that bai: " + orderStats.getFailCount());
            System.out.println("KET THUC DEMO");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main bi gian doan.");
        } catch (Exception e) {
            System.err.println("Khong the chay demo: " + e.getMessage());
        } finally {
            closeZooKeeper(adminZooKeeper);
        }
    }

    public static String inputNonEmptyString(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String value = scanner.nextLine().trim();

            if (!value.isEmpty()) {
                return value;
            }

            System.out.println("Ten san pham khong duoc bo trong. Vui long nhap lai.");
        }
    }

    public static int inputIntegerMin(Scanner scanner, String message, int minValue) {
        while (true) {
            System.out.print(message);
            String value = scanner.nextLine().trim();

            try {
                int number = Integer.parseInt(value);
                if (number >= minValue) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // In thông báo thống nhất bên dưới rồi yêu cầu nhập lại.
            }

            if (minValue == 0) {
                System.out.println("Ton kho phai la so nguyen >= 0. Vui long nhap lai.");
            } else {
                System.out.println(
                        "So luong khach hang phai la so nguyen > 0. Vui long nhap lai."
                );
            }
        }
    }

    private static List<Thread> createCustomerThreads(
            String productName,
            String productPath,
            int customerCount,
            OrderStats orderStats,
            CountDownLatch startSignal
    ) {
        List<Thread> customerThreads = new ArrayList<>();

        for (int i = 1; i <= customerCount; i++) {
            String customerName = "Customer-" + i;
            customerThreads.add(new Thread(
                    new WorkerClient(
                            customerName,
                            productName,
                            productPath,
                            orderStats,
                            startSignal
                    ),
                    customerName + "-Thread"
            ));
        }

        return customerThreads;
    }

    private static void waitForCustomers(List<Thread> customerThreads)
            throws InterruptedException {
        for (Thread customerThread : customerThreads) {
            customerThread.join();
        }
    }

    private static ZooKeeper connectZooKeeper() throws IOException, InterruptedException {
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

    private static void closeZooKeeper(ZooKeeper zooKeeper) {
        if (zooKeeper == null) {
            return;
        }

        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
