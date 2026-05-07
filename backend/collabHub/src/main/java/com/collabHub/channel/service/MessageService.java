package com.collabHub.channel.service;

import com.collabHub.channel.dto.CreateMessageDTO;
import com.collabHub.channel.dto.MessageResponseDTO;
import com.collabHub.channel.dto.UpdateMessageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface MessageService {

    /**
     * Send a message to a channel
     */
    MessageResponseDTO sendMessage(Long channelId, CreateMessageDTO createMessageDTO, String senderEmail);

    /**
     * Get a message by ID
     */
    MessageResponseDTO getMessageById(Long messageId, String userEmail);

    /**
     * Get all messages in a channel (paginated)
     */
    Page<MessageResponseDTO> getChannelMessages(Long channelId, String userEmail, Pageable pageable);

    /**
     * Edit a message (only sender can edit)
     */
    MessageResponseDTO editMessage(Long messageId, UpdateMessageDTO updateMessageDTO, String userEmail);

    /**
     * Delete a message
     */
    void deleteMessage(Long messageId, String userEmail);

    /**
     * Add a reaction to a message
     */
    MessageResponseDTO addReaction(Long messageId, String emoji, String userEmail);

    /**
     * Remove a reaction from a message
     */
    MessageResponseDTO removeReaction(Long messageId, String emoji, String userEmail);
}
