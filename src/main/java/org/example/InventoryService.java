package org.example;

public class InventoryService {

    private final String productName;
    private volatile int stock;

    public InventoryService(String productName, int initialStock) {
        if (initialStock < 0) {
            throw new IllegalArgumentException("So luong ton kho ban dau khong duoc am.");
        }

        this.productName = productName;
        this.stock = initialStock;
    }

    public String getProductName() {
        return productName;
    }

    public int getStock() {
        return stock;
    }

    public void buyProduct(String clientName) throws InterruptedException {
        // Phương thức này không tự khóa; WorkerClient phải giữ distributed lock khi gọi.
        System.out.println(clientName + " kiem tra ton kho hien tai: " + stock);

        // Mô phỏng thời gian kiểm tra và cập nhật dữ liệu trong hệ thống thực tế.
        Thread.sleep(1_000);

        if (stock > 0) {
            stock--;
            System.out.println(clientName + " mua hang thanh cong.");
            System.out.println("Ton kho con lai: " + stock);
        } else {
            System.out.println(clientName + " mua hang that bai vi het hang.");
        }
    }
}
