package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Game extends Thread {
    private static final int NUMBER_OF_QUESTIONS = 5;
    private static final int QUESTIONS_TIME = 30000; // milliseconds
    private static final int TIME_BETWEEN_QUESTIONS = 3000;
    private static final int REFRESH_TIME = 100;
    private static final int DELAY_BEFORE_START_LISTENING = 100;
    private static final int QUEUE_CAPACITY = 100;
    private static volatile int idCounter = 0;
    private final Question[] questions;
    private final HashMap<Integer, Player> players;
    private final BlockingQueue<Answer> answersQueue;
    private final Timer timer;
    private final int matchId;
    private Question currentQuestion;
    private Answer winningAnswer;

    public Game(QuestionsGenerator questionsGenerator) {
        questions = questionsGenerator.getRandomQuestionsSet(NUMBER_OF_QUESTIONS);
        answersQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        players = new HashMap<>();
        timer = new Timer();
        matchId = nextId();
    }

    private synchronized int nextId() {
        int id = idCounter;
        idCounter += 1;
        return id;
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        player.setQuestionTime(QUESTIONS_TIME);
        Logger.log(player + " was added to " + this);
    }

    @Override
    public String toString() {
        return "Game " + matchId;
    }

    /**
     * main game method. it goes through these steps for each question:
     * 1- send question to players
     * 2- sets a timer and waits until all players send an answer or for the timer to go up
     * 3- goes through the answers and finds the first correct answer
     * 4- adds points to the player who got the correct answer first and sends the result to all players
     * after the game ends, the final results are sent to all players
     */
    @Override
    public void run() {
        Logger.log(this + " has started");
        notifyAllPlayers("You were added to a match.\nMatch is starting...");
        for (Question question : questions) {
            waitFor(TIME_BETWEEN_QUESTIONS);
            askQuestion(question);
            waitForAnswers();
            findFirstToAnswer();
            processResult();
        }
        findWinner();
    }

    private void waitFor(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Logger.log("sleep interrupted at " + this);
        }
    }

    /**
     * sends question to all players each in a separate thread
     *
     * @param question question to broadcast
     */
    private void askQuestion(Question question) {
        answersQueue.clear();
        currentQuestion = question;
        for (Player player : players.values()) {
            new Thread(() -> askQuestionToPlayer(player)).start();
        }
    }

    /**
     * sends the current question to a player and wait for an answer
     *
     * @param player player to be asked
     */
    private void askQuestionToPlayer(Player player) {
        Answer answer = player.sendQuestion(currentQuestion);
        try {
            answersQueue.put(answer);
        } catch (InterruptedException e) {
            Logger.log("queue operation interrupted at " + this);
        }
    }

    /**
     * waits until either all players send an answer or the timer goes up
     */
    private void waitForAnswers() {
        TimerTask timeUpTask = new TimerTask() {
            @Override
            public void run() {
                timeUpCallback();
            }
        };
        TimerTask checkAnswersTask = new TimerTask() {
            @Override
            public void run() {
                checkIfAllPlayersAnswered();
            }
        };

        timer.schedule(timeUpTask, QUESTIONS_TIME);
        timer.schedule(checkAnswersTask, DELAY_BEFORE_START_LISTENING, REFRESH_TIME);

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                Logger.log("wait interrupted at " + this);
            }
        }

        timeUpTask.cancel();
        checkAnswersTask.cancel();
    }

    private void checkIfAllPlayersAnswered() {
        if (answersQueue.size() == players.size()) {
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * gets called when timer goes up
     */
    private void timeUpCallback() {
        synchronized (this) {
            notify();
        }
    }

    private void findFirstToAnswer() {
        winningAnswer = null;
        while (!answersQueue.isEmpty()) {
            try {
                Answer attempt = answersQueue.take();
                if (checkAnswer(attempt)) {
                    winningAnswer = attempt;
                    break;
                }
            } catch (InterruptedException e) {
                Logger.log("queue operation interrupted at " + this);
            }
        }
    }

    private boolean checkAnswer(Answer answer) {
        if (answer.getQuestionId() == currentQuestion.getId()) {
            return answer.getAnswerNumber() == currentQuestion.getCorrectAnswer();
        } else {
            return false;
        }
    }

    /**
     * sends results of a round to all players
     */
    private void processResult() {
        String message = "Correct answer is: " + currentQuestion.getCorrectAnswer() + '\n';
        if (winningAnswer == null) {
            message += "No correct answers. Points don't go to anyone.";
        } else {
            Player winner = players.get(winningAnswer.getPlayerID());
            int questionPoints = currentQuestion.getPoints();
            winner.addPoints(questionPoints);
            message += winner.getName() + " got " + currentQuestion.getPoints() + " points for this question.";
        }
        notifyAllPlayers(message);
    }

    /**
     * finds the player with most points.
     */
    private void findWinner() {
        int maxPoints = 0;
        ArrayList<Player> winners = new ArrayList<>();
        for (Player player : players.values()) {
            int points = player.getPoints();
            if (points == maxPoints) {
                winners.add(player);
            } else if (points > maxPoints) {
                maxPoints = points;
                winners.clear();
                winners.add(player);
            }
        }

        if (maxPoints == 0) {
            notifyAllPlayers("Everyone loses");
        } else if (winners.size() > 1) {
            notifyAllPlayers("It's a tie!");
        } else {
            Player winner = winners.get(0);
            notifyAllPlayers(winner.getName() + " wins!");
        }
        Logger.log(this + " ended");
    }

    private void notifyAllPlayers(String message) {
        for (Player player : players.values()) {
            player.sendMessage(message);
        }
    }
}
