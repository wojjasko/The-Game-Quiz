package com.gamequiz.domain.stage1;

import java.util.ArrayList;
import java.util.List;

/**
 * A straight line of {@link #LENGTH} tiles on the stage-1 board (horizontal, vertical or diagonal).
 * Owning a whole line gives a bonus. All lines of the board are generated once instead of being typed by hand.
 */
public record Line(int startRow, int startCol, int rowStep, int colStep) {

    public static final int LENGTH = 3;

    /** Every possible line of the board: 8 horizontal, 8 vertical and 8 diagonal ones for a 4x4 board. */
    public static final List<Line> ALL = generateAll(Stage1State.SIZE);

    /** The {@code [row, col]} coordinates of the tiles of this line. */
    public int[][] cells() {
        int[][] cells = new int[LENGTH][];
        for (int i = 0; i < LENGTH; i++) {
            cells[i] = new int[]{startRow + i * rowStep, startCol + i * colStep};
        }
        return cells;
    }

    private static List<Line> generateAll(int size) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        List<Line> lines = new ArrayList<>();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                for (int[] direction : directions) {
                    int endRow = row + direction[0] * (LENGTH - 1);
                    int endCol = col + direction[1] * (LENGTH - 1);
                    if (endRow < size && endCol >= 0 && endCol < size) {
                        lines.add(new Line(row, col, direction[0], direction[1]));
                    }
                }
            }
        }
        return List.copyOf(lines);
    }
}
