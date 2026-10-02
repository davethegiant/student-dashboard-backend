package com.example.studentdashboard.util;

import java.util.UUID;

/** Formats human-readable business codes from a surrogate auto-increment ID. */
public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String studentCode(Long id) {
        return "STU" + String.format("%04d", id);
    }

    public static String teacherCode(Long id) {
        return "TCH" + String.format("%03d", id);
    }

    /**
     * Postgres's GENERATED ALWAYS AS IDENTITY id isn't known until the row
     * is actually inserted, but student_code/teacher_code are NOT NULL
     * UNIQUE — so creation is a two-step save (insert with a throwaway-but-
     * unique placeholder, then update to the real code once the id exists).
     * 16 chars, well within the VARCHAR(20) column.
     */
    public static String tempCode() {
        return "TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 13).toUpperCase();
    }
}
