package us.bliven.sudoku;

import java.util.EventListener;

public interface SudokuSquareChangedListener extends EventListener {
	public static enum ChangeType {
		SELECTED,
		REJECTED,
		UNSELECTED,
		UNREJECTED,
		RESET
	}
	
	/**
	 * Indicates a change occurred in a SudokuSquare.
	 * @param target The SudokuSquare that changed
	 * @param element The element selected or rejected, if applicable
	 * @param changeType The type of change: SELECTED, REJECTED, etc.
	 * @param chainChanges 
	 */
	public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType, boolean chainChanges);
}
