package com.collabHub.workspace.service;

import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.common.exception.UserBannedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.dto.AddWorkspaceMemberDTO;
import com.collabHub.workspace.dto.TransferOwnershipDTO;
import com.collabHub.workspace.dto.WorkspaceMemberDTO;
import com.collabHub.workspace.entity.Workspace;
import com.collabHub.workspace.entity.WorkspaceMember;
import com.collabHub.workspace.entity.WorkspaceRole;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import com.collabHub.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {

    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Override
    public WorkspaceMemberDTO addMember(Long workspaceId, AddWorkspaceMemberDTO request, String requesterEmail) {
        log.info("Adding member {} to workspace {}", request.getUserId(), workspaceId);

        // 1. Verify requester exists and is not banned
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("Requester user not found"));

        if (requester.getDeletedAt() != null) {
            throw new UserNotFoundException("Requester account is deleted");
        }

        if (UserStatus.BANNED.equals(requester.getStatus())) {
            throw new UserBannedException("Your account has been banned");
        }

        // 2. Find workspace
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new UserNotFoundException("Workspace not found"));

        // 3. Check if requester is the owner of the workspace
        if (!workspace.getOwner().getId().equals(requester.getId())) {
            log.warn("User {} attempted to add member to workspace {} they don't own", requesterEmail, workspaceId);
            throw new UserAccessDeniedException("Only the workspace owner can add members");
        }

        // 4. Find the user to be added
        User userToAdd = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User to add not found with ID: " + request.getUserId()));

        // 5. Check if user to add is banned
        if (UserStatus.BANNED.equals(userToAdd.getStatus())) {
            throw new UserBannedException("Cannot add banned user to workspace");
        }

        if (userToAdd.getDeletedAt() != null) {
            throw new UserNotFoundException("Cannot add deleted user to workspace");
        }

        // 6. Check if user is already a member
        if (memberRepository.existsByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, request.getUserId())) {
            throw new UserAccessDeniedException("User is already a member of this workspace");
        }

        // 7. Parse and validate role
        String roleStr = request.getRole() != null ? request.getRole() : "MEMBER";
        WorkspaceRole role;
        try {
            role = WorkspaceRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr);
        }

        // 8. Create workspace member record
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(userToAdd)
                .role(role)
                .canManageMembers(role == WorkspaceRole.OWNER)
                .build();

        memberRepository.save(member);
        log.info("Successfully added user {} as {} to workspace {}", request.getUserId(), role, workspaceId);

        return convertToDTO(member);
    }

    @Override
    public List<WorkspaceMemberDTO> getWorkspaceMembers(Long workspaceId, String requesterEmail) {
        log.info("Fetching members for workspace {}", workspaceId);

        // 1. Verify requester exists
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("Requester user not found"));

        // 1b. Check if requester is banned
        if (UserStatus.BANNED.equals(requester.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 2. Verify workspace exists
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new UserNotFoundException("Workspace not found"));

        // 3. Check if requester is a member of the workspace
        if (!memberRepository.existsByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, requester.getId())) {
            log.warn("User {} attempted to view members of workspace {} they're not part of", requesterEmail, workspaceId);
            throw new UserAccessDeniedException("You must be a member of this workspace to view its members");
        }

        // 4. Get all active members
        List<WorkspaceMember> members = memberRepository.findByWorkspaceIdAndRemovedAtIsNull(workspaceId);

        return members.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WorkspaceMemberDTO getMember(Long workspaceId, Long userId, String requesterEmail) {
        log.info("Fetching member {} details in workspace {}", userId, workspaceId);

        // 1. Verify requester exists
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("Requester user not found"));

        // 1b. Check if requester is banned
        if (UserStatus.BANNED.equals(requester.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 2. Verify workspace exists
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new UserNotFoundException("Workspace not found"));

        // 3. Check if requester is a member of the workspace
        if (!memberRepository.existsByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, requester.getId())) {
            throw new UserAccessDeniedException("You must be a member of this workspace");
        }

        // 4. Get the specific member
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, userId)
                .orElseThrow(() -> new UserNotFoundException("Member not found in this workspace"));

        return convertToDTO(member);
    }

    @Override
    @Transactional
    public WorkspaceMemberDTO transferOwnership(Long workspaceId, TransferOwnershipDTO request, String requesterEmail) {

        // 0. Extract userId from DTO 
        Long newOwnerUserId = request.getNewOwnerUserId();

        log.info("Transferring ownership of workspace {} to user {}", workspaceId, newOwnerUserId);

        // 1. Validate requester
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("Requester not found"));

        if (UserStatus.BANNED.equals(requester.getStatus())) {
            throw new UserBannedException("Your account has been banned");
        }

        // 2. Fetch workspace
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new UserNotFoundException("Workspace not found"));

        // 3. Ensure requester is current owner
        if (!workspace.getOwner().getId().equals(requester.getId())) {
            throw new UserAccessDeniedException("Only owner can transfer ownership");
        }

        // 4. Validate input
        if (newOwnerUserId == null) {
            throw new IllegalArgumentException("New owner userId is required");
        }

        // 5. Prevent transferring to self
        if (requester.getId().equals(newOwnerUserId)) {
            throw new IllegalArgumentException("You are already the owner");
        }

        // 6. Get current owner member
        WorkspaceMember currentOwnerMember = memberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, requester.getId())
                .orElseThrow(() -> new UserNotFoundException("Current owner membership not found"));

        // 7. Get target member
        WorkspaceMember newOwnerMember = memberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, newOwnerUserId)
                .orElseThrow(() -> new UserNotFoundException("Target user is not a member of this workspace"));

        // 8. If already owner (idempotent check)
        if (newOwnerMember.getRole() == WorkspaceRole.OWNER) {
            log.info("User {} is already the owner of workspace {}", newOwnerUserId, workspaceId);
            return convertToDTO(newOwnerMember);
        }

        // 9. Transfer ownership

        // Demote current owner
        currentOwnerMember.setRole(WorkspaceRole.MEMBER);
        currentOwnerMember.setCanManageMembers(false);

        // Promote new owner
        newOwnerMember.setRole(WorkspaceRole.OWNER);
        newOwnerMember.setCanManageMembers(true);

        // Update workspace owner reference
        workspace.setOwner(newOwnerMember.getUser());

        // 10. Save all changes (transaction ensures atomicity)
        memberRepository.save(currentOwnerMember);
        memberRepository.save(newOwnerMember);
        workspaceRepository.save(workspace);

        log.info("Ownership transferred from {} to {}", requester.getId(), newOwnerUserId);

        return convertToDTO(newOwnerMember);
    }

    @Override
    public void removeMember(Long workspaceId, Long userId, String requesterEmail) {
        log.info("Removing member {} from workspace {}", userId, workspaceId);
        
        //  Verify requester exists
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("Requester user not found"));
        
        //  Check if requester is banned
        if (UserStatus.BANNED.equals(requester.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }        

        //  Find workspace
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new UserNotFoundException("Workspace not found"));

        //  Check if requester is the owner of the workspace
        if (!workspace.getOwner().getId().equals(requester.getId())) {
            log.warn("User {} attempted to remove member from workspace {} they don't own", requesterEmail, workspaceId);
            throw new UserAccessDeniedException("Only the workspace owner can remove members");
        }

        //  Check that owner is not trying to remove themselves
        if (workspace.getOwner().getId().equals(userId)) {
            throw new UserAccessDeniedException("Owner cannot remove themselves. Transfer ownership first.");
        }

        //  Find and remove the member (soft delete)
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, userId)
                .orElseThrow(() -> new UserNotFoundException("Member not found in this workspace"));

        member.setRemovedAt(LocalDateTime.now());
        memberRepository.save(member);
        log.info("Successfully removed member {} from workspace {}", userId, workspaceId);
    }

    @Override
    public boolean isMember(Long workspaceId, Long userId) {
        return memberRepository.existsByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, userId);
    }

    @Override
    public boolean isOwner(Long workspaceId, Long userId) {
        return workspaceRepository.findById(workspaceId)
                .map(workspace -> workspace.getOwner().getId().equals(userId))
                .orElse(false);
    }

    @Override
    public long getMembersCount(Long workspaceId) {
        return memberRepository.countByWorkspaceIdAndRemovedAtIsNull(workspaceId);
    }

    /**
     * Convert WorkspaceMember entity to DTO
     */
    private WorkspaceMemberDTO convertToDTO(WorkspaceMember member) {
        return WorkspaceMemberDTO.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .userName(member.getUser().getName())
                .userEmail(member.getUser().getEmail())
                .role(member.getRole())
                .canManageMembers(member.getCanManageMembers())
                .joinedAt(member.getJoinedAt())
                .isActive(member.getRemovedAt() == null)
                .build();
    }
}
