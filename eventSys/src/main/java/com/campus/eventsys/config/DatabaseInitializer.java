package com.campus.eventsys.config;

import com.campus.eventsys.entity.User;
import com.campus.eventsys.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campus.eventsys.utils.CoverImageSeedUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 数据库初始化器
 * 应用启动时自动执行SQL脚本创建表和初始化数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** 上传根目录，与 WebMvcConfig 静态映射使用同一配置，保证 /images/** 能命中补齐后的文件 */
    @Value("${file.upload.path:uploads/images}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        try {
            // 检查是否需要初始化（通过检查user表是否存在）
            if (!isTableExists("user")) {
                log.info("数据库表不存在，开始初始化数据库...");
                executeScript("db/schema.sql");
                executeScript("db/data.sql");

                // 对初始用户的明文密码进行加密
                encryptInitialPasswords();

                log.info("数据库初始化完成！");
            } else {
                log.info("数据库表已存在，跳过初始化。");
            }

            // 兼容迁移：为老库补齐身份绑定相关列（user_type / teacher_id），保证升级不丢数据
            migrateUserIdentityColumns();

            // 封面图补齐：初始数据的 cover_image 指向的文件在上传目录中缺失时自动生成占位封面
            seedActivityCoverImages();

            // 成果贴图片补齐：activity_result.images 引用的文件缺失时同样自动生成占位图，
            // 保证 /achievements 成果卡片封面不出现加载失败
            seedActivityResultImages();
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 兼容迁移：user 表身份绑定列补齐
     * 用 PRAGMA table_info(user) 检查列是否存在，缺失则 ALTER TABLE ADD COLUMN；
     * 列已存在（新库由 schema.sql 建表）时为空操作，老库升级不影响既有数据。
     */
    private void migrateUserIdentityColumns() {
        try {
            // PRAGMA 不支持占位符参数，表名固定为 user 无注入风险
            List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(user)");
            boolean hasUserType = columns.stream()
                    .anyMatch(col -> "user_type".equalsIgnoreCase(String.valueOf(col.get("name"))));
            boolean hasTeacherId = columns.stream()
                    .anyMatch(col -> "teacher_id".equalsIgnoreCase(String.valueOf(col.get("name"))));

            // 用户身份：0-学生，1-教师；DEFAULT 0 兼容历史数据（历史账号学号/工号为空视为未绑定）
            if (!hasUserType) {
                jdbcTemplate.execute("ALTER TABLE user ADD COLUMN user_type INTEGER DEFAULT 0");
                log.info("兼容迁移：user 表已新增 user_type 列");
            }
            // 工号：教师为6位数字，学生为NULL【模拟校验规则，可替换为学籍接口对接】
            if (!hasTeacherId) {
                jdbcTemplate.execute("ALTER TABLE user ADD COLUMN teacher_id VARCHAR(50)");
                log.info("兼容迁移：user 表已新增 teacher_id 列");
            }

            // 与新版 schema.sql 对齐：补齐身份相关索引（IF NOT EXISTS 保证幂等）
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_teacher_id ON user(teacher_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_user_type ON user(user_type)");

            if (hasUserType && hasTeacherId) {
                log.info("user 表身份绑定列已存在，无需迁移。");
            }
        } catch (Exception e) {
            log.error("user 表身份绑定列迁移失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 封面图种子补齐
     * 初始数据（db/data.sql）中 cover_image 存的是 /images/activity_N/activityN.jpg 形式的站内路径，
     * 而上传目录 uploads/images/ 属于运行时数据（已被 .gitignore 忽略），克隆仓库或重新初始化后
     * 该目录为空，前端通过 /api/images/** 访问会 404。
     * 启动时扫描 activity 表全部封面路径，缺失的文件由 CoverImageSeedUtil 程序化生成占位封面；
     * 新库、老库每次启动都会执行，文件已存在则跳过，幂等安全。
     */
    private void seedActivityCoverImages() {
        try {
            if (!isTableExists("activity")) {
                return;
            }
            // 与 WebMvcConfig 保持一致：解析为运行目录下的绝对路径
            Path uploadsDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, title, cover_image FROM activity WHERE deleted = 0 AND cover_image IS NOT NULL AND cover_image <> ''");
            int generated = 0;
            for (Map<String, Object> row : rows) {
                // id / cover_image 任一异常（脏数据）时跳过该行，不影响其余封面
                Object idObj = row.get("id");
                Object coverObj = row.get("cover_image");
                if (!(idObj instanceof Number) || !(coverObj instanceof String)) {
                    continue;
                }
                boolean created = CoverImageSeedUtil.ensurePlaceholderCover(
                        (String) coverObj,
                        String.valueOf(row.get("title")),
                        ((Number) idObj).longValue(),
                        uploadsDir);
                if (created) {
                    generated++;
                }
            }
            if (generated > 0) {
                log.info("封面图补齐完成：本次新生成 {} 张占位封面，目录：{}", generated, uploadsDir);
            } else {
                log.info("封面图完整，无需补齐。");
            }
        } catch (Exception e) {
            // 封面补齐属于锦上添花，任何失败都不能阻断启动
            log.error("封面图补齐失败（不影响系统启动）: {}", e.getMessage(), e);
        }
    }

    /**
     * 成果贴图片种子补齐
     * 初始数据（db/data.sql）中部分成果贴的 images（JSON 数组，如 /images/result5_1.jpg）
     * 引用的文件从未落盘，前端 /achievements 成果卡片封面会显示加载失败。
     * 与 seedActivityCoverImages 同一机制：启动时扫描 activity_result 的 images 数组，
     * 缺失的文件由 CoverImageSeedUtil 程序化生成占位图；新库、老库每次启动都会执行，
     * 文件已存在则跳过，幂等安全。
     */
    private void seedActivityResultImages() {
        try {
            if (!isTableExists("activity_result")) {
                return;
            }
            // 与 WebMvcConfig 保持一致：解析为运行目录下的绝对路径
            Path uploadsDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT r.activity_id, r.title AS result_title, r.images, a.title AS activity_title "
                            + "FROM activity_result r LEFT JOIN activity a ON r.activity_id = a.id "
                            + "WHERE r.deleted = 0 AND r.images IS NOT NULL AND r.images <> ''");
            ObjectMapper objectMapper = new ObjectMapper();
            int generated = 0;
            for (Map<String, Object> row : rows) {
                // activity_id / images 任一异常（脏数据）时跳过该行，不影响其余成果贴
                Object idObj = row.get("activity_id");
                Object imagesObj = row.get("images");
                if (!(idObj instanceof Number) || !(imagesObj instanceof String)) {
                    continue;
                }
                // images 为 JSON 字符串数组，解析失败（脏数据）时跳过该行
                List<String> imageList;
                try {
                    imageList = objectMapper.readValue((String) imagesObj,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                } catch (Exception e) {
                    log.warn("成果贴图片字段解析失败，跳过: {}", imagesObj);
                    continue;
                }
                // 占位图上绘制所属活动标题（取不到时退回成果贴标题）
                Object activityTitleObj = row.get("activity_title");
                Object resultTitleObj = row.get("result_title");
                String drawTitle = activityTitleObj instanceof String && !((String) activityTitleObj).isEmpty()
                        ? (String) activityTitleObj
                        : (resultTitleObj instanceof String ? (String) resultTitleObj : null);
                long activityId = ((Number) idObj).longValue();
                for (String image : imageList) {
                    boolean created = CoverImageSeedUtil.ensurePlaceholderCover(
                            image, drawTitle, activityId, uploadsDir);
                    if (created) {
                        generated++;
                    }
                }
            }
            if (generated > 0) {
                log.info("成果贴图片补齐完成：本次新生成 {} 张占位图，目录：{}", generated, uploadsDir);
            } else {
                log.info("成果贴图片完整，无需补齐。");
            }
        } catch (Exception e) {
            // 成果贴图片补齐属于锦上添花，任何失败都不能阻断启动
            log.error("成果贴图片补齐失败（不影响系统启动）: {}", e.getMessage(), e);
        }
    }

    /**
     * 对初始用户的明文密码进行BCrypt加密
     */
    private void encryptInitialPasswords() {
        log.info("开始加密初始用户密码...");

        // 查询所有用户
        List<User> users = userMapper.selectList(null);

        for (User user : users) {
            String rawPassword = user.getPassword();

            // 检查密码是否已经是BCrypt格式（以$2a$开头）
            if (rawPassword != null && !rawPassword.startsWith("$2a$")) {
                // 对明文密码进行加密
                String encodedPassword = passwordEncoder.encode(rawPassword);
                user.setPassword(encodedPassword);
                userMapper.updateById(user);
                log.info("用户 [{}] 密码已加密", user.getUsername());
            }
        }

        log.info("初始用户密码加密完成！");
    }

    /**
     * 检查表是否存在
     */
    private boolean isTableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class,
                tableName
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行SQL脚本
     * 脚本文件为 UTF-8 编码，必须通过 EncodedResource 显式指定读取编码：
     * ScriptUtils.executeSqlScript(connection, Resource) 重载按 JVM 平台默认编码读取，
     * Windows 中文环境下为 GBK，会把 UTF-8 的中文注释与数据解码成乱码后写入数据库，
     * 导致活动标题、分类名等全部乱码，进而使占位封面上绘制的标题也是乱码。
     */
    private void executeScript(String scriptPath) {
        try (Connection connection = dataSource.getConnection()) {
            Resource resource = new ClassPathResource(scriptPath);
            if (resource.exists()) {
                log.info("开始执行SQL脚本: {}", scriptPath);
                ScriptUtils.executeSqlScript(connection, new EncodedResource(resource, StandardCharsets.UTF_8));
                log.info("执行SQL脚本成功: {}", scriptPath);
            } else {
                log.warn("SQL脚本不存在: {}", scriptPath);
            }
        } catch (SQLException e) {
            log.error("执行SQL脚本失败 [{}]: {}, SQL状态码: {}", scriptPath, e.getMessage(), e.getSQLState(), e);
            throw new RuntimeException("执行SQL脚本失败: " + scriptPath, e);
        }
    }
}
