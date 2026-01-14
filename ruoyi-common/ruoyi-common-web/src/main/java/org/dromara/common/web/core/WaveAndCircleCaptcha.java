package org.dromara.common.web.core;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.img.GraphicsUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 带干扰线、波浪、圆的验证码
 *
 * @author Lion Li
 */
public class WaveAndCircleCaptcha extends AbstractCaptcha {

    @Serial
    private static final long serialVersionUID = 1L;

    // 构造方法（略，与之前一致）
    public WaveAndCircleCaptcha(int width, int height) {
        this(width, height, 4);
    }

    public WaveAndCircleCaptcha(int width, int height, int codeCount) {
        this(width, height, codeCount, 6);
    }

    public WaveAndCircleCaptcha(int width, int height, int codeCount, int interfereCount) {
        this(width, height, new RandomGenerator(codeCount), interfereCount);
    }

    public WaveAndCircleCaptcha(int width, int height, CodeGenerator generator, int interfereCount) {
        super(width, height, generator, interfereCount);
    }

    public WaveAndCircleCaptcha(int width, int height, int codeCount, int interfereCount, float size) {
        super(width, height, new RandomGenerator(codeCount), interfereCount, size);
    }

    @Override
    public Image createImage(String code) {
        final BufferedImage image = new BufferedImage(
            width,
            height,
            (null == this.background) ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_INT_RGB
        );
        final Graphics2D g = ImgUtil.createGraphics(image, this.background);

        try {
            drawString(g, code);
            // 扭曲
            shear(g, this.width, this.height, ObjectUtil.defaultIfNull(this.background, Color.WHITE));
            drawInterfere(g);
        } finally {
            g.dispose();
        }

        return image;
    }

    private void drawString(Graphics2D g, String code) {
        // 设置抗锯齿（让字体渲染更清晰）
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (this.textAlpha != null) {
            g.setComposite(this.textAlpha);
        }

        GraphicsUtil.drawStringColourful(g, code, this.font, this.width, this.height);
    }

    protected void drawInterfere(Graphics2D g) {
        ThreadLocalRandom random = RandomUtil.getRandom();
        int circleCount = Math.max(0, this.interfereCount - 1);

        // 圈圈
        for (int i = 0; i < circleCount; i++) {
            g.setColor(ImgUtil.randomColor(random));
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int w = random.nextInt(height >> 1);
            int h = random.nextInt(height >> 1);
            g.drawOval(x, y, w, h);
        }

        // 仅 1 条平滑波浪线
        if (this.interfereCount >= 1) {
            g.setColor(getRandomColor(120, 230, random));
            drawSmoothWave(g, random);
        }
    }

    private void drawSmoothWave(Graphics2D g, ThreadLocalRandom random) {
        int amplitude = random.nextInt(8) + 5;        // 波动幅度
        int wavelength = random.nextInt(40) + 30;     // 波长
        double phase = random.nextDouble() * Math.PI * 2;

        // ✅ 关键：限制 baseY 在中间区域
        int centerY = height / 2;
        int verticalJitter = Math.max(5, height / 6); // 至少偏移5像素
        int baseY = centerY - verticalJitter + random.nextInt(verticalJitter * 2);

        g.setStroke(new BasicStroke(2.5f)); // 线宽

        int[] xPoints = new int[width];
        int[] yPoints = new int[width];
        for (int x = 0; x < width; x++) {
            int y = baseY + (int) (amplitude * Math.sin((double) x / wavelength * 2 * Math.PI + phase));
            // 限制 y 不要超出图像边界（可选）
            y = Math.max(amplitude, Math.min(y, height - amplitude));
            xPoints[x] = x;
            yPoints[x] = y;
        }
        g.drawPolyline(xPoints, yPoints, width);
    }

    private Color getRandomColor(int min, int max, ThreadLocalRandom random) {
        int range = max - min;
        return new Color(
            min + random.nextInt(range),
            min + random.nextInt(range),
            min + random.nextInt(range)
        );
    }

    /**
     * 扭曲
     *
     * @param g     {@link Graphics}
     * @param w1    w1
     * @param h1    h1
     * @param color 颜色
     */
    private void shear(Graphics g, int w1, int h1, Color color) {
        shearX(g, w1, h1, color);
        shearY(g, w1, h1, color);
    }

    /**
     * X坐标扭曲
     *
     * @param g     {@link Graphics}
     * @param w1    宽
     * @param h1    高
     * @param color 颜色
     */
    private void shearX(Graphics g, int w1, int h1, Color color) {

        int period = RandomUtil.randomInt(this.width);

        int frames = 1;
        int phase = RandomUtil.randomInt(2);

        for (int i = 0; i < h1; i++) {
            double d = (double) (period >> 1) * Math.sin((double) i / (double) period + (6.2831853071795862D * (double) phase) / (double) frames);
            g.copyArea(0, i, w1, 1, (int) d, 0);
            g.setColor(color);
            g.drawLine((int) d, i, 0, i);
            g.drawLine((int) d + w1, i, w1, i);
        }

    }

    /**
     * Y坐标扭曲
     *
     * @param g     {@link Graphics}
     * @param w1    宽
     * @param h1    高
     * @param color 颜色
     */
    private void shearY(Graphics g, int w1, int h1, Color color) {

        int period = RandomUtil.randomInt(this.height >> 1);

        int frames = 20;
        int phase = 7;
        for (int i = 0; i < w1; i++) {
            double d = (double) (period >> 1) * Math.sin((double) i / (double) period + (6.2831853071795862D * (double) phase) / (double) frames);
            g.copyArea(i, 0, 1, h1, 0, (int) d);
            g.setColor(color);
            // 擦除原位置的痕迹
            g.drawLine(i, (int) d, i, 0);
            g.drawLine(i, (int) d + h1, i, h1);
        }

    }

}
