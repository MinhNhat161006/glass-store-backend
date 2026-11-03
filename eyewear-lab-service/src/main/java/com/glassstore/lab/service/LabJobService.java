package com.glassstore.lab.service;

import com.glassstore.lab.dto.CreateLabJobRequest;
import com.glassstore.lab.dto.LabJobResponse;
import com.glassstore.lab.dto.UpdateJobStatusRequest;
import com.glassstore.lab.entity.LabJobStatus;

import java.util.List;

public interface LabJobService {

    /**
     * Lấy danh sách lệnh gia công mài kính, sắp xếp giảm dần theo createdAt.
     * Nạp kèm thông tin thông số mắt từ PrescriptionRepository vào LabJobResponse.
     * Nếu không có kết quả thì trả về danh sách rỗng [], không ném lỗi.
     *
     * @param status Trạng thái gia công cần lọc (tùy chọn)
     * @return Danh sách LabJobResponse
     */
    List<LabJobResponse> getLabJobs(LabJobStatus status);

    /**
     * Lấy chi tiết lệnh mài kính theo ID kèm thông số toa kính.
     *
     * @param id ID của lệnh mài kính
     * @return LabJobResponse
     */
    LabJobResponse getLabJobById(Long id);

    /**
     * Cập nhật trạng thái gia công mài lắp kính & kiểm định QC.
     * - IN_PROGRESS: gán startedAt = LocalDateTime.now(), technicianId.
     * - QC_PASSED / COMPLETED: gán completedAt = LocalDateTime.now().
     * - QC_FAILED: bắt buộc qcNotes không được rỗng (throw IllegalArgumentException).
     *
     * @param id ID của lệnh mài kính
     * @param request Thông tin cập nhật trạng thái
     * @return LabJobResponse sau cập nhật
     */
    LabJobResponse updateJobStatus(Long id, UpdateJobStatusRequest request);

    /**
     * Tạo mới lệnh mài kính với trạng thái ban đầu là PENDING.
     *
     * @param request Thông tin tạo lệnh gia công
     * @return LabJobResponse vừa được tạo
     */
    LabJobResponse createLabJob(CreateLabJobRequest request);
}

