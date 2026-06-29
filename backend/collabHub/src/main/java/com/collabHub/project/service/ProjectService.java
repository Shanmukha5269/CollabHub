package com.collabHub.project.service;

import com.collabHub.project.dto.CreateProjectDTO;
import com.collabHub.project.dto.ProjectResponseDTO;
import com.collabHub.project.dto.UpdateProjectDTO;

import java.util.List;

public interface ProjectService {

    ProjectResponseDTO createProject(CreateProjectDTO createProjectDTO, String creatorEmail);

    ProjectResponseDTO getProjectById(Long projectId, String userEmail);

    ProjectResponseDTO getProjectByKey(String projectKey, Long workspaceId, String userEmail);

    List<ProjectResponseDTO> getProjectsByWorkspace(Long workspaceId, String userEmail);

    List<ProjectResponseDTO> getProjectsByUser(String userEmail);

    ProjectResponseDTO updateProject(Long projectId, UpdateProjectDTO updateProjectDTO, String userEmail);

    void deleteProject(Long projectId, String userEmail);
}
