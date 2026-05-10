package com.collabHub.message.service;

import com.collabHub.message.dto.CreateMessageDTO;
import com.collabHub.message.dto.MessageResponseDTO;
import com.collabHub.message.dto.UpdateMessageDTO;
import com.collabHub.channel.entity.Channel;
import com.collabHub.message.entity.Message;
import com.collabHub.channel.repository.ChannelRepository;
import com.collabHub.message.repository.MessageRepository;
import com.collabHub.message.websocket.RawWebSocketHandler;
import com.collabHub.common.exception.*;
import com.collabHub.user.entity.User;
import com.collabHub.user.entity.UserStatus;
import com.collabHub.user.repository.UserRepository;
import com.collabHub.workspace.entity.Workspace;
import com.collabHub.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RawWebSocketHandler rawWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public MessageResponseDTO sendMessage(Long channelId, CreateMessageDTO createMessageDTO, String senderEmail) {
        log.info("Sending message to channel: {} by user: {}", channelId, senderEmail);

        // 1. Find and validate the sender
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + senderEmail));

        if (sender.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(sender.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot send messages.");
        }

        // 2. Find and validate the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found with ID: " + channelId));

        // 3. Check if workspace is suspended
        if (channel.getWorkspace().getSuspended()) {
            throw new WorkspaceSuspendedException("Cannot send messages in a suspended workspace");
        }

        // 4. Check if sender is a member of the channel (for private channels) or workspace (for public)
        boolean isChannelMember = channel.getMembers().contains(sender);
        boolean isWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(channel.getWorkspace().getId(), sender.getId())
                .isPresent();

        if (!isWorkspaceMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }

        if (channel.getIsPrivate() && !isChannelMember) {
            throw new UserAccessDeniedException("You are not a member of this private channel");
        }

        // 5. Create and save the message
        Message message = Message.builder()
                .content(createMessageDTO.getContent())
                .channel(channel)
                .sender(sender)
                .build();

        // 6. Add mentioned users if any
        if (createMessageDTO.getMentionedUserIds() != null 
                && !createMessageDTO.getMentionedUserIds().isEmpty()) {
                
            Set<User> mentionedUsers = new HashSet<>(
                    userRepository.findAllById(
                            createMessageDTO.getMentionedUserIds()));
                    
            // Check if all users exist
            if (mentionedUsers.size() != createMessageDTO.getMentionedUserIds().size()) {
                throw new UserNotFoundException(
                        "One or more mentioned users not found");
            }
        
            // Validate mentioned users
            for (User mentionedUser : mentionedUsers) {
            
                boolean isInWorkspace = workspaceMemberRepository
                        .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(
                                channel.getWorkspace().getId(),
                                mentionedUser.getId())
                        .isPresent();
                        
                if (!isInWorkspace) {
                    throw new UserAccessDeniedException(
                            "Mentioned user is not a member of this workspace");
                }
            
                // Private channel validation
                if (channel.getIsPrivate()) {
                
                    boolean isInChannel = channel.getMembers()
                            .stream()
                            .anyMatch(member ->
                                    member.getId().equals(mentionedUser.getId()));
                            
                    if (!isInChannel) {
                        throw new UserAccessDeniedException(
                                "Mentioned user is not a member of this private channel");
                    }
                }
            }
        
            message.setMentions(mentionedUsers);
        }

        Message savedMessage = messageRepository.save(message);
        log.info("Message sent successfully with ID: {}", savedMessage.getId());

        // return convertToResponseDTO(savedMessage);

        MessageResponseDTO responseDTO = convertToResponseDTO(savedMessage);

        // Broadcast realtime update
        try {
        
            String json = objectMapper.writeValueAsString(responseDTO);
        
            rawWebSocketHandler.broadcast(json);
        
        } catch (Exception e) {
        
            log.error("Failed to broadcast websocket message", e);
        }

        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponseDTO getMessageById(Long messageId, String userEmail) {
        log.info("Fetching message: {} for user: {}", messageId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        // 2. Find the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with ID: " + messageId));

        // 3. Check if user has access to the channel
        Channel channel = message.getChannel();
        boolean isChannelMember = channel.getMembers().contains(user);
        boolean isWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(channel.getWorkspace().getId(), user.getId())
                .isPresent();

        if (!isWorkspaceMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }

        if (channel.getIsPrivate() && !isChannelMember) {
            throw new UserAccessDeniedException("You are not a member of this private channel");
        }

        return convertToResponseDTO(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponseDTO> getChannelMessages(Long channelId, String userEmail, Pageable pageable) {
        log.info("Fetching messages for channel: {} for user: {}", channelId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        // 2. Find and validate the channel
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel not found with ID: " + channelId));

        // 3. Check if user has access to the channel
        boolean isChannelMember = channel.getMembers().contains(user);
        boolean isWorkspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(channel.getWorkspace().getId(), user.getId())
                .isPresent();

        if (!isWorkspaceMember) {
            throw new UserAccessDeniedException("You are not a member of this workspace");
        }

        if (channel.getIsPrivate() && !isChannelMember) {
            throw new UserAccessDeniedException("You are not a member of this private channel");
        }

        // 4. Fetch paginated messages
        Page<Message> messages = messageRepository.findByChannelIdPaginated(channelId, pageable);
        return messages.map(this::convertToResponseDTO);
    }

    @Override
    public MessageResponseDTO editMessage(Long messageId, UpdateMessageDTO updateMessageDTO, String userEmail) {
        log.info("Editing message: {} by user: {}", messageId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        if (UserStatus.BANNED.equals(user.getStatus())) {
            throw new UserBannedException("Your account has been banned. You cannot edit messages.");
        }

        // 2. Find the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with ID: " + messageId));

        // 3. Check if user is the sender
        if (!message.getSender().getId().equals(user.getId())) {
            throw new UserAccessDeniedException("You can only edit your own messages");
        }

        // 4. Add mentioned users if any
        if (updateMessageDTO.getMentionedUserIds() != null) {

            Set<User> mentionedMembers =
                    new HashSet<>(userRepository.findAllById(
                            updateMessageDTO.getMentionedUserIds()));
                    
            // Check if all users exist
            if (mentionedMembers.size() != updateMessageDTO.getMentionedUserIds().size()) {
                throw new UserNotFoundException(
                        "One or more mentioned users not found");
            }
        
            Workspace workspace = message.getChannel().getWorkspace();
        
            for (User member : mentionedMembers) {
            
                boolean isInMember = workspaceMemberRepository
                        .findByWorkspaceIdAndUserIdAndRemovedAtIsNull(
                                workspace.getId(),
                                member.getId())
                        .isPresent();
                        
                if (!isInMember) {
                    throw new UserAccessDeniedException(
                            "Mentioned user is not part of workspace");
                }
            
                // Validate private channel membership
                if (message.getChannel().getIsPrivate()) {
                
                    boolean isInChannel = message.getChannel()
                            .getMembers()
                            .stream()
                            .anyMatch(channelMember ->
                                    channelMember.getId().equals(member.getId()));
                            
                    if (!isInChannel) {
                        throw new UserAccessDeniedException(
                                "Mentioned user is not part of private channel");
                    }
                }
            }
        
            message.setMentions(mentionedMembers);
        }

        // 5. Update message content
        if (updateMessageDTO.getContent() != null && !updateMessageDTO.getContent().isEmpty()) {
            message.setContent(updateMessageDTO.getContent());
            message.setIsEdited(true);
        }

        Message updatedMessage = messageRepository.save(message);
        log.info("Message edited successfully with ID: {}", updatedMessage.getId());

        // return convertToResponseDTO(updatedMessage);

        MessageResponseDTO responseDTO = convertToResponseDTO(updatedMessage);

        // Broadcast realtime update
        try {
        
            String json = objectMapper.writeValueAsString(responseDTO);
        
            rawWebSocketHandler.broadcast(json);
        
        } catch (Exception e) {
        
            log.error("Failed to broadcast websocket message", e);
        }

        return responseDTO;
    }

    @Override
    public void deleteMessage(Long messageId, String userEmail) {
        log.info("Deleting message: {} by user: {}", messageId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        // 2. Find the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with ID: " + messageId));

        // 3. Check if user is the sender
        if (!message.getSender().getId().equals(user.getId())) {
            throw new UserAccessDeniedException("You can only delete your own messages");
        }

        // 4. Delete the message
        messageRepository.delete(message);
        log.info("Message deleted successfully with ID: {}", messageId);
    }

    @Override
    public MessageResponseDTO addReaction(Long messageId, String emoji, String userEmail) {
        log.info("Adding reaction: {} to message: {} by user: {}", emoji, messageId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        // 2. Find the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with ID: " + messageId));


        // 3. Add or increment reaction
        message.getReactions().put(emoji, message.getReactions().getOrDefault(emoji, 0) + 1);

        Message updatedMessage = messageRepository.save(message);
        log.info("Reaction added successfully to message: {}", messageId);

        return convertToResponseDTO(updatedMessage);
    }

    @Override
    public MessageResponseDTO removeReaction(Long messageId, String emoji, String userEmail) {
        log.info("Removing reaction: {} from message: {} by user: {}", emoji, messageId, userEmail);

        // 1. Find and validate the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User account is deleted");
        }

        // 2. Find the message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with ID: " + messageId));

        // 3. Remove or decrement reaction
        if (message.getReactions().containsKey(emoji)) {
            int count = message.getReactions().get(emoji);
            if (count > 1) {
                message.getReactions().put(emoji, count - 1);
            } else {
                message.getReactions().remove(emoji);
            }
        }

        Message updatedMessage = messageRepository.save(message);
        log.info("Reaction removed successfully from message: {}", messageId);

        return convertToResponseDTO(updatedMessage);
    }

    /**
     * Convert Message entity to MessageResponseDTO
     */
    private MessageResponseDTO convertToResponseDTO(Message message) {
        return MessageResponseDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .channelId(message.getChannel().getId())
                .channelName(message.getChannel().getName())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .senderEmail(message.getSender().getEmail())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .isEdited(message.getIsEdited())
                .reactions(message.getReactions())
                .mentions(message.getMentions().stream()
                        .map(user -> MessageResponseDTO.UserMinimalDTO.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .build())
                        .collect(Collectors.toSet()))
                .build();
    }
}
