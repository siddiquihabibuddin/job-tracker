package com.jobtracker.apps.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//REST controller for managing job applications
@RestController
@RequestMapping("/v1/jobs")
public class JobsController {
    // Implementation goes here
    //REST Api to manage job applications
    //Supports CRUD operations and filtering
    public JobsController() {
    }
}
