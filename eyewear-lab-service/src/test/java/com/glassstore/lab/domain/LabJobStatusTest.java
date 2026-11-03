package com.glassstore.lab.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabJobStatusTest {

    @Test
    @DisplayName("QC must pass before handover (BR-34)")
    void qcGate() {
        assertThat(LabJobStatus.QC_CHECK.canTransitionTo(LabJobStatus.READY_FOR_DELIVERY)).isFalse();
        assertThat(LabJobStatus.QC_PASSED.canTransitionTo(LabJobStatus.READY_FOR_DELIVERY)).isTrue();
        assertThat(LabJobStatus.READY_FOR_DELIVERY.canTransitionTo(LabJobStatus.DELIVERED)).isTrue();
    }

    @Test
    @DisplayName("Failed QC can be reworked")
    void rework() {
        assertThat(LabJobStatus.QC_FAILED.canTransitionTo(LabJobStatus.IN_PROGRESS)).isTrue();
        assertThat(LabJobStatus.QC_FAILED.requiresReasonToEnter()).isTrue();
    }

    @Test
    @DisplayName("Legacy aliases still parse")
    void aliases() {
        assertThat(LabJobStatus.parse("WAITING")).isEqualTo(LabJobStatus.WAITING_PROCESS);
        assertThat(LabJobStatus.parse("READY_FOR_HANDOVER")).isEqualTo(LabJobStatus.READY_FOR_DELIVERY);
        assertThatThrownBy(() -> LabJobStatus.parse("NOT_A_STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
