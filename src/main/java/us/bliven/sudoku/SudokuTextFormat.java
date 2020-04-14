package us.bliven.sudoku;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;

/**
 * Output format for sudoku board
 * 
 * <h3>Example:</h3>
 * <pre>
   | 31|2
1  |297| 4
  7|   |
---+---+---
 4 |982|  7
   | 4 |
2  |173| 6
---+---+---
   |   |8
 5 |768|  4
  9|52 |
 * </pre>
 * 
 * <h3>Rules</h3>
 * <ul>
 * 	<li>Simple fixed-width text format
 *  <li>Blanks can be represented by space,  period, or underscores
 *  <li>Other characters are ignored, allowing for ascii-art
 *  <li>Trailing blanks are optional
 *  <li>Characters following # are ignored
 *  <li>Empty lines (or lines without blanks/numbers) are ignored. For an all-blank row, include at least one blank.
 *  <li>Files should contain exactly 9 non-comment rows
 * </ul>
 * @author bliven_s
 *
 */
public class SudokuTextFormat {
	public static final String blanks = " ._";
	
	public static final String[] EXTENSIONS = {"sudoku", "txt"}; 
	
	/**
	 * Read a sudoku board into a 9x9 array.
	 * 
	 * 0 indicates blank.
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static int[][] read(Reader reader) throws IOException {
		int[][] squares = new int[9][9];
		int currRow = 0, currCol = 0;
		int lineNr = 1;
		boolean inComment = false;
		
		int c;
		while((c = reader.read()) != -1) {
			if(c == '\n' || c == '\r') {
				lineNr++;
				inComment = false;
				if(currCol > 0) {
					currRow++;
					currCol = 0;
				}
			} else if(inComment) {
				// ignore comment
			} else if(c == '#') {
				inComment = true;
			} else if('1' <= c && c <= '9') {
				if(currCol >= 9) throw new IOException(String.format("Line %d too long", lineNr));
				if(currRow >= 9) throw new IOException("Too many lines");
				squares[currRow][currCol] = Character.getNumericValue(c);
				currCol++;
			} else if(blanks.indexOf(c) >= 0) {
				if(currCol >= 9) throw new IOException(String.format("Line %d too long", lineNr));
				if(currRow >= 9) throw new IOException("Too many lines");
				// squares[currRow][currCol] is 0 by default
				currCol++;
			}
		}
		if(currCol > 0) {
			currRow++;
		}
		if(currRow != 9) {
			throw new IOException(String.format("Expected exactly 9 lines, found %d", currRow));
		}
		return squares;
	}

	public static void write(Writer writer, int[][] board, boolean asciiart, char blank) throws IOException {
		assert(board.length == 9);
		for(int row=0; row < board.length; row++) {
			for(int col=0; col < board[row].length; col++) {
				if(board[row][col] == 0) {
					writer.write(blank);
				} else {
					writer.write(Integer.toString(board[row][col]));
				}
				if(asciiart && col < board[row].length - 1 && col % 3 == 2) {
					writer.write("|");
				}
			}
			writer.write(System.lineSeparator());
			if(asciiart && row < board.length - 1 && row % 3 == 2) {
				writer.write("---+---+---");
				writer.write(System.lineSeparator());
			}
		}
	}
	public static String toString(int[][] board, boolean asciiart) {
		StringWriter str = new StringWriter();
		try {
			write(str, board, asciiart, ' ');
		} catch (IOException e) {
			throw new UncheckedIOException(e); // StringWriters don't throw IOExceptions
		}
		return str.toString();
	}
}
