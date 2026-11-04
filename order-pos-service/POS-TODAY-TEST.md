# POS: voucher, điểm, đơn tạm và hủy đơn — local

Mở `http://localhost:5173/pos`, đăng nhập `admin` / `123456`, chọn Chi nhánh 01.
Chưa tạo PR/push cho phần này. Các thay đổi tích hợp chi nhánh/kho trước đó vẫn được giữ.

## Chính sách khởi đầu

- Khách vãng lai không dùng điểm. Thành viên dùng số dư thật từ `db_order.loyalty_accounts`.
- 1 điểm = 1.000đ; tối đa 20% tiền hàng sau voucher; được cộng dồn nếu voucher cho phép.
- Cấu hình bằng `pos.loyalty.enabled`, `point-value`, `max-redemption-percent`, `allow-voucher-stack`.
- Chỉ một voucher trên đơn. Kiểm tra hạn, trạng thái, đơn tối thiểu, giới hạn tổng lượt/thành viên và điều kiện thành viên.
- Tính thử không tiêu hao lượt/điểm. F10 kiểm tra lại giá Catalog và quyền lợi, lưu vào transaction với đơn/payment.
- VAT 8% tính trên tiền hàng đã giảm. Không tự tích điểm từ doanh thu trong task trừ điểm này.
- Hủy thành công hoàn lại điểm đã dùng và lượt voucher. Voucher đã hết hạn vẫn hết hạn sau hoàn.

## Dữ liệu test local

`src/main/resources/local-pos-test-data.sql` chỉ chạy thủ công ở local, không tự chạy production:

| Mã | Ưu đãi | Đơn tối thiểu |
|---|---|---|
| POS10 | 10%, tối đa 100.000đ | 500.000đ |
| POS50K | 50.000đ | 500.000đ |
| EXPIRED | Đã hết hạn, phải bị từ chối | — |

Mã hợp lệ có 100 lượt, hạn 30 ngày từ lần tạo đầu. SQL không ghi đè số dư/lượt dùng hiện có.
Thành viên test: customer ID 10, SĐT `0918732998`, được cấp ban đầu 100 điểm trong fixture local.

## Test F4

1. Chọn khách, thêm hàng, bấm F4: lưu thành công rồi giỏ trở về Khách vãng lai.
2. Tạo giỏ B. Mở Đơn tạm, chọn Khôi phục A: xác nhận lưu B trước khi thay thế.
3. Reload trang: đơn tạm vẫn có. Các đơn được tách theo thu ngân/chi nhánh.
4. Khôi phục kiểm tra lại tồn/giá; thiếu hàng thì giữ nguyên bản tạm và báo SKU.
5. Không lưu được localStorage thì giữ nguyên giỏ. Không giữ trước tồn/điểm/lượt voucher.

## Test voucher/điểm/K80

1. Thêm sản phẩm trên 500.000đ. Mở ưu đãi, nhập POS10 rồi Áp dụng.
2. Chọn thành viên theo SĐT, nhập 50 điểm nếu giới hạn cho phép.
3. Đối chiếu Tạm tính − Voucher − Điểm = Tiền tính thuế; cộng VAT 8% = Tổng.
4. F10, in K80: phải có mã voucher, số điểm, giá trị từng khoản giảm và VAT đã lưu.
5. Thử EXPIRED, mã không tồn tại, điểm vượt số dư và khách vãng lai dùng điểm: phải bị chặn.

## Test hủy và hoàn tiền

1. Bấm Hủy đơn đã thanh toán, tra mã đơn, nhập lý do.
2. Chỉ đánh dấu Đã hoàn tiền thực tế khi thật sự đã trả tiền cho khách. Không chọn thì payment ở REFUND_PENDING.
3. Xác nhận hủy: CANCEL_PENDING trong khi chờ kho; CANCELLED khi kho và quyền lợi đã xử lý.
4. Kiểm tra kho gốc tăng lại, điểm và lượt voucher được hoàn. Gửi hủy lại không hoàn thêm lần nữa.
5. Sau khi hủy xong, có thể Xác nhận đã hoàn tiền: payment đổi REFUNDED. Không có tích hợp tự trả tiền ngân hàng.
6. Khi Catalog lỗi mạng/5xx, tác vụ PENDING được thử lại. Lỗi nghiệp vụ/4xx cần đối chiếu rồi bấm Thử lại hoàn tất hủy.

Catalog lưu dấu hủy kể cả khi yêu cầu trừ kho chưa tới, ngăn trừ kho muộn. Worker và hủy đơn khóa cùng thứ tự và dùng transaction mới sau commit.

## API và dữ liệu

- POST `/api/promotions/apply`: tính thử; POST `/api/orders`: thanh toán, gửi Idempotency-Key.
- GET `/api/loyalty/accounts/{id}`: số dư và chính sách.
- POST `/api/promotions/vouchers`: Admin tạo voucher; POST `/api/loyalty/accounts/{id}/adjust`: Admin điều chỉnh điểm có lý do.
- PATCH `/api/orders/{id}/cancel`: `{reason, refundConfirmed}`; POST `/api/orders/{id}/refund-confirm`: xác nhận hoàn tiền thực tế.
- Quyền POS/ADMIN được xác minh qua Identity; các API quản trị chỉ ADMIN.
- Bảng: vouchers, voucher_redemptions, loyalty_accounts, loyalty_entries, orders, payments, stock_deduction_outbox; Catalog: inventory_deductions, inventory_movements.
- Schema mới theo `ddl-auto: update` hiện có của nhóm; cần migration chính thức trước triển khai môi trường chung.

## Kiểm thử

Maven test dùng H2 test riêng:
Chạy bằng JDK 25 theo `pom.xml`. Phải chỉ định cả datasource H2, không chỉ đổi dialect/DDL (test upstream có thể dùng datasource MySQL mặc định):

`mvn -pl catalog-service,order-pos-service -am "-Dspring.datasource.url=jdbc:h2:mem:pos-test;MODE=MySQL;DB_CLOSE_DELAY=-1" -Dspring.datasource.driver-class-name=org.h2.Driver -Dspring.datasource.username=sa -Dspring.datasource.password= -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect -Dspring.jpa.hibernate.ddl-auto=create-drop test`

Đã có test transaction cho quyền lợi/idempotency, hoàn kho đúng một lần và ngăn trừ muộn. Script QA local kiểm tra API/MySQL và trình duyệt, tạo rồi hủy đơn test để hoàn trả số dư và tồn.
