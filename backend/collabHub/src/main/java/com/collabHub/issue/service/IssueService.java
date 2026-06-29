package com.collabHub.issue.service;

import com.collabHub.issue.dto.CreateIssueDTO;
import com.collabHub.issue.dto.IssueResponseDTO;
import com.collabHub.issue.dto.UpdateIssueDTO;
import com.collabHub.issue.dto.UpdateIssueStatusDTO;
import com.collabHub.issue.entity.IssuePriority;
import com.collabHub.issue.entity.IssueStatus;
import com.collabHub.issue.entity.IssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssueService {

    IssueResponseDTO createIssue(Long projectId, CreateIssueDTO dto, String reporterEmail);

    IssueResponseDTO getIssueById(Long issueId, String userEmail);

    IssueResponseDTO getIssueByKey(String issueKey, String userEmail);

    Page<IssueResponseDTO> getIssuesByProject(Long projectId, String userEmail, Pageable pageable);

    Page<IssueResponseDTO> getIssuesByProjectAndStatus(Long projectId, IssueStatus status, String userEmail, Pageable pageable);

    Page<IssueResponseDTO> getIssuesByProjectAndPriority(Long projectId, IssuePriority priority, String userEmail, Pageable pageable);

    Page<IssueResponseDTO> getIssuesByProjectAndType(Long projectId, IssueType type, String userEmail, Pageable pageable);

    Page<IssueResponseDTO> getIssuesByAssignee(Long projectId, Long assigneeId, String userEmail, Pageable pageable);

    IssueResponseDTO updateIssue(Long issueId, UpdateIssueDTO dto, String userEmail);

    IssueResponseDTO updateIssueStatus(Long issueId, UpdateIssueStatusDTO dto, String userEmail);

    void deleteIssue(Long issueId, String userEmail);
}
