package com.karasuno.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Summary of an ingested document shown in the Document Manager. */
@Data
@AllArgsConstructor
public class DocumentSummaryDto {
    private String documentName;
    private String documentType;
    private long totalChunks;
}
