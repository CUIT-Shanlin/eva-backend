package edu.cuit.app;

import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.infra.property.RecordDataProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class RecordImageManagerTest {

    @Autowired
    private RecordDataProperties recordDataProperties;

    @Autowired
    private RecordImageManager recordImageManager;

    @Test
    void uploadRecordImages_Success() throws IOException {
        // Arrange
        Integer recordId = 1;
        InputStream[] inputStreams = new InputStream[2];

        inputStreams[0] = new BufferedInputStream(
                new FileInputStream("D:\\deckFiles\\logo\\3015-full-logo.png"));

        inputStreams[1] = new BufferedInputStream(
                new FileInputStream("D:\\deckFiles\\touxiang.jpg")
        );

        recordImageManager.uploadRecordImages(recordId, inputStreams);
    }

    @Test
    void uploadRecordImages_IOException() throws IOException {
        // Arrange
        Integer recordId = 1;
        InputStream[] inputStreams = {mock(InputStream.class)};

        // Mock ImageIO.read() to throw IOException
        when(ImageIO.read(any(InputStream.class))).thenThrow(new IOException("Test IO Exception"));

        // Act & Assert
        SysException exception = assertThrows(SysException.class,
                () -> recordImageManager.uploadRecordImages(recordId, inputStreams));

        assertEquals("评教记录图片上传失败，请联系管理员", exception.getMessage());
    }

    @Test
    void uploadRecordImages_NullPointerException() throws IOException {
        // Arrange
        Integer recordId = 1;
        InputStream[] inputStreams = {mock(InputStream.class)};

        // Mock ImageIO.read() to return null
        when(ImageIO.read(any(InputStream.class))).thenReturn(null);

        // Act & Assert
        BizException exception = assertThrows(BizException.class,
                () -> recordImageManager.uploadRecordImages(recordId, inputStreams));

        assertEquals("图片格式暂不支持", exception.getMessage());
    }

    @Test
    void uploadRecordImages_EmptyArray() {
        // Arrange
        Integer recordId = 1;
        InputStream[] inputStreams = new InputStream[0];

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> recordImageManager.uploadRecordImages(recordId, inputStreams));
    }
}