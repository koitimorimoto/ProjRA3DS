package com.example.projra3ds.controller;

import com.example.projra3ds.model.Player;
import com.example.projra3ds.model.Team;
import javafx.scene.control.ListCell;

import java.util.List;
import java.util.Optional;

final class ControllerUi {
    private ControllerUi() {
    }

    static Optional<Team> findTeam(List<Team> teams, Long teamId) {
        if (teamId == null) {
            return Optional.empty();
        }
        return teams.stream().filter(team -> team.getId().equals(teamId)).findFirst();
    }

    static Optional<Player> findPlayer(List<Player> players, Long playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return players.stream().filter(player -> player.getId().equals(playerId)).findFirst();
    }

    static ListCell<Team> teamCell() {
        return teamCell(false);
    }

    static ListCell<Team> teamCell(boolean nullable) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Team team, boolean empty) {
                super.updateItem(team, empty);
                if (empty) {
                    setText(null);
                } else if (team == null) {
                    setText(nullable ? "Sem time" : null);
                } else {
                    setText(team.getName());
                }
            }
        };
    }

    static ListCell<Player> playerCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                setText(empty || player == null ? null : player.getName());
            }
        };
    }
}
