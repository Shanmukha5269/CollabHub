package com.collabHub.project.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectDTO {

    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /** Pass null to keep the current lead, pass a userId to change it. */
    private Long leadId;
}
