# Bài tập 1: Sửa lỗi mất kết nối Grafana - Prometheus

## 1. Mục tiêu kiến thức
- Ôn tập và rèn luyện kỹ năng **debug hệ thống Container** (Docker / Docker Compose).
- Hiểu và thực hành cấu hình **Datasource trong Grafana** chuẩn hóa cho môi trường container.
- Hiểu rõ cơ chế **Embedded DNS** của Docker để kết nối giữa các service qua Service Name thay vì hardcode IP tĩnh.

---

## 2. Tình huống nghiệp vụ & Nguyên nhân lỗi

### 2.1 Tình huống nghiệp vụ
Toàn bộ các Dashboard trên Grafana hiển thị lỗi **"No data"**. Hệ thống không cập nhật được các metric giám sát từ Prometheus.

### 2.2 Nguyên nhân
Cấu hình Datasource của Grafana tại file `grafana/provisioning/datasources/datasource.yml` hiện tại đang hardcode IP tĩnh của Prometheus:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://192.168.1.15:9090
    isDefault: true
```

Khi `docker-compose` hoặc hệ thống khởi động lại, IP của container Prometheus (`192.168.1.15`) bị thay đổi, dẫn đến Grafana không thể kết nối tới Prometheus qua địa chỉ IP cũ.

---

## 3. Các bước thực hiện

### Bước 1: Kiểm tra log lỗi của Grafana
Chạy lệnh kiểm tra log của container Grafana để xác nhận lỗi kết nối:

```bash
docker compose logs grafana
```
*(Hoặc `docker logs <grafana_container_id>`)*

**Log ghi nhận lỗi tương tự:**
```text
logger=context userId=1 orgId=1 uname=admin uname=admin msg="Failed to connect to datasource" error="http: can't connect to 192.168.1.15:9090: connection refused / timeout"
```

---

### Bước 2: Cập nhật file cấu hình `datasource.yml`
Mở file `grafana/provisioning/datasources/datasource.yml` và thay đổi địa chỉ IP tĩnh hardcode thành tên service nội bộ của Prometheus (`prometheus:9090`):

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

---

### Bước 3: Khởi động lại dịch vụ Grafana
Sau khi chỉnh sửa file cấu hình, tiến hành restart dịch vụ Grafana để áp dụng thay đổi:

```bash
docker compose restart grafana
```

Hoặc tái tạo container nếu cần:
```bash
docker compose up -d --force-recreate grafana
```

---

### Bước 4: Kiểm tra và xác minh kết nối

1. **Truy cập Grafana Web UI**: Mở trình duyệt tại địa chỉ `http://localhost:3000`.
2. **Kiểm tra Datasource**: Vào **Connections** -> **Data sources** -> Chọn **Prometheus** -> Nhấn nút **Save & test**.
   - Kết quả thành công: Nhận thông báo xanh `Successfully queried the Prometheus API`.
3. **Kiểm tra Dashboard**: Mở các Dashboard giám sát và kiểm tra xem dữ liệu mét đã hiển thị lại bình thường chưa.

---

## 4. Kết quả đạt được
- Grafana kết nối thành công với Prometheus thông qua **DNS nội bộ của Docker** (`http://prometheus:9090`).
- Dashboard trên Grafana hoạt động ổn định và hiển thị dữ liệu đầy đủ trở lại.
- Tránh được sự cố mất kết nối khi khởi động lại các container hoặc khi IP nội bộ bị thay đổi.
