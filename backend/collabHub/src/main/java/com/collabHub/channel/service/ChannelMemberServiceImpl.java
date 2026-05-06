package com.collabHub.channel.service;

import com.collabHub.channel.dto.AddMemberDTO;
import com.collabHub.channel.dto.ChannelMemberResponseDTO;
import com.collabHub.channel.dto.RemoveMemberDTO;
import com.collabHub.channel.entity.Channel;
import com.collabHub.channel.repository.ChannelRepository;
import com.collabHub.common.exception.ChannelNotFoundException;
import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.common.exception.UserBannedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
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
@Transactional
public class ChannelMemberServiceImpl implements ChannelMemberService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public ChannelMemberResponseDTO addMemberToChannel(Long channelId, AddMemberDTO addMemberDTO, String currentUserEmail) {
        log.info("Adding member {} to channel ID: {} by user: {}", addMemberDTO.getUserId(), channelId, currentUserEmail);

        // 1. Find and validate the current user (who is adding the member)
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + currentUserEmail));

        if (currentUser.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(currentUser.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 2. Find and validate the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found with ID: " + channelId));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot manage members in a suspended workspace");
        }

        // 4. Check if current user is the channel creator or workspace owner
        if (!isChannelOwner(channel, currentUser)) {
            throw new UserAccessDeniedException("Only the channel creator or workspace owner can add members");
        }

        // 5. Find and validate the user to be added
        User userToAdd = userRepository.findById(addMemberDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + addMemberDTO.getUserId()));

        if (userToAdd.getDeletedAt() != null) {
            throw new UserNotFoundException("Cannot add deleted user to channel");
        }

        if (UserStatus.BANNED.equals(userToAdd.getStatus())) {
            throw new UserBannedException("Cannot add banned user to channel");
        }

        // 6. Check if user is a member of the workspace
        boolean isWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(channel.getWorkspace().getId(), addMemberDTO.getUserId())
                .isPresent();

        if (!isWorkspaceMember) {
            throw new UserAccessDeniedException("User is not a member of this workspace");
        }

        // 7. Check if user is already a member of the channel
        if (channel.getMembers().contains(userToAdd)) {
            throw new IllegalArgumentException("User is already a member of this channel");
        }

        // 8. Add the user to the channel
        channel.getMembers().add(userToAdd);
        Channel savedChannel = channelRepository.save(channel);
        log.info("Member {} added to channel ID: {} successfully", addMemberDTO.getUserId(), channelId);

        return convertToResponseDTO(savedChannel, userToAdd);
    }

    @Override
    public ChannelMemberResponseDTO removeMemberFromChannel(Long channelId, RemoveMemberDTO removeMemberDTO, String currentUserEmail) {
        log.info("Removing member {} from channel ID: {} by user: {}", removeMemberDTO.getUserId(), channelId, currentUserEmail);

        // 1. Find and validate the current user (who is removing the member)
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + currentUserEmail));

        if (currentUser.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(currentUser.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 2. Find and validate the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found with ID: " + channelId));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot manage members in a suspended workspace");
        }

        // 4. Find the user to remove
        User userToRemove = userRepository.findById(removeMemberDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + removeMemberDTO.getUserId()));

        // 5. Check if current user is the channel creator, workspace owner, or removing themselves
        boolean isOwner = isChannelOwner(channel, currentUser);
        boolean isRemovingSelf = currentUser.getId().equals(userToRemove.getId());

        if (!isOwner && !isRemovingSelf) {
            throw new UserAccessDeniedException("Only the channel creator, workspace owner, or the user themselves can remove members");
        }

        // 6. Check if user is a member of the channel
        if (!channel.getMembers().contains(userToRemove)) {
            throw new IllegalArgumentException("User is not a member of this channel");
        }

        // 7. Remove the user from the channel
        channel.getMembers().remove(userToRemove);
        Channel savedChannel = channelRepository.save(channel);
        log.info("Member {} removed from channel ID: {} successfully", removeMemberDTO.getUserId(), channelId);

        return convertToResponseDTO(savedChannel, userToRemove);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelMemberResponseDTO> getChannelMembers(Long channelId, String userEmail) {
        log.info("Fetching members for channel: {} for user: {}", channelId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned");
        }

        // 2. Find and validate the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found with ID: " + channelId));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("This workspace is suspended");
        }

        // 4. Check if user is workspace member
        boolean isWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(channel.getWorkspace().getId(), user.getId())
                .isPresent();

        if (!isWorkspaceMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }

        // 5. For private channels, check if user is a member
        if (channel.getIsPrivate() && !channel.getMembers().contains(user)) {
            throw new UserAccessDeniedException("You do not have access to this private channel");
        }

        // 6. Return all channel members
        return channel.getMembers().stream()
                .map(member -> convertToResponseDTO(channel, member))
                .collect(Collectors.toList());
    }

    /**
     * Check if a user is the channel owner (either channel creator or workspace owner)
     */
    private boolean isChannelOwner(Channel channel, User user) {
        // Check if user is the channel creator
        if (channel.getCreator().getId().equals(user.getId())) {
            return true;
        }

        // Check if user is the workspace owner
        if (channel.getWorkspace().getOwner().getId().equals(user.getId())) {
            return true;
        }

        return false;
    }

    /**
     * Convert member information to response DTO
     */
    private ChannelMemberResponseDTO convertToResponseDTO(Channel channel, User member) {
        boolean isChannelCreator = channel.getCreator().getId().equals(member.getId());
        boolean isWorkspaceOwner = channel.getWorkspace().getOwner().getId().equals(member.getId());

        return ChannelMemberResponseDTO.builder()
                .memberId(member.getId())
                .memberName(member.getName())
                .memberEmail(member.getEmail())
                .channelId(channel.getId())
                .channelName(channel.getName())
                .workspaceId(channel.getWorkspace().getId())
                .workspaceName(channel.getWorkspace().getName())
                .isChannelCreator(isChannelCreator)
                .isWorkspaceOwner(isWorkspaceOwner)
                .addedAt(LocalDateTime.now())
                .build();
    }
}
