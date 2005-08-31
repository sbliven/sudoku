/* Sudoku
 * file SudokuSolver.java
 * 
 * Created on Aug 24, 2005 by blivens
 */
package blivens.sudoku.solver;

import javax.swing.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import static java.awt.BorderLayout.*;

/**
 * @author Spencer Bliven
 */
public class SudokuSolver extends JFrame {

	private SudokuSquare[][] grid;
	private JPanel sudoku;
	//private int[][] rows;// rows[ row# (0-8)][int n (1-9)-1] = number of squares in that row that could be n
	//private int[][] cols;
	//private int[][][] boxes;//boxes[ col (0-2) ][ row (0-2) ][n]
	
	public SudokuSolver() {
		super("Sudoku Solver");
		JPanel main = new JPanel(new BorderLayout());
		
		grid = new SudokuSquare[9][9];

		for(int r=0;r<9;r++)
			for(int c=0;c<9;c++) {
				grid[c][r] = new SudokuSquare(this,c,r);
			}
		/*rows = new int[9][9];
		cols = new int[9][9];
		boxes = new int[3][3][9];
		for(int i=0;i<81;i++) {
			rows[i/9][i%9] = 9;
			cols[i/9][i%9] = 9;
			boxes[i%3][(i/3)%3][i/9] = 9;
		}*/
		
		sudoku = createGridPane(grid);
		main.add(sudoku, CENTER);
		/*
		JPanel savePanel = new JPanel();
		savePanel.setBorder(BorderFactory.createTitledBorder("Save/Load"));
		savePanel.add(new SaveButton());
		savePanel.add(new LoadButton());
		main.add(savePanel, SOUTH);
		*/
		getContentPane().add(main);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
		
	private static JPanel createGridPane(SudokuSquare[][] grid) {
		JPanel sudoku = new JPanel(new GridLayout(3,3));
		for(int j=0;j<9;j+=3)
			for(int i=0;i<9;i+=3) {
				JPanel p = new JPanel(new GridLayout(3,3));
				p.setBorder(BorderFactory.createEtchedBorder());
				
				for(int r=j;r<j+3;r++)
					for(int c=i;c<i+3;c++) {
						p.add(grid[c][r]);
					}
				
				sudoku.add(p);
			}
		return sudoku;
	}

	public void selected(int x, int y, String selection) {
		int xs=x/3;
		int ys=y/3;
		int n = Integer.parseInt(selection) -1;
		//for each sector
		for(int sx=0;sx<3;sx++)
			for(int sy=0;sy<3;sy++) {
				//remove some options
				//update rows cols boxes
				
				//corner quadrant
				if(sx != xs  &&  sy != ys) {
				}
				//center quadrant
				else if(sx == xs && ys == sy) {
					for(int i=sx*3;i<(sx+1)*3;i++)
						for(int j=sy*3;j<(sy+1)*3;j++) {
							if(i==x && j==y) {//calling coords
								
							}
							else {
								grid[i][j].removeItem(selection);
							}
						}
				}
				//row quadrant;same y
				else if(sy == ys) {
					for(int i=sx*3;i<(sx+1)*3;i++) {
						grid[i][y].removeItem(selection);
					}
				}
				//col quadrant;same x
				else if (sx==xs) {
					for(int j=sy*3;j<sy*3+3;j++) {
						grid[x][j].removeItem(selection);
					}
				}
			}

		/*/update row and columns
		rows[y][n] = 0;
		cols[x][n] = 0;
		boxes[x/3][y/3][n] = 0;*/
		
		//check for single choices
		/*System.out.println("("+x+","+y+")");
		for(int j=0;j<9;j++) {
			for(int i=0;i<9;i++) {
				SudokuSquare itm = grid[i][j];
				int count = itm.getItemCount();
				System.out.print(count-1);
			}
			System.out.print("\n");
		}
		System.out.print("\n");*/
		for(int j=0;j<9;j++) {
			for(int i=0;i<9;i++) {
				SudokuSquare itm = grid[i][j];
				int count = itm.getItemCount();
				if(count==2 && itm.isEnabled()) {
					System.out.println("Auto from ("+i+","+j+"): "+(count-1));
					itm.setSelectedIndex(1);
					return;
				}
			}
		}
		
		//check for rows/cols

		Map<String,SudokuSquare>[] rows = new Map[9];
		Map<String,SudokuSquare>[] cols = new Map[9];
		Map<String,SudokuSquare>[] boxes = new Map[9];
		for(int i=0;i<9;i++) {
			rows[i] = new HashMap<String,SudokuSquare>(9,1f);
			cols[i] = new HashMap<String,SudokuSquare>(9,1f);
			boxes[i] = new HashMap<String,SudokuSquare>(9,1f);
		}
		
		for(int c=0;c<9;c++)
			for(int r=0;r<9;r++) {
				SudokuSquare sq = grid[c][r];
				int count = sq.getItemCount();
				for(int i=0;i<count-1;i++) {
					String sel = (String)sq.getItemAt(i+1);
					if(!rows[r].containsKey(sel)) {
						rows[r].put(sel,sq);
					} else {
						rows[r].put(sel,null);
					}
					if(!cols[c].containsKey(sel)) {
						cols[c].put(sel,sq);
					}else {
						cols[c].put(sel,sq);
					}
					r/=3;
					c/=3;
					if(!boxes[r*3+c].containsKey(sel)) {
						boxes[r*3+c].put(sel,sq);
					} else {
						boxes[r*3+c].put(sel,null);
					}
				}
			}
		for(int r=0;r<9;r++) {
			for(String sel : rows[r].keySet()) {
				SudokuSquare sq = rows[r].get(sel);
				if(sq!=null) {
					System.out.println("Rows: "+sel+" set at ("+sq.getGridX()+","+sq.getGridY()+")");
					sq.setSelectedItem(sel);
					return;
				}
			}
		}
		for(int c=0;c<9;c++) {
			for(String sel :cols[c].keySet()) {
				SudokuSquare sq = cols[c].get(sel);
				if(sq!=null) {
					System.out.println("Cols: "+sel+" set at ("+sq.getGridX()+","+sq.getGridY()+")");
					sq.setSelectedItem(sel);
					return;
				}
			}
		}
		for(int b=0;b<9;b++) {
			for(String sel :boxes[b].keySet()) {
				SudokuSquare sq = boxes[b].get(sel);
				if(sq!=null) {
					System.out.println("Boxes: "+sel+" set at ("+sq.getGridX()+","+sq.getGridY()+")");
					sq.setSelectedItem(sel);
					return;
				}
			}
		}

	}
	
	/*void updateMetaData(int x, int y, int n) {
		//update row and columns
		rows[y][n]--;
		cols[x][n]--;
		boxes[x/3][y/3][n]--;
	}*/
	
	private void save(String file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
	    		ObjectOutputStream oos = new ObjectOutputStream(fos);
	
	    		oos.writeObject(grid);
	    		oos.close();
	    		fos.close();
		}
		catch(Throwable ex) {
			ex.printStackTrace();
		}
	}
	
	private void load(String file) {
		setVisible(false);
		try {
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			
			grid = (SudokuSquare[][]) ois.readObject();
			sudoku = createGridPane(grid);
		}
		catch(Throwable ex) {
			ex.printStackTrace();
		}
		setVisible(true);
	}
	public static void main(String[] args) {
		new SudokuSolver();
	}

	private class SaveButton extends JButton implements ActionListener {
		public SaveButton() {
			super("Save");
			addActionListener(this);
		}
		public void actionPerformed(ActionEvent e) {
			save("saveGame.sudoku");
		}
	}
	private class LoadButton extends JButton implements ActionListener {
		public LoadButton() {
			super("Save");
			addActionListener(this);
		}
		public void actionPerformed(ActionEvent e) {
			load("saveGame.sudoku");
		}
	}

}
