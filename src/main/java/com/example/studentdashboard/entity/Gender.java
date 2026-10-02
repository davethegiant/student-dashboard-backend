package com.example.studentdashboard.entity;

/**
 * Constant names are deliberately capitalized ("Male"/"Female") rather than
 * "MALE"/"FEMALE" so default JSON serialization matches the existing
 * frontend's expected casing exactly — no custom @JsonValue needed.
 */
public enum Gender {
    Male, Female
}
