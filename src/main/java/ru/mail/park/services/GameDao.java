package ru.mail.park.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.domain.Board;
import ru.mail.park.domain.BoardMeta;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.mail.park.info.constants.Constants.MAPS_ON_PAGE;

@Service
@Transactional
public class GameDao {
    private EntityManager em;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(GameDao.class);

    public GameDao(EntityManager em) {
        this.em = em;
    }

    public List<BoardMeta> getBoards(@Nullable String sort, @Nullable Integer page) {
        StringBuilder sql = new StringBuilder("select m from BoardMeta m");
        if (sort != null) {
            sql.append(" order by ").append(sort);
        }
        if (page != null) {
            return getBoardsPaged(sql, page);
        }
        try {
            return em.createQuery(sql.toString(), BoardMeta.class)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private List<BoardMeta> getBoardsPaged(StringBuilder sql, Integer page) {
        sql.append(" limit :limit offset :offset");
        try {
            return em.createQuery(sql.toString(), BoardMeta.class)
                    .setParameter("limit", MAPS_ON_PAGE)
                    .setParameter("offset", MAPS_ON_PAGE * (page - 1))
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    public String getBoard(Integer id) {
        try {
            Board board = em.find(Board.class, id);
            return board.getData();
        } catch (NoResultException e) {
            return null;
        }
    }

    public BoardMeta createBoard(Map<String, Object> boardDataMap, BoardMeta boardMeta) throws JsonProcessingException {
        Board board = new Board();
        board.setData(MAPPER.writeValueAsString(boardDataMap));
        em.persist(board);
        boardMeta.setBoard(board);
        em.persist(boardMeta);
        return boardMeta;
    }

    public BoardMeta updateBoard(Integer id, Map<String, Object> boardDataMap, BoardMeta boardMeta) {
        Board board = em.find(Board.class, id);
        if (board == null) {
            /* ToDo: 03.11.2017 Throw NotFoundException */
            return null;
        }
        try {
            if (boardDataMap != null) {
                board.setData(MAPPER.writeValueAsString(boardDataMap));
            }
            if (boardMeta != null) {
                updateBoardMeta(board.getMeta(), boardMeta);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return board.getMeta();
    }

    private void updateBoardMeta(BoardMeta boardMeta, BoardMeta metaNew) {
        if (metaNew.getName() != null) {
            boardMeta.setName(metaNew.getName());
        }
        if (metaNew.getTimer() != null) {
            boardMeta.setTimer(metaNew.getTimer());
        }
        if (metaNew.getLevel() != null) {
            boardMeta.setLevel(metaNew.getLevel());
        }
        if (metaNew.getPlayers() != null) {
            boardMeta.setPlayers(metaNew.getPlayers());
        }
        if (metaNew.getPreview() != null) {
            boardMeta.setPreview(metaNew.getPreview());
        }
    }
}
