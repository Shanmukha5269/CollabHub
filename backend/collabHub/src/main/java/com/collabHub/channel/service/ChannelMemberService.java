package com.collabHub.channel.service;

import com.collabHub.channel.dto.AddMemberDTO;
import com.collabHub.channel.dto.RemoveMemberDTO;
import com.collabHub.channel.dto.ChannelMemberResponseDTO;

import java.util.List;

public interface ChannelMemberService {

    /**
     * Add a member to a channel
     */
    ChannelMemberResponseDTO addMemberToChannel(Long channelId, AddMemberDTO addMemberDTO, String currentUserEmail);

    /**
     * Remove a member from a channel
     */
    ChannelMemberResponseDTO removeMemberFromChannel(Long channelId, RemoveMemberDTO removeMemberDTO, String currentUserEmail);

    /**
     * Get all members of a channel
     */
    List<ChannelMemberResponseDTO> getChannelMembers(Long channelId, String userEmail);

}
