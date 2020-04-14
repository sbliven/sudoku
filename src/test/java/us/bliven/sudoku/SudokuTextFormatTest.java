package us.bliven.sudoku;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Scanner;

import org.junit.jupiter.api.Test;


class SudokuTextFormatTest {
	@Test
	void test_read_sample_games() throws IOException {
		String filename = "/games/02_five_star.sudoku";
		InputStream file = SudokuTextFormatTest.class.getResourceAsStream(filename);
		int[][] board = SudokuTextFormat.read(new InputStreamReader(file));
		
		assertEquals(0, board[0][0]);
		assertEquals(3, board[0][4]);
		assertEquals(1, board[0][5]);
		assertEquals(0, board[0][7]);
		assertEquals(1, board[1][0]);
		assertEquals(4, board[7][8]);
		assertEquals(2, board[8][4]);
		assertEquals(0, board[8][8]);
	}
	
	@Test
	void test_weird_board() throws IOException {
		InputStream weird = SudokuTextFormatTest.class.getResourceAsStream("/games/weird.sudoku");
		InputStream normal = SudokuTextFormatTest.class.getResourceAsStream("/games/02_five_star.sudoku");
		
		int[][] weirdBoard = SudokuTextFormat.read(new InputStreamReader(weird));
		int[][] normalBoard = SudokuTextFormat.read(new InputStreamReader(normal));
		assertArrayEquals(normalBoard, weirdBoard);
	}
	
	@Test
	void test_write() throws IOException {
		String filename = "/games/02_five_star.sudoku";
		InputStream file = SudokuTextFormatTest.class.getResourceAsStream(filename);

		try (Scanner scanner = new Scanner(file, "UTF-8" )) {
		    String content = scanner.useDelimiter("\\A").next();

		    int[][] board = SudokuTextFormat.read(new StringReader(content));
		    
		    String written = SudokuTextFormat.toString(board, true);
		    
		    assertEquals(content, written);
		}
	}

}
