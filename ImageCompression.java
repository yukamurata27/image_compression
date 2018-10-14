/*
 * Programming Part on Vector Quantization
 *
 * Programmed by Yuka Murata
 * USC ID: 6434262018
 *
 * CSCI 576:  Multimedia Systems Design
 * Professor: Dr. Havaldar
 *
 * Submitted on October 15, 2018
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

	byte[][] red = new byte[height][width];
	byte[][] green = new byte[height][width];
	byte[][] blue = new byte[height][width];

	// Combination of pixel values kept individually at the same index
	ArrayList<Integer> dr1, dr2, dg1, dg2, db1, db2;

	// Keeps number of venctors at each location in x-y coordinate
	ArrayList<Integer> numVector;

	// Keeps closest codeword number (0 though N-1) to each vector
	ArrayList<Integer> codewordNumber;

	/* 
	 * codewords[0][x] -> positions of each codeword for dr1
	 * codewords[1][x] -> positions of each codeword for dr2
	 * ...
	 * codewords[6][x] -> number of vectors belonging to the codeword
	 */
	int[][] codewords;
	int[][] oldCodewords;

	public static void main(String[] args) {
		ImageCompression ren = new ImageCompression();

		// Read a parameter from command line
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
		codewords = new int[7][N];
		oldCodewords = new int[7][N];
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
		numVector = new ArrayList<Integer>();
		codewordNumber = new ArrayList<Integer>();

		dr1 = new ArrayList<Integer>();
		dr2 = new ArrayList<Integer>();
		dg1 = new ArrayList<Integer>();
		dg2 = new ArrayList<Integer>();
		db1 = new ArrayList<Integer>();
		db2 = new ArrayList<Integer>();

		// Locate initial locations of each codeword
		for (int i = 0; i < N; i++) {
			codewords[0][i] = i;
			for (int j = 1; j < 7; j++) {
				codewords[j][i] = 0;
			}
		}

		System.out.println("Start compressing...");
		System.out.println("When N is large, it may take a few minutes to complete");

		countVectors();
		placeCodewords(N);
		return quantize();
	}

	// Set d1, d2, numVector here
	private void countVectors () {
		int idxr1, idxr2, idxg1, idxg2, idxb1, idxb2, idx;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				idxr1 = red[y][x] & 0xFF;
				idxr2 = red[y][x+1] & 0xFF;
				idxg1 = green[y][x] & 0xFF;
				idxg2 = green[y][x+1] & 0xFF;
				idxb1 = blue[y][x] & 0xFF;
				idxb2 = blue[y][x+1] & 0xFF;

				// Find the combinations and return the index
				idx = findIdx(idxr1, idxr2, idxg1, idxg2, idxb1, idxb2);

				// If the combination of color is new, add to library
				if (idx == -1) {
					dr1.add(idxr1);
					dr2.add(idxr2);
					dg1.add(idxg1);
					dg2.add(idxg2);
					db1.add(idxb1);
					db2.add(idxb2);
					numVector.add(1);
				} else {
					numVector.set(idx, numVector.get(idx) + 1);
				}
			}
		}
	}

	// Operate codewords
	private void placeCodewords (int N) {
		for (int i = 0; i < N; i++) {
			if (firstIteration(N)) break;
			if (noMove(N)) return;
		}

		codewordNumber = new ArrayList<Integer>();

		setCodewordNumber(N);
		moveCodeword(N);
		placeCodewords(N);
	}

	private boolean firstIteration (int N) {
		int last = codewords.length - 1;

		for (int i = 0; i < N; i++) {
			if (codewords[last][i] != 0) return false;
		}

		return true;
	}

	private boolean noMove (int N) {
		int last = codewords.length - 1;

		for (int i = 0; i < N; i++) {
			if (codewords[last][i] != oldCodewords[last][i]) return false;
		}

		return true;
	}

	// Find the closest codeword to each vector
	private void setCodewordNumber (int N) {
		double smDist, dist;
		int closeCodeword = 0;
		int last = codewords.length - 1;

		for (int i = 0; i < N; i++) {
			oldCodewords[last][i] = codewords[last][i];
			codewords[last][i] = 0;
		}

		for (int i = 0; i < numVector.size(); i++) {
			smDist = 1200;

			// Find the closest codeword to vector
			for (int j = 0; j < N; j++) {
				dist = Math.sqrt(Math.pow(dr1.get(i)-codewords[0][j], 2) + Math.pow(dr2.get(i)-codewords[1][j], 2)
								+ Math.pow(dg1.get(i)-codewords[2][j], 2) + Math.pow(dg2.get(i)-codewords[3][j], 2)
								+ Math.pow(db1.get(i)-codewords[4][j], 2) + Math.pow(db2.get(i)-codewords[5][j], 2));
				if (dist < smDist) {
					smDist = dist;
					closeCodeword = j;
				}
			}
			codewordNumber.add(closeCodeword);
		}

		// Update the number of vectors belonginf to each codeword
		for (int i = 0; i < codewordNumber.size(); i++) {
			codewords[last][codewordNumber.get(i)] += numVector.get(i);
		}
	}

	// Take average and mode the codeword's location
	private void moveCodeword (int N) {
		int sumXr, sumYr, sumXg, sumYg, sumXb, sumYb, num;

		for (int i = 0; i < N; i++) {
			sumXr = sumYr = sumXg = sumYg = sumXb = sumYb = num = 0;

			for (int j = 0; j < codewordNumber.size(); j++) {
				if (codewordNumber.get(j) == i) {
					sumXr += dr1.get(j) * numVector.get(j);
					sumYr += dr2.get(j) * numVector.get(j);
					sumXg += dg1.get(j) * numVector.get(j);
					sumYg += dg2.get(j) * numVector.get(j);
					sumXb += db1.get(j) * numVector.get(j);
					sumYb += db2.get(j) * numVector.get(j);
					num += numVector.get(j);
				}
			}

			if (num != 0) {
				codewords[0][i] = Math.round(sumXr/num);
				codewords[1][i] = Math.round(sumYr/num);
				codewords[2][i] = Math.round(sumXg/num);
				codewords[3][i] = Math.round(sumYg/num);
				codewords[4][i] = Math.round(sumXb/num);
				codewords[5][i] = Math.round(sumYb/num);
			}
		}
	}

	private BufferedImage quantize () {
		int idxr1, idxr2, idxg1, idxg2, idxb1, idxb2, idx;
		byte r, g, b;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				idxr1 = red[y][x] & 0xFF;
				idxr2 = red[y][x+1] & 0xFF;
				idxg1 = green[y][x] & 0xFF;
				idxg2 = green[y][x+1] & 0xFF;
				idxb1 = blue[y][x] & 0xFF;
				idxb2 = blue[y][x+1] & 0xFF;
				idx = findIdx(idxr1, idxr2, idxg1, idxg2, idxb1, idxb2);

				r = (byte) Math.round((codewords[0][codewordNumber.get(idx)] + codewords[1][codewordNumber.get(idx)]) / 2);
				g = (byte) Math.round((codewords[2][codewordNumber.get(idx)] + codewords[3][codewordNumber.get(idx)]) / 2);
				b = (byte) Math.round((codewords[4][codewordNumber.get(idx)] + codewords[5][codewordNumber.get(idx)]) / 2);
			
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				img.setRGB(x, y, pix);
				img.setRGB(x+1, y, pix);
			}
		}

		System.out.println("Compression completed!");
		return img;
	}

	private int findIdx (int idxr1, int idxr2, int idxg1, int idxg2, int idxb1, int idxb2) {
		for (int i = 0; i < dr1.size(); i++) {
			if (dr1.get(i) == idxr1 && dr2.get(i) == idxr2 && dg1.get(i) == idxg1 && dg2.get(i) == idxg2 && db1.get(i) == idxb1 && db2.get(i) == idxb2)
				return i;
		}
		return -1;
	}
}
