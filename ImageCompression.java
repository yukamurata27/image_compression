/*
 ** Copying this program is prohibited **
 *
 * University of Southern California
 * Viterbi School of Engineering
 * CSCI 576: Multimedia Systems Design
 * Professor: Dr. Havaldar
 *
 * Vector Quantization
 *
 * Programmed by Yuka Murata
 * Last Update: October 15, 2018
 *
*/

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.Math.*;

public class ImageCompression {

	int width = 352;
	int height = 288;

	// Original pixel data
	byte[][] red = new byte[height][width];
	byte[][] green = new byte[height][width];
	byte[][] blue = new byte[height][width];

	// Resulting pixel data
	byte[][] newR = new byte[height][width];
	byte[][] newG = new byte[height][width];
	byte[][] newB = new byte[height][width];

	// Number of venctors at each x-y coordinate
	ArrayList<Integer> numVector;
	// Closest codeword number (0 though N-1) for each vector
	ArrayList<Integer> codewordNumber;
	// Position at each dimention (x and y)
	ArrayList<Integer> d1, d2;

	/* 
	 * codewords[0][x] -> x positions of each codeword
	 * codewords[1][x] -> y positions of each codeword
	 * codewords[2][x] -> number of vectors belonging to the codeword
	 */
	int[][] codewords;
	int[][] oldCodewords;

	public static void main(String[] args) {
		ImageCompression ren = new ImageCompression();

		// Read parameters from command line
		File file = new File(args[0]);
		int N = Integer.parseInt(args[1]);

		ren.showImgs(file, N, args[0].endsWith("raw"));
	}

	public void showImgs(File file, int N, boolean isRaw){
		BufferedImage original, result;

		initializeVariable(N);
		original = processImgFile(file, isRaw);
		result = compressImg(N);

		// Use labels to display the images
		JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Original Image (Left)");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Compressed Image (Right)");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbIm1 = new JLabel(new ImageIcon(original));
		JLabel lbIm2 = new JLabel(new ImageIcon(result));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);
	}

	private void initializeVariable (int N) {
		codewords = new int[3][N];
		oldCodewords = new int[3][N];
	}

	// Read image file and process data
	private BufferedImage processImgFile (File file, boolean raw) {
		FileInputStream fileInputStream = null;
		byte[] stream = new byte[(int) file.length()];
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int baseIdx;
		
		try {
			//convert file into byte array
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(stream);
			fileInputStream.close();
		}
		catch (Exception e) {}

		// Save each R, G, and B values of image in byte
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				baseIdx = x + width * y;

				red[y][x] = stream[baseIdx];
				if (raw) {
					green[y][x] = stream[baseIdx];
					blue[y][x] = stream[baseIdx];
				} else {
					green[y][x] = stream[baseIdx + (height * width)];
					blue[y][x] = stream[baseIdx + 2 * (height * width)];
				}

				int pix = 0xff000000 | ((red[y][x] & 0xff) << 16) | ((green[y][x] & 0xff) << 8) | (blue[y][x] & 0xff);
				img.setRGB(x, y, pix);
			}
		}

		return img;
	}

	private BufferedImage compressImg (int N) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		newR = subCompression(N, red);
		newG = subCompression(N, green);
		newB = subCompression(N, blue);

		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x ++) {
				int pix = 0xff000000 | ((newR[y][x] & 0xff) << 16) | ((newG[y][x] & 0xff) << 8) | (newB[y][x] & 0xff);
				result.setRGB(x, y, pix);
			}
		}

		return result;
	}

	private byte[][] subCompression (int N, byte[][] data) {
		// Initialize variables
		numVector = new ArrayList<Integer>();
		codewordNumber = new ArrayList<Integer>();
		d1 = new ArrayList<Integer>();
		d2 = new ArrayList<Integer>();

		for (int i = 0; i < N; i++) {
			codewords[0][i] = i;
			codewords[1][i] = 0;
			codewords[2][i] = 0;
		}

		countVectors(data);
		placeCodewords(N, data);
		return quantize(data);
	}

	// Set d1, d2, numVector
	private void countVectors (byte[][] data) {
		int idx1, idx2, idx;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				idx1 = data[y][x] & 0xFF;
				idx2 = data[y][x+1] & 0xFF;
				idx = findIdx(idx1, idx2);
				if (idx == -1) {
					d1.add(idx1);
					d2.add(idx2);
					numVector.add(1);
				} else {
					numVector.set(idx, numVector.get(idx) + 1);
				}
			}
		}
	}

	// set codeword locations each of which has almost same number of vectors belonging to it
	private void placeCodewords (int N, byte[][] data) {
		for (int i = 0; i < N; i++) {
			if (firstIteration(N, 2)) break;
			if (noMove(N, 2)) return;
		}

		codewordNumber = new ArrayList<Integer>();

		setCodewordNumber(N);
		moveCodeword(N);

		placeCodewords(N, data);
	}

	private boolean firstIteration (int N, int idx) {
		for (int i = 0; i < N; i++) {
			if (codewords[idx][i] != 0) return false;
		}
		return true;
	}

	// Check codewords' movement
	private boolean noMove (int N, int idx) {
		for (int i = 0; i < N; i++) {
			if (codewords[idx][i] != oldCodewords[idx][i]) return false;
		}
		return true;
	}

	// Indicates which codeword each vector belongs to
	private void setCodewordNumber (int N) {
		double smDist, dist;
		int closeCodeword = 0;

		for (int i = 0; i < N; i++) {
			oldCodewords[2][i] = codewords[2][i];
			codewords[2][i] = 0;
		}

		for (int i = 0; i < numVector.size(); i++) {
			smDist = 500;

			// Find the closest codeword to vector
			for (int j = 0; j < N; j++) {
				dist = Math.sqrt(Math.pow(d1.get(i)-codewords[0][j], 2) + Math.pow(d2.get(i)-codewords[1][j], 2));
				if (dist < smDist) {
					smDist = dist;
					closeCodeword = j;
				}
			}
			codewordNumber.add(closeCodeword);
		}

		for (int i = 0; i < codewordNumber.size(); i++) {
			codewords[2][codewordNumber.get(i)] += numVector.get(i);
		}
	}

	// Take average and move codeword locations
	private void moveCodeword (int N) {
		int sumX, sumY, num;

		for (int i = 0; i < N; i++) {
			sumX = sumY = num = 0;

			for (int j = 0; j < codewordNumber.size(); j++) {
				if (codewordNumber.get(j) == i) {
					sumX += d1.get(j) * numVector.get(j);
					sumY += d2.get(j) * numVector.get(j);
					num += numVector.get(j);
				}
			}

			if (num != 0) {
				codewords[0][i] = Math.round(sumX/num);
				codewords[1][i] = Math.round(sumY/num);
			}
		}
	}

	// Quantize the image by taking two pizels side by side
	private byte[][] quantize (byte[][] data) {
		int idx1, idx2, idx;
		byte[][] newData = new byte[height][width];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				idx1 = data[y][x] & 0xFF;
				idx2 = data[y][x+1] & 0xFF;
				idx = findIdx(idx1, idx2);
				newData[y][x+1] = newData[y][x] = (byte)Math.round((codewords[0][codewordNumber.get(idx)]
																	+ codewords[1][codewordNumber.get(idx)]) / 2);
			}
		}

		return newData;
	}

	// Find index for vector location
	private int findIdx (int idx1, int idx2) {
		for (int i = 0; i < d1.size(); i++) {
			if (d1.get(i) == idx1 && d2.get(i) == idx2) return i;
		}
		return -1;
	}
}
