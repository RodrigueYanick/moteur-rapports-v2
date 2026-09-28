package com.rapports.moteur.service.rendering;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageExportResult {
    private byte[] data;
    private String contentType;
    private String filename;
    private int pageCount;
    private boolean isZip;
}
