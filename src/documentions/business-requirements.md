# Business Requirements


**Dự án:** Hệ thống quản lý thực tập sinh (Internship Management System - IMS)

## 1. Tổng quan
Hệ thống hỗ trợ doanh nghiệp quản lý toàn bộ vòng đời thực tập sinh: tiếp nhận hồ sơ → xét duyệt → phân công mentor → giao việc/đánh giá → chấm công → báo cáo.

## 2. Stakeholders & vai trò
- **Admin:** quản trị người dùng, phân quyền, cấu hình hệ thống.
- **HR:** tiếp nhận/xét duyệt hồ sơ, quản lý chương trình, hợp đồng, báo cáo.
- **Mentor:** giao nhiệm vụ, phản hồi, đánh giá, theo dõi tiến độ.
- **Thực tập sinh:** nộp hồ sơ, cập nhật thông tin, nộp báo cáo, xem lịch/nhiệm vụ.

## 3. Phạm vi chức năng (theo Epic/Feature)
## Quản lý hồ sơ thực tập sinh

### Quản lý thông tin cá nhân

- Là HR, tôi muốn thêm mới hồ sơ thực tập sinh để lưu trữ thông tin.
- Là HR, tôi muốn chỉnh sửa hồ sơ thực tập sinh để cập nhật thông tin thay đổi.
- Là HR, tôi muốn tìm kiếm và lọc thực tập sinh theo trường/ngành để dễ dàng quản lý.

### Quản lý tài liệu

- Là thực tập sinh, tôi muốn upload CV và đơn xin thực tập để hoàn thiện hồ sơ.
- Là HR, tôi muốn xem và duyệt tài liệu của thực tập sinh để xác thực hồ sơ.

## Tiếp nhận và xét duyệt

### Đăng ký & nộp hồ sơ

- Là thực tập sinh, tôi muốn đăng ký tài khoản và nộp hồ sơ trực tuyến để tham gia chương trình thực tập.

### Xét duyệt hồ sơ

- Là HR, tôi muốn duyệt hoặc từ chối hồ sơ để chọn ứng viên phù hợp.
- Là hệ thống, tôi muốn gửi email thông báo kết quả xét duyệt để thực tập sinh nhận được thông tin kịp thời.

### Quản lý hợp đồng

- Là HR, tôi muốn tải lên hợp đồng thực tập để quản lý giấy tờ.
- Là thực tập sinh, tôi muốn xác nhận hợp đồng trên hệ thống để hoàn tất thủ tục.

## Quản lý chương trình thực tập

### Chương trình & nhóm thực tập

- Là HR, tôi muốn tạo chương trình thực tập theo phòng ban để tổ chức kế hoạch.
- Là HR, tôi muốn phân công thực tập sinh cho mentor để họ được hướng dẫn.

### Lịch thực tập

- Là HR, tôi muốn thiết lập ngày bắt đầu và kết thúc chương trình để quản lý thời gian.
- Là thực tập sinh, tôi muốn xem lịch thực tập cá nhân để biết kế hoạch.

## Quản lý công việc & đánh giá

### Giao nhiệm vụ

- Là mentor, tôi muốn giao nhiệm vụ cho thực tập sinh để họ có công việc cụ thể.
- Là thực tập sinh, tôi muốn cập nhật tiến độ công việc để mentor theo dõi.

### Báo cáo

- Là thực tập sinh, tôi muốn nộp báo cáo tuần để báo cáo kết quả thực tập.
- Là mentor, tôi muốn xem báo cáo và phản hồi để hỗ trợ thực tập sinh.

### Đánh giá

- Là mentor, tôi muốn đánh giá kỹ năng và thái độ của thực tập sinh để tổng kết.
- Là HR, tôi muốn tổng hợp đánh giá thành báo cáo cuối kỳ để gửi cho trường/ban lãnh đạo.

## Quản lý chấm công & thời gian

### Chấm công

- Là thực tập sinh, tôi muốn check-in/check-out trên hệ thống để ghi nhận thời gian làm việc.
- Là HR, tôi muốn xem báo cáo đi làm và nghỉ phép để quản lý sự chuyên cần.

### Lịch làm việc

- Là HR, tôi muốn thiết lập lịch làm việc linh hoạt để phù hợp với từng nhóm.
- Là thực tập sinh, tôi muốn đăng ký nghỉ phép để báo trước cho HR.

## Quản lý hỗ trợ & quyền lợi

### Phụ cấp

- Là HR, tôi muốn nhập thông tin phụ cấp của thực tập sinh để quản lý quyền lợi.
- Là thực tập sinh, tôi muốn xem lịch sử nhận phụ cấp để theo dõi thu nhập.

### Yêu cầu hỗ trợ

- Là thực tập sinh, tôi muốn gửi yêu cầu hỗ trợ (ví dụ: chứng nhận, giấy tờ) để được giải quyết.
- Là HR, tôi muốn duyệt và phản hồi yêu cầu hỗ trợ để hỗ trợ thực tập sinh kịp thời.

## Quản lý mentor & phòng ban

### Quản lý mentor

- Là HR, tôi muốn thêm mới mentor để phân công cho thực tập sinh.
- Là HR, tôi muốn gán mentor cho thực tập sinh để họ được hướng dẫn.

### Theo dõi mentor

- Là HR, tôi muốn xem số lượng thực tập sinh mỗi mentor quản lý để cân bằng khối lượng công việc.

## Báo cáo & thống kê

### Thống kê

- Là HR, tôi muốn xem số lượng thực tập sinh theo trường/ngành để phân tích nguồn ứng viên.
- Là HR, tôi muốn xem tỷ lệ hoàn thành chương trình để đánh giá chất lượng thực tập.

### Xuất báo cáo

- Là HR, tôi muốn xuất báo cáo ra Excel/PDF để chia sẻ với lãnh đạo hoặc trường đại học.

## Tích hợp & thông báo

### Thông báo

- Là hệ thống, tôi muốn gửi email tự động khi có lịch họp để thông báo cho thực tập sinh.
- Là thực tập sinh, tôi muốn nhận thông báo trên ứng dụng để không bỏ lỡ lịch trình.

### Tích hợp hệ thống

- Là admin, tôi muốn tích hợp hệ thống với HRM để đồng bộ dữ liệu nhân sự.
- Là admin, tôi muốn tích hợp với hệ thống chấm công QR/thẻ để thuận tiện quản lý.

## Quản trị hệ thống

### Tài khoản & phân quyền

- Là admin, tôi muốn tạo tài khoản cho HR, mentor và thực tập sinh để họ sử dụng hệ thống.
- Là admin, tôi muốn phân quyền chi tiết để kiểm soát chức năng mà mỗi vai trò có thể sử dụng.

### Bảo mật & sao lưu

- Là hệ thống, tôi muốn sao lưu dữ liệu định kỳ để đảm bảo an toàn.
- Là admin, tôi muốn xem nhật ký hoạt động để theo dõi các thao tác trong hệ thống.


## 4. Yêu cầu phi chức năng (NFR)
- **Bảo mật:** JWT, mật khẩu hash (BCrypt), phân quyền theo vai trò; audit log cho hành động quan trọng.
- **Hiệu năng:** danh sách/tra cứu có phân trang, lọc; API < 300ms cho truy vấn phổ biến (mục tiêu).
- **Khả dụng:** lỗi API trả về chuẩn JSON (error code/message), có retry cho email.
- **Mở rộng:** thiết kế theo module (intern/application/program/task/attendance/report/admin).
- **Tính đúng:** ràng buộc dữ liệu (FK, unique), trạng thái workflow rõ ràng.
- **UX:** giao diện responsive; các form có validate và thông báo lỗi rõ.

## 5. Out of scope (gợi ý)
- Tích hợp SSO/LDAP (có thể làm phase sau).
- Tính lương/chi trả phức tạp.
- Mobile app native.
