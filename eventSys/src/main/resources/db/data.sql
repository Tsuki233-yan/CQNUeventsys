-- 校园活动综合管理系统 初始数据脚本 (SQLite)
-- 数据本地化改造：重庆师范大学场景
-- 数据基准时间：2026年11月（2026-2027学年第一学期期中），全部活动日期以该基准设计相对天数；
--   学期语义对应：秋季运动会/新生杯/师范生技能大赛对应秋季学期前半段，元旦晚会对应秋季学期末，
--   三下乡成果展对应暑期实践结束后的秋季展示期，十一月批次支教对应本学期期中后，均无过期或矛盾的学期描述
-- 三个校区：大学城校区（主校区）、沙坪坝校区、北碚校区
-- 所有时间采用 datetime('now', '+N days') 相对写法，随启动时间动态生成
-- 用户密码统一为 test，由 DatabaseInitializer.encryptInitialPasswords() 启动时自动 BCrypt 加密
-- 主外键引用自洽说明：
--   activity_registration 按插入顺序编号：组织者记录 id=1~21（活动1~21 各一条），
--   参与者记录 id=22~104（行尾注释标注隐含 id，签到表 registration_id 严格按此引用）

-- =============================================
-- 1. 插入用户数据（共 20 条：1 管理员 + 6 教师 + 13 学生）
-- 身份绑定：学生 user_type=0 且填写 10 位数字学号（student_id）、teacher_id 为 NULL；
--           教师 user_type=1 且填写 6 位数字工号（teacher_id）、student_id 为 NULL；
--           管理员 role=1 豁免身份绑定校验，user_type=0 且学号/工号为 NULL
-- 邮箱统一 @cqnu.edu.cn；学院均为重庆师范大学真实学院名
-- =============================================
INSERT INTO user (username, password, nickname, email, phone, real_name, user_type, student_id, teacher_id, college, major, grade, gender, bio, role, status, organized_count, participated_count, create_time, update_time, deleted) VALUES
('admin', 'test', '系统管理员', 'admin@cqnu.edu.cn', NULL, '管理员', 0, NULL, NULL, '网络信息管理中心', '系统管理', NULL, 1, '系统管理员账号，负责平台维护与活动审核', 1, 1, 0, 0, datetime('now', '-120 days'), datetime('now'), 0),
('liminghua', 'test', '李明华', 'limh@cqnu.edu.cn', '13752860021', '李明华', 1, NULL, '200101', '计算机学院', '计算机科学与技术', NULL, 1, '计算机学院副教授，研究方向为人工智能教育应用，主讲《现代教育技术》', 0, 1, 2, 0, datetime('now', '-118 days'), datetime('now'), 0),
('wangxuemei', 'test', '王雪梅', 'wangxm@cqnu.edu.cn', '13752860022', '王雪梅', 1, NULL, '200102', '历史与社会学院', '历史学（课程与教学论方向）', NULL, 0, '历史与社会学院副教授，校师范生技能训练中心指导教师，热心基础教育公益', 0, 1, 3, 0, datetime('now', '-117 days'), datetime('now'), 0),
('zhangjianjun', 'test', '张建军', 'zhangjj@cqnu.edu.cn', '13752860023', '张建军', 1, NULL, '200103', '体育与健康科学学院', '体育教育', NULL, 1, '体育与健康科学学院教师，田径国家级裁判，校田径队主教练', 0, 1, 2, 0, datetime('now', '-116 days'), datetime('now'), 0),
('chenxiaofang', 'test', '陈晓芳', 'chenxf@cqnu.edu.cn', '13752860024', '陈晓芳', 1, NULL, '200104', '教育科学学院', '应用心理学', NULL, 0, '教育科学学院教师，应用心理学方向，长期指导师范生技能训练与支教志愿服务', 0, 1, 4, 0, datetime('now', '-115 days'), datetime('now'), 0),
('liuzhiqiang', 'test', '刘志强', 'liuzq@cqnu.edu.cn', '13752860025', '刘志强', 1, NULL, '200105', '经济与管理学院', '工商管理', NULL, 1, '经济与管理学院教师，校就业指导中心兼职讲师，专注大学生职业生涯规划教育', 0, 1, 2, 0, datetime('now', '-114 days'), datetime('now'), 0),
('zhaoqin', 'test', '赵倩', 'zhaoq@cqnu.edu.cn', '13752860026', '赵倩', 1, NULL, '200106', '音乐学院', '音乐学', NULL, 0, '音乐学院教师，校合唱团常任指挥，校园歌手大赛评委', 0, 1, 2, 0, datetime('now', '-113 days'), datetime('now'), 0),
('2023011234', 'test', '张雨欣', '2023011234@cqnu.edu.cn', '15823740001', '张雨欣', 0, '2023011234', NULL, '计算机学院', '软件工程', '2023', 0, '软件工程专业，校编程兴趣小组成员，喜欢用技术解决校园里的小问题', 0, 1, 0, 10, datetime('now', '-95 days'), datetime('now'), 0),
('2022012234', 'test', '李浩然', '2022012234@cqnu.edu.cn', '15823740002', '李浩然', 0, '2022012234', NULL, '物理与电子工程学院', '物理学（师范）', '2022', 1, '物理学（师范）专业，考研备考中，偶尔弹弹吉他', 0, 1, 0, 7, datetime('now', '-92 days'), datetime('now'), 0),
('2023013234', 'test', '王思远', '2023013234@cqnu.edu.cn', '15823740003', '王思远', 0, '2023013234', NULL, '文学院', '汉语言文学（师范）', '2023', 0, '汉语言文学（师范）专业，学生会文艺部成员，普通话一级乙等', 0, 1, 2, 7, datetime('now', '-91 days'), datetime('now'), 0),
('2022024234', 'test', '刘承宇', '2022024234@cqnu.edu.cn', '15823740004', '刘承宇', 0, '2022024234', NULL, '体育与健康科学学院', '体育教育（师范）', '2022', 1, '体育教育（师范）专业，校篮球队前锋，户外运动爱好者', 0, 1, 1, 2, datetime('now', '-90 days'), datetime('now'), 0),
('2023015234', 'test', '周佩琪', '2023015234@cqnu.edu.cn', '15823740005', '周佩琪', 0, '2023015234', NULL, '外国语学院', '英语（师范）', '2023', 0, '英语（师范）专业，外语配音社成员，梦想成为中学英语老师', 0, 1, 0, 6, datetime('now', '-88 days'), datetime('now'), 0),
('2022016234', 'test', '吴佳琪', '2022016234@cqnu.edu.cn', '15823740006', '吴佳琪', 0, '2022016234', NULL, '教育科学学院', '小学教育（师范）', '2022', 0, '小学教育（师范）专业，心理服务社团社长，喜欢和孩子打交道', 0, 1, 1, 7, datetime('now', '-86 days'), datetime('now'), 0),
('2023017234', 'test', '郑文博', '2023017234@cqnu.edu.cn', '15823740007', '郑文博', 0, '2023017234', NULL, '数学科学学院', '数学与应用数学（师范）', '2023', 1, '数学与应用数学（师范）专业，校读者协会会长，数学建模爱好者', 0, 1, 1, 7, datetime('now', '-85 days'), datetime('now'), 0),
('2022018234', 'test', '孙梦洁', '2022018234@cqnu.edu.cn', '15823740008', '孙梦洁', 0, '2022018234', NULL, '音乐学院', '音乐学（师范）', '2022', 0, '音乐学（师范）专业，弦乐四重奏成员，校园歌手大赛常客', 0, 1, 0, 5, datetime('now', '-83 days'), datetime('now'), 0),
('2023019234', 'test', '何俊杰', '2023019234@cqnu.edu.cn', '15823740009', '何俊杰', 0, '2023019234', NULL, '历史与社会学院', '历史学（师范）', '2023', 1, '历史学（师范）专业，巴渝文化研究兴趣小组成员，纪录片发烧友', 0, 1, 0, 4, datetime('now', '-82 days'), datetime('now'), 0),
('2023020234', 'test', '杨雨桐', '2023020234@cqnu.edu.cn', '15823740010', '杨雨桐', 0, '2023020234', NULL, '新闻与传媒学院', '网络与新媒体', '2023', 0, '网络与新媒体专业，摄影协会会长，用镜头记录校园四季', 0, 1, 1, 6, datetime('now', '-80 days'), datetime('now'), 0),
('2022021234', 'test', '马晨曦', '2022021234@cqnu.edu.cn', '15823740011', '马晨曦', 0, '2022021234', NULL, '地理与旅游学院', '地理科学（师范）', '2022', 1, '地理科学（师范）专业，地理研学社团成员，地图收集爱好者', 0, 1, 0, 5, datetime('now', '-78 days'), datetime('now'), 0),
('2023022234', 'test', '黄雅静', '2023022234@cqnu.edu.cn', '15823740012', '黄雅静', 0, '2023022234', NULL, '经济与管理学院', '经济学', '2023', 0, '经济学专业，职业发展协会成员，简历已投遍半个朋友圈', 0, 1, 0, 9, datetime('now', '-76 days'), datetime('now'), 0),
('2022023234', 'test', '罗俊豪', '2022023234@cqnu.edu.cn', '15823740013', '罗俊豪', 0, '2022023234', NULL, '化学学院', '化学（师范）', '2022', 1, '化学（师范）专业，院篮球队队员', 0, 0, 0, 3, datetime('now', '-60 days'), datetime('now', '-10 days'), 0);

-- =============================================
-- 2. 插入活动分类数据（六大类，activity_count 与活动表实际分布一致）
-- =============================================
INSERT INTO category (name, description, icon, sort_order, status, activity_count, create_time, update_time, deleted) VALUES
('师范技能', '师范生教学技能大赛、三笔字比赛、模拟课堂等教学基本功训练与竞赛', 'icon-teaching', 1, 1, 3, datetime('now'), datetime('now'), 0),
('学术讲座', '学术报告、专题讲座、经验分享会等学术活动', 'icon-academic', 2, 1, 4, datetime('now'), datetime('now'), 0),
('文体赛事', '运动会、篮球联赛、歌手大赛、音乐会等文体活动', 'icon-sport', 3, 1, 5, datetime('now'), datetime('now'), 0),
('志愿服务', '支教、三下乡、社区服务等志愿公益活动', 'icon-volunteer', 4, 1, 3, datetime('now'), datetime('now'), 0),
('社团活动', '学生社团日常活动、读书节、心理健康节等校园文化活动', 'icon-club', 5, 1, 4, datetime('now'), datetime('now'), 0),
('就业创业', '就业双选会、职业生涯规划大赛、创业分享等就业创业活动', 'icon-career', 6, 1, 2, datetime('now'), datetime('now'), 0);

-- =============================================
-- 3. 插入活动数据（共 21 条，状态全覆盖）
-- 状态分布：报名中(2)×12、即将开始(3)×1、进行中(4)×1、已结束(5)×3、
--           待审核(1)×1、草稿(0)×1、已拒绝(-1)×1、已下架(-2)×1
-- current_participants = 该活动 role=2 且 status=1 的报名记录数
-- favorite_count / comment_count / review_count / avg_rating 与后续各表实际记录一致
-- =============================================
INSERT INTO activity (title, description, cover_image, organizer_id, location, start_time, end_time, registration_start_time, registration_end_time, max_participants, current_participants, tags, status, category_id, need_review, view_count, favorite_count, comment_count, avg_rating, review_count, create_time, update_time, deleted) VALUES
('第三届师范生教学技能大赛', '面向全校师范生的教学技能比拼，设片段教学、板书设计、即席演讲三个环节，决赛全程开放观摩，欢迎各学院师范生同台竞技、互相学习。', '/images/activity_1/activity1.jpg', 5, '大学城校区·田家炳教育书院706智慧教室', datetime('now', '+12 days'), datetime('now', '+12 days', '+4 hours'), datetime('now', '-6 days'), datetime('now', '+9 days'), 100, 5, '师范技能,教学大赛,片段教学,师范生', 2, 1, 1, 289, 3, 4, NULL, 0, datetime('now', '-7 days'), datetime('now'), 0),
('「三笔字」教学基本功比赛', '粉笔字、钢笔字、毛笔字三项目同场竞技，弘扬中华优秀传统文化，夯实师范生教学基本功，获奖作品将在图书馆一楼展厅集中展出。', '/images/activity_2/activity2.jpg', 3, '大学城校区·田家炳教育书院204微格教室', datetime('now', '+15 days'), datetime('now', '+15 days', '+3 hours'), datetime('now', '-4 days'), datetime('now', '+12 days'), 80, 5, '三笔字,板书,传统文化,师范技能', 2, 1, 1, 176, 0, 3, NULL, 0, datetime('now', '-6 days'), datetime('now'), 0),
('第二十届校园歌手大赛', '一年一度的校园歌唱盛宴，海选、复赛、决赛三轮赛制，本届增设最佳原创作品奖，决赛舞台设在大礼堂，欢迎全校热爱歌唱的同学登台一展歌喉。', '/images/activity_3/activity3.jpg', 7, '大学城校区·大礼堂', datetime('now', '+10 days'), datetime('now', '+10 days', '+3 hours'), datetime('now', '-5 days'), datetime('now', '+7 days'), 50, 4, '歌手大赛,音乐,校园文化,比赛', 2, 3, 1, 352, 3, 4, NULL, 0, datetime('now', '-8 days'), datetime('now'), 0),
('秋季田径运动会', '一年一度的全校田径运动会，设径赛、田赛、趣味项目三大板块，以学院为单位组队参赛，体育教育专业方阵将亮相开幕式，欢迎师生到场观赛助威。', '/images/activity_4/activity4.jpg', 4, '大学城校区·田径场', datetime('now', '+2 days'), datetime('now', '+4 days'), datetime('now', '-10 days'), datetime('now', '-1 days'), 0, 4, '运动会,田径,体育赛事', 3, 3, 0, 431, 3, 2, NULL, 0, datetime('now', '-12 days'), datetime('now'), 0),
('「三下乡」暑期社会实践成果展', '暑期「三下乡」社会实践队伍成果集中展示：支教课堂、乡村墙绘、直播助农、非遗调研四大板块，现场同步开展优秀实践报告分享。', '/images/activity_5/activity5.jpg', 5, '大学城校区·学生活动中心一楼展厅', datetime('now', '-8 days'), datetime('now', '-7 days'), datetime('now', '-25 days'), datetime('now', '-10 days'), 60, 5, '三下乡,社会实践,成果展,志愿服务', 5, 4, 1, 268, 3, 3, 4.0, 1, datetime('now', '-28 days'), datetime('now'), 0),
('人工智能与基础教育数字化前沿讲座', '邀请市教育科学研究院专家与我校计算机学院教师同台开讲，围绕人工智能赋能课堂教学、数字画像与精准教研等议题分享最新实践案例。', '/images/activity_6/activity6.jpg', 2, '大学城校区·图书馆学术报告厅', datetime('now', '+18 days'), datetime('now', '+18 days', '+2 hours'), datetime('now', '-3 days'), datetime('now', '+16 days'), 300, 5, '人工智能,教育数字化,学术讲座', 2, 2, 0, 127, 3, 3, NULL, 0, datetime('now', '-5 days'), datetime('now'), 0),
('「萤火」支教志愿服务进社区', '联合北碚区澄江镇社区开展课后陪伴服务，设作业辅导、阅读启蒙、科普实验三个小组，服务对象为社区双职工家庭子女，行前将统一培训。', '/images/activity_7/activity7.jpg', 3, '北碚区澄江镇社区服务中心（北碚校区乘车前往）', datetime('now', '+8 days'), datetime('now', '+8 days', '+6 hours'), datetime('now', '-5 days'), datetime('now', '+5 days'), 30, 4, '支教,志愿服务,社区,公益', 2, 4, 1, 154, 0, 3, NULL, 0, datetime('now', '-6 days'), datetime('now'), 0),
('第十一届校园读书节', '为期一周的校园阅读嘉年华：旧书市集、经典诵读、名师导读、自然笔记展（生命科学学院联办）等板块轮番进行，主会场设在图书馆前广场。', '/images/activity_8/activity8.jpg', 14, '大学城校区·图书馆前广场及各阅览区', datetime('now', '-2 days'), datetime('now', '+5 days'), datetime('now', '-9 days'), datetime('now', '-3 days'), 200, 5, '读书节,阅读,社团活动,书香校园', 4, 5, 0, 316, 4, 3, NULL, 0, datetime('now', '-10 days'), datetime('now'), 0),
('「心晴护航」大学生心理健康宣传月', '秋季学期新生适应季主题活动：心理游园会、团体辅导工作坊、朋辈倾听角三大板块同步开展，教育科学学院心理服务社团与校医院心理咨询中心联合承办，初等教育学院、特殊教育学院志愿者提供引导服务。', '/images/activity_9/activity9.jpg', 13, '大学城校区·学生活动中心广场', datetime('now', '+6 days'), datetime('now', '+6 days', '+5 hours'), datetime('now', '-4 days'), datetime('now', '+4 days'), 100, 5, '心理健康,新生适应,游园会,朋辈互助', 2, 5, 0, 189, 3, 2, NULL, 0, datetime('now', '-5 days'), datetime('now'), 0),
('考研经验分享会', '邀请2026届考取部属师范大学与本校硕士研究生的学长学姐返校分享备考经验，覆盖公共课复习、专业课答题、复试调剂等环节，并设现场答疑。', '/images/activity_10/activity10.jpg', 10, '沙坪坝校区·第二教学楼202教室', datetime('now', '+9 days'), datetime('now', '+9 days', '+2 hours'), datetime('now', '-3 days'), datetime('now', '+7 days'), 120, 6, '考研,经验分享,学业发展', 2, 2, 0, 208, 3, 3, NULL, 0, datetime('now', '-4 days'), datetime('now'), 0),
('师范类毕业生就业双选会', '市内外60余所中小学、幼儿园及教育类企业进场招聘，覆盖语数外、音体美、学前教育等全部师范专业方向，马克思主义学院、生命科学学院等紧缺学科岗位同步开放。', '/images/activity_11/activity11.jpg', 6, '大学城校区·体育馆', datetime('now', '+16 days'), datetime('now', '+16 days', '+7 hours'), datetime('now', '-6 days'), datetime('now', '+14 days'), 500, 6, '双选会,就业,师范生,招聘', 2, 6, 0, 394, 4, 3, NULL, 0, datetime('now', '-7 days'), datetime('now'), 0),
('学术讲座：巴渝文化研究的前沿与田野', '从钓鱼城遗址到长江水文碑刻，主讲人结合二十年田野调查经历，讲述巴渝历史文化研究的方法与前沿进展，历史学（师范）专业师生优先入场。', '/images/activity_12/activity12.jpg', 3, '大学城校区·图书馆学术报告厅', datetime('now', '-14 days'), datetime('now', '-14 days', '+2 hours'), datetime('now', '-24 days'), datetime('now', '-16 days'), 150, 5, '巴渝文化,历史,学术讲座,田野调查', 5, 2, 0, 245, 3, 2, 5.0, 1, datetime('now', '-26 days'), datetime('now'), 0),
('「冬之序曲」室内乐专场音乐会', '音乐学院师生联袂呈现弦乐四重奏、钢琴三重奏与声乐艺术歌曲，曲目涵盖经典民歌改编与原创作品，凭学生证免费领取入场券。', '/images/activity_13/activity13.jpg', 7, '大学城校区·音乐厅', datetime('now', '+20 days'), datetime('now', '+20 days', '+2 hours'), datetime('now', '-2 days'), datetime('now', '+17 days'), 200, 3, '音乐会,室内乐,艺术展演', 2, 3, 0, 167, 3, 2, NULL, 0, datetime('now', '-4 days'), datetime('now'), 0),
('「新生杯」篮球联赛', '以学院为单位的新生篮球联赛，小组循环加淘汰赛制，历时十天，决赛将在体育馆副馆举行，体育教育专业学生担任临场裁判。', '/images/activity_14/activity14.jpg', 4, '大学城校区·体育馆副馆', datetime('now', '+13 days'), datetime('now', '+22 days'), datetime('now', '-8 days'), datetime('now', '+10 days'), 64, 5, '篮球,联赛,新生杯,体育赛事', 2, 3, 0, 279, 4, 2, NULL, 0, datetime('now', '-9 days'), datetime('now'), 0),
('摄影协会·秋日校园外拍', '秋日限定校园外拍活动，路线覆盖北碚校区老校门、梧桐道与大学城校区共青湖，协会骨干现场讲解构图与用光技巧，优秀作品将推荐至校史馆秋季展。', '/images/activity_15/activity15.jpg', 17, '北碚校区·老校门及梧桐道沿线（协会统一集合）', datetime('now', '+5 days'), datetime('now', '+5 days', '+4 hours'), datetime('now', '-3 days'), datetime('now', '+3 days'), 20, 3, '摄影,外拍,社团活动', 2, 5, 1, 143, 2, 2, NULL, 0, datetime('now', '-4 days'), datetime('now'), 0),
('模拟课堂观摩教研活动', '组织师范生走进附属中学真实课堂观摩教学，课后与一线教师开展评课研讨，面向初等教育学院、特殊教育学院三年级师范生开放40个观摩名额。', '/images/activity_16/activity16.jpg', 5, '大学城校区·田家炳教育书院305微格教室', datetime('now', '+25 days'), datetime('now', '+25 days', '+3 hours'), datetime('now', '+14 days'), datetime('now', '+22 days'), 40, 0, '模拟课堂,观摩,教研,师范技能', 1, 1, 1, 0, 0, 0, NULL, 0, datetime('now', '-1 days'), datetime('now'), 0),
('2027年元旦晚会', '2027年元旦晚会策划案草稿：拟设「辞旧迎新·师路启航」主题篇章，包含音乐学院管弦合奏、各学院新年寄语短片与全场合唱环节，待学生会文艺部完善节目单后提交审核。', '/images/activity_17/activity17.jpg', 10, '大学城校区·大礼堂', datetime('now', '+43 days'), datetime('now', '+43 days', '+3 hours'), datetime('now', '+25 days'), datetime('now', '+41 days'), 100, 0, '元旦晚会,文艺演出,筹备', 0, 3, 0, 0, 0, 0, NULL, 0, datetime('now', '-1 days'), datetime('now'), 0),
('初冬缙云山徒步露营活动', '拟组织周末前往缙云山国家级自然保护区徒步露营，因保护区属全年森林防火重点区域、禁止野外用火与露营，未通过安全审核。', '/images/activity_18/activity18.jpg', 11, '缙云山国家级自然保护区（校外）', datetime('now', '+9 days'), datetime('now', '+10 days'), datetime('now', '-2 days'), datetime('now', '+6 days'), 40, 0, '徒步,露营,户外', -1, 5, 0, 31, 0, 0, NULL, 0, datetime('now', '-2 days'), datetime('now'), 0),
('智慧课堂技术专题讲座（第三期）', '原定讲授智慧课堂互动系统的操作进阶，因课件版本与现行教学平台不匹配、内容已过期，由管理员下架，更新后另行发布。', '/images/activity_19/activity19.jpg', 2, '大学城校区·图书馆学术报告厅', datetime('now', '-3 days'), datetime('now', '-3 days', '+2 hours'), datetime('now', '-12 days'), datetime('now', '-4 days'), 200, 0, '智慧课堂,教育技术,讲座', -2, 2, 0, 96, 0, 0, NULL, 0, datetime('now', '-15 days'), datetime('now'), 0),
('乡村小学支教志愿服务活动（十一月批次）', '组织志愿者前往沙坪坝区回龙坝镇中心小学开展一天支教服务，课程覆盖阅读推广、趣味数学与美术手工，美术学院志愿者协助布置校园文化墙。', '/images/activity_20/activity20.jpg', 5, '沙坪坝区回龙坝镇中心小学（学校乘车统一前往）', datetime('now', '-6 days'), datetime('now', '-5 days'), datetime('now', '-16 days'), datetime('now', '-8 days'), 25, 5, '支教,乡村教育,志愿服务', 5, 4, 1, 227, 3, 2, 4.5, 2, datetime('now', '-18 days'), datetime('now'), 0),
('大学生职业生涯规划大赛', '以「规划青春·逐梦讲台」为主题，设成长赛道与就业赛道，参赛选手围绕职业目标、行动计划进行现场陈述与答辩，优胜者将推荐参加市级赛事。', '/images/activity_21/activity21.jpg', 6, '大学城校区·田家炳教育书院报告厅', datetime('now', '+17 days'), datetime('now', '+17 days', '+4 hours'), datetime('now', '-1 days'), datetime('now', '+14 days'), 60, 3, '职业生涯规划,就业创业,大赛', 2, 6, 1, 118, 2, 2, NULL, 0, datetime('now', '-3 days'), datetime('now'), 0);

-- =============================================
-- 4. 插入活动报名/组织者记录（共 104 条）
-- 组织者记录（role=1）：id=1~21 与活动1~21 一一对应
-- 参与者记录（role=2）：id=22~104，行尾注释标注隐含 id，签到表据此引用
-- need_review=1 的活动报名需管理员审核（reviewer_id=1）；need_review=0 的直接通过
-- =============================================
INSERT INTO activity_registration (activity_id, user_id, role, status, remark, reviewer_id, review_time, review_remark, create_time, update_time, deleted) VALUES
-- 组织者记录（role=1，仅用于权限标识，不应显示在报名者列表中）
(1, 5, 1, 1, '活动组织者', 1, datetime('now', '-4 days'), '审核通过', datetime('now', '-5 days'), datetime('now'), 0),   -- id=1
(2, 3, 1, 1, '活动组织者', 1, datetime('now', '-4 days'), '审核通过', datetime('now', '-5 days'), datetime('now'), 0),   -- id=2
(3, 7, 1, 1, '活动组织者', 1, datetime('now', '-6 days'), '审核通过', datetime('now', '-7 days'), datetime('now'), 0),   -- id=3
(4, 4, 1, 1, '活动组织者', 1, datetime('now', '-10 days'), '审核通过', datetime('now', '-11 days'), datetime('now'), 0),  -- id=4
(5, 5, 1, 1, '活动组织者', 1, datetime('now', '-26 days'), '审核通过', datetime('now', '-27 days'), datetime('now'), 0),  -- id=5
(6, 2, 1, 1, '活动组织者', 1, datetime('now', '-3 days'), '审核通过', datetime('now', '-4 days'), datetime('now'), 0),   -- id=6
(7, 3, 1, 1, '活动组织者', 1, datetime('now', '-4 days'), '审核通过', datetime('now', '-5 days'), datetime('now'), 0),   -- id=7
(8, 14, 1, 1, '活动组织者（校读者协会）', 1, datetime('now', '-8 days'), '审核通过', datetime('now', '-9 days'), datetime('now'), 0), -- id=8
(9, 13, 1, 1, '活动组织者（心理服务社团）', 1, datetime('now', '-3 days'), '审核通过', datetime('now', '-4 days'), datetime('now'), 0), -- id=9
(10, 10, 1, 1, '活动组织者', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0),  -- id=10
(11, 6, 1, 1, '活动组织者（校就业指导中心）', 1, datetime('now', '-5 days'), '审核通过', datetime('now', '-6 days'), datetime('now'), 0), -- id=11
(12, 3, 1, 1, '活动组织者', 1, datetime('now', '-24 days'), '审核通过', datetime('now', '-25 days'), datetime('now'), 0), -- id=12
(13, 7, 1, 1, '活动组织者', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0),  -- id=13
(14, 4, 1, 1, '活动组织者', 1, datetime('now', '-7 days'), '审核通过', datetime('now', '-8 days'), datetime('now'), 0),  -- id=14
(15, 17, 1, 1, '活动组织者（摄影协会）', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0), -- id=15
(16, 5, 1, 0, '活动组织者（待审核）', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                  -- id=16
(17, 10, 1, 0, '活动组织者（草稿）', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                   -- id=17
(18, 11, 1, 0, '活动组织者（已拒绝）', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                 -- id=18
(19, 2, 1, 1, '活动组织者（活动已下架）', 1, datetime('now', '-13 days'), '审核通过', datetime('now', '-14 days'), datetime('now'), 0), -- id=19
(20, 5, 1, 1, '活动组织者', 1, datetime('now', '-16 days'), '审核通过', datetime('now', '-17 days'), datetime('now'), 0), -- id=20
(21, 6, 1, 1, '活动组织者', 1, datetime('now', '-1 days'), '审核通过', datetime('now', '-2 days'), datetime('now'), 0),  -- id=21
-- 参与者记录（role=2）
-- 活动1 师范生教学技能大赛（报名审核已通过）
(1, 8, 2, 1, '想观摩优秀同侪的片段教学，提升自己的教学设计能力', 1, datetime('now', '-3 days'), '审核通过，请按时到场抽签', datetime('now', '-4 days'), datetime('now'), 0),   -- id=22
(1, 10, 2, 1, '报名片段教学赛道，准备了一节《背影》微格课', 1, datetime('now', '-3 days'), '审核通过，请按时到场抽签', datetime('now', '-4 days'), datetime('now'), 0),       -- id=23
(1, 12, 2, 1, '英语教学设计方向，希望向其他学院的同学学习', 1, datetime('now', '-3 days'), '审核通过，请按时到场抽签', datetime('now', '-4 days'), datetime('now'), 0),       -- id=24
(1, 13, 2, 1, '小学教育专业，想观摩高年级学段的教学展示', 1, datetime('now', '-3 days'), '审核通过，请按时到场抽签', datetime('now', '-4 days'), datetime('now'), 0),         -- id=25
(1, 14, 2, 1, '想了解数学课堂的板书设计技巧', 1, datetime('now', '-3 days'), '审核通过，请按时到场抽签', datetime('now', '-4 days'), datetime('now'), 0),                    -- id=26
-- 活动2 三笔字比赛（1 条待审核）
(2, 8, 2, 1, '钢笔字练了三年，第一次参加正式比赛，紧张又期待', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0),                   -- id=27
(2, 10, 2, 1, '师范生不能丢了粉笔字，来向各位书法高手学习', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0),                     -- id=28
(2, 13, 2, 1, '想为将来的板书打基础，报名毛笔字项目', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0),                           -- id=29
(2, 14, 2, 1, '数学老师的板书尤其重要，必须来练', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0),                               -- id=30
(2, 16, 2, 1, '碑帖临摹多年，希望作品能进图书馆展厅', 1, datetime('now', '-2 days'), '审核通过', datetime('now', '-3 days'), datetime('now'), 0),                           -- id=31
(2, 17, 2, 0, '作品还在准备中，希望能赶上报名末班车', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                     -- id=32
-- 活动3 校园歌手大赛（1 条待审核）
(3, 9, 2, 1, '想唱一首原创民谣，求舞台', 1, datetime('now', '-3 days'), '审核通过，海选安排另行通知', datetime('now', '-4 days'), datetime('now'), 0),                       -- id=33
(3, 15, 2, 1, '音乐学专业的日常，来交流也来比赛', 1, datetime('now', '-3 days'), '审核通过，海选安排另行通知', datetime('now', '-4 days'), datetime('now'), 0),             -- id=34
(3, 17, 2, 1, '朋友怂恿来报名的，唱歌纯属热爱', 1, datetime('now', '-3 days'), '审核通过，海选安排另行通知', datetime('now', '-4 days'), datetime('now'), 0),               -- id=35
(3, 10, 2, 1, '普通话过得去，想挑战一下舞台表现力', 1, datetime('now', '-3 days'), '审核通过，海选安排另行通知', datetime('now', '-4 days'), datetime('now'), 0),           -- id=36
(3, 19, 2, 0, '曲目还没最终确定，先占个名额', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                             -- id=37
-- 活动4 秋季田径运动会（无需报名审核，直接通过）
(4, 11, 2, 1, '报名男子100米与4×100接力', NULL, NULL, NULL, datetime('now', '-8 days'), datetime('now'), 0),                                                                 -- id=38
(4, 20, 2, 1, '化学学院接力队集合！', NULL, NULL, NULL, datetime('now', '-7 days'), datetime('now'), 0),                                                                     -- id=39
(4, 18, 2, 1, '参加跳远和趣味项目', NULL, NULL, NULL, datetime('now', '-6 days'), datetime('now'), 0),                                                                       -- id=40
(4, 8, 2, 1, '报名学生组3000米，重在参与', NULL, NULL, NULL, datetime('now', '-5 days'), datetime('now'), 0),                                                                -- id=41
-- 活动5 三下乡成果展（历史报名，均已审核通过）
(5, 8, 2, 1, '我们队伍的成果终于要展出了', 1, datetime('now', '-20 days'), '审核通过', datetime('now', '-21 days'), datetime('now'), 0),                                     -- id=42
(5, 13, 2, 1, '跟着支教队伍走了整个暑假，想看看成果展', 1, datetime('now', '-20 days'), '审核通过', datetime('now', '-21 days'), datetime('now'), 0),                       -- id=43
(5, 17, 2, 1, '直播助农小组前来报到', 1, datetime('now', '-19 days'), '审核通过', datetime('now', '-20 days'), datetime('now'), 0),                                         -- id=44
(5, 18, 2, 1, '乡村调研成果想和更多同学交流', 1, datetime('now', '-19 days'), '审核通过', datetime('now', '-20 days'), datetime('now'), 0),                                 -- id=45
(5, 19, 2, 1, '作为实践队员参加，帮队伍布置展板', 1, datetime('now', '-18 days'), '审核通过', datetime('now', '-19 days'), datetime('now'), 0),                             -- id=46
-- 活动6 人工智能与基础教育数字化前沿讲座（无需报名审核）
(6, 9, 2, 1, '想了解AI在物理课堂上的应用可能', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                            -- id=47
(6, 14, 2, 1, '对教育数字化很感兴趣，必须来听', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                           -- id=48
(6, 19, 2, 1, '将来要做老师，数字化教学技能先学起来', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                     -- id=49
(6, 8, 2, 1, '计算机专业学生想听听专家的行业视角', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                        -- id=50
(6, 12, 2, 1, '想了解AI辅助英语教学的最新案例', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                           -- id=51
-- 活动7 萤火支教进社区（1 条待审核）
(7, 13, 2, 1, '报名作业辅导组，有小学家教经验', 1, datetime('now', '-3 days'), '审核通过，请参加行前培训', datetime('now', '-4 days'), datetime('now'), 0),                 -- id=52
(7, 10, 2, 1, '报名阅读启蒙组，绘本资源可以自带', 1, datetime('now', '-3 days'), '审核通过，请参加行前培训', datetime('now', '-4 days'), datetime('now'), 0),               -- id=53
(7, 19, 2, 1, '报名科普实验组，第一次做志愿服务很期待', 1, datetime('now', '-3 days'), '审核通过，请参加行前培训', datetime('now', '-4 days'), datetime('now'), 0),         -- id=54
(7, 12, 2, 1, '报名英语启蒙组', 1, datetime('now', '-3 days'), '审核通过，请参加行前培训', datetime('now', '-4 days'), datetime('now'), 0),                                 -- id=55
(7, 17, 2, 0, '想负责活动摄影记录，等待审核中', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                           -- id=56
-- 活动8 校园读书节（无需报名审核，进行中）
(8, 10, 2, 1, '经典诵读专场必来，还想淘几本旧书', NULL, NULL, NULL, datetime('now', '-7 days'), datetime('now'), 0),                                                         -- id=57
(8, 16, 2, 1, '冲着旧书市集的地方志来的', NULL, NULL, NULL, datetime('now', '-6 days'), datetime('now'), 0),                                                                 -- id=58
(8, 19, 2, 1, '想听名师导读环节', NULL, NULL, NULL, datetime('now', '-6 days'), datetime('now'), 0),                                                                         -- id=59
(8, 8, 2, 1, '读书节每届都参加，今年报个名打卡', NULL, NULL, NULL, datetime('now', '-5 days'), datetime('now'), 0),                                                          -- id=60
(8, 9, 2, 1, '想看看自然笔记展，顺便参加诵读', NULL, NULL, NULL, datetime('now', '-5 days'), datetime('now'), 0),                                                            -- id=61
-- 活动9 心理健康节（无需报名审核）
(9, 8, 2, 1, '带室友一起来体验解压工作坊', NULL, NULL, NULL, datetime('now', '-3 days'), datetime('now'), 0),                                                                -- id=62
(9, 12, 2, 1, '期中季学业压力不小，来给自己充充电', NULL, NULL, NULL, datetime('now', '-3 days'), datetime('now'), 0),                                                       -- id=63
(9, 15, 2, 1, '游园会小游戏看起来很有意思', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                               -- id=64
(9, 17, 2, 1, '想给心理健康节做志愿者随拍', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                               -- id=65
(9, 14, 2, 1, '心理社的朋辈倾听角想体验一下', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                             -- id=66
-- 活动10 考研经验分享会（无需报名审核）
(10, 14, 2, 1, '想听听学长学姐们数学分析备考的时间安排', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                  -- id=67
(10, 9, 2, 1, '二战上岸的学姐也会来分享，机会难得', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                       -- id=68
(10, 16, 2, 1, '已经记好笔记了，等分享会结束整理问答发到年级群', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                          -- id=69
(10, 20, 2, 1, '化学师范生也准备考研，来取取经', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                          -- id=70
(10, 18, 2, 1, '想了解地理学科教学方向的考研路径', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                        -- id=71
(10, 19, 2, 1, '虽然不考研，但想了解升学与就业怎么平衡', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                  -- id=72
-- 活动11 就业双选会（无需报名审核）
(11, 19, 2, 1, '重点中学的招聘名额不容错过，简历已备好', NULL, NULL, NULL, datetime('now', '-5 days'), datetime('now'), 0),                                                  -- id=73
(11, 8, 2, 1, '虽然才大三，先来感受一下就业氛围', NULL, NULL, NULL, datetime('now', '-4 days'), datetime('now'), 0),                                                         -- id=74
(11, 12, 2, 1, '英语教师岗位不少，来投递简历', NULL, NULL, NULL, datetime('now', '-4 days'), datetime('now'), 0),                                                            -- id=75
(11, 15, 2, 1, '想了解音乐教师岗位的招聘要求', NULL, NULL, NULL, datetime('now', '-3 days'), datetime('now'), 0),                                                            -- id=76
(11, 17, 2, 1, '新媒体运营类岗位也想了解一下', NULL, NULL, NULL, datetime('now', '-3 days'), datetime('now'), 0),                                                            -- id=77
(11, 13, 2, 1, '小学教育专业对口岗位很多，提前踩点', NULL, NULL, NULL, datetime('now', '-2 days'), datetime('now'), 0),                                                      -- id=78
-- 活动12 巴渝文化学术讲座（历史报名，已结束并签到）
(12, 16, 2, 1, '历史学专业的必修课外延伸，必须到场', 1, datetime('now', '-22 days'), '审核通过', datetime('now', '-23 days'), datetime('now'), 0),                           -- id=79
(12, 10, 2, 1, '文学院学生对巴渝文化也很感兴趣', 1, datetime('now', '-22 days'), '审核通过', datetime('now', '-23 days'), datetime('now'), 0),                               -- id=80
(12, 18, 2, 1, '地理视角看历史文化，角度很新颖', 1, datetime('now', '-21 days'), '审核通过', datetime('now', '-22 days'), datetime('now'), 0),                               -- id=81
(12, 14, 2, 1, '想听听跨学科的研究方法', 1, datetime('now', '-21 days'), '审核通过', datetime('now', '-22 days'), datetime('now'), 0),                                       -- id=82
(12, 9, 2, 1, '陪同学一起来，顺便拓展视野', 1, datetime('now', '-20 days'), '审核通过', datetime('now', '-21 days'), datetime('now'), 0),                                    -- id=83
-- 活动13 室内乐音乐会
(13, 15, 2, 1, '弦乐四重奏成员，本场演出者', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                              -- id=84
(13, 9, 2, 1, '理科生也想接受艺术熏陶', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                                   -- id=85
(13, 17, 2, 1, '想拍摄音乐会现场记录', NULL, NULL, NULL, datetime('now'), datetime('now'), 0),                                                                               -- id=86
-- 活动14 新生杯篮球联赛
(14, 11, 2, 1, '体育教育2班首战对战数学科学学院', NULL, NULL, NULL, datetime('now', '-6 days'), datetime('now'), 0),                                                         -- id=87
(14, 20, 2, 1, '化学学院联队集训中，气势不能输', NULL, NULL, NULL, datetime('now', '-5 days'), datetime('now'), 0),                                                          -- id=88
(14, 18, 2, 1, '报名队伍后勤与记录台', NULL, NULL, NULL, datetime('now', '-5 days'), datetime('now'), 0),                                                                    -- id=89
(14, 14, 2, 1, '数学科学学院新生代表队成员', NULL, NULL, NULL, datetime('now', '-4 days'), datetime('now'), 0),                                                              -- id=90
(14, 9, 2, 1, '物理学院联队报名，友谊第一', NULL, NULL, NULL, datetime('now', '-4 days'), datetime('now'), 0),                                                               -- id=91
-- 活动15 摄影协会秋日外拍（1 条待审核）
(15, 8, 2, 1, '新买的定焦镜头正好练手', 1, datetime('now', '-2 days'), '审核通过，注意外拍安全', datetime('now', '-3 days'), datetime('now'), 0),                           -- id=92
(15, 19, 2, 1, '想学点摄影技巧，发朋友圈不再随手拍', 1, datetime('now', '-2 days'), '审核通过，注意外拍安全', datetime('now', '-3 days'), datetime('now'), 0),             -- id=93
(15, 13, 2, 1, '想拍好人像，给未来学生拍照也好看', 1, datetime('now', '-2 days'), '审核通过，注意外拍安全', datetime('now', '-3 days'), datetime('now'), 0),               -- id=94
(15, 9, 2, 0, '刚入手二手相机，等待审核中', NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),                                                               -- id=95
-- 活动20 乡村小学支教十一月批次（历史报名，均已审核通过并签到）
(20, 13, 2, 1, '第三次参加支教，已经和孩子们约好这学期再见', 1, datetime('now', '-14 days'), '审核通过，请参加行前培训', datetime('now', '-15 days'), datetime('now'), 0),   -- id=96
(20, 10, 2, 1, '报名阅读推广课，绘本已备好', 1, datetime('now', '-14 days'), '审核通过，请参加行前培训', datetime('now', '-15 days'), datetime('now'), 0),                   -- id=97
(20, 19, 2, 1, '想走出校园看看真实的乡村课堂', 1, datetime('now', '-13 days'), '审核通过，请参加行前培训', datetime('now', '-14 days'), datetime('now'), 0),               -- id=98
(20, 12, 2, 1, '报名趣味英语角', 1, datetime('now', '-13 days'), '审核通过，请参加行前培训', datetime('now', '-14 days'), datetime('now'), 0),                               -- id=99
(20, 8, 2, 1, '负责课程录像与资料整理', 1, datetime('now', '-12 days'), '审核通过，请参加行前培训', datetime('now', '-13 days'), datetime('now'), 0),                       -- id=100
-- 活动21 职业生涯规划大赛（1 条待审核）
(21, 19, 2, 1, '规划书主题已定，冲成长赛道', 1, datetime('now'), '审核通过，请提前准备答辩', datetime('now', '-1 days'), datetime('now'), 0),                                -- id=101
(21, 15, 2, 1, '音乐师范生的职业规划也想被听见', 1, datetime('now'), '审核通过，请提前准备答辩', datetime('now', '-1 days'), datetime('now'), 0),                            -- id=102
(21, 17, 2, 1, '从新媒体运营到教育传播，我的规划有点跨界', 1, datetime('now'), '审核通过，请提前准备答辩', datetime('now', '-1 days'), datetime('now'), 0),                 -- id=103
(21, 12, 2, 0, '答辩幻灯片还在打磨，先提交占位', NULL, NULL, NULL, datetime('now'), datetime('now'), 0);                                                                      -- id=104

-- =============================================
-- 5. 插入活动收藏数据（共 46 条，favorite_count 与此一致）
-- =============================================
INSERT INTO activity_favorite (activity_id, user_id, create_time, deleted) VALUES
(1, 8, datetime('now', '-4 days'), 0),
(1, 10, datetime('now', '-4 days'), 0),
(1, 12, datetime('now', '-3 days'), 0),
(3, 9, datetime('now', '-4 days'), 0),
(3, 15, datetime('now', '-3 days'), 0),
(3, 19, datetime('now', '-2 days'), 0),
(4, 11, datetime('now', '-8 days'), 0),
(4, 20, datetime('now', '-7 days'), 0),
(4, 18, datetime('now', '-6 days'), 0),
(5, 8, datetime('now', '-20 days'), 0),
(5, 13, datetime('now', '-19 days'), 0),
(5, 17, datetime('now', '-18 days'), 0),
(6, 9, datetime('now', '-2 days'), 0),
(6, 14, datetime('now', '-2 days'), 0),
(6, 19, datetime('now', '-1 days'), 0),
(8, 10, datetime('now', '-7 days'), 0),
(8, 16, datetime('now', '-6 days'), 0),
(8, 19, datetime('now', '-5 days'), 0),
(8, 8, datetime('now', '-4 days'), 0),
(9, 8, datetime('now', '-3 days'), 0),
(9, 12, datetime('now', '-3 days'), 0),
(9, 15, datetime('now', '-2 days'), 0),
(10, 14, datetime('now', '-2 days'), 0),
(10, 9, datetime('now', '-2 days'), 0),
(10, 16, datetime('now', '-1 days'), 0),
(11, 19, datetime('now', '-5 days'), 0),
(11, 8, datetime('now', '-4 days'), 0),
(11, 12, datetime('now', '-4 days'), 0),
(11, 15, datetime('now', '-3 days'), 0),
(12, 16, datetime('now', '-22 days'), 0),
(12, 10, datetime('now', '-21 days'), 0),
(12, 18, datetime('now', '-20 days'), 0),
(13, 15, datetime('now', '-1 days'), 0),
(13, 9, datetime('now', '-1 days'), 0),
(13, 17, datetime('now'), 0),
(14, 11, datetime('now', '-6 days'), 0),
(14, 20, datetime('now', '-5 days'), 0),
(14, 18, datetime('now', '-4 days'), 0),
(14, 14, datetime('now', '-4 days'), 0),
(15, 17, datetime('now', '-3 days'), 0),
(15, 8, datetime('now', '-2 days'), 0),
(20, 13, datetime('now', '-14 days'), 0),
(20, 10, datetime('now', '-13 days'), 0),
(20, 19, datetime('now', '-12 days'), 0),
(21, 19, datetime('now', '-1 days'), 0),
(21, 15, datetime('now'), 0);

-- =============================================
-- 6. 插入签到记录数据（共 20 条）
-- registration_id 与第 4 段参与者记录的隐含 id 严格对应（id=签到对象报名记录）
-- check_in_type: 1-扫码签到, 2-手动签到
-- 已结束活动（5/12/20）全员签到；进行中活动（8）已有部分签到
-- =============================================
INSERT INTO check_in_record (activity_id, user_id, registration_id, check_in_time, check_in_type, location, remark, create_time, deleted) VALUES
(5, 8, 42, datetime('now', '-8 days'), 2, '学生活动中心一楼展厅', '观展签到', datetime('now', '-8 days'), 0),
(5, 13, 43, datetime('now', '-8 days'), 2, '学生活动中心一楼展厅', '观展签到', datetime('now', '-8 days'), 0),
(5, 17, 44, datetime('now', '-8 days'), 1, '学生活动中心一楼展厅', '扫码签到', datetime('now', '-8 days'), 0),
(5, 18, 45, datetime('now', '-8 days'), 1, '学生活动中心一楼展厅', '扫码签到', datetime('now', '-8 days'), 0),
(5, 19, 46, datetime('now', '-7 days'), 1, '学生活动中心一楼展厅', '布展后补签', datetime('now', '-7 days'), 0),
(8, 10, 57, datetime('now', '-1 days'), 1, '图书馆前广场', '经典诵读专场扫码签到', datetime('now', '-1 days'), 0),
(8, 16, 58, datetime('now', '-1 days'), 1, '图书馆前广场', '旧书市集扫码签到', datetime('now', '-1 days'), 0),
(8, 19, 59, datetime('now', '-1 days'), 1, '图书馆前广场', '名师导读扫码签到', datetime('now', '-1 days'), 0),
(8, 8, 60, datetime('now', '-1 days'), 1, '图书馆前广场', '扫码签到', datetime('now', '-1 days'), 0),
(8, 9, 61, datetime('now', '-1 days'), 2, '图书馆阅览区', '手动签到', datetime('now', '-1 days'), 0),
(12, 16, 79, datetime('now', '-14 days'), 2, '图书馆学术报告厅', '讲座手动签到', datetime('now', '-14 days'), 0),
(12, 10, 80, datetime('now', '-14 days'), 2, '图书馆学术报告厅', '讲座手动签到', datetime('now', '-14 days'), 0),
(12, 18, 81, datetime('now', '-14 days'), 1, '图书馆学术报告厅', '讲座扫码签到', datetime('now', '-14 days'), 0),
(12, 14, 82, datetime('now', '-14 days'), 1, '图书馆学术报告厅', '讲座扫码签到', datetime('now', '-14 days'), 0),
(12, 9, 83, datetime('now', '-14 days'), 1, '图书馆学术报告厅', '讲座扫码签到', datetime('now', '-14 days'), 0),
(20, 13, 96, datetime('now', '-6 days'), 1, '回龙坝镇中心小学', '抵达签到', datetime('now', '-6 days'), 0),
(20, 10, 97, datetime('now', '-6 days'), 1, '回龙坝镇中心小学', '抵达签到', datetime('now', '-6 days'), 0),
(20, 19, 98, datetime('now', '-6 days'), 1, '回龙坝镇中心小学', '抵达签到', datetime('now', '-6 days'), 0),
(20, 12, 99, datetime('now', '-6 days'), 2, '回龙坝镇中心小学', '乘车后手动签到', datetime('now', '-6 days'), 0),
(20, 8, 100, datetime('now', '-6 days'), 2, '回龙坝镇中心小学', '设备组手动签到', datetime('now', '-6 days'), 0);

-- =============================================
-- 7. 插入评论数据（共 51 条）
-- target_type: 1-活动（id=1~45，各活动 comment_count 与此一致）, 2-成果展示贴（id=46~51）
-- parent_id / reply_to_user_id 与前序评论严格对应
-- =============================================
INSERT INTO comment (target_id, target_type, user_id, content, parent_id, reply_to_user_id, create_time, update_time, deleted) VALUES
-- 活动1 师范生教学技能大赛评论（id=1~4）
(1, 1, 12, '已报名决赛，紧张又期待，希望大家都能发挥出自己的水平！', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                          -- id=1
(1, 1, 13, '一起加油！片段教学环节记得控制好时间。', 1, 12, datetime('now', '-1 days'), datetime('now'), 0),                                                         -- id=2
(1, 1, 8, '作为信息技术方向的观察员报名，想看看优秀师范生怎么把数字化工具融入课堂。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                        -- id=3
(1, 1, 5, '欢迎各位同学到场观摩，决赛将全程开放旁听，观摩座位先到先得。', 0, 0, datetime('now', '-3 days'), datetime('now'), 0),                                    -- id=4
-- 活动2 三笔字比赛评论（id=5~7）
(2, 1, 10, '每天练一张粉笔字，终于等到比赛了，冲！', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                                          -- id=5
(2, 1, 16, '请问毛笔字项目需要自带宣纸吗？', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                                                  -- id=6
(2, 1, 3, '比赛统一提供米字格粉笔和宣纸，钢笔自备即可。', 6, 16, datetime('now', '-1 days'), datetime('now'), 0),                                                    -- id=7
-- 活动3 校园歌手大赛评论（id=8~11）
(3, 1, 15, '海选曲目已定，一首原创民谣，希望唱到大礼堂的舞台上！', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                            -- id=8
(3, 1, 9, '楼上加油，我去现场给你应援！', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                                     -- id=9
(3, 1, 7, '本届大赛增设最佳原创作品奖，欢迎同学们展示自己的作品。', 0, 0, datetime('now', '-3 days'), datetime('now'), 0),                                          -- id=10
(3, 1, 17, '摄影协会会来记录决赛现场，记得对镜头微笑～', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                      -- id=11
-- 活动4 田径运动会评论（id=12~13）
(4, 1, 11, '体育教育专业方阵正在加紧排练，后天开幕式见！', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                    -- id=12
(4, 1, 20, '化学学院4×100接力名单已确定，目标是小组第一。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                   -- id=13
-- 活动5 三下乡成果展评论（id=14~16）
(5, 1, 8, '暑期跟着队伍去了巫溪支教，这段经历真的很难忘。', 0, 0, datetime('now', '-6 days'), datetime('now'), 0),                                                   -- id=14
(5, 1, 17, '成果展有我们队伍的直播助农板块，欢迎大家来打卡！', 0, 0, datetime('now', '-6 days'), datetime('now'), 0),                                                -- id=15
(5, 1, 13, '刚看完展览，学长学姐们的实践报告写得真好。', 0, 0, datetime('now', '-7 days'), datetime('now'), 0),                                                      -- id=16
-- 活动6 人工智能讲座评论（id=17~19）
(6, 1, 9, '想问一下讲座会涉及AI助教在课堂中的应用案例吗？', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                  -- id=17
(6, 1, 2, '会的，本次讲座将结合多所中学的真实课堂案例展开分析。', 17, 9, datetime('now', '-1 days'), datetime('now'), 0),                                            -- id=18
(6, 1, 14, '已经在自学提示词工程了，正好来听听专家的看法。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                  -- id=19
-- 活动7 支教进社区评论（id=20~22）
(7, 1, 13, '报名社区课后辅导岗，希望能帮孩子们养成好的学习习惯。', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                            -- id=20
(7, 1, 10, '三次支教经历的我又来了，社区的孩子还记得我们呢。', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                                -- id=21
(7, 1, 12, '想报英语启蒙组，请问需要试讲吗？', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                                -- id=22
-- 活动8 读书节评论（id=23~25）
(8, 1, 10, '今天在阅览区听了《陶行知教育文集》导读分享，收获很大。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                          -- id=23
(8, 1, 16, '旧书市集淘到两本绝版的巴蜀地方志，太值了！', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                      -- id=24
(8, 1, 14, '明天下午的经典诵读专场还有名额，欢迎现场报名。', 0, 0, datetime('now'), datetime('now'), 0),                                                             -- id=25
-- 活动9 心理健康节评论（id=26~27）
(9, 1, 8, '上次参加过解压工作坊，这次带了室友一起来。', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                                       -- id=26
(9, 1, 15, '广场上的心理游园会小游戏很有意思，还能盖章换奖品。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                              -- id=27
-- 活动10 考研分享会评论（id=28~30）
(10, 1, 14, '想听听学长学姐们数学分析备考的时间安排。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                       -- id=28
(10, 1, 9, '二战上岸的学姐也会来分享，机会难得。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                            -- id=29
(10, 1, 16, '已经准备好问题清单了，等分享会结束整理成问答发到年级群。', 0, 0, datetime('now'), datetime('now'), 0),                                                  -- id=30
-- 活动11 就业双选会评论（id=31~33）
(11, 1, 19, '参展学校名单里有好几所重点中学，简历已经准备就绪！', 0, 0, datetime('now', '-3 days'), datetime('now'), 0),                                            -- id=31
(11, 1, 12, '请问非师范专业可以进场吗？', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                                                     -- id=32
(11, 1, 6, '面向全校毕业生开放，简历建议多准备几份。', 32, 12, datetime('now', '-2 days'), datetime('now'), 0),                                                      -- id=33
-- 活动12 巴渝文化讲座评论（id=34~35）
(12, 1, 16, '王教授讲到的钓鱼城遗址田野调查方法让我大开眼界。', 0, 0, datetime('now', '-13 days'), datetime('now'), 0),                                              -- id=34
(12, 1, 10, '讲座录音整理稿会在学院公众号发布，错过的同学可以关注。', 0, 0, datetime('now', '-12 days'), datetime('now'), 0),                                        -- id=35
-- 活动13 音乐会评论（id=36~37）
(13, 1, 15, '这次室内乐专场我们弦乐四重奏会演奏《梁祝》选段！', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                               -- id=36
(13, 1, 9, '物理学院的理科生也想接受艺术熏陶，求一张入场券！', 0, 0, datetime('now'), datetime('now'), 0),                                                            -- id=37
-- 活动14 篮球联赛评论（id=38~39）
(14, 1, 11, '体育教育2班首战对战数学科学学院，欢迎来副馆观战。', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                              -- id=38
(14, 1, 20, '化学学院联队集训中，虽然水平一般但气势不能输！', 0, 0, datetime('now', '-2 days'), datetime('now'), 0),                                                 -- id=39
-- 活动15 摄影外拍评论（id=40~41）
(15, 1, 17, '外拍集合地点改为北碚校区老校门，注意查收群通知。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                               -- id=40
(15, 1, 8, '新买的定焦镜头正好练手，期待梧桐道的秋日光影。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                                  -- id=41
-- 活动20 支教十一月批次评论（id=42~43）
(20, 1, 13, '给孩子们上了三节阅读课，临走时他们送了画，破防了。', 0, 0, datetime('now', '-4 days'), datetime('now'), 0),                                             -- id=42
(20, 1, 10, '下学期还想继续去，希望支教活动能长期办下去。', 0, 0, datetime('now', '-4 days'), datetime('now'), 0),                                                   -- id=43
-- 活动21 职业规划大赛评论（id=44~45）
(21, 1, 19, '我的规划书主题是「从师范生到卓越教师」，期待评委指点。', 0, 0, datetime('now', '-1 days'), datetime('now'), 0),                                         -- id=44
(21, 1, 15, '音乐学院也要出人了，目标是用三年成长为骨干教师。', 0, 0, datetime('now'), datetime('now'), 0),                                                          -- id=45
-- 成果展示贴评论（target_type=2，id=46~51；成果贴1=活动5、成果贴2=活动20）
(1, 2, 8, '看到我们队伍的展板被挂在最中间，暑期所有的汗水都值得了！', 0, 0, datetime('now', '-4 days'), datetime('now'), 0),                                         -- id=46
(1, 2, 17, '直播助农那晚大家都守到凌晨，销售额数字出来时激动得跳起来。', 0, 0, datetime('now', '-4 days'), datetime('now'), 0),                                      -- id=47
(1, 2, 5, '各队伍的完整实践数据已汇总在展板右下角，欢迎前来核对。', 47, 17, datetime('now', '-3 days'), datetime('now'), 0),                                         -- id=48
(2, 2, 13, '成果册做得好用心，孩子们的笑脸都被镜头记录下来了。', 0, 0, datetime('now', '-3 days'), datetime('now'), 0),                                              -- id=49
(2, 2, 10, '建议把课程设计模板整理分享出来，下批次支教的同学可以直接复用。', 0, 0, datetime('now', '-3 days'), datetime('now'), 0),                                  -- id=50
(2, 2, 5, '已安排！模板会随下一批次支教招募公告一起发布。', 50, 10, datetime('now', '-2 days'), datetime('now'), 0);                                                 -- id=51

-- =============================================
-- 8. 插入通知数据（共 37 条）
-- type: 1-系统公告(user_id=0), 2-活动提醒, 3-审核结果, 4-成果展示贴发布, 5-评论回复
-- 公告填写 publish_scope/is_pinned/publish_time/status/announce_category（对齐 notification 扩展字段）
-- =============================================
INSERT INTO notification (user_id, title, content, type, activity_id, is_read, read_time, create_time, deleted, publish_scope, is_pinned, publish_time, status, announce_category) VALUES
-- 系统公告
(0, '系统维护通知', '平台将于本周六22:00至次日2:00进行系统维护，期间可能出现短暂无法访问，请提前保存好您的数据。', 1, NULL, 0, NULL, datetime('now', '-1 days'), 0, 'ALL', 0, datetime('now', '-1 days'), 1, 'SYSTEM'),
(0, '期中季校园活动上新', '本学期各学院活动持续上新，师范技能、学术讲座、文体赛事、志愿服务、社团活动、就业创业六大类活动欢迎同学们积极报名！', 1, NULL, 0, NULL, datetime('now', '-2 days'), 0, 'ALL', 1, datetime('now', '-2 days'), 1, 'ACTIVITY'),
(0, '校园活动管理条例更新', '《校园活动管理条例（修订版）》已发布，请各活动组织者规范活动申报、审核与安全预案流程。', 1, NULL, 0, NULL, datetime('now', '-3 days'), 0, 'ALL', 0, datetime('now', '-3 days'), 1, 'SYSTEM'),
(0, '第十一届校园读书节开幕', '读书节今日开幕，旧书市集、经典诵读、名师导读、自然笔记展等活动持续一周，欢迎师生参与。', 1, 8, 0, NULL, datetime('now', '-2 days'), 0, 'ALL', 0, datetime('now', '-2 days'), 1, 'ACTIVITY'),
(0, '师范类毕业生就业双选会预告', '来自市内外60余所中学、小学及教育企业的招聘单位将进驻体育馆，欢迎2027届毕业生备好简历前来洽谈。', 1, 11, 0, NULL, datetime('now', '-2 days'), 0, 'ALL', 0, NULL, 1, 'ACTIVITY'),
-- 活动审核结果通知（发给活动组织者）
(5, '活动审核通过', '您创建的活动「第三届师范生教学技能大赛」已通过审核，现已发布。', 3, 1, 1, datetime('now', '-12 hours'), datetime('now', '-3 days'), 0, NULL, 0, NULL, 1, NULL),
(3, '活动审核通过', '您创建的活动「三笔字教学基本功比赛」已通过审核，现已发布。', 3, 2, 0, NULL, datetime('now', '-3 days'), 0, NULL, 0, NULL, 1, NULL),
(7, '活动审核通过', '您创建的活动「第二十届校园歌手大赛」已通过审核，现已发布。', 3, 3, 0, NULL, datetime('now', '-5 days'), 0, NULL, 0, NULL, 1, NULL),
(5, '活动审核通过', '您创建的活动「三下乡暑期社会实践成果展」已通过审核，现已发布。', 3, 5, 0, NULL, datetime('now', '-25 days'), 0, NULL, 0, NULL, 1, NULL),
(2, '活动审核通过', '您创建的活动「人工智能与基础教育数字化前沿讲座」已通过审核，现已发布。', 3, 6, 0, NULL, datetime('now', '-3 days'), 0, NULL, 0, NULL, 1, NULL),
(3, '活动审核通过', '您创建的活动「萤火支教志愿服务进社区」已通过审核，现已发布。', 3, 7, 0, NULL, datetime('now', '-4 days'), 0, NULL, 0, NULL, 1, NULL),
(14, '活动审核通过', '您创建的活动「第十一届校园读书节」已通过审核，现已发布。', 3, 8, 0, NULL, datetime('now', '-8 days'), 0, NULL, 0, NULL, 1, NULL),
(6, '活动审核通过', '您创建的活动「师范类毕业生就业双选会」已通过审核，现已发布。', 3, 11, 0, NULL, datetime('now', '-5 days'), 0, NULL, 0, NULL, 1, NULL),
(3, '活动审核通过', '您创建的活动「学术讲座：巴渝文化研究的前沿与田野」已通过审核，现已发布。', 3, 12, 0, NULL, datetime('now', '-24 days'), 0, NULL, 0, NULL, 1, NULL),
(17, '活动审核通过', '您创建的活动「摄影协会·秋日校园外拍」已通过审核，现已发布。', 3, 15, 0, NULL, datetime('now', '-2 days'), 0, NULL, 0, NULL, 1, NULL),
(11, '活动审核未通过', '您创建的活动「初冬缙云山徒步露营活动」未通过审核。原因：缙云山保护区属全年森林防火重点区域，禁止野外用火与露营，不符合校园活动安全管理规定。', 3, 18, 0, NULL, datetime('now', '-1 days'), 0, NULL, 0, NULL, 1, NULL),
(2, '活动已下架', '您创建的活动「智慧课堂技术专题讲座（第三期）」因内容时效性原因已由管理员下架，如需重新上架请更新内容后再次提交审核。', 3, 19, 0, NULL, datetime('now', '-2 days'), 0, NULL, 0, NULL, 1, NULL),
-- 报名审核结果通知（发给报名参与者）
(8, '报名审核通过', '您报名的「第三届师范生教学技能大赛」已通过审核，请按时到场抽签。', 3, 1, 1, datetime('now', '-12 hours'), datetime('now', '-3 days'), 0, NULL, 0, NULL, 1, NULL),
(10, '报名审核通过', '您报名的「第三届师范生教学技能大赛」已通过审核，请按时到场抽签。', 3, 1, 0, NULL, datetime('now', '-3 days'), 0, NULL, 0, NULL, 1, NULL),
(13, '报名审核通过', '您报名的「三笔字教学基本功比赛」已通过审核，比赛用具由主办方统一提供。', 3, 2, 0, NULL, datetime('now', '-2 days'), 0, NULL, 0, NULL, 1, NULL),
(13, '报名审核通过', '您报名的「萤火支教志愿服务进社区」已通过审核，请注意查收行前培训通知。', 3, 7, 0, NULL, datetime('now', '-3 days'), 0, NULL, 0, NULL, 1, NULL),
(8, '报名审核通过', '您报名的「摄影协会·秋日校园外拍」已通过审核，请自备摄影器材并注意安全。', 3, 15, 0, NULL, datetime('now', '-2 days'), 0, NULL, 0, NULL, 1, NULL),
(19, '报名审核通过', '您报名的「大学生职业生涯规划大赛」已通过审核，请提前准备规划书陈述与答辩。', 3, 21, 0, NULL, datetime('now'), 0, NULL, 0, NULL, 1, NULL),
-- 活动提醒通知
(11, '活动即将开始', '您报名的「秋季田径运动会」后天开幕，请体育教育专业方阵成员提前30分钟到场。', 2, 4, 0, NULL, datetime('now'), 0, NULL, 0, NULL, 1, NULL),
(20, '活动即将开始', '您报名的「秋季田径运动会」后天开幕，请提前领取号码布并完成检录。', 2, 4, 0, NULL, datetime('now'), 0, NULL, 0, NULL, 1, NULL),
(10, '活动进行中', '您报名的「第十一届校园读书节」正在进行，经典诵读专场明天下午继续，欢迎参加。', 2, 8, 0, NULL, datetime('now'), 0, NULL, 0, NULL, 1, NULL),
(8, '活动即将开始', '您报名的「第三届师范生教学技能大赛」将在12天后开始，请提前准备片段教学材料。', 2, 1, 0, NULL, datetime('now', '-1 days'), 0, NULL, 0, NULL, 1, NULL),
(16, '活动即将开始', '您报名的「考研经验分享会」将在9天后开始，可提前提交问题清单。', 2, 10, 0, NULL, datetime('now', '-1 days'), 0, NULL, 0, NULL, 1, NULL),
-- 评论回复通知（引用第 7 段对应评论内容）
(12, '评论回复通知', '吴佳琪回复了您在「第三届师范生教学技能大赛」中的评论：一起加油！片段教学环节记得控制好时间。', 5, 1, 0, NULL, datetime('now', '-1 days'), 0, NULL, 0, NULL, 1, NULL),
(16, '评论回复通知', '王雪梅回复了您在「三笔字教学基本功比赛」中的评论：比赛统一提供米字格粉笔和宣纸，钢笔自备即可。', 5, 2, 0, NULL, datetime('now', '-1 days'), 0, NULL, 0, NULL, 1, NULL),
(9, '评论回复通知', '李明华回复了您在「人工智能与基础教育数字化前沿讲座」中的评论：会的，本次讲座将结合多所中学的真实课堂案例展开分析。', 5, 6, 0, NULL, datetime('now', '-1 days'), 0, NULL, 0, NULL, 1, NULL),
(12, '评论回复通知', '刘志强回复了您在「师范类毕业生就业双选会」中的评论：面向全校毕业生开放，简历建议多准备几份。', 5, 11, 0, NULL, datetime('now', '-2 days'), 0, NULL, 0, NULL, 1, NULL),
(17, '评论回复通知', '陈晓芳回复了您在「三下乡暑期社会实践成果展」成果贴中的评论：各队伍的完整实践数据已汇总在展板右下角，欢迎前来核对。', 5, 5, 0, NULL, datetime('now', '-3 days'), 0, NULL, 0, NULL, 1, NULL),
(10, '评论回复通知', '陈晓芳回复了您在「乡村小学支教志愿服务活动（十一月批次）」成果贴中的评论：已安排！模板会随下一批次支教招募公告一起发布。', 5, 20, 0, NULL, datetime('now', '-2 days'), 0, NULL, 0, NULL, 1, NULL),
-- 成果展示贴发布通知（type=4，activity_id 关联活动）
(8, '成果展示贴发布', '您参与的「三下乡暑期社会实践成果展」已发布成果展示贴，欢迎查看。', 4, 5, 0, NULL, datetime('now', '-5 days'), 0, NULL, 0, NULL, 1, NULL),
(13, '成果展示贴发布', '您参与的「三下乡暑期社会实践成果展」已发布成果展示贴，欢迎查看。', 4, 5, 0, NULL, datetime('now', '-5 days'), 0, NULL, 0, NULL, 1, NULL),
(17, '成果展示贴发布', '您参与的「三下乡暑期社会实践成果展」已发布成果展示贴，欢迎查看。', 4, 5, 0, NULL, datetime('now', '-5 days'), 0, NULL, 0, NULL, 1, NULL),
(13, '成果展示贴发布', '您参与的「乡村小学支教志愿服务活动（十一月批次）」已发布成果展示贴，欢迎查看。', 4, 20, 0, NULL, datetime('now', '-4 days'), 0, NULL, 0, NULL, 1, NULL),
(10, '成果展示贴发布', '您参与的「乡村小学支教志愿服务活动（十一月批次）」已发布成果展示贴，欢迎查看。', 4, 20, 0, NULL, datetime('now', '-4 days'), 0, NULL, 0, NULL, 1, NULL);

-- =============================================
-- 9. 插入活动审核记录数据（共 20 条）
-- audit_type: 1-活动发布审核；status: 0-待审核, 1-已通过, 2-已拒绝
-- 已发布活动（1~15、19~21）均有通过记录；活动16 待审核；活动17 草稿未提交（无记录）；活动18 已拒绝
-- =============================================
INSERT INTO activity_audit (activity_id, audit_type, status, submitter_id, submit_time, auditor_id, audit_time, audit_opinion, create_time, update_time, deleted) VALUES
(1, 1, 1, 5, datetime('now', '-5 days'), 1, datetime('now', '-4 days'), '师范技能赛事导向鲜明，环节设置规范，审核通过。', datetime('now', '-5 days'), datetime('now', '-4 days'), 0),
(2, 1, 1, 3, datetime('now', '-5 days'), 1, datetime('now', '-4 days'), '三笔字比赛有助于夯实师范生教学基本功，审核通过。', datetime('now', '-5 days'), datetime('now', '-4 days'), 0),
(3, 1, 1, 7, datetime('now', '-7 days'), 1, datetime('now', '-6 days'), '校园文化品牌活动，组织方案完备，审核通过。', datetime('now', '-7 days'), datetime('now', '-6 days'), 0),
(4, 1, 1, 4, datetime('now', '-11 days'), 1, datetime('now', '-10 days'), '全校性体育赛事，安全预案完善，审核通过。', datetime('now', '-11 days'), datetime('now', '-10 days'), 0),
(5, 1, 1, 5, datetime('now', '-27 days'), 1, datetime('now', '-26 days'), '社会实践成果展示意义重大，审核通过。', datetime('now', '-27 days'), datetime('now', '-26 days'), 0),
(6, 1, 1, 2, datetime('now', '-4 days'), 1, datetime('now', '-3 days'), '学术讲座主题契合教育数字化方向，审核通过。', datetime('now', '-4 days'), datetime('now', '-3 days'), 0),
(7, 1, 1, 3, datetime('now', '-5 days'), 1, datetime('now', '-4 days'), '社区志愿服务活动组织规范，行前培训安排到位，审核通过。', datetime('now', '-5 days'), datetime('now', '-4 days'), 0),
(8, 1, 1, 14, datetime('now', '-9 days'), 1, datetime('now', '-8 days'), '读书节活动方案完整，多学院联动参与，审核通过。', datetime('now', '-9 days'), datetime('now', '-8 days'), 0),
(9, 1, 1, 13, datetime('now', '-4 days'), 1, datetime('now', '-3 days'), '心理健康教育活动有益学生身心，审核通过。', datetime('now', '-4 days'), datetime('now', '-3 days'), 0),
(10, 1, 1, 10, datetime('now', '-3 days'), 1, datetime('now', '-2 days'), '学业发展类活动内容充实，审核通过。', datetime('now', '-3 days'), datetime('now', '-2 days'), 0),
(11, 1, 1, 6, datetime('now', '-6 days'), 1, datetime('now', '-5 days'), '就业双选会组织方案与安全预案完备，审核通过。', datetime('now', '-6 days'), datetime('now', '-5 days'), 0),
(12, 1, 1, 3, datetime('now', '-25 days'), 1, datetime('now', '-24 days'), '学术讲座内容质量高，审核通过。', datetime('now', '-25 days'), datetime('now', '-24 days'), 0),
(13, 1, 1, 7, datetime('now', '-3 days'), 1, datetime('now', '-2 days'), '艺术展演活动丰富校园文化生活，审核通过。', datetime('now', '-3 days'), datetime('now', '-2 days'), 0),
(14, 1, 1, 4, datetime('now', '-8 days'), 1, datetime('now', '-7 days'), '新生篮球联赛赛制合理，裁判安排规范，审核通过。', datetime('now', '-8 days'), datetime('now', '-7 days'), 0),
(15, 1, 1, 17, datetime('now', '-3 days'), 1, datetime('now', '-2 days'), '社团外拍活动路线安全可控，审核通过。', datetime('now', '-3 days'), datetime('now', '-2 days'), 0),
(19, 1, 1, 2, datetime('now', '-14 days'), 1, datetime('now', '-13 days'), '技术类讲座内容符合教学需要，审核通过。', datetime('now', '-14 days'), datetime('now', '-13 days'), 0),
(20, 1, 1, 5, datetime('now', '-17 days'), 1, datetime('now', '-16 days'), '乡村支教活动意义深远，安全保障到位，审核通过。', datetime('now', '-17 days'), datetime('now', '-16 days'), 0),
(21, 1, 1, 6, datetime('now', '-2 days'), 1, datetime('now', '-1 days'), '职业生涯规划大赛契合就业育人导向，审核通过。', datetime('now', '-2 days'), datetime('now', '-1 days'), 0),
(16, 1, 0, 5, datetime('now', '-1 days'), NULL, NULL, NULL, datetime('now', '-1 days'), datetime('now'), 0),
(18, 1, 2, 11, datetime('now', '-2 days'), 1, datetime('now', '-1 days'), '活动地点位于全年森林防火重点区域的自然保护区，禁止野外用火与露营，存在安全隐患，不符合校园活动安全管理规定，审核不通过。', datetime('now', '-2 days'), datetime('now', '-1 days'), 0);

-- =============================================
-- 10. 插入活动成果展示贴数据（共 2 条：活动5 与活动20 各一篇）
-- images 沿用 /images/resultN_M.jpg 路径模式
-- =============================================
INSERT INTO activity_result (activity_id, title, content, images, creator_id, view_count, comment_count, status, create_time, update_time, deleted) VALUES
(5, '青春三下乡：把课堂搬到田野上——暑期社会实践成果回顾', '# 青春三下乡：把课堂搬到田野上

今年暑期，全校32支「三下乡」实践队伍分赴市内外14个区县开展社会实践，现将成果集中回顾如下。

## 实践亮点

1. **支教课堂** - 教育科学学院队伍在巫溪县开设留守儿童暑期课堂，累计授课120学时
2. **乡村墙绘** - 美术学院志愿者为对口乡镇绘制主题文化墙8面
3. **直播助农** - 经济与管理学院、新闻与传媒学院联队助力柑橘销售，直播间观看量突破5万人次
4. **理论宣讲** - 马克思主义学院宣讲团走进社区开展主题微宣讲16场

## 数据一览

- 参与师生460余人
- 服务群众超过1.2万人次
- 形成调研报告32份

感谢每一位顶烈日、走田埂的同学，把论文写在祖国大地上！', '["/images/result5_1.jpg", "/images/result5_2.jpg", "/images/result5_3.jpg"]', 5, 186, 3, 1, datetime('now', '-5 days'), datetime('now'), 0),
(20, '十一月支教纪实：一颗种子的双向奔赴', '# 十一月支教纪实：一颗种子的双向奔赴

十一月批次支教活动走进沙坪坝区回龙坝镇中心小学，25名志愿者为三年级的孩子们带去了一天的课程与陪伴。

## 今日课程

1. **阅读推广** - 汉语言文学（师范）专业志愿者带来绘本共读课
2. **趣味数学** - 数学科学学院志愿者设计「超市购物」情景数学游戏
3. **美术手工** - 美术学院志愿者协助孩子们完成「我的校园」主题剪贴画

## 志愿者说

> 临走时孩子们把画塞到我们手里，那一刻所有疲惫都值得。

下批次支教预计十二月中旬启动，敬请关注。', '["/images/result20_1.jpg", "/images/result20_2.jpg"]', 5, 134, 3, 1, datetime('now', '-4 days'), datetime('now'), 0);

-- =============================================
-- 11. 插入活动评价数据（共 4 条）
-- 业务规则：仅已结束(status=5)且已签到的参与者可评价，每人对同一活动仅可评价一次
-- 当前已结束活动：活动5、活动12、活动20（参与者均已签到）
-- 已评价：活动20-用户13/用户10，活动12-用户16，活动5-用户8
-- 可用于评价功能测试（已签到未评价）：活动20-用户19/12/8，活动12-用户10/18/14/9，活动5-用户13/17/18/19
-- =============================================
INSERT INTO activity_review (activity_id, user_id, rating, content, reply, create_time, update_time, deleted) VALUES
(20, 13, 5, '第一次走进乡村小学的课堂，孩子们求知的眼神让我真正理解了「学高为师、身正为范」的含义，组织方的行前培训也很扎实，希望学校多组织这样的活动。', '感谢你的用心付出！十二月批次正在筹备中，欢迎继续报名，我们会持续优化行前培训与课程对接环节。', datetime('now', '-4 days'), datetime('now', '-3 days'), 0),
(20, 10, 4, '课程设计环节很锻炼人，但支教当天往返车程较长，建议下次考虑错峰出发或延长服务周期。', NULL, datetime('now', '-4 days'), datetime('now', '-4 days'), 0),
(12, 16, 5, '王教授把二十年田野调查的经验浓缩在两小时里，从碑刻释读到口述史访谈，干货满满，期待系列讲座下一期。', NULL, datetime('now', '-12 days'), datetime('now', '-12 days'), 0),
(5, 8, 4, '成果展的支教课堂板块让人印象深刻，实践报告分享环节互动略少，建议明年增加现场问答。', NULL, datetime('now', '-6 days'), datetime('now', '-6 days'), 0);
