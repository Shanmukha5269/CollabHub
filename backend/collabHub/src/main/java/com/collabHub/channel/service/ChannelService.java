package com.collabHub.channel.service;

import com.collabHub.channel.dto.ChannelResponseDTO;
import com.collabHub.channel.dto.CreateChannelDTO;
import com.collabHub.channel.dto.UpdateChannelDTO;

import java.util.List;

public interface ChannelService {

    ChannelResponseDTO createChannel(CreateChannelDTO createChannelDTO, String creatorEmail);

    ChannelResponseDTO getChannelById(Long channelId, String userEmail);

    List<ChannelResponseDTO> getChannelsByUser(String userEmail);

    List<ChannelResponseDTO> getChannelsByWorkspace(Long workspaceId, String userEmail);

    ChannelResponseDTO updateChannel(Long channelId, UpdateChannelDTO updateChannelDTO, String userEmail);

    void deleteChannel(Long channelId, String userEmail);
}