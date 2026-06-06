# Distributed-Locking-Coordination

Demo Distributed Lock bằng Apache ZooKeeper.

## Công nghệ sử dụng

- Java 17
- Maven
- Apache ZooKeeper 3.9.3
- Docker
- IntelliJ IDEA

## Chạy ZooKeeper bằng Docker

Tạo và chạy container ZooKeeper:

```bash
docker run -d --name zookeeper-demo -p 2181:2181 zookeeper:3.9.3
```

Nếu container `zookeeper-demo` đã tồn tại:

```bash
docker start zookeeper-demo
```

ZooKeeper sẽ lắng nghe tại `localhost:2181`.

## Chạy demo trong IntelliJ IDEA

1. Mở thư mục dự án bằng IntelliJ IDEA.
2. Chờ IntelliJ tải các dependency Maven.
3. Đảm bảo container ZooKeeper đang chạy.
4. Mở `src/main/java/org/example/Main.java`.
5. Nhấn **Run 'Main.main()'**.

## Kết quả mong đợi

Chương trình khởi chạy đồng thời ba client: `Client-1`, `Client-2`, `Client-3`.
Mỗi client tạo một node `EPHEMERAL_SEQUENTIAL` dưới `/locks` và chờ node
đứng ngay trước bằng ZooKeeper Watcher. Vì vậy, tại một thời điểm chỉ có một
client giữ khóa và thực hiện tác vụ quan trọng trong khoảng 3 giây.

Thứ tự client lấy khóa có thể thay đổi giữa các lần chạy. Kết quả có dạng:

```text
Client-2 da lay duoc khoa.
Client-1 da lay duoc khoa.
Client-3 da lay duoc khoa.
KET THUC DEMO
```
