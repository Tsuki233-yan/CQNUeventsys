package com.campus.eventsys.utils;

import lombok.extern.slf4j.Slf4j;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * 活动封面占位图生成工具
 * 数据初始脚本 db/data.sql 中 cover_image 存的是 /images/activity_N/activityN.jpg 形式的站内路径，
 * 而上传目录 uploads/images/ 属于运行时数据（已被 .gitignore 忽略），克隆仓库或重新初始化后
 * 目录为空，前端通过 /api/images/** 访问会 404。
 * 本工具在启动时对缺失的封面文件程序化生成一张占位封面（渐变底 + 活动标题），
 * 使用 JDK 自带的 BufferedImage/ImageIO，不引入任何第三方依赖，保证全新环境也能正常显示图片。
 */
@Slf4j
public class CoverImageSeedUtil {

    private CoverImageSeedUtil() {}

    /** 占位封面尺寸（16:9），与前端封面展示比例一致 */
    private static final int WIDTH = 800;
    private static final int HEIGHT = 450;

    /** 渐变配色板：{起点R,G,B, 终点R,G,B}，按活动 id 取模轮换，保证同一活动颜色固定 */
    private static final int[][] GRADIENTS = {
            {66, 133, 244, 21, 101, 192},    // 蓝
            {52, 168, 83, 27, 94, 32},       // 绿
            {255, 167, 38, 230, 81, 0},      // 橙
            {171, 71, 188, 74, 20, 140},     // 紫
            {255, 112, 67, 183, 28, 28},     // 红
            {0, 172, 193, 1, 87, 155},       // 青
            {121, 85, 72, 62, 39, 35},       // 棕
            {94, 53, 177, 48, 63, 159}       // 深紫蓝
    };

    /** 中文字体优先级列表：按平台可用性依次选择，兜底为逻辑字体 SansSerif */
    private static final String[] PREFERRED_FONTS = {
            "Microsoft YaHei", "微软雅黑", "SimHei", "黑体",
            "Noto Sans CJK SC", "Source Han Sans SC", "思源黑体",
            "PingFang SC", "WenQuanYi Micro Hei"
    };

    /**
     * 确保封面文件存在：目标文件缺失时生成占位封面
     *
     * @param coverImage 数据库中的封面路径（如 /images/activity_1/activity1.jpg）
     * @param title      活动标题（绘制到占位图上）
     * @param activityId 活动 id（用于选取配色与编号水印）
     * @param uploadsDir 上传根目录（即配置 file.upload.path 的绝对路径，如 uploads/images）
     * @return 是否新生成了封面文件
     */
    public static boolean ensurePlaceholderCover(String coverImage, String title, Long activityId, Path uploadsDir) {
        try {
            // 仅处理站内相对路径；外链（http/https）与空路径直接跳过
            String clean = coverImage == null ? "" : coverImage.trim();
            String lower = clean.toLowerCase(Locale.ROOT);
            if (lower.startsWith("http://") || lower.startsWith("https://") || clean.isEmpty()) {
                return false;
            }
            clean = clean.replace('\\', '/');
            while (clean.startsWith("/")) {
                clean = clean.substring(1);
            }
            // 兼容两种库内路径前缀：/uploads/images/**（新上传）与 /images/**（初始数据）
            String relative;
            if (clean.startsWith("uploads/images/")) {
                relative = clean.substring("uploads/images/".length());
            } else if (clean.startsWith("images/")) {
                relative = clean.substring("images/".length());
            } else {
                // 非上传目录内的路径，不越权处理
                return false;
            }
            if (relative.isEmpty()) {
                return false;
            }

            Path target = uploadsDir.resolve(relative).normalize();
            // 安全校验：解析后的目标必须仍位于上传目录内，防止恶意数据路径穿越
            if (!target.startsWith(uploadsDir.normalize())) {
                log.warn("封面路径越界，跳过生成: {}", coverImage);
                return false;
            }
            // 文件已存在则无需补齐
            if (Files.exists(target)) {
                return false;
            }

            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            generate(target, title == null || title.isEmpty() ? "校园活动" : title,
                    activityId == null ? 0L : activityId);
            log.info("已生成占位封面: {}", target);
            return true;
        } catch (Exception e) {
            // 单张封面生成失败只记录日志，不影响启动与其他封面
            log.warn("生成占位封面失败 [{}]: {}", coverImage, e.getMessage());
            return false;
        }
    }

    /**
     * 绘制并写出占位封面 JPEG：对角渐变底 + 半透明装饰圆 + 分类角标 + 活动标题
     */
    private static void generate(Path target, String title, Long activityId) throws Exception {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // 抗锯齿，保证中文文字平滑
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 1. 对角线性渐变背景（按活动 id 从配色板轮换，同一活动颜色固定）
            int[] colors = GRADIENTS[(int) (Math.abs(activityId) % GRADIENTS.length)];
            Color start = new Color(colors[0], colors[1], colors[2]);
            Color end = new Color(colors[3], colors[4], colors[5]);
            g.setPaint(new GradientPaint(0, 0, start, WIDTH, HEIGHT, end));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 2. 半透明装饰圆，增加层次感
            g.setComposite(AlphaComposite.SrcOver.derive(0.12f));
            g.setColor(Color.WHITE);
            g.fillOval(560, -120, 360, 360);
            g.fillOval(-90, 300, 260, 260);
            g.setComposite(AlphaComposite.SrcOver.derive(0.08f));
            g.fillOval(660, 250, 220, 220);
            g.setComposite(AlphaComposite.SrcOver); // 恢复完全不透明

            // 3. 左上角分类角标：「校园活动」圆角标签
            Font tagFont = pickFont(Font.PLAIN, 22);
            g.setFont(tagFont);
            FontMetrics tagMetrics = g.getFontMetrics();
            String tagText = "校园活动";
            int tagPaddingX = 16;
            int tagWidth = tagMetrics.stringWidth(tagText) + tagPaddingX * 2;
            int tagHeight = tagMetrics.getHeight() + 10;
            g.setColor(new Color(255, 255, 255, 70));
            g.fill(new RoundRectangle2D.Float(36, 34, tagWidth, tagHeight, 18, 18));
            g.setColor(Color.WHITE);
            g.drawString(tagText, 36 + tagPaddingX, 34 + tagMetrics.getAscent() + 4);

            // 4. 活动标题：大号粗体，自动换行最多 3 行，超出省略
            Font titleFont = pickFont(Font.BOLD, 46);
            g.setFont(titleFont);
            FontMetrics titleMetrics = g.getFontMetrics();
            int maxWidth = WIDTH - 96;
            List<String> lines = wrapText(title, titleMetrics, maxWidth, 3);
            int lineHeight = titleMetrics.getHeight() + 6;
            int textBottom = HEIGHT - 80; // 标题整体贴向底部
            int firstLineTop = textBottom - lines.size() * lineHeight;
            g.setColor(Color.WHITE);
            int y = firstLineTop + titleMetrics.getAscent();
            for (String line : lines) {
                g.drawString(line, 48, y);
                y += lineHeight;
            }

            // 5. 右下角活动编号水印
            Font noFont = pickFont(Font.PLAIN, 20);
            g.setFont(noFont);
            FontMetrics noMetrics = g.getFontMetrics();
            String noText = "NO." + String.format("%04d", activityId);
            g.setColor(new Color(255, 255, 255, 160));
            g.drawString(noText, WIDTH - 48 - noMetrics.stringWidth(noText), HEIGHT - 36);

            File outputFile = target.toFile();
            // JPEG 不支持透明通道，TYPE_INT_RGB 直接写出即可
            ImageIO.write(image, "jpg", outputFile);
        } finally {
            g.dispose();
        }
    }

    /**
     * 文字自动换行：逐字符累计宽度，超出 maxWidth 换行；超过 maxLines 行时末尾以「…」省略
     */
    private static List<String> wrapText(String text, FontMetrics metrics, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                c = ' ';
            }
            current.append(c);
            if (metrics.stringWidth(current.toString()) > maxWidth) {
                // 当前字符放不下，回退一个字符换行
                if (current.length() > 1) {
                    current.deleteCharAt(current.length() - 1);
                }
                lines.add(current.toString());
                current.setLength(0);
                current.append(c);
                if (lines.size() == maxLines) {
                    // 达到行数上限，截断并省略
                    String last = lines.get(maxLines - 1);
                    lines.set(maxLines - 1, abbreviate(last, metrics, maxWidth));
                    return lines;
                }
            }
        }
        if (current.length() > 0) {
            if (lines.size() == maxLines) {
                lines.set(maxLines - 1, abbreviate(lines.get(maxLines - 1), metrics, maxWidth));
            } else {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    /** 将一行文字截断到指定宽度并以「…」结尾 */
    private static String abbreviate(String line, FontMetrics metrics, int maxWidth) {
        StringBuilder builder = new StringBuilder(line);
        while (builder.length() > 1 && metrics.stringWidth(builder + "…") > maxWidth) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder + "…";
    }

    /**
     * 选择可用的中文字体：按优先级在系统已安装字体中查找，找不到时退回逻辑字体 SansSerif
     * （Windows/Linux 桌面环境下逻辑字体通常可正常渲染中文，极端环境下退化为方框也不影响系统启动）
     */
    private static Font pickFont(int style, int size) {
        try {
            Set<String> available = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String name : PREFERRED_FONTS) {
                if (available.contains(name)) {
                    return new Font(name, style, size);
                }
            }
        } catch (Exception ignored) {
            // 字体枚举失败时使用逻辑字体兜底
        }
        return new Font(Font.SANS_SERIF, style, size);
    }
}
