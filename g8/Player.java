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
	private HashMap<Move, Shape> move_rotation = new HashMap<Move, Shape>();
	private HashMap<Move, Point> move_point = new HashMap<Move, Point>();
	List<Move> stackingMoves = new ArrayList<Move>();
	int run = 0;
	private int[] count =  new int[3];
	Iterator<Move> iterStackingMoves = null;
	private Shape curShape;
	private List<Shape> used_shapes = new ArrayList<Shape>(); 

public Shape cutter(int length, Shape[] shapes, Shape[] opponent_shapes)
	{
		Point[] cutter = new Point [length];
		ShapeGen sg = new ShapeGen();


		// first time picking shape
		if (row_2.length != cutter.length - 1) {
			row_2 = new boolean [cutter.length - 1];
			if (length == 11) {
				curShape = sg.elevenShape(length, count[0]);
				count[0]++;
			}
			else if (length == 8) {
				curShape = sg.createDynamicShape(opponent_shapes[0], length);
				count[1]++;
				used_shapes.add(curShape);
			}
			else {
				curShape = sg.createDynamicShape(opponent_shapes[0], length);
				count[2]++;
				used_shapes.add(curShape);
			}
		}
		// backup shapes
		else {
			if (length == 11) {
				curShape = sg.elevenShape(length, count[0]);

				count[0]++;
			}
			else {
				curShape = sg.changeShape(curShape, used_shapes);
			}	
			used_shapes.add(curShape);
		}
		return curShape;
	}


	public Move cut(Dough dough, Shape[] shapes, Shape[] opponent_shapes)
	{
		// prune larger shapes if initial move
		if (dough.uncut()) {
			dough_cache = new int[dough.side()][dough.side()];
			int min = Integer.MAX_VALUE;
			for (Shape s : shapes) {
				if (min > s.size()) {
					min = s.size();
				}
			}
			for (int s = 0 ; s != shapes.length ; ++s) {
				if (shapes[s].size() != min) {
					shapes[s] = null;
				}
			}
		}

		//if other team starts, create dough after first move
		if(dough.countCut() == 5) {
			dough_cache = new int[dough.side()][dough.side()];
		}		

		List <Move> commonMoves = new ArrayList<Move>();
		Set<Point> opponent_move = getOpponentMove(dough);
		Set<Point> neighbors = new HashSet<Point>();
		List<Move> destructiveMoves = new ArrayList<Move>();
		int i = 0;
		Move nextMove = null;
		boolean found = false;

		for (i = 11; i >=5; i -= 3) {
			for (double offset = 0.4; offset >= -0.2; offset -= 0.1) {
				if(!opponent_move.isEmpty()) {
					neighbors = convexHull(opponent_move, dough, offset);
				}

				destructiveMoves = destructOpponent(neighbors, dough, i, shapes, destructiveMoves);
				if (!destructiveMoves.isEmpty()) {
					found = true;
					break;
				}
			}

			

			if (found) {
				System.out.println("Found " + destructiveMoves.size() + " destructive moves for shape: " + i );
				System.out.println("Picking the first one...");
				// if (percentCut(dough) > 0)
					nextMove = getBestMove(destructiveMoves, dough, shapes, opponent_shapes);
				// else
				// 	nextMove = destructiveMoves.get(0);
				break;
			}
			else {
				System.out.println("Couldn't find a destructive move!");
				List<Move> moreMoves = new ArrayList<Move>();
				moreMoves = cutShapes(dough, i, shapes);

				if (!moreMoves.isEmpty()) {
					// if (percentCut(dough) > 0)
						nextMove = getBestMove(moreMoves, dough, shapes, opponent_shapes);
					// else
					// 	nextMove = moreMoves.get(0);
					break;
				}
			}
		}			
		
		Move myMove = nextMove;
		Shape myShape = move_rotation.get(myMove);
		Point q = move_point.get(myMove);
		Iterator<Point> pts = myShape.iterator();
		while(pts.hasNext())
		{
			Point p = pts.next();
			dough_cache[p.i + q.i][p.j + q.j] = 1;
		}
		run++;
		return myMove;
	}

	private Move getBestMove(List<Move> candidateMoves, Dough dough, Shape[] shapes, Shape[] opponent_shapes) {

		if (run == 0) {
			return candidateMoves.get(0);
		}
		List<Move> copyOfCandidateMoves = new ArrayList<Move>();
		for (Move move: candidateMoves) {
			copyOfCandidateMoves.add(move);
		}

		int maxSoFar = Integer.MIN_VALUE;
		Move bestMove = null;
		// System.out.println("Number of moves to check: " + candidateMoves.size());
		int i = 0;
		int repeat = 0;
		int prevDiff = Integer.MIN_VALUE;
		List<Move> oppMovesHere = new ArrayList<Move>();
		Iterator<Move> iter = candidateMoves.iterator();

		System.out.println("Found " + candidateMoves.size() + " moves to choose from");
		while(iter.hasNext()) {

			Move move = iter.next();

			Point thisPoint = move.point;
			int shapeIndex = 11;
			while (shapeIndex >= 8) {
				oppMovesHere.addAll(cutShapes(dough, shapeIndex, opponent_shapes));
				shapeIndex -= 3;
			}
			if (oppMovesHere.isEmpty()) {
				iter.remove();
				System.out.println("removing this move. now left with " + candidateMoves.size());
				continue;
			}

			int oppScore = computeMoveScore(dough, opponent_shapes, move);
			int myScore = computeMoveScore(dough, shapes, move);
			
			System.out.println(++i + " My score: " + myScore + " and opponent's score: " + oppScore);
			
			int diff = myScore - oppScore;
			if (diff == prevDiff) {
				repeat++;

				if (repeat > 10) {
					System.out.println("Found the same difference over a 100 times! Chuck it");
					break;
				}
			}
			else {
				repeat = 0;

				if (diff > maxSoFar) {
					maxSoFar = myScore - oppScore;
					bestMove = move;
				}
			}
			prevDiff = diff;
			if (i == Math.min(100, candidateMoves.size()/10))
				break;
		}
		System.out.println("Max difference seen: " + maxSoFar);
		if (bestMove == null)
			return copyOfCandidateMoves.get(0);
		return bestMove;
	}

	private int computeMoveScore(Dough dough, Shape[] shapes, Move thisMove) {
		Dough dummyDough = new DummyDough(dough.side());
		boolean[][] dummyDoughState = ((DummyDough)dummyDough).getDough();

		for (int i = 0; i < dough.side(); i++) {
			for (int j = 0; j < dough.side(); j++) {
				if (thisMove.point.i == i && thisMove.point.j == j)
					dummyDoughState[i][j] = true;
				else 
					dummyDoughState[i][j] = (dough_cache[i][j] == 1) ? true : false;
			}
		}


		int[] values = new int[1];
		int shapeIndex = 11;
		// int shapeIndex = shapes[thisMove.shape].size();
		for (int i = 0; i < values.length; i++) {
			values[i] = cutShapes(dummyDough, shapeIndex, shapes).size();
			shapeIndex -= 3;
		}

		int[] weights = {11, 8, 5};
		// int[] weights = {shapeIndex};
		int score = 0;
		for (int i = 0; i < values.length; i++) {
			score += values[i] * weights[i];
		}

		return score;

	}

	private List<Move> destructOpponent(Set<Point> neighbors, Dough dough, int index, Shape[] shapes, List<Move> moves) {	
		for (Point p: neighbors) {
			for (int si = 0 ; si != shapes.length ; ++si) {
				if (shapes[si] == null) continue;
				if (shapes[si].size() != index) continue;
				Shape[] rotations = shapes[si].rotations();
				for (int ri = 0 ; ri != rotations.length ; ++ri)  {
					Shape s = rotations[ri];
					if (dough.cuts(s, p)) {
						Move cur_Move = new Move(si, ri, p);
						moves.add(cur_Move);
						move_rotation.put(cur_Move, rotations[ri]);
						move_point.put(cur_Move, p);
					}
				}
			}
		}
		return moves;
	}

	private List<Move> cutShapes(Dough dough, int index, Shape[] shapes) {

		int oddRow = 0;
		int oddStep = 0;
		List <Move> moves = new ArrayList <Move> ();
		for (int i = 0; i < dough.side(); i++) {
			for (int j = 0; j < dough.side(); j++) {
				Point p = new Point(i, j);
				for (int si = 0 ; si != shapes.length ; ++si) {
					if (shapes[si] == null) continue;
					if (shapes[si].size() != index) continue;
					Shape[] rotations = shapes[si].rotations();
					for (int ri = 0; ri < rotations.length; ri++) {
						Shape s = rotations[ri];
						if (dough.cuts(s, p)) {
							Move cur_Move = new Move(si, ri, p);
							moves.add(cur_Move);
							move_rotation.put(cur_Move, rotations[ri]);
							move_point.put(cur_Move, p);
						}
					}
				}
			}
		}
		return moves;
	}

	private Set<Point> getOpponentMove(Dough dough) {
		Set<Point> opponent_moves = new HashSet<Point>();
		for(int i = 0; i < dough.side(); i++) {
			for(int j = 0; j < dough.side(); j++) {
				if(dough_cache[i][j] == 0 && !dough.uncut(i,j)) {
					dough_cache[i][j] = 1;
					opponent_moves.add(new Point(i,j));
				}
			}
		}
		return opponent_moves;
	}

	private Set<Point> getNeighbors(Set<Point> points) {
		Set<Point> neighbors = new HashSet<Point>();
		for(Point point: points) {
			neighbors.addAll(new HashSet<Point>(Arrays.asList(point.neighbors())));
		}
		return neighbors;
	}

	private Set<Point> convexHull(Set<Point> opponent_moves, Dough dough, double offset) {
		
		Set<Point> result = new HashSet<Point>();
		int minLength = Integer.MAX_VALUE;
		int minWidth = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		int maxWidth = Integer.MIN_VALUE;
		
		for(Point p : opponent_moves) {
			minLength = Math.min(minLength, p.i);
			maxLength = Math.max(maxLength, p.i);
			minWidth = Math.min(minWidth, p.j);
			maxWidth = Math.max(maxWidth, p.j);
		}

		int side1 = maxLength - minLength + 1;
		int side2 = maxWidth - minWidth + 1;
		int largerSide = Math.max(side1, side2);
		int smallerSide = Math.min(side1, side2);

		System.out.println("convex hull is: " + side1 + " by " + side2);
		System.out.println("Corners: (" + minLength + ", " + minWidth + ") and (" + maxLength + ", " + maxWidth + ")");

		int midWidth = (int)((maxWidth + minWidth)/2);
		int midLength = (int)((maxLength+ minLength)/2);
		System.out.println("midwidth: " + midWidth);
		System.out.println("midLength: " + midLength);

		if (largerSide == side1) {
			minWidth = midWidth - (int) ((largerSide + 1)/2);
			maxWidth = midWidth + largerSide + 1 - (int) ((largerSide + 1)/2);
			minLength--;
			maxLength++;
		}
		else {
			minLength = midLength - (int) ((largerSide + 1)/2);
			maxLength = midLength + largerSide + 1 - (int) ((largerSide + 1)/2);
			minWidth--;
			maxWidth++;
		}

		System.out.println("convex hull now is: " + (maxLength - minLength + 1) + " by " + (maxWidth - minWidth + 1));
		System.out.println("Corners: (" + minLength + ", " + minWidth + ") and (" + maxLength + ", " + maxWidth + ")");

		side1 = maxLength - minLength + 1;
		side2 = maxWidth - minWidth + 1;

		minWidth = minWidth + (int) (offset * side2);
		maxWidth = maxWidth - (int) (offset * side2);

		minLength = minLength + (int) (offset * side1);
		maxLength = maxLength - (int) (offset * side1);

		minLength = Math.max(Math.min(minLength, dough.side()), 0);
		maxLength = Math.max(Math.min(maxLength, dough.side()), 0);
		minWidth = Math.max(Math.min(minWidth, dough.side()), 0);
		maxWidth = Math.max(Math.min(maxWidth, dough.side()), 0);

		System.out.println("rectified convex hull is: " + (maxLength - minLength + 1) + " by " + (maxWidth - minWidth + 1));
		System.out.println("Corners: (" + minLength + ", " + minWidth + ") and (" + maxLength + ", " + maxWidth + ")");

		for(int row = minLength; row <= maxLength; row++) {
			for(int col = minWidth; col <= maxWidth; col++) {
				result.add(new Point(row, col));
			}
		}
		return result;
	}

	// returns Map<Integer, List<Integer>> fitting convex hull
	private Map<Integer, List<Integer>> createDynamicShape(Shape opponent_shape, int n) {

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

		boolean[][] newblock = new boolean[maxLength + 4][maxWidth + 4];
		for (int i = 0; i < block.length; i++) {
			for (int j = 0; j < block[i].length; j++) {
				newblock[i + 2][j + 2] = block[i][j];
			}
		}
		
		for (int i = 0; i < newblock.length; i++) {
			for (int j = 0; j < newblock[i].length; j++) {
				System.out.print(newblock[i][j] + " ");
			}
			System.out.println();
		}

		Set<Point> points = new HashSet<Point>();
		System.out.println("maxwidth, maxlength: " + maxWidth + ", " + maxLength);
		if (maxWidth == 0 || maxLength == 0) {
			if (n == 5) {
				points.add(new Point(0, 0));
				points.add(new Point(0, 1));
				points.add(new Point(0, 2));
				points.add(new Point(0, 3));
				points.add(new Point(0, 4));
			}
			else if (n == 8) {
				points.add(new Point(0, 0));
				points.add(new Point(0, 1));
				points.add(new Point(0, 2));
				points.add(new Point(0, 3));
				points.add(new Point(0, 4));
				points.add(new Point(0, 5));
				points.add(new Point(0, 6));
				points.add(new Point(0, 7));
				
			}
			
		}
		else {
			points = createShape(newblock, n, points, 2);
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

						if (points.size() == 0) {
							Point p = new Point(i, j);
							cutout[i][j] = true;
							points.add(p);
						}
						else if (points.size() != 0 && checkAllAdjacent(points, i, j)){
							if (checkAdjacent(points, i, j)) {
								Point p = new Point(i, j);
								cutout[i][j] = true;
								points.add(p);
							}
							else if (points.size() + 2 <= n){
								Point p = new Point(i, j);
								cutout[i][j] = true;
								points.add(p);
								
								Point new_p = returnAdjPoint(cutout, points, i, j);
								cutout[new_p.i][new_p.j] = true;
								points.add(new_p);
							}
						}
						if (points.size() >= n) {
							return points;
						}
					}
				}
			}
		}
		return createShape(cutout, n, points, edges - 1);
	}

	private boolean checkAllAdjacent(Set<Point> point, int i, int j) {
		for (Point p : point) {
			if ((p.i == i - 1 && p.j == j) || (p.i == i + 1 && p.j == j) || (p.i == i && p.j == j - 1) || (p.i == i && p.j == j + 1)
					|| (p.i == i - 1 && p.j == j - 1) || (p.i == i + 1 && p.j == j - 1) || (p.i == i - 1 && p.j == j + 1) || (p.i == i + 1 && p.j == j + 1)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkAdjacent(Set<Point> point, int i, int j) {
		for (Point p : point) {
			if ((p.i == i - 1 && p.j == j) || (p.i == i + 1 && p.j == j) || (p.i == i && p.j == j - 1) || (p.i == i && p.j == j + 1)) {
				return true;
			}
		}
		return false;
	}


	private Point returnAdjPoint(boolean[][] cutout, Set<Point> point, int i, int j) {
		for (Point p : point) {
			if ((p.i - 1 == i - 1 || p.i - 1 == i || p.i - 1 == i + 1) && (p.j == j - 1 || p.j == j || p.j == j + 1)) {
				if (p.i - 1 >= 0 && !cutout[p.i - 1][p.j]) {
					return new Point(p.i - 1, p.j);
				}
			}
			if ((p.i + 1 == i - 1 || p.i + 1 == i || p.i + 1 == i + 1) && (p.j == j - 1 || p.j == j || p.j == j + 1)) {
				if (p.i + 1 <= i && !cutout[p.i + 1][p.j])
					return new Point(p.i + 1, p.j);
			}
			if ((p.i == i - 1 || p.i == i || p.i == i + 1) && (p.j - 1 == j - 1 || p.j - 1 == j || p.j - 1 == j + 1)) {
				if (p.j - 1 >= 0 && !cutout[p.i][p.j - 1])
					return new Point(p.i, p.j - 1);
			}
			if ((p.i == i - 1 || p.i == i || p.i == i + 1) && (p.j + 1 == j - 1 || p.j + 1 == j || p.j + 1 == j + 1))
				if (p.j + 1 <= j && !cutout[p.i][p.j + 1])
					return new Point(p.i, p.j + 1);
		}
		return new Point(i, j);
	}

	private double percentCut(Dough dough) {
		int area = (dough.side() - 2*offset)*(dough.side()-2*offset);
		int num_cuts = 0;
		for (int i = offset ; i != dough.side()- offset ; ++i) {
			for (int j = offset; j != dough.side()-offset; ++j) {
				if (dough.uncut(i,j) == false) {
					num_cuts++;
				}
			}
		}
		return num_cuts/((double)area);
	}

}



