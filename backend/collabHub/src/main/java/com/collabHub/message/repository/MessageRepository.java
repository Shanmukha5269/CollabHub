package com.collabHub.message.repository;

import com.collabHub.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all messages by channel ID with pagination, excluding soft-deleted messages
     */
    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId ORDER BY m.createdAt DESC")
    Page<Message> findByChannelIdPaginated(@Param("channelId") Long channelId, Pageable pageable);

    /**
     * Find all messages sent by a specific user in a channel
     */
    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.sender.id = :senderId")
    List<Message> findByChannelIdAndSenderId(@Param("channelId") Long channelId, @Param("senderId") Long senderId);

    /**
     * Find all messages in a channel created after a specific date
     */
    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.createdAt > :fromDate ORDER BY m.createdAt DESC")
    List<Message> findByChannelIdAndCreatedAfter(@Param("channelId") Long channelId, @Param("fromDate") LocalDateTime fromDate);

    /**
     * Count total messages in a channel (excluding soft-deleted)
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.channel.id = :channelId")
    Long countByChannelId(@Param("channelId") Long channelId);
}
