package com.collabHub.sprint.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSprintDTO {

    @Size(min = 1, max = 100, message = "Sprint name must be between 1 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Goal must not exceed 1000 characters")
    private String goal;

    private LocalDate startDate;
    private LocalDate endDate;
}
