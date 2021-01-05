package server;

public class Answer {
    private final int questionId;
    private final int answerNumber;
    private final int playerID;

    public Answer(int questionId, int answerNumber, int playerID) {
        this.questionId = questionId;
        this.answerNumber = answerNumber;
        this.playerID = playerID;
    }

    public int getQuestionId() {
        return questionId;
    }

    public int getPlayerID() {
        return playerID;
    }

    public int getAnswerNumber() {
        return answerNumber;
    }
}
