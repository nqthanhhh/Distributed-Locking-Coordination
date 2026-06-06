# Distributed-Locking-Coordination

Demo Flash Sale sử dụng Apache ZooKeeper Distributed Lock để tránh race
condition khi nhiều khách hàng cùng đặt mua sản phẩm có tồn kho giới hạn.

## Công nghệ sử dụng

- Java
- Maven
- Apache ZooKeeper
- Docker
- IntelliJ IDEA
- Git/GitHub

## Chức năng chính

- Nhập tên sản phẩm từ console.
- Nhập số lượng tồn kho ban đầu từ console.
- Nhập số lượng khách hàng tham gia từ console.
- Kiểm tra dữ liệu nhập và yêu cầu nhập lại khi không hợp lệ.
- Lưu tồn kho trong ZooKeeper node `/inventory/<product-slug>`.
- Mô phỏng nhiều customer đặt hàng đồng thời bằng Thread.
- Sử dụng Distributed Lock với node `EPHEMERAL_SEQUENTIAL`.
- Chỉ cho phép một customer đọc và cập nhật tồn kho tại mỗi thời điểm.
- Thống kê số đơn hàng thành công và thất bại.

Ví dụ đường dẫn sản phẩm:

- `Laptop Gaming` → `/inventory/laptop-gaming`
- `Iphone 15` → `/inventory/iphone-15`
- `Ao Khoac Nam` → `/inventory/ao-khoac-nam`

## Chạy ZooKeeper bằng Docker

Tạo và chạy container ZooKeeper:

```bash
docker run -d --name zookeeper-demo -p 2181:2181 zookeeper:3.9.3
```

Nếu container `zookeeper-demo` đã tồn tại:

```bash
docker start zookeeper-demo
```

Kiểm tra container:

```bash
docker ps
```

ZooKeeper sẽ lắng nghe tại `localhost:2181`.

## Chạy demo

1. Mở project bằng IntelliJ IDEA.
2. Chờ IntelliJ tải các dependency Maven.
3. Đảm bảo container ZooKeeper đang chạy.
4. Chạy file `src/main/java/org/example/Main.java`.
5. Nhập đầy đủ dữ liệu theo yêu cầu trên console.

## Ví dụ nhập dữ liệu

```text
Nhap ten san pham: Laptop Gaming
Nhap ton kho ban dau: 2
Nhap so luong khach hang: 5
```

Tên sản phẩm không được trống. Tồn kho phải là số nguyên lớn hơn hoặc bằng `0`;
số khách hàng phải là số nguyên lớn hơn `0`. Chương trình sẽ yêu cầu nhập lại
cho đến khi dữ liệu hợp lệ.

## Kết quả mong đợi

Với tồn kho `2` và `5` customer:

- 5 customer cùng yêu cầu mua sản phẩm.
- ZooKeeper tạo các node `EPHEMERAL_SEQUENTIAL` trong `/locks`.
- Customer có node `/locks/order-lock-...` nhỏ nhất được giữ khóa trước.
- Mỗi thời điểm chỉ một customer kiểm tra và trừ tồn kho.
- Chỉ 2 customer đặt hàng thành công.
- 3 customer còn lại thất bại vì hết hàng.
- Tồn kho cuối cùng là `0` và không bị âm.

Thứ tự customer có thể khác nhau giữa các lần chạy vì các Thread được JVM và hệ
điều hành lập lịch không cố định. Customer gửi request tới ZooKeeper trước sẽ
nhận lock node có số thứ tự nhỏ hơn và được xử lý trước.

## Ý nghĩa demo

Demo cho thấy ZooKeeper có thể điều phối các tiến trình trong hệ thống phân tán,
đảm bảo tính nhất quán dữ liệu và tránh race condition khi nhiều client cùng cập
nhật một tài nguyên chung.
