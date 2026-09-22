# Bài tập 3: Xử lý cảnh báo giả (False Alarm)

## 1. Mục tiêu kiến thức
- Hiểu cách cấu hình và tối ưu hóa **Alerting Rules** trong Prometheus để phản ánh chính xác tình trạng sức khỏe của hệ thống.
- Khắc phục hiện tượng **Alert Fatigue** (quá tải/nhiễu cảnh báo giả) bằng cách điều chỉnh ngưỡng (`threshold`) và khoảng thời gian chờ (`for`).
- Sử dụng công cụ **`promtool`** để kiểm tra tính hợp lệ và cú pháp (syntax check) của file cấu hình Alert Rules trước khi triển khai.

---

## 2. Tình huống nghiệp vụ & Nguyên nhân lỗi

### 2.1 Tình huống nghiệp vụ
Đội ngũ vận hành (DevOps/SRE) liên tục nhận được email/notification cảnh báo **"High CPU Usage"**, gây phiền nhiễu dù các dịch vụ và hệ thống vẫn đang hoạt động hoàn toàn bình thường.

### 2.2 Nguyên nhân
Khi kiểm tra file `prometheus/alert_rules.yml`, phát hiện rule cảnh báo CPU đang được cấu hình quá nhạy cảm:

```yaml
groups:
  - name: CPU_Alerts
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100) > 20
        for: 10s
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage detected"
```

- Ngưỡng cảnh báo **`> 20%`** là quá thấp cho môi trường sản xuất.
- Thời gian đánh giá **`for: 10s`** quá ngắn, dẫn đến bất kỳ xung đột tải tạm thời (CPU Spike ngắn hạn từ 5-10s) nào cũng vô tình kích hoạt cảnh báo rác.

---

## 3. Các bước thực hiện

### Bước 1: Chỉnh sửa file `alert_rules.yml`
Mở file `prometheus/alert_rules.yml` và tiến hành cập nhật:
1. Thay đổi ngưỡng CPU trong `expr` từ **`> 20`** thành **`> 85`** (cảnh báo khi CPU vượt quá 85%).
2. Tăng thời gian theo dõi `for` từ **`10s`** lên **`5m`** (chỉ cảnh báo nếu duy trì liên tục trên 85% trong 5 phút).

**Nội dung sau khi chỉnh sửa:**

```yaml
groups:
  - name: CPU_Alerts
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100) > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage detected"
          description: "CPU usage on instance {{ $labels.instance }} has exceeded 85% for more than 5 minutes."
```

---

### Bước 2: Kiểm tra cú pháp bằng `promtool`
Trước khi đưa vào hoạt động, sử dụng CLI **`promtool`** để validate cú pháp file rule.

#### Cách 1: Chạy trực tiếp qua Promtool CLI (nếu đã cài đặt local)
```bash
promtool check rules prometheus/alert_rules.yml
```

#### Cách 2: Chạy qua Docker Container (không cần cài local)
```bash
docker run --rm -v %cd%/prometheus:/etc/prometheus prom/prometheus promtool check rules /etc/prometheus/alert_rules.yml
```
*(Nếu dùng Linux/macOS, thay `%cd%` bằng `$PWD`)*

**Kết quả thành công:**
```text
Checking prometheus/alert_rules.yml
  SUCCESS: 1 rules found
```

---

### Bước 3: Khai báo Rule File vào `prometheus.yml` & Reload Prometheus
Đảm bảo file `prometheus/prometheus.yml` đã được liên kết tới file `alert_rules.yml`:

```yaml
rule_files:
  - "alert_rules.yml"
```

Tiến hành reload cấu hình Prometheus:
```bash
curl -X POST http://localhost:9090/-/reload
# Hoặc: docker compose restart prometheus
```

---

### Bước 4: Kiểm tra trên Prometheus UI
1. Truy cập trang Alerts của Prometheus tại: `http://localhost:9090/alerts`.
2. Kiểm tra rule `HighCPUUsage` đã cập nhật thông số mới:
   - State: `Inactive` (hoặc `Pending` nếu đang theo dõi).
   - Ngưỡng điều kiện: `> 85`.
   - Thời gian duy trì: `5m`.

---

## 4. Kết quả đạt được
- Hệ thống **không còn bắn cảnh báo rác (false alarms)** khi CPU tăng nhẹ tạm thời.
- Cảnh báo `HighCPUUsage` chỉ thực sự được kích hoạt khi mức sử dụng CPU vượt quá 85% liên tục trong 5 phút, phản ánh đúng sự cố thực sự của hệ thống.
- Đảm bảo quy trình vận hành tin cậy và không gây quá tải thông tin cho đội ngũ DevOps.
