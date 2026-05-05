package com.collabHub.channel.service;

import com.collabHub.channel.dto.ChannelResponseDTO;
import com.collabHub.channel.dto.CreateChannelDTO;
import com.collabHub.channel.dto.UpdateChannelDTO;
import com.collabHub.channel.entity.Channel;
import com.collabHub.channel.repository.ChannelRepository;
import com.collabHub.common.exception.UserAccessDeniedException;
import com.collabHub.common.exception.UserBannedException;
import com.collabHub.common.exception.UserNotFoundException;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.entity.Workspace;
import com.collabHub.workspace.repository.WorkspaceRepository;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public ChannelResponseDTO createChannel(CreateChannelDTO createChannelDTO, String creatorEmail) {
        log.info("Creating channel: '{}' in workspace: {} for user: {}",
                createChannelDTO.getName(), createChannelDTO.getWorkspaceId(), creatorEmail);

        // 1. Find and validate the creator
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + creatorEmail));

        if (creator.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(creator.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 2. Find and validate the workspace
        Workspace workspace = workspaceRepository.findById(createChannelDTO.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        if (workspace.getSuspended()) {
            throw new IllegalArgumentException("Cannot create channels in suspended workspace");
        }

        // 3. Check if user is a member of the workspace
        boolean isMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspace.getId(), creator.getId())
                .isPresent();

        if (!isMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }

        // 4. Check if channel name already exists in this workspace
        if (channelRepository.existsByNameAndWorkspace(createChannelDTO.getName(), workspace)) {
            throw new IllegalArgumentException("Channel name already exists in this workspace");
        }

        // 5. Create the channel
        Channel channel = Channel.builder()
                .name(createChannelDTO.getName())
                .description(createChannelDTO.getDescription())
                .workspace(workspace)
                .isPrivate(createChannelDTO.getIsPrivate() != null ? createChannelDTO.getIsPrivate() : false)
                .creator(creator)
                .build();

        // Add creator as member
        channel.getMembers().add(creator);

        Channel savedChannel = channelRepository.save(channel);
        log.info("Channel created successfully with ID: {}", savedChannel.getId());

        return convertToResponseDTO(savedChannel);
    }

    @Override
    public ChannelResponseDTO getChannelById(Long channelId, String userEmail) {
        log.info("Fetching channel ID: {} for user: {}", channelId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot access channels.");
        }

        // 2. Find the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        // 3. Check if user has access to the channel
        checkChannelAccess(channel, user);

        return convertToResponseDTO(channel);
    }

    @Override
    public List<ChannelResponseDTO> getChannelsByUser(String userEmail) {
        log.info("Fetching all channels for user: {}", userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot access channels.");
        }

        // 2. Get all workspaces where the user is a member (not removed)
        List<Workspace> userWorkspaces = workspaceMemberRepository
                .findByUserIdAndRemovedAtIsNull(user.getId())
                .stream()
                .map(member -> member.getWorkspace())
                .filter(workspace -> !workspace.getSuspended()) // Exclude suspended workspaces
                .collect(Collectors.toList());

        if (userWorkspaces.isEmpty()) {
            log.info("User: {} is not a member of any active workspace", userEmail);
            return List.of();
        }

        // 3. Collect all channels from all user's workspaces
        List<Channel> allChannels = new ArrayList<>();

        for (Workspace workspace : userWorkspaces) {
            // Get public channels in the workspace
            List<Channel> publicChannels = channelRepository.findByWorkspaceAndIsPrivate(workspace, false);
            allChannels.addAll(publicChannels);

            // Get private channels where user is a member
            List<Channel> privateChannels = channelRepository.findByWorkspaceAndIsPrivate(workspace, true);
            List<Channel> userPrivateChannels = privateChannels.stream()
                    .filter(channel -> channel.getMembers().contains(user))
                    .collect(Collectors.toList());
            allChannels.addAll(userPrivateChannels);
        }

        log.info("Retrieved {} channels for user: {} across {} workspaces", allChannels.size(), userEmail, userWorkspaces.size());

        return allChannels.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelResponseDTO> getChannelsByWorkspace(Long workspaceId, String userEmail) {
        log.info("Fetching channels for workspace: {} for user: {}", workspaceId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot access channels.");
        }

        // 2. Check if user is a member of the workspace
        boolean isMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(workspaceId, user.getId())
                .isPresent();

        if (!isMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        // 3. Check if workspace is suspended
        if (workspace.getSuspended()) {
            throw new IllegalArgumentException("This workspace is suspended. Channels cannot be accessed.");
        }

        // 4. Get public channels in the workspace
        List<Channel> channels = new ArrayList<>(channelRepository.findByWorkspaceAndIsPrivate(workspace, false));

        // 5. Add private channels that the user is a member of
        List<Channel> privateChannels = channelRepository.findByWorkspaceAndIsPrivate(workspace, true);
        List<Channel> userPrivateChannels = privateChannels.stream()
                .filter(channel -> channel.getMembers().contains(user))
                .collect(Collectors.toList());

        channels.addAll(userPrivateChannels);

        log.info("Retrieved {} channels for user: {} in workspace: {}", channels.size(), userEmail, workspaceId);

        return channels.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChannelResponseDTO updateChannel(Long channelId, UpdateChannelDTO updateChannelDTO, String userEmail) {
        log.info("Updating channel ID: {} for user: {}", channelId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 2. Find the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot update channels in a suspended workspace");
        }

        // 4. Check if user can modify the channel (creator or admin)
        if (!channel.getCreator().getId().equals(user.getId())) {
            throw new UserAccessDeniedException("Only the channel creator can modify the channel");
        }

        // 5. Update fields
        if (updateChannelDTO.getName() != null) {
            // Check if new name conflicts
            if (!channel.getName().equals(updateChannelDTO.getName()) &&
                channelRepository.existsByNameAndWorkspace(updateChannelDTO.getName(), channel.getWorkspace())) {
                throw new IllegalArgumentException("Channel name already exists in this workspace");
            }
            channel.setName(updateChannelDTO.getName());
        }

        if (updateChannelDTO.getDescription() != null) {
            channel.setDescription(updateChannelDTO.getDescription());
        }

        if (updateChannelDTO.getIsPrivate() != null) {
            channel.setIsPrivate(updateChannelDTO.getIsPrivate());
        }

        Channel savedChannel = channelRepository.save(channel);
        log.info("Channel updated successfully with ID: {}", channelId);

        return convertToResponseDTO(savedChannel);
    }

    @Override
    @Transactional
    public void deleteChannel(Long channelId, String userEmail) {
        log.info("Deleting channel ID: {} for user: {}", channelId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot perform this action.");
        }

        // 2. Find the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot delete channels in a suspended workspace");
        }

        // 4. Check if user can delete the channel (creator or admin)
        if (!channel.getCreator().getId().equals(user.getId())) {
            throw new UserAccessDeniedException("Only the channel creator can delete the channel");
        }

        channelRepository.delete(channel);
        log.info("Channel deleted successfully with ID: {}", channelId);
    }

    @Override
    @Transactional
    public ChannelResponseDTO addMemberToChannel(Long channelId, Long userId, String currentUserEmail) {
        log.info("Adding member {} to channel ID: {} by user: {}", userId, channelId, currentUserEmail);

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
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot manage members in a suspended workspace");
        }

        // 4. Check if current user is the channel creator or admin
        if (!channel.getCreator().getId().equals(currentUser.getId())) {
            throw new UserAccessDeniedException("Only the channel creator can add members");
        }

        // 5. Find and validate the user to be added
        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (userToAdd.getDeletedAt() != null) {
            throw new UserNotFoundException("Cannot add deleted user to channel");
        }

        if (UserStatus.BANNED.equals(userToAdd.getStatus())) {
            throw new UserBannedException("Cannot add banned user to channel");
        }

        // 6. Check if user is a member of the workspace
        boolean isWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(channel.getWorkspace().getId(), userId)
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
        log.info("Member {} added to channel ID: {} successfully", userId, channelId);

        return convertToResponseDTO(savedChannel);
    }

    @Override
    @Transactional
    public ChannelResponseDTO removeMemberFromChannel(Long channelId, Long userId, String currentUserEmail) {
        log.info("Removing member {} from channel ID: {} by user: {}", userId, channelId, currentUserEmail);

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
                .orElseThrow(() -> new IllegalArgumentException("Channel not found"));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("Cannot manage members in a suspended workspace");
        }

        // 4. Check if current user is the channel creator or removing themselves
        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        boolean isCreator = channel.getCreator().getId().equals(currentUser.getId());
        boolean isRemovingSelf = currentUser.getId().equals(userToRemove.getId());

        if (!isCreator && !isRemovingSelf) {
            throw new UserAccessDeniedException("Only the channel creator or the user themselves can remove members");
        }

        // 5. Check if user is a member of the channel
        if (!channel.getMembers().contains(userToRemove)) {
            throw new IllegalArgumentException("User is not a member of this channel");
        }

        // 6. Remove the user from the channel
        channel.getMembers().remove(userToRemove);
        Channel savedChannel = channelRepository.save(channel);
        log.info("Member {} removed from channel ID: {} successfully", userId, channelId);

        return convertToResponseDTO(savedChannel);
    }

    private void checkChannelAccess(Channel channel, User user) {
        // Check if user is deleted
        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        // Check if user is banned
        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot access channels.");
        }

        // Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new IllegalArgumentException("This workspace is suspended and channels cannot be accessed");
        }

        // Check if user is member of the workspace
        boolean isWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(channel.getWorkspace().getId(), user.getId())
                .isPresent();

        if (!isWorkspaceMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }

        // For private channels, check if user is a member of the channel
        if (channel.getIsPrivate() && !channel.getMembers().contains(user)) {
            throw new UserAccessDeniedException("You do not have access to this private channel");
        }

        log.info("Channel access verified for user: {} on channel: {}", user.getEmail(), channel.getId());
    }

    private ChannelResponseDTO convertToResponseDTO(Channel channel) {
        return ChannelResponseDTO.builder()
                .id(channel.getId())
                .name(channel.getName())
                .description(channel.getDescription())
                .workspaceId(channel.getWorkspace().getId())
                .workspaceName(channel.getWorkspace().getName())
                .isPrivate(channel.getIsPrivate())
                .creatorId(channel.getCreator().getId())
                .creatorName(channel.getCreator().getName())
                .memberCount(channel.getMembers().size())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }
}