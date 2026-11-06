-- 为演示课程接入两个具有开放许可的真实 MP4 视频。
-- 文件由 scripts/download-demo-videos.ps1 放入 MEDIA_ROOT 后使用。
-- 其余章节继续使用 login.mp4，避免把同一个大文件复制多份。
UPDATE edu_course_section s
JOIN edu_course c ON c.id = s.course_id
SET s.video_file = CASE
    WHEN s.sort_no = 1 THEN 'course-demo-big-buck-bunny.mp4'
    WHEN s.sort_no = 2 THEN 'course-demo-tears-of-steel.mp4'
    ELSE s.video_file
END
WHERE c.status = 'PUBLISHED'
  AND s.sort_no IN (1, 2);
