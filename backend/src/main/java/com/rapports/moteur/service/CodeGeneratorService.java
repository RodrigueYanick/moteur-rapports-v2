package com.rapports.moteur.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Service
public class CodeGeneratorService {

    /** Génère un QR code et retourne une data URI base64 prête pour <img src="...">. */
    public String generateQrCodeDataUri(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1
            );
            BitMatrix matrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, size, size, hints
            );
            return toDataUri(matrix);
        } catch (Exception e) {
            return null;
        }
    }

    /** Génère un code-barres (Code 128) et retourne une data URI base64. */
    public String generateBarcodeDataUri(String content, int width, int height) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.CODE_128, width, height
            );
            return toDataUri(matrix);
        } catch (Exception e) {
            return null;
        }
    }

    private String toDataUri(BitMatrix matrix) throws IOException, WriterException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
        return "data:image/png;base64," + base64;
    }
}