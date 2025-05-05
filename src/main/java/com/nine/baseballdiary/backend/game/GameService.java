package com.nine.baseballdiary.backend.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    public void saveGame(Game game) {
        gameRepository.save(game);
    }

    public Game getGameById(String gameId) {
        return gameRepository.findById(gameId).orElse(null);
    }

    public void updateGame(Game game) {
        gameRepository.save(game);
    }


}
