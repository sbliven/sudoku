package us.bliven.sudoku;

/**
 * Indicates that the SudokuSolver has entered a logically inconsistent state.
 * @author blivens
 *
 */
public class InconsistentSudokuException extends IllegalStateException {
	private static final long serialVersionUID = 1478865135115413019L;

	public InconsistentSudokuException() {
		super();
	}

	public InconsistentSudokuException(String message, Throwable cause) {
		super(message, cause);
	}

	public InconsistentSudokuException(String s) {
		super(s);
	}

	public InconsistentSudokuException(Throwable cause) {
		super(cause);
	}
	
}