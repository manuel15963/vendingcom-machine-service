package com.vendingcom.machine_service.infrastructure.adapters.outbound.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "machine_documents")
public class MachineDocumentEntity {

    @Id
    @Column("document_id")
    private Integer documentId;

    @Column("machine_id")
    private Integer machineId;

    @Column("document_type_id")
    private Integer documentTypeId;

    @Column("file_name")
    private String fileName;

    @Column("file_url")
    private String fileUrl;

    @Column("file_size")
    private Long fileSize;

    @Column("mime_type")
    private String mimeType;

    @Column("uploaded_by_user_id")
    private Integer uploadedByUserId;

    @Column("uploaded_at")
    private LocalDateTime uploadedAt;
}
