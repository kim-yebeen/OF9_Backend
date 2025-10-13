package com.nine.baseballdiary.backend.player;

import com.nine.baseballdiary.backend.report.dto.PlayerInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;

    public List<PlayerInfoDto> searchPlayers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        return playerRepository.searchByName(query.trim()).stream()
                .map(p -> PlayerInfoDto.builder()
                        .name(p.getName())
                        .team(p.getTeam())
                        .position(p.getPosition())
                        .imageUrl(null)
                        .build())
                .collect(Collectors.toList());
    }
}