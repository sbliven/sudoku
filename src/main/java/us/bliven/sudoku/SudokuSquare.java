/**
 * 
 */
package us.bliven.sudoku;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import us.bliven.sudoku.SudokuSquareChangedListener.ChangeType;

/**
 * @author blivens
 *
 */
public class SudokuSquare {
	public static Integer[] Elements = {1,2,3,4,5,6,7,8,9};
	
	private boolean[] available;
	private List<SudokuSquareChangedListener> sscListeners;
	private List<UndoableEditListener> undoListeners;
	private Integer selected;

	/*
	 * Just used as tags for position; Not used internally.
	 */
	private final int x;
	private final int y;
	
	public SudokuSquare(int x, int y) {		
		sscListeners = new LinkedList<SudokuSquareChangedListener>();
		undoListeners = new LinkedList<UndoableEditListener>();
		
		this.x=x;
		this.y=y;
		
		// Unselected; everything available
		this.reset();
	}
	
	
	
	/**
	 * 
	 * @param element
	 * @return True on success. False if something else was already selected.
	 */
	public boolean select(Integer element) throws InconsistentSudokuException {
		return select(element, true);
	}
	public boolean select(Integer element, boolean significant) throws InconsistentSudokuException {
		// disallow duplicate selections
		if(selected != null) {
			if( selected.equals( element )) {
				return true; // Already selected
			} else {
				//throw new IllegalStateException("Changing selection from "+selected+" to "+element+".");
				return false; // Ignore second selections
			}
		}
		// check if it was available
		if(!isAvailable(element)) {
			throw new InconsistentSudokuException("Error: Cannot select "+element+" because it is not available.");
		}
		
		
		// Select
		selected = element;
		
		// Notify Listeners
		UndoableEdit edit = new SudokuEdit(ChangeType.SELECTED, element, significant);
		fireUndoableEdit(edit);
		
		fireSudokuSquareChanged(element, SudokuSquareChangedListener.ChangeType.SELECTED);

		// Reject other elements
		for(Integer elem : SudokuSquare.Elements) {
			if( !elem.equals( element )) {
				reject(elem, false); //insignificant
			}
		}

		return true;
	}
	
	public void unselect(Integer element) throws InconsistentSudokuException {
		unselect(element, true);
	}
	public void unselect(Integer element, boolean significant) throws InconsistentSudokuException {
		if(selected == null || !selected.equals(element)) {
			throw new InconsistentSudokuException(String.format("Trying to unselect %s but previously selected %s", element, selected));
		}
		
		// unselect
		selected = null;
		setAvailable(element, true);
		
		// notify listeners
		//UndoableEdit edit = new SudokuEdit(ChangeType.UNSELECTED, element, significant);
		//fireUndoableEdit(edit);
		
		fireSudokuSquareChanged(element, SudokuSquareChangedListener.ChangeType.UNSELECTED);
	}
	
	public void reject(Integer element) throws InconsistentSudokuException {
		reject(element, true);
	}
	public void reject(Integer element, boolean significant) throws InconsistentSudokuException {
		if( element.equals(getSelected())) {
			throw new InconsistentSudokuException("Error: Cannot reject "+element+" because it is selected.");
		}
		if(!isAvailable(element) ) {
			return; // Silently ignore previously rejected elements
		}
		
		setAvailable(element, false);
		
		// Notify Listeners
		UndoableEdit edit = new SudokuEdit(ChangeType.REJECTED, element, significant);
		fireUndoableEdit(edit);
		
		fireSudokuSquareChanged(element, SudokuSquareChangedListener.ChangeType.REJECTED);
	}
	
	public void unreject(Integer element) throws InconsistentSudokuException {
		unreject(element, true);
	}
	public void unreject(Integer element, boolean significant) throws InconsistentSudokuException {
		if(selected != null && selected.equals(element)) {
			throw new InconsistentSudokuException(String.format("Trying to unreject %s but previously selected %s", element, selected));
		}
		if(isAvailable(element) ) {
			return; // Silently ignore previously available elements
		}

		// unselect
		setAvailable(element, true);
		
		// notify listeners
		//UndoableEdit edit = new SudokuEdit(ChangeType.UNREJECTED, element, significant);
		//fireUndoableEdit(edit);
		
		fireSudokuSquareChanged(element, SudokuSquareChangedListener.ChangeType.UNREJECTED);
	}

	public void reset() {
		selected = null;
		available = new boolean[Elements.length];
		for(int i=0;i< available.length; i++ )
			available[i] = true;

		// Notify Listeners
		//fireUndoableEdit(new SudokuEdit(ChangeType.RESET, null, false));
		fireSudokuSquareChanged(null, SudokuSquareChangedListener.ChangeType.RESET);

	}
	
	
	public Integer getSelected() {
		return selected;
	}

	public boolean isSelected() {
		return selected != null;
	}
	
	public boolean isAvailable(Integer element) {
		return available[element-1];
	}
	private void setAvailable(Integer element, boolean b) {
		available[element-1] = b;
	}
	
	
	/*
	 * SudokuSquareChanged events
	 */
	public void addSudokuSquareChangedListener(SudokuSquareChangedListener l) {
		sscListeners.add(l);
	}
	public void removeSudokuSquareChangedListener(SudokuSquareChangedListener l) {
		sscListeners.remove(l);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	/**
	 * Hashes SudokuSquares by x,y coordinate, numbering each row sequentially.
	 */
	public int hashCode() {
		return getX()+getY()*SudokuSquare.Elements.length;
	}


	protected void fireUndoableEdit(UndoableEdit e) {
		for(UndoableEditListener l : undoListeners) {
			l.undoableEditHappened(new UndoableEditEvent(this,e));
		}
	}
	public void addUndoableEditListener(UndoableEditListener l) {
		undoListeners.add(l);
	}

	protected void fireSudokuSquareChanged(Integer element, ChangeType type) {
		for(SudokuSquareChangedListener l : sscListeners) {
			l.squareChanged(this, element, type);
		}
	}
	
	protected class SudokuEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 2895214120901613335L;
		
		private Integer element;
		private ChangeType type;
		private boolean significant;
		
		public SudokuEdit(ChangeType type, Integer element, boolean significant) {
			this.element = element;
			this.type = type;
			this.significant = significant;
		}
		
		@Override
		public String getPresentationName() {
			return String.format("%s %s at %s,%s", type, element, getX(), getY());
		}
		
		@Override
		public boolean isSignificant() {
			return this.significant;
		}
		
		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			switch(type) {
			case SELECTED:
				unselect(element, false);
				break;
			case UNSELECTED:
				select(element, false);
				break;
			case REJECTED:
				unreject(element, false);
				break;
			case UNREJECTED:
				reject(element, false);
				break;
			case RESET:
				throw new CannotUndoException();
			}
		}
		
		@Override
		public void redo() throws CannotRedoException {
			super.redo();
			switch(type) {
			case SELECTED:
				select(element, false);
				break;
			case UNSELECTED:
				unselect(element, false);
				break;
			case REJECTED:
				reject(element, false);
				break;
			case UNREJECTED:
				unreject(element, false);
				break;
			case RESET:
				throw new CannotRedoException();
			}
		}	
	}
	
}
