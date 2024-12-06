package com.boot.gugi.service;

import com.boot.gugi.base.dto.TeamDTO;
import com.boot.gugi.model.TeamRank;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface MainService {
    List<TeamDTO.RankResponse> getRanks();

    TeamRank saveRank(TeamDTO.RankRequest rankRequest, MultipartFile teamLogo);
}