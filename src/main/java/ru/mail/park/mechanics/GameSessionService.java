package ru.mail.park.mechanics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mail.park.domain.Board;
import ru.mail.park.domain.Id;
import ru.mail.park.domain.User;
import ru.mail.park.mechanics.objects.BodyFrame;
import ru.mail.park.services.GameDao;
import ru.mail.park.services.UserDao;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionService {
    private Map<Id<User>, GameSession> gameSessionMap = new ConcurrentHashMap<>();
    private Map<Id<User>, Player> playerMap = new ConcurrentHashMap<>();
    private Set<GameSession> sessions = new LinkedHashSet<>();
    private final GameDao gameDao;
    private final UserDao userDao;
    private final RemotePointService remotePointService;
    private final WorldRunnerService worldRunnerService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(GameSessionService.class);

    public GameSessionService(
            GameDao gameDao,
            UserDao userDao,
            RemotePointService remotePointService,
            WorldRunnerService worldRunnerService
    ) {
        this.gameDao = gameDao;
        this.userDao = userDao;
        this.remotePointService = remotePointService;
        this.worldRunnerService = worldRunnerService;
    }

    public Set<GameSession> getSessions() {
        return sessions;
    }

    public boolean isMovingState(Id<User> userId) {
        GameSession session = getSessionFor(userId);
        if (session == null) {
            return false;
        }
        return session.getState() == GameState.MOVING;
    }

    public boolean isPlaying(Id<User> userId) {
        return gameSessionMap.containsKey(userId);
    }

    public boolean isSimulationStartedFor(Id<User> userId) {
        GameSession gameSession = gameSessionMap.get(userId);
        return gameSession == null || gameSession.getState() == GameState.SIMULATION;
    }

    public boolean isTeamReady(GameSession session) {
        return session.getPlayers().stream()
                .allMatch(id -> playerMap.get(id).isReady());
    }

    public boolean isTeamReady(Id<User> userId) {
        GameSession session = gameSessionMap.get(userId);
        if (session == null) {
            LOGGER.error("isTeamReady() - session is null");
            return false;
        }
        return isTeamReady(session);
    }

    public boolean isTeamFinished(Id<User> userId) {
        GameSession session = gameSessionMap.get(userId);
        if (session == null) {
            LOGGER.error("isTeamFinished() - session is null");
            return false;
        }
        return session.getPlayers().stream()
                .allMatch(id -> {
                    Player player = playerMap.get(id);
                    return player == null || player.isFinished();
                });
    }

    public GameSession getSessionFor(Id<User> userId) {
        return gameSessionMap.get(userId);
    }

    public void prepareSimulation(Id<User> userId, List<BodyFrame> snap) {
        LOGGER.warn("Trying to start simulation");
        if (!isPlaying(userId)) {
            LOGGER.error("Should start game before simulation");
            return;
        }
        if (isSimulationStartedFor(userId)) {
            LOGGER.error("Already in simulation");
            return;
        }
        setReadyForPlayer(userId);
        getSessionFor(userId)
                .putSnapFor(userId, snap);
        if (isTeamReady(userId)) {
            setReadyForSession(userId);
        }
    }

    public void joinGame(Id<Board> boardId, Set<Id<User>> players) {
        GameSession gameSession = new GameSession(boardId, players);
        players.forEach(player -> {
            gameSessionMap.put(player, gameSession);
            playerMap.put(player, new Player(userDao.findUserById(player.getId())));
        });
        sessions.add(gameSession);
    }

    public void setMovingForSession(Id<User> userId) {
        GameSession session = getSessionFor(userId);
        if (session == null || session.getState() == GameState.MOVING) {
            LOGGER.warn("Session is null or already in Moving state");
            return;
        }
        session.setState(GameState.MOVING);
    }

    public void setReadyForPlayer(Id<User> userId) {
        Player player = playerMap.get(userId);
        if (player == null) {
            LOGGER.warn("Can't set ready for player");
            return;
        }
        player.setReady(true);
    }

    public void setReadyForSession(Id<User> userId) {
        GameSession session = getSessionFor(userId);
        if (session == null || session.getState() == GameState.READY) {
            LOGGER.warn("Session is null or already in Ready state");
            return;
        }
        session.setState(GameState.READY);
    }

    public void setFinishedForPlayer(Id<User> userId) {
        Player player = playerMap.get(userId);
        if (player == null) {
            LOGGER.warn("Can't set finished for player");
            return;
        }
        player.setFinished();
    }

    public void setFinishedForSession(Id<User> userId) {
        GameSession session = getSessionFor(userId);
        if (session == null || session.getState() == GameState.FINISHED) {
            LOGGER.warn("Session is null or already in Finished state");
            return;
        }
        session.setState(GameState.FINISHED);
    }

    public void removeSessionFor(Id<User> userId) {
        gameSessionMap.remove(userId);
        playerMap.remove(userId);
    }

    public void removeSessionForTeam(GameSession session) {
        if (session == null) {
            return;
        }
        session.getPlayers().stream()
                .filter(Objects::nonNull)
                .forEach(player -> {
                    LOGGER.warn("Removing game session for user");
                    gameSessionMap.remove(player);
                    playerMap.remove(player);
                });
        sessions.remove(session);
        worldRunnerService.removeWorldRunnerFor(session);
    }

    public void removeSessionForTeam(Id<User> userId) {
        GameSession session = gameSessionMap.get(userId);
        removeSessionForTeam(session);
    }

    public Set<Id<User>> getTeamOf(Id<User> userId) {
        GameSession gameSession = gameSessionMap.get(userId);
        if (gameSession == null) {
            return null;
        }
        return gameSession.getPlayers();
    }
}
