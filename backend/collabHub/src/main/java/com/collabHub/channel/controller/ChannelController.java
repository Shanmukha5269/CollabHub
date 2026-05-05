package com.collabHub.channel.controller;

import com.collabHub.channel.dto.ChannelResponseDTO;
import com.collabHub.channel.dto.CreateChannelDTO;
import com.collabHub.channel.dto.UpdateChannelDTO;
import com.collabHub.channel.service.ChannelService;
import com.collabHub.common.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel Controller
 * Handles channel management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    /**
     * Create a new channel in a workspace
     *
     * @param request channel creation details
     * @return created channel response
     */
    @PostMapping
    public ResponseEntity<ChannelResponseDTO> createChannel(@Valid @RequestBody CreateChannelDTO request) {
        log.info("Channel creation request: {} in workspace {}", request.getName(), request.getWorkspaceId());

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ChannelResponseDTO response = channelService.createChannel(request, currentUserEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a channel by ID
     * User must be a member of the workspace and have access to the channel
     *
     * @param id channel ID
     * @return channel details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChannelResponseDTO> getChannelById(@PathVariable Long id) {
        log.info("Get channel request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ChannelResponseDTO response = channelService.getChannelById(id, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all channels for the authenticated user
     * Returns all channels from all workspaces the user is a member of
     *
     * @return list of channels across all user's workspaces
     */
    @GetMapping
    public ResponseEntity<List<ChannelResponseDTO>> getChannelsByUser() {
        log.info("Get all channels request for authenticated user");

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        List<ChannelResponseDTO> response = channelService.getChannelsByUser(currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all channels in a specific workspace
     * User must be a member of the workspace
     *
     * @param workspaceId workspace ID
     * @return list of channels in the workspace
     */
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<ChannelResponseDTO>> getChannelsByWorkspace(@PathVariable Long workspaceId) {
        log.info("Get channels request for workspace: {}", workspaceId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        List<ChannelResponseDTO> response = channelService.getChannelsByWorkspace(workspaceId, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Update a channel
     * Only the channel creator can update the channel
     *
     * @param id channel ID
     * @param request update details
     * @return updated channel response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChannelResponseDTO> updateChannel(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateChannelDTO request) {
        log.info("Update channel request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ChannelResponseDTO response = channelService.updateChannel(id, request, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a channel
     * Only the channel creator can delete the channel
     *
     * @param id channel ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        log.info("Delete channel request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        channelService.deleteChannel(id, currentUserEmail);

        return ResponseEntity.noContent().build();
    }

    /**
     * Add a member to a channel
     *
     * @param id channel ID
     * @param userId user ID to add
     * @return updated channel response
     */
    @PostMapping("/{id}/members/{userId}")
    public ResponseEntity<ChannelResponseDTO> addMemberToChannel(@PathVariable Long id,
                                                                @PathVariable Long userId) {
        log.info("Add member {} to channel {} request", userId, id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ChannelResponseDTO response = channelService.addMemberToChannel(id, userId, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a member from a channel
     *
     * @param id channel ID
     * @param userId user ID to remove
     * @return updated channel response
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ChannelResponseDTO> removeMemberFromChannel(@PathVariable Long id,
                                                                     @PathVariable Long userId) {
        log.info("Remove member {} from channel {} request", userId, id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        ChannelResponseDTO response = channelService.removeMemberFromChannel(id, userId, currentUserEmail);

        return ResponseEntity.ok(response);
    }
}