package com.example.studentdashboard.service;

import com.example.studentdashboard.dto.response.MetaResponse;

public interface MetaService {

    /** Bootstrap payload for the frontend's dropdowns/filters — sessions, terms, classes, subjects. */
    MetaResponse getMeta();
}
