ALTER TABLE edu_course_section ADD COLUMN video_file VARCHAR(255) NULL;
UPDATE edu_course_section SET video_file='login.mp4';
