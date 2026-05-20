package com.flow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "node_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private WorkflowRun workflowRun;

    @Column(nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private String nodeType;

    @Column(columnDefinition = "TEXT")
    private String inputJson;

    @Column(columnDefinition = "TEXT")
    private String outputJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NodeStatus status = NodeStatus.PENDING;

    private String errorMessage;

    private Integer durationMs;

    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt;

    @PrePersist
    public void prePersist() {
        this.executedAt = LocalDateTime.now();
    }

    public enum NodeStatus {
        PENDING, SUCCESS, FAILED, SKIPPED
    }
}
