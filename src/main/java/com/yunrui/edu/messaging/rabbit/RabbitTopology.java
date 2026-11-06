package com.yunrui.edu.messaging.rabbit;

public final class RabbitTopology {
    public static final String EDUCATION_EXCHANGE = "yunrui.education.events";
    public static final String COURSE_PURCHASED_QUEUE = "yunrui.learning.course-purchased";
    public static final String COURSE_PURCHASED_KEY = "course.purchased";

    private RabbitTopology() {
    }
}
