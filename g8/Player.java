package cc2.g8;

import cc2.sim.Point;
import cc2.sim.Shape;
import cc2.sim.Dough;
import cc2.sim.Move;

import java.util.*;

public class Player implements cc2.sim.Player {

	private boolean firstCheck = true;

	private static int offset = 0;
	private boolean[] row_2 = new boolean [0];
	private int[] count =  new int[3];
	private List<Shape> used_shapes = new ArrayList<Shape>(); 
	private Random gen = new Random();
	private static int[][] dough_cache;
	private HashMap<Move, Shape> move_rotation = new HashMap<Move, Shape>();
	private HashMap<Move, Point> move_point = new HashMap<Move, Point>();
	List<Move> stackingMoves = new ArrayList<Move>();
	int run = 0;
	Iterator<Move> iterStackingMoves = null;
	Shape curShape;

	public Shape cutter(int length, Shape[] shapes, Shape[] opponent_shapes) {


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
				nextMove = destructiveMoves.get(0);
				// nextMove = getBestMove(destructiveMoves, dough, shapes, opponent_shapes);
				break;
			}
			else {
				System.out.println("Couldn't find a destructive move!");
				List<Move> moreMoves = new ArrayList<Move>();
				moreMoves = cutShapes(dough, i, shapes);
				if (!moreMoves.isEmpty()) {
					nextMove = moreMoves.get(0);
					// nextMove = getBestMove(moreMoves, dough, shapes, opponent_shapes);
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
		return myMove;
	}

	// private Move<move> getBestMove(List<Move> candidateMoves, Dough dough, Shape[] shapes, Shape[] opponent_shapes) {

	// 	int maxSoFar = Math.MIN_VALUE;
	// 	Move bestMove = null;
	// 	for (Move move: candidateMoves) {
	// 		int oppScore = computeMoveScore(opponent_shapes, move);
	// 		int myScore = computeMoveScore(shapes, move);
	// 		if (myScore - oppScore > maxSoFar) {
	// 			maxSoFar = myScore - oppScore;
	// 			bestMove = move;
	// 		}
	// 	}
	// 	return bestMove;
	// }

	// private int computeMoveScore(Dough dough, Shape[] shapes, Move thisMove) {
	// 	Dough dummyDough = new int[dough.side()][dough.side()];

	// 	for (int i = 0; i < dough.side(); i++) {
	// 		for (int j = 0; j < dough.side(); j++) {
	// 			if (thisMove.point.i == i && thisMove.point.j == j)
	// 				dummyDough[i][j] = 1;
	// 			else 
	// 				dummyDough[i][j] = dough_cache[i][j];
	// 		}
	// 	}
	// 	int[] values = new int[3];
	// 	int shapeIndex = 11;
	// 	for (int i = 0; i < 3; i++) {
	// 		values[i] = cutShapes(dummyDough, shapeIndex, opponent_shapes);
	// 		shapeIndex -= 3;
	// 	}

	// 	int[] weights = {11, 8, 5};
	// 	int score = 0;
	// 	for (int i = 0; i < values.size(); i++) {
	// 		score += values[i] * weights[i];
	// 	}
	// 	return score;

	// }

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

}

