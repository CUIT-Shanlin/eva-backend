package edu.cuit.app;

import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.infra.property.RecordDataProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 评教记录照片管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecordImageManager {

    private final RecordDataProperties recordDataProperties;

    /**
     * 获取评教记录图片base64数据
     *
     * @param recordId 评教记录id
     * @return 图片base64编码数据
     */
    public List<String> getRecordImages(Integer recordId) {
        List<File> recordImageFiles = getRecordImageFile(recordId);
        if (recordImageFiles.isEmpty()) return List.of();
        // 图片转换为base64
        return recordImageFiles.stream().map(file -> {
                    try {
                        return Files.readAllBytes(file.toPath());
                    } catch (IOException e) {
                        log.error("读取评教记录照片失败", e);
                        throw new SysException("读取评教记录照片失败，请联系管理员");
                    }
                }).map(bytes -> "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(bytes))
                .toList();
    }
//TODO
    /**
     * 上传评教记录图片
     * @param recordId           评教记录id
     * @param imageInputStream 文件流
     */
    public void uploadRecordImages(Integer recordId, InputStream[] imageInputStream) {
        for (int i = 0; i < imageInputStream.length; i++) {
            try {
                BufferedImage readImage = ImageIO.read(imageInputStream[i]);
                int width = readImage.getWidth();
                int height = readImage.getHeight();

                BufferedImage bfImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                bfImage.getGraphics().drawImage(readImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);

                BufferedImage whiteImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                whiteImage.createGraphics().drawImage(bfImage, 0, 0, Color.WHITE, null);

                ImageIO.write(whiteImage, "jpg", new File(new File(recordDataProperties.getDirectory()), recordId + "-" + i + ".jpg"));
            } catch (IOException e) {
                log.error("上传评教记录图片失败", e);
                throw new SysException("评教记录图片上传失败，请联系管理员");
            } catch (NullPointerException e) {
                throw new BizException("图片格式暂不支持");
            }
        }

    }

    private List<File> getRecordImageFile(Integer recordId) {
        File directory = new File(recordDataProperties.getDirectory());
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                SysException e = new SysException("发生内部异常");
                log.error("创建评教记录图片文件夹失败", e);
                throw e;
            }
        }
        int count = 0;
        List<File> files = new ArrayList<>();
        File file = new File(directory, recordId + "-" + count + ".jpg");
        while (file.exists()) {
            files.add(file);
            file = new File(directory, recordId + "-" + (++count) + ".jpg");
        }
        return files;
    }

}
