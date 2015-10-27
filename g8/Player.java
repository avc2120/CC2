package cc2.g8;

import cc2.sim.Point;
import cc2.sim.Shape;
import cc2.sim.Dough;
import cc2.sim.Move;

import java.util.*;

public class Player implements cc2.sim.Player {

	private boolean[] row_2 = new boolean [0];

	private static int offset = 0;
	private Random gen = new Random();

	public Shape cutter(int length, Shape[] shapes, Shape[] opponent_shapes)
	{
		// check if first try of given cutter length
		Point[] cutter = new Point [length];
		if (row_2.length != cutter.length - 1) {
			// save cutter length to check for retries
			row_2 = new boolean [cutter.length - 1];
			for (int i = 0 ; i != cutter.length ; ++i)
				//points each piece occupies
				cutter[i] = new Point(i, 0);
		} else {
			// pick a random cell from 2nd row but not same
			int i;
			do {
				i = gen.nextInt(cutter.length - 1);
			} while (row_2[i]);
			row_2[i] = true;
			cutter[cutter.length - 1] = new Point(i, 1);
			for (i = 0 ; i != cutter.length - 1 ; ++i)
				cutter[i] = new Point(i, 0);
		}
		return new Shape(cutter);
	}

	public Move cut(Dough dough, Shape[] shapes, Shape[] opponent_shapes)
	{
		// prune larger shapes if initial move
		if (dough.uncut()) {
			offset = (int)((3.0/8)*dough.side());
			int min = Integer.MAX_VALUE;
			for (Shape s : shapes)
				if (min > s.size())
					min = s.size();
			for (int s = 0 ; s != shapes.length ; ++s)
				if (shapes[s].size() != min)
					shapes[s] = null;
		}
		// find all valid cuts
		int area = (dough.side() - 2*offset)*(dough.side()-2*offset);
		if (percentCut(dough, offset, area) > 0.5 && offset >= 0)
		{
			offset = offset/2;
		}

		ArrayList <Move> moves = new ArrayList <Move> ();
		for (int i = offset ; i != dough.side()- offset ; ++i)
			for (int j = offset; j != dough.side()-offset; ++j) {
				Point p = new Point(i, j);
				for (int si = 0 ; si != shapes.length ; ++si) {
					if (shapes[si] == null) continue;
					Shape[] rotations = shapes[si].rotations();
					for (int ri = 0 ; ri != rotations.length ; ++ri) {
						Shape s = rotations[ri];
						if (dough.cuts(s, p))
							moves.add(new Move(si, ri, p));
					}
				}
			}
		// return a cut randomly
		return moves.get(gen.nextInt(moves.size()));
	}

	private double percentCut(Dough dough, int offset, int area)
	{
		int num_cuts = 0;
		for (int i = offset ; i != dough.side()- offset ; ++i)
		{
			for (int j = offset; j != dough.side()-offset; ++j) 
			{
				if (dough.uncut(i,j) == false)
				{
					num_cuts++;
				}
			}
		}
		return num_cuts/((double)area);
	}
}
