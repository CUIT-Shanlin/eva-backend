package edu.cuit.app;

import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.property.AvatarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 用户头像管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AvatarManager {

    private final AvatarProperties avatarProperties;

    private final UserQueryGateway userQueryGateway;

    /**
     * 获取用户头像base64数据
     * @param userId 用户id
     * @return 图片base64编码数据
     */
    public byte[] getUserAvatarBytes(Integer userId) {
        checkUserId(userId);
        try {
            File file = getUserAvatarFile(userId);
            if (!file.exists()) return new byte[]{};
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            log.error("读取用户图片失败",e);
            throw new SysException("用户头像读取失败，请联系管理员");
        }
    }

    /**
     * 上传用户头像
     * @param userId 用户id
     * @param imageInputStream 文件流
     */
    public void uploadUserAvatar(Integer userId, InputStream imageInputStream) {
        checkUserId(userId);
        File userAvatarFile = getUserAvatarFile(userId);
        try {
            BufferedImage readImage = ImageIO.read(imageInputStream);
            boolean jpg = ImageIO.write(readImage, "jpg", userAvatarFile);
            System.out.println(jpg);
        } catch (IOException e) {
            log.error("读取用户图片失败",e);
            throw new SysException("用户头像读取失败，请联系管理员");
        }
    }

    private void checkUserId(Integer id) {
        userQueryGateway.findUsernameById(id).orElseThrow(() -> new BizException("该用户不存在"));
    }

    private File getUserAvatarFile(Integer userId) {
        File directory = new File(avatarProperties.getDirectory());
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                SysException e = new SysException("发生内部异常");
                log.error("创建头像文件夹失败",e);
                throw e;
            }
        }
        return new File(directory, userId + ".jpg");
    }

}
