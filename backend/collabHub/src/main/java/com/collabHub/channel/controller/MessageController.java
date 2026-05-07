package com.collabHub.channel.controller;

import com.collabHub.channel.dto.CreateMessageDTO;
import com.collabHub.channel.dto.MessageResponseDTO;
import com.collabHub.channel.dto.UpdateMessageDTO;
import com.collabHub.channel.service.MessageService;
import com.collabHub.common.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Message Controller
 * Handles message management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/channels/{channelId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Send a message to a channel
     *
     * @return created message response
     */
    @PostMapping
    public ResponseEntity<MessageResponseDTO> sendMessage(@Valid @PathVariable Long channelId, @RequestBody CreateMessageDTO request) {
        log.info("Message send request to channel: {}", channelId);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        MessageResponseDTO response = messageService.sendMessage(channelId, request, currentUserEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a message by ID
     *
     * @param id message ID
     * @return message details
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageResponseDTO> getMessageById(@PathVariable Long id) {
        log.info("Get message request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        MessageResponseDTO response = messageService.getMessageById(id, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all messages in a channel (paginated)
     * Default page size is 20, maximum is 100
     *
     * @param channelId channel ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return paginated messages
     */
    @GetMapping
    public ResponseEntity<Page<MessageResponseDTO>> getChannelMessages(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Get channel messages request for channel: {} - page: {}, size: {}", channelId, page, size);

        // Validate and limit page size
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        Page<MessageResponseDTO> response = messageService.getChannelMessages(channelId, currentUserEmail, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Edit a message (only sender can edit)
     *
     * @param id message ID
     * @param request update details
     * @return updated message response
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessageResponseDTO> editMessage(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateMessageDTO request) {
        log.info("Edit message request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        MessageResponseDTO response = messageService.editMessage(id, request, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a message (only sender can delete)
     *
     * @param id message ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        log.info("Delete message request for ID: {}", id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        messageService.deleteMessage(id, currentUserEmail);

        return ResponseEntity.noContent().build();
    }

    /**
     * Add a reaction to a message
     *
     * @param id message ID
     * @param emoji emoji to add as reaction
     * @return updated message response
     */
    @PostMapping("/{id}/reactions/{emoji}")
    public ResponseEntity<MessageResponseDTO> addReaction(@PathVariable Long id,
                                                         @PathVariable String emoji) {
        log.info("Add reaction: {} to message: {} request", emoji, id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        MessageResponseDTO response = messageService.addReaction(id, emoji, currentUserEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a reaction from a message
     *
     * @param id message ID
     * @param emoji emoji to remove from reactions
     * @return updated message response
     */
    @DeleteMapping("/{id}/reactions/{emoji}")
    public ResponseEntity<MessageResponseDTO> removeReaction(@PathVariable Long id,
                                                            @PathVariable String emoji) {
        log.info("Remove reaction: {} from message: {} request", emoji, id);

        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        MessageResponseDTO response = messageService.removeReaction(id, emoji, currentUserEmail);

        return ResponseEntity.ok(response);
    }
}
