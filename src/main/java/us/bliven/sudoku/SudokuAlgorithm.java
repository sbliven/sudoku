package us.bliven.sudoku;

public interface SudokuAlgorithm extends SudokuSquareChangedListener {
	public String getName();
	public boolean isEnabled();
	public void setEnabled(boolean enabled);
	public Integer getMnemonic();
}
