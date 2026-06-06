package org.example;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String PRODUCT_NAME = "Laptop Gaming";
    private static final int INITIAL_STOCK = 2;
    private static final int CLIENT_COUNT = 5;

    public static void main(String[] args) {
        // Giữ log nội bộ của ZooKeeper ở mức cảnh báo để output demo dễ theo dõi.
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

        System.out.println("BAT DAU DEMO QUAN LY TON KHO VOI ZOOKEEPER DISTRIBUTED LOCK");

        InventoryService inventoryService = new InventoryService(PRODUCT_NAME, INITIAL_STOCK);
        System.out.println("San pham: " + inventoryService.getProductName());
        System.out.println("So luong ton kho ban dau: " + inventoryService.getStock());
        System.out.println();

        List<Thread> clientThreads = new ArrayList<>();

        for (int i = 1; i <= CLIENT_COUNT; i++) {
            String clientName = "Client-" + i;
            Thread clientThread = new Thread(
                    new WorkerClient(clientName, inventoryService),
                    clientName + "-Thread"
            );
            clientThreads.add(clientThread);
        }

        // Khởi chạy liên tiếp để năm client cùng cạnh tranh một distributed lock.
        clientThreads.forEach(Thread::start);

        for (Thread clientThread : clientThreads) {
            try {
                clientThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Main bi gian doan khi cho cac client ket thuc.");
                return;
            }
        }

        System.out.println();
        System.out.println("Ton kho cuoi cung: " + inventoryService.getStock());
        System.out.println("KET THUC DEMO");
    }
}
