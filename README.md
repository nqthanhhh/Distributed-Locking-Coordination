# Distributed-Locking-Coordination

Demo mô phỏng 5 client cùng đặt mua một sản phẩm có tồn kho giới hạn, sử dụng
Apache ZooKeeper Distributed Lock để tránh lỗi cập nhật tồn kho đồng thời.

## Công nghệ sử dụng

- Java
- Maven
- Apache ZooKeeper
- Docker
- IntelliJ IDEA
- Git/GitHub

## Chạy ZooKeeper bằng Docker

Tạo và chạy container ZooKeeper:

```bash
docker run -d --name zookeeper-demo -p 2181:2181 zookeeper:3.9.3
```

Nếu container `zookeeper-demo` đã tồn tại:

```bash
docker start zookeeper-demo
```

Kiểm tra container đang chạy:

```bash
docker ps
```

ZooKeeper sẽ lắng nghe tại `localhost:2181`.

## Chạy demo

1. Mở project bằng IntelliJ IDEA.
2. Chờ IntelliJ tải các dependency Maven.
3. Đảm bảo container ZooKeeper đang chạy.
4. Chạy file `src/main/java/org/example/Main.java`.

## Kết quả mong đợi

- 5 client cùng yêu cầu mua sản phẩm `Laptop Gaming`.
- ZooKeeper tạo các node `EPHEMERAL_SEQUENTIAL` trong `/locks`.
- Client có lock node nhỏ nhất được giữ khóa trước.
- Client chưa đến lượt watch lock node đứng ngay trước nó.
- Mỗi thời điểm chỉ một client kiểm tra và trừ tồn kho.
- Tồn kho ban đầu là 2 nên chỉ 2 client mua thành công.
- 3 client còn lại thất bại vì hết hàng.
- Tồn kho cuối cùng là 0 và không bao giờ bị âm.

Thứ tự các client lấy khóa có thể thay đổi giữa các lần chạy vì chúng hoạt động
đồng thời.

## Ý nghĩa demo

Demo cho thấy ZooKeeper có thể điều phối các tiến trình trong hệ thống phân tán,
đảm bảo tính nhất quán dữ liệu và tránh race condition khi nhiều client cùng cập
nhật một tài nguyên chung.
