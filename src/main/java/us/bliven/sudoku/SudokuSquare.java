/**
 * 
 */
package us.bliven.sudoku;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;

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
		
		selected = element;
		
		// Notify Listeners
		for(SudokuSquareChangedListener l : sscListeners) {
			l.squareChanged(this, element, SudokuSquareChangedListener.ChangeType.SELECTED);
		}
		
		// Reject other elements
		for(Integer elem : SudokuSquare.Elements) {
			if( !elem.equals( selected )) {
				reject(elem);
			}
		}
		
		return true;
	}
	
	public void reject(Integer element) throws InconsistentSudokuException {
		if( element.equals(getSelected())) {
			throw new InconsistentSudokuException("Error: Cannot reject "+element+" because it is selected.");
		}
		if(!isAvailable(element) ) {
			return; // Silently ignore previously rejected elements
		}
		
		setAvailable(element, false);
		
		// Notify Listeners
		for(SudokuSquareChangedListener l : sscListeners) {
			l.squareChanged(this, element, SudokuSquareChangedListener.ChangeType.REJECTED);
		}
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


	public void fireUndoableEdit(UndoableEdit e) {
		//TODO use this method
		for(UndoableEditListener l : undoListeners) {
			l.undoableEditHappened(new UndoableEditEvent(this,e));
		}
	}
	public void addUndoableEditListener(UndoableEditListener l) {
		undoListeners.add(l);
	}



	public void reset() {
		selected = null;
		available = new boolean[Elements.length];
		for(int i=0;i< available.length; i++ )
			available[i] = true;

		// Notify Listeners
		for(SudokuSquareChangedListener l : sscListeners) {
			l.squareChanged(this, null, SudokuSquareChangedListener.ChangeType.RESET);
		}
	}


	
}
