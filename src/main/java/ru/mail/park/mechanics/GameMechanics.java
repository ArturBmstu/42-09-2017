package ru.mail.park.mechanics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import ru.mail.park.domain.Board;
import ru.mail.park.domain.BoardMeta;
import ru.mail.park.domain.Id;
import ru.mail.park.domain.User;
import ru.mail.park.mechanics.objects.BodyFrame;
import ru.mail.park.services.GameDao;
import ru.mail.park.services.UserDao;
import ru.mail.park.websocket.message.to.FinishedMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class GameMechanics {
    private final UserDao userDao;
    private final GameDao gameDao;
    private final RemotePointService remotePointService;
    private final GameSessionService gameSessionService;
    private static final Logger LOGGER = LoggerFactory.getLogger(GameMechanics.class);

    private Map<Id<User>, Id<Board>> userBoardMap = new ConcurrentHashMap<>();
    private Queue<Id<User>> waiters = new ConcurrentLinkedQueue<>();
    private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public GameMechanics(
            UserDao userDao,
            GameDao gameDao,
            RemotePointService remotePointService,
            GameSessionService gameSessionService
    ) {
        this.userDao = userDao;
        this.gameDao = gameDao;
        this.remotePointService = remotePointService;
        this.gameSessionService = gameSessionService;
    }

    public void addWaiter(Id<User> userId, Id<Board> board) {
        if (gameSessionService.isPlaying(userId)) {
            return;
        }
        waiters.add(userId);
        userBoardMap.put(userId, board);
        LOGGER.info("User added in queue");
    }

    public void tryStartGame(Id<User> userId, Id<Board> boardId) {
        LOGGER.warn("Trying to start the game");
        if (gameSessionService.isPlaying(userId)) {
            LOGGER.warn("Player is in game now");
            return;
        }
        userBoardMap.remove(userId);
        if (!checkCandidate(userId)) {
            return;
        }
        BoardMeta meta = gameDao.getMetaOf(boardId.getId());
        int players = 1;
        if (meta != null) {
            players = meta.getPlayers();
            LOGGER.info("Players: " + players);
            LOGGER.info("Name: " + meta.getName());
        } else {
            LOGGER.error("Meta is null");
        }
        if (players == 1) {
            LOGGER.warn("Starting with one player");
            gameSessionService.startGame(userId, null, boardId);
            return;
        }
        LOGGER.info("Map size: " + userBoardMap.size());
        for (Map.Entry<Id<User>, Id<Board>> entry : userBoardMap.entrySet()) {
            if (entry.getValue().equals(boardId) && !entry.getKey().equals(userId)) {
                Id<User> opponent = entry.getKey();
                if (!checkCandidate(opponent)) {
                    userBoardMap.remove(opponent);
                    LOGGER.warn("Opponent is not connected or playing or he doesn't exists");
                    continue;
                }
                LOGGER.info("Opponent found. Starting game");
                removeWaiter(userId);
                removeWaiter(opponent);
                gameSessionService.startGame(userId, opponent, boardId);
                return;
            }
        }
        userBoardMap.put(userId, boardId);
        LOGGER.info("Player added!");
    }

    public void tryStartSimulation(Id<User> userId, List<BodyFrame> snap) {
        LOGGER.warn("Trying to start simulation");
        if (!gameSessionService.isPlaying(userId)) {
            LOGGER.error("Should start game before simulation");
            return;
        }
        if (gameSessionService.isSimulationStartedFor(userId)) {
            LOGGER.error("Already in simulation");
            return;
        }
        gameSessionService.setReady(userId);
        if (gameSessionService.isTeamReady(userId)) {
            LOGGER.warn("Starting simulation");
            GameSession gameSession = gameSessionService.getSessionFor(userId);
            gameSession.putSnapFor(userId, snap);
            gameSessionService.startSimulation(gameSession);
        }
    }

    public void handleFinish(Id<User> userId) {
        try {
            remotePointService.sendMessageTo(userId, new FinishedMessage(1L));
        } catch (IOException e) {
            LOGGER.error("Can't send finish message");
        }

        removeWaiter(userId);
        gameSessionService.removeSessionFor(userId);
        remotePointService.cutDownConnection(userId, CloseStatus.SERVER_ERROR);
    }

    public boolean checkCandidate(Id<User> userId) {
        return remotePointService.isConnected(userId)
                && !gameSessionService.isPlaying(userId)
                && userDao.findUserById(userId.getId()) != null;
    }

    public void removeWaiter(Id<User> userId) {
        LOGGER.warn("Removing board waiter with username %s",
                userDao.findUserById(userId.getId()).getUsername()
        );
        userBoardMap.remove(userId);
    }
}
