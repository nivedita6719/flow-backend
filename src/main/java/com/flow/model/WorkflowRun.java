package com.flow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RunStatus status = RunStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private Workflow.TriggerType triggerType;

    @Column(columnDefinition = "TEXT")
    private String triggerPayload;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer durationMs;

    @PrePersist
    public void prePersist() {
        this.startedAt = LocalDateTime.now();
    }

    public enum RunStatus {
        PENDING, RUNNING, SUCCESS, FAILED
    }
}
