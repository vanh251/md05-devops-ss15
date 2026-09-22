# Bài tập 2: Tối ưu hóa chu kỳ Scrape (Vận dụng cơ bản)

## 1. Mục tiêu kiến thức
- Hiểu ý nghĩa và tầm quan trọng của thông số `scrape_interval` (chu kỳ thu thập dữ liệu) và `evaluation_interval` (chu kỳ đánh giá quy tắc alert/record) trong Prometheus.
- Biết cách tối ưu hóa tần suất scrape để tránh gây quá tải CPU, RAM cho Prometheus server cũng như các target service.
- Thực hành reload cấu hình Prometheus linh hoạt mà không làm gián đoạn hệ thống.

---

## 2. Tình huống nghiệp vụ & Nguyên nhân lỗi

### 2.1 Tình huống nghiệp vụ
Hệ thống giám sát Prometheus đang tiêu thụ quá nhiều CPU và RAM. Log của container Prometheus liên tục xuất hiện cảnh báo overload hoặc gián đoạn do số lượng HTTP request thu thập metric quá lớn.

### 2.2 Nguyên nhân
Khi kiểm tra file cấu hình `prometheus/prometheus.yml`, phát hiện các thông số thu thập dữ liệu được cài đặt ở mức quá ngắn (`1s`):

```yaml
global:
  scrape_interval: 1s       # Thu thập metric mỗi 1 giây
  evaluation_interval: 1s   # Đánh giá rule/alert mỗi 1 giây

scrape_configs:
  - job_name: 'backend-app'
    static_configs:
      - targets: ['backend:8080']
```

Chu kỳ 1 giây khiến Prometheus phải liên tục gửi request lấy dữ liệu tới tất cả các target endpoints, làm tăng tải mạng, CPU và bộ nhớ đệm TSDB (Time Series Database) không cần thiết.

---

## 3. Các bước thực hiện

### Bước 1: Mở file cấu hình `prometheus.yml`
File cấu hình nằm tại đường dẫn: `prometheus/prometheus.yml`.

---

### Bước 2: Chỉnh sửa thông số `global` từ `1s` thành `15s`
Cập nhật file `prometheus/prometheus.yml` với thông số tối ưu chuẩn là **`15s`**:

```yaml
global:
  scrape_interval: 15s       # Đổi từ 1s thành 15s
  evaluation_interval: 15s   # Đổi từ 1s thành 15s

scrape_configs:
  - job_name: 'order-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['order-service:8080']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
```

---

### Bước 3: Reload cấu hình Prometheus
Có 2 cách áp dụng file cấu hình mới mà bạn có thể lựa chọn:

#### Cách 1: Reload trực tiếp qua API (Không làm gián đoạn service)
Gửi request HTTP `POST` đến endpoint `/-/reload` của Prometheus (yêu cầu Prometheus được bật flag `--web.enable-lifecycle`):

```bash
curl -X POST http://localhost:9090/-/reload
```

#### Cách 2: Khởi động lại container Prometheus qua Docker Compose
Sử dụng lệnh restart container đơn giản:

```bash
docker compose restart prometheus
```

---

### Bước 4: Kiểm tra và xác minh kết quả

1. **Kiểm tra trạng thái Prometheus Target**:
   - Truy cập Prometheus Web UI tại: `http://localhost:9090/targets`.
   - Kiểm tra thông số **Last Scrape** và **Scrape Duration** để đảm bảo chu kỳ thu thập đã chuyển sang 15 giây.
2. **Kiểm tra đồ thị trên Grafana**:
   - Truy cập Dashboard trên Grafana (`http://localhost:3000`).
   - Kiểm tra dữ liệu biểu đồ vẫn được cập nhật liên tục mỗi 15 giây, không bị trễ hay mất kết nối (lỗi `No data`).
3. **Kiểm tra mức độ tiêu thụ tài nguyên**:
   - Chạy lệnh `docker stats prometheus` để xác nhận lượng CPU và RAM sử dụng đã giảm xuống rõ rệt so với ban đầu.

---

## 4. Kết quả đạt được
- Tải CPU và bộ nhớ RAM của Prometheus container giảm đáng kể.
- Log hệ thống không còn cảnh báo bị overload.
- Grafana cập nhật metric mượt mà mỗi 15 giây, đảm bảo tính liên tục và chính xác cho hệ thống giám sát.
