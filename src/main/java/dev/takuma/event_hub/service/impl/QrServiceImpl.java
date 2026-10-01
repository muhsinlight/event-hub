package dev.takuma.event_hub.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.takuma.event_hub.service.QrService;
import dev.takuma.event_hub.utils.ServerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class QrServiceImpl implements QrService {

	@Override
	public byte[] png(String text) {
		try {
			BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(matrix, "PNG", output);
			return output.toByteArray();
		}
		catch (WriterException | IOException exception) {
			throw new ServerException("QR could not be created", exception);
		}
	}

	@Override
	public String base64(String text) {
		return Base64.getEncoder().encodeToString(png(text));
	}

}
