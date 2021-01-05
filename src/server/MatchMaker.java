package server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MatchMaker extends Thread {
    private static final int QUEUE_CAPACITY = 100;
    private static final int GAME_SIZE = 2;
    private static final int REFRESH_TIME = 100;
    private final BlockingQueue<Player> playersWaitingList;
    private final QuestionsGenerator questionsGenerator;
    private final Timer timer;

    public MatchMaker() {
        playersWaitingList = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        questionsGenerator = new QuestionsGenerator();
        timer = new Timer();
    }

    /**
     * keeps checking if there are enough players to create a new game
     */
    @Override
    public void run() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (playersWaitingList.size() >= GAME_SIZE) {
                    createGame();
                }
            }
        };
        timer.schedule(task, 0, REFRESH_TIME);
    }

    /**
     * creates a new game and adds GAME_SIZE players to it from the waiting list
     */
    private synchronized void createGame() {
        Game game = new Game(questionsGenerator);
        Logger.log("created " + game);
        for (int i = 0; i < GAME_SIZE; i++) {
            game.addPlayer(nextPlayer());
        }
        game.start();
    }

    private Player nextPlayer() {
        Player player = null;
        try {
            player = playersWaitingList.take();
        } catch (InterruptedException e) {
            Logger.log("queue operation interrupted at match maker");
        }
        return player;
    }

    /**
     * adds player to the waiting list to be matched with other players
     *
     * @param player player to be added to the waiting list
     */
    public void add(Player player) {
        try {
            playersWaitingList.put(player);
            player.sendMessage("Waiting for other players...");
            Logger.log(player + " was added to the waiting queue");
        } catch (InterruptedException e) {
            Logger.log("failed to add " + player + " to the waiting queue");
        }
    }
}
