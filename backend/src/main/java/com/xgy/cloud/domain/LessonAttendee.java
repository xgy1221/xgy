package com.xgy.cloud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lesson_attendees")
public class LessonAttendee extends BaseEntity {

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 32)
    private String type = "REGULAR";

    @Column(name = "home_class_id")
    private Long homeClassId;

    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Column(name = "teacher_rating")
    private Integer teacherRating;

    @Column(name = "teacher_comment", length = 512)
    private String teacherComment;

    @Column(name = "student_rating")
    private Integer studentRating;

    @Column(name = "student_comment", length = 512)
    private String studentComment;

    @Column(nullable = false)
    private Boolean consumed = false;

    @Column(nullable = false)
    private Boolean absent = false;
}
