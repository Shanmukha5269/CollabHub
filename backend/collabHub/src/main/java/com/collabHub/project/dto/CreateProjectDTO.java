package com.collabHub.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectDTO {

    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Short uppercase key for issue ID prefixes (e.g. "COLL", "PROJ", "DEV").
     * 2–10 uppercase letters only — validated by the regex below.
     */
    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Project key must be 2–10 uppercase letters (e.g. COLL, PROJ)")
    private String projectKey;

    @NotNull(message = "Workspace ID is required")
    private Long workspaceId;

    /** Optional: assign a workspace member as project lead at creation time. */
    private Long leadId;
}
