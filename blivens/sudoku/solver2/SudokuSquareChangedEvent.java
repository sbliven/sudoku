package blivens.sudoku.solver2;

import java.util.EventObject;

public class SudokuSquareChangedEvent extends EventObject {
	private static final long serialVersionUID = -36106578362917154L;

	public SudokuSquareChangedEvent(SudokuSquare source) {
		super(source);
	}

}
