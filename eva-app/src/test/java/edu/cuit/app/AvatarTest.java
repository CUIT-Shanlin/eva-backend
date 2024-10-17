package edu.cuit.app;

import edu.cuit.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

@SpringBootTest(classes = Application.class)
public class AvatarTest {

    private final String PATH = "D:\\Programming\\Java\\Projects\\eva-backend\\avatar";

    @Autowired
    private AvatarManager avatarManager;

    @Test
    public void testDirectory() {
        System.out.println(System.getProperty("user.dir"));
    }

    @Test
    public void testReadAvatar() {
        System.out.println(Arrays.toString(avatarManager.getUserAvatarBytes(1)));
    }

    @Test
    public void testUploadAvatar() throws FileNotFoundException {
        File file = new File("D:\\deckFiles\\CUIT校标\\成都信息工程大学校标logo发布\\校标-标准色反.jpg");
        avatarManager.uploadUserAvatar(2,new BufferedInputStream(new FileInputStream(file)));
    }

}
