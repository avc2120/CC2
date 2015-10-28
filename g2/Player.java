package cc2.g2;

import cc2.sim.Point;
import cc2.sim.Shape;
import cc2.sim.Dough;
import cc2.sim.Move;

import java.util.*;

public class Player implements cc2.sim.Player {

	private boolean[] row_2 = new boolean [0];

	private Random gen = new Random();

	public Shape cutter(int length, Shape[] shapes, Shape[] opponent_shapes)
	{
		// check if first try of given cutter length
		Point[] cutter = new Point [length];
		if (row_2.length != cutter.length - 1) {
			// save cutter length to check for retries
			row_2 = new boolean [cutter.length - 1];
			for (int i = 0 ; i != cutter.length ; ++i)
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
		for(Shape s : shapes) {
			System.out.println(s==null ? "NULL" : s.size());
		}
		System.out.println("----------");
		if (dough.uncut()) {
			int min = Integer.MAX_VALUE;
			for (Shape s : shapes)
				if (min > s.size())
					min = s.size();
			for (int s = 0 ; s != shapes.length ; ++s)
				if (shapes[s].size() != min)
					shapes[s] = null;
		}
		// find all valid cuts
		HashMap<Move, Integer> moves = new HashMap <Move, Integer> ();
		for(int si = 0; si < shapes.length; ++si) {
			Shape shape = shapes[si];
			if(shape == null)
				continue;
			for (int i = 0 ; i != dough.side(); ++i)
				for (int j = 0 ; j != dough.side() ; ++j) {
					Point p = new Point(i, j);
					Shape[] rotations = shape.rotations();
					for (int ri = 0 ; ri != rotations.length ; ++ri) {
						Shape s = rotations[ri];
						if (dough.cuts(s, p)) {
							Dough trymove = new ModdableDough(dough);
							moves.put(new Move(si, ri, p), score(trymove, opponent_shapes));
						}
					}
				}
			if(moves.size() > 0)
				break;
		}
		
		// return cut resulting in lowest score
		int bestScore = Integer.MAX_VALUE;
		Move bestMove = null;
		for(Move move : moves.keySet()) {
			if(moves.get(move) < bestScore) {
				bestScore = moves.get(move);
				bestMove = move;
			}
		}
		System.out.println(moves.size());
		return bestMove;
	}
	
	public int score(Dough dough, Shape[] opponent_shapes) {
		for(Shape nextLargestShape : opponent_shapes) {
			int nmoves = 0;
			for (int i = 0 ; i != dough.side() ; ++i) {
				for (int j = 0 ; j != dough.side() ; ++j) {
					Point p = new Point(i, j);
					Shape[] rotations = nextLargestShape.rotations();
					for (int ri = 0 ; ri != rotations.length ; ++ri) {
						Shape s = rotations[ri];
						if (dough.cuts(s, p))
							nmoves++;
					}
				}
			}
			if(nmoves > 0)
				return nmoves*nextLargestShape.size();
		}
		return 0;
	}
}
