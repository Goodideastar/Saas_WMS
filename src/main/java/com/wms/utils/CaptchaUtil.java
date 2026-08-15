package com.wms.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Component
public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_COUNT = 4;
    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final long CAPTCHA_EXPIRE_MINUTES = 3;

    private final Random random = new Random();

    public CaptchaResult generate(StringRedisTemplate redisTemplate) {
        String code = generateCode();
        String key = UUID.randomUUID().toString().replace("-", "");

        BufferedImage image = drawImage(code);
        String base64Image = toBase64(image);

        redisTemplate.opsForValue().set("captcha:" + key, code, CAPTCHA_EXPIRE_MINUTES, java.util.concurrent.TimeUnit.MINUTES);

        return new CaptchaResult(key, base64Image);
    }

    public void verify(StringRedisTemplate redisTemplate, String key, String code) {
        if (key == null || code == null) {
            throw new IllegalArgumentException("Captcha key or code is null");
        }
        Object cached = redisTemplate.opsForValue().get("captcha:" + key);
        if (cached == null) {
            throw new IllegalArgumentException("Captcha expired");
        }
        if (!cached.toString().equalsIgnoreCase(code)) {
            throw new IllegalArgumentException("Captcha code is incorrect");
        }
        redisTemplate.delete("captcha:" + key);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_COUNT; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private BufferedImage drawImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(new Color(240, 245, 250));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Noise lines
        for (int i = 0; i < 60; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = x1 + random.nextInt(3) - 1;
            int y2 = y1 + random.nextInt(3) - 1;
            g2d.setColor(new Color(random.nextInt(180), random.nextInt(180), random.nextInt(180)));
            g2d.drawLine(x1, y1, x2, y2);
        }

        Color[] colors = {new Color(6, 182, 212), new Color(59, 130, 246),
                new Color(139, 92, 246), new Color(236, 72, 153)};
        Font[] fonts = {new Font("Arial", Font.BOLD, 24),
                new Font("Verdana", Font.BOLD, 22),
                new Font("Georgia", Font.BOLD, 24),
                new Font("Courier New", Font.BOLD, 23)};

        for (int i = 0; i < code.length(); i++) {
            g2d.setColor(colors[i % colors.length]);
            g2d.setFont(fonts[i % fonts.length]);
            float x = 12 + i * 26 + random.nextFloat() * 4;
            float y = 28 + random.nextFloat() * 6 - 3;
            double angle = (random.nextDouble() - 0.5) * 0.4;
            AffineTransform transform = AffineTransform.getRotateInstance(angle, x, y);
            g2d.setTransform(transform);
            g2d.drawString(String.valueOf(code.charAt(i)), x, y);
            g2d.setTransform(new AffineTransform());
        }

        // Curved干扰线
        for (int i = 0; i < 3; i++) {
            g2d.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            g2d.setStroke(new BasicStroke(1.5f));
            GeneralPath path = new GeneralPath();
            path.moveTo(random.nextInt(WIDTH), random.nextInt(HEIGHT));
            path.quadTo(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
            g2d.draw(path);
        }

        g2d.dispose();
        return image;
    }

    private String toBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }
    }

    public record CaptchaResult(String key, String image) {}
}
