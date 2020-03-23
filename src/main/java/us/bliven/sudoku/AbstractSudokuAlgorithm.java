package us.bliven.sudoku;

public abstract class AbstractSudokuAlgorithm implements SudokuAlgorithm {
	protected String name;
	protected boolean enabled;
	
	public AbstractSudokuAlgorithm(String name) {
		this.name = name;
		this.enabled = true;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	};
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public Integer getMnemonic() {
		return null;
	}
}
