# Distributed-Locking-Coordination

Dự án bài tập lớn tìm hiểu Apache ZooKeeper trong nhóm Distributed Locking & Coordination.

## Mục tiêu

Xây dựng demo đơn giản sử dụng Apache ZooKeeper để minh họa cơ chế Distributed Lock trong hệ thống phân tán.

## Công nghệ sử dụng

- Java
- Maven
- Apache ZooKeeper
- Docker

## Chức năng đã thực hiện

- Kết nối Java Client tới ZooKeeper Server
- Tạo node gốc `/locks`
- Tạo lock bằng EPHEMERAL_SEQUENTIAL node
- Mô phỏng tiến trình lấy lock, xử lý tác vụ và nhả lock

## Cách chạy ZooKeeper bằng Docker

```bash
docker run -d --name zookeeper-demo -p 2181:2181 zookeeper:3.9.3