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
	private static int[][] dough_cache;
	private boolean check = false;

	public Shape cutter(int length, Shape[] shapes, Shape[] opponent_shapes)
	{
		// check if first try of given cutter length
		Point[] cutter = new Point [length];
		Map<Integer, List<Integer>> pairs = new HashMap<Integer, List<Integer>>();
		
		if (row_2.length != cutter.length - 1) {
			// save cutter length to check for retries
			row_2 = new boolean [cutter.length - 1];

			if (length == 11) {
				pairs.put(0, Arrays.asList(0,3));
				pairs.put(1, Arrays.asList(0, 1, 2, 3, 4));
				pairs.put(2, Arrays.asList(1, 3));
				pairs.put(3, Arrays.asList(3, 4));	
			}

			else if (length == 8) {
				pairs.put(0, Arrays.asList(1, 2));
				pairs.put(1, Arrays.asList(2, 3, 4));
				pairs.put(2, Arrays.asList(0, 1, 2));
			}

			else {
				System.out.println("11 cutter: " + opponent_shapes[0].toString());
				pairs = convexHull(opponent_shapes[0]);

			}

			int i = 0;
			while (i < cutter.length) {
				for (Map.Entry<Integer, List<Integer>> pair: pairs.entrySet()) {
					for (int j : pair.getValue()) {
						cutter[i] = new Point(pair.getKey(), j);
						i++;
					}
				}
			}
		}
		else {
			int i;
			if (length == 5 && !check) {
				pairs.put(0, Arrays.asList(0, 1));
				pairs.put(1, Arrays.asList(1));
				pairs.put(2, Arrays.asList(0, 1));
				check = true;
				int a = 0;
				while (a < cutter.length) {
					for (Map.Entry<Integer, List<Integer>> pair: pairs.entrySet()) {
						for (int j : pair.getValue()) {
							cutter[a] = new Point(pair.getKey(), j);
							a++;
						}
					}
				}
			}
			else {
				do {
					i = gen.nextInt(cutter.length - 1);
				} while (row_2[i]);
				row_2[i] = true;
				cutter[cutter.length - 1] = new Point(i, 1);
				for (i = 0 ; i != cutter.length - 1 ; ++i)
					cutter[i] = new Point(i, 0);
			}
			
		}
		return new Shape(cutter);
	}

	public Move cut(Dough dough, Shape[] shapes, Shape[] opponent_shapes)
	{
		// prune larger shapes if initial move
		if (dough.uncut()) {
			dough_cache = new int[dough.side()][dough.side()];
			int min = Integer.MAX_VALUE;
			for (Shape s : shapes)

				if (min > s.size())
					min = s.size();
			for (int s = 0 ; s != shapes.length ; ++s)
				if (shapes[s].size() != min)
					shapes[s] = null;
		}
		if(dough.countCut() ==5)
		{
			dough_cache = new int[dough.side()][dough.side()];
		}

		// find all valid cuts
		ArrayList <Move> moves = new ArrayList<Move>();
		Set<Point> opponent_move = getOpponentMove(dough);
		Set<Point> neighbors = new HashSet<Point>();
		if(!opponent_move.isEmpty())
			neighbors = getNeighbors(opponent_move);

		if(moves.isEmpty())
		{
			neighbors = getNeighbors(opponent_move);
			moves = destructOpponent(neighbors, dough, 11, shapes, moves);
			while(moves.isEmpty() &&  neighbors.size() != 0 && neighbors.size() < dough.side()*dough.side())
			{
				neighbors.addAll(getNeighbors(neighbors));
				moves = destructOpponent(neighbors, dough, 11, shapes, moves);
			}
		}
		if(moves.isEmpty())
		{
			moves = cutShapes(dough, 11, shapes, moves);
		}

		if(moves.isEmpty())
		{
			neighbors = getNeighbors(opponent_move);
			moves = destructOpponent(neighbors, dough, 8, shapes, moves);
			while(moves.isEmpty() && neighbors.size() != 0 && neighbors.size() < dough.side()*dough.side())
			{
				neighbors.addAll(getNeighbors(neighbors));
				moves = destructOpponent(neighbors, dough, 8, shapes, moves);
			}
		}
		if(moves.isEmpty())
		{
			moves = cutShapes(dough, 8, shapes, moves);
		}

		if(moves.isEmpty())
		{
			neighbors = getNeighbors(opponent_move);
			moves = destructOpponent(neighbors, dough, 5, shapes, moves);
			while(moves.isEmpty() && neighbors.size()!= 0 && neighbors.size() < dough.side()*dough.side())
			{
				neighbors.addAll(getNeighbors(neighbors));
				moves = destructOpponent(neighbors, dough, 5, shapes, moves);
			}
		}
		if(moves.isEmpty())
		{
			moves = cutShapes(dough, 5, shapes, moves);
		}


		Move myMove = moves.get(gen.nextInt(moves.size()));
		Shape myShape = shapes[myMove.shape];
		Iterator <Point> myPoints = myShape.iterator();
		while(myPoints.hasNext())
		{
			Point p = myPoints.next();
			dough_cache[p.i][p.j] = 1;
		}
		return myMove;
	}
	
	private ArrayList<Move> destructOpponent(Set<Point> neighbors, Dough dough, int index, Shape[] shapes, ArrayList<Move> moves)
	{
		for (Point p: neighbors)
		{
			for (int si = 0 ; si != shapes.length ; ++si) 
			{
				if (shapes[si] == null) continue;
				if (shapes[si].size() != index) continue;
				Shape[] rotations = shapes[si].rotations();
				for (int ri = 0 ; ri != rotations.length ; ++ri) 
				{
					Shape s = rotations[ri];
					if (dough.cuts(s, p))
						moves.add(new Move(si, ri, p));
				}
			}
		}
		return moves;
	}

	private ArrayList<Move> cutShapes(Dough dough, int index, Shape[] shapes, ArrayList<Move> moves) {
		for (int i = offset ; i != dough.side() - offset ; ++i) {
			for (int j = offset; j != dough.side() - offset; ++j) {
				Point p = new Point(i, j);
				for (int si = 0 ; si != shapes.length ; ++si) {
					if (shapes[si] == null) continue;
					if (shapes[si].size() != index) continue;
					Shape[] rotations = shapes[si].rotations();
					for (int ri = 0 ; ri != rotations.length ; ++ri) {
						Shape s = rotations[ri];
						if (dough.cuts(s, p))
							moves.add(new Move(si, ri, p));
					}
				}
			}
		}
		return moves;
	}

	private Set<Point> getOpponentMove(Dough dough)
	{
		Set<Point> opponent_moves = new HashSet<Point>();
		for(int i = 0; i < dough.side(); i++)
		{
			for(int j = 0; j < dough.side(); j++)
			{
				if(dough_cache[i][j] == 0 && !dough.uncut(i,j))
				{
					dough_cache[i][j] = 1;
					opponent_moves.add(new Point(i,j));
				}
			}
		}
		return opponent_moves;
	}

	private Set<Point> getNeighbors(Set<Point> points)
	{
		Set<Point> neighbors = new HashSet<Point>();
		for(Point point: points)
		{
			neighbors.addAll(new HashSet<Point>(Arrays.asList(point.neighbors())));
		}
		return neighbors;
	}

	private void printDough()
	{
		for(int i = 0; i < dough_cache.length; i++)
		{
			for(int j = 0; j < dough_cache.length; j++)
			{
				System.out.print(dough_cache[i][j]);
			}
			System.out.print("\n");
		}
	}

	// returns Map<Integer, List<Integer>> fitting convex hull
	private Map<Integer, List<Integer>> convexHull(Shape opponent_shape) {
		
		Iterator <Point> it = opponent_shape.iterator();
		int minLength = Integer.MAX_VALUE;
		int minWidth = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		int maxWidth = Integer.MIN_VALUE;
		
		while (it.hasNext()) {
			Point p = it.next();
			minLength = Math.min(minLength, p.i);
			maxLength = Math.max(maxLength, p.i);
			minWidth = Math.min(minWidth, p.j);
			maxWidth = Math.max(maxWidth, p.j);
		}
		
		boolean[][] block = new boolean[maxLength + 1][maxWidth + 1];
		Iterator <Point> it2 = opponent_shape.iterator();
		
		while (it2.hasNext()) {
			Point p = it2.next();
			block[p.i][p.j] = true;
		}
		
		for (int i = 0; i < block.length; i++) {
			for (int j = 0; j < block[i].length; j++) {
				System.out.print(block[i][j] + " ");
			}
			System.out.println();
		}
		
		Set<Point> points = new HashSet<Point>();
		System.out.println("maxwidth, maxlength: " + maxWidth + ", " + maxLength);
		if (maxWidth == 0 || maxLength == 0) {
			points.add(new Point(0, 0));
			points.add(new Point(0, 1));
			points.add(new Point(0, 2));
			points.add(new Point(0, 3));
			points.add(new Point(0, 4));
		}
		else {
			points = createShape(block, 5, points, 2);
		}
		
		
		Map<Integer, List<Integer>> rMap = new HashMap<Integer, List<Integer>>();
		for (Point p : points) {
			if (!rMap.containsKey(p.i)) {
				List<Integer> list = new ArrayList<Integer>();
				list.add(p.j);
				rMap.put(p.i, list);
			}
			else {
				rMap.get(p.i).add(p.j);
			}
		}
		return rMap;
		
	}
	
	// find a shape from opponent's shape
	private Set<Point> createShape(boolean[][] cutout, int n, Set<Point> points, int edges) {
		for (int i = 0; i < cutout.length; i++) {
			for (int j = 0; j < cutout.length; j++) {
				int count = 0;
				if (cutout[i][j] == false) {
					if (i > 0 && cutout[i - 1][j]) {
						count++;
					}
					if (i < cutout.length - 1 && cutout[i + 1][j]) {
						count++;
					}
					if (j > 0 && cutout[i][j - 1]) {
						count++;
					}
					if (j < cutout[j].length - 1 && cutout[i][j + 1]) {
						count++;
					}
					if (count >= edges) {
						if (points.size() == 0 || (points.size() != 0 && checkAdjacent(points, i, j))) {
							Point p = new Point(i, j);
							cutout[i][j] = true;
							points.add(p);
							if (points.size() >= n) {
								return points;
							}
						}
						
					}
				}
				
			}
		}
		System.out.println("current points: " + points);
		
		return createShape(cutout, n, points, edges - 1);
	}
	
	private boolean checkAdjacent(Set<Point> point, int i, int j) {
		for (Point p : point) {
			System.out.println("p.i, p.j: " + p.i + p.j);
			if ((p.i == i - 1 && p.j == j) || (p.i == i + 1 && p.j == j) || (p.i == i && p.j == j - 1) || (p.i == i && p.j == j + 1)) {
				return true;
			}
		}
		return false;
	}
	
}
