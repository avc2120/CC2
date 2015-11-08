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
	Iterator<Move> iterStackingMoves = null;

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

				pairs.put(0, Arrays.asList(0, 1));
				pairs.put(1, Arrays.asList(0, 1, 2));
				pairs.put(2, Arrays.asList(1, 2));
				pairs.put(3, Arrays.asList(1));

			}

			else {
				System.out.println("11 cutter: " + opponent_shapes[0].toString());
				pairs = create5Shape(opponent_shapes[0]);
			}
			
		}
		else {
			if (length == 5) {
				pairs.put(0, Arrays.asList(0, 1));
				pairs.put(1, Arrays.asList(1));
				pairs.put(2, Arrays.asList(0, 1));
				// pairs.put(0, Arrays.asList(0, 1, 2));
				// pairs.put(1, Arrays.asList(2, 3));

			}
			else if (length == 8) {
				pairs.put(0, Arrays.asList(0, 1, 2));
				pairs.put(1, Arrays.asList(1, 2));
				pairs.put(2, Arrays.asList(0, 1));
				pairs.put(3, Arrays.asList(1));
			}
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

		return new Shape(cutter);
	}


	public Move cut(Dough dough, Shape[] shapes, Shape[] opponent_shapes)
	{
		// prune larger shapes if initial move
		if (dough.uncut()) 
		{
			dough_cache = new int[dough.side()][dough.side()];
			int min = Integer.MAX_VALUE;
			for (Shape s : shapes)
			{
				if (min > s.size())
				{
					min = s.size();
				}
			}
			for (int s = 0 ; s != shapes.length ; ++s)
			{
				if (shapes[s].size() != min)
				{
					shapes[s] = null;
				}
			}
		}

				//if other team starts, create dough after first move
		if(dough.countCut() == 5)
		{
			dough_cache = new int[dough.side()][dough.side()];
		}		

		// if (percentCut(dough, offset, dough.side()*dough.side()) > 0.5) {
		// 	System.out.println("BOARD 50% FULLLLL!!!!");
		// 	stackingMoves.clear();
		// }
			

		boolean found = false;
		List <Move> commonMoves = new ArrayList<Move>();

		Set<Point> opponent_move = getOpponentMove(dough);
		Set<Point> neighbors = new HashSet<Point>();

		if(!opponent_move.isEmpty())
		{
			neighbors = convexHull(opponent_move, dough);
		}

		List<Move> destructiveMoves = new ArrayList<Move>();

		

		while(!found) 
		{
					//iterate over moves in cutshapes
			while (iterStackingMoves != null && iterStackingMoves.hasNext()) 
			{

				System.out.println("starting while");

				run += 1;
				
				for (int i = 11; i >=5; i -= 3) {
					destructiveMoves.addAll(destructOpponent(neighbors, dough, i, shapes, destructiveMoves));
				}
				
				System.out.println("found destructive moves: " + destructiveMoves.size());

				commonMoves = getIntersection(destructiveMoves, stackingMoves);
				Iterator<Move> iterCommonMoves = commonMoves.iterator();

				System.out.println("Found common moves: " + commonMoves.size());

				Move nextMove = null;
				boolean valid = false;
				
				while (!valid) 
				{
					nextMove = null;
					// System.out.println("Still invalid");

					if (commonMoves.isEmpty()) 
					{
						// System.out.println("no common moves");

						if (iterStackingMoves.hasNext()) {
							nextMove = iterStackingMoves.next();
							iterStackingMoves.remove();
						}
					}
					else 
					{
						// System.out.println("selecting common move");
						if (iterCommonMoves.hasNext()) {
							nextMove = iterCommonMoves.next();
							Iterator<Move> tempIter = stackingMoves.iterator();

							while(tempIter.hasNext())
							{
								if (tempIter.next() == nextMove)
								{
									tempIter.remove();
								}
							}
						}

					}

					if (nextMove == null)
						break;

					// System.out.println("This move is @ point " + nextMove.point.toString());

					Shape[] rotations = shapes[nextMove.shape].rotations();

					if (dough.cuts(rotations[nextMove.rotation], nextMove.point)) {
						// System.out.println("This is still a valid move! @ point " + nextMove.point.toString());
						found = true;
						valid = true;
						if (run == 1) {
							// System.out.println("Clearing 5 shape moves");
							stackingMoves.clear();
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
				return nextMove;	


			}

			int shapeIndex = 11;
					//find next set of moves 
			while (stackingMoves.isEmpty() && shapeIndex >= 5) {

				// System.out.println("Finding next set of moves for shape " + shapeIndex);
				stackingMoves = cutShapes(dough, shapeIndex, shapes);
				iterStackingMoves = stackingMoves.iterator();


				if (stackingMoves.isEmpty()) {
					// System.out.println("Found no moves for shape " + shapeIndex);
				}
				else {
					// System.out.println("Found " + stackingMoves.size() + " moves for shape " + shapeIndex);
				}
				shapeIndex -=3;

			}
		}

		return null;

		
	}

	public List<Move> getIntersection(List<Move> list1, List<Move> list2) 
	{
		List<Move> temp = new ArrayList<Move>();
		for(Move m1: list1)
		{
			for(Move m2: list2)
			{
				if (m1.shape == m2.shape && m1.rotation == m2.rotation && m1.point.equals(m2.point))
				{
					System.out.println("SHAPES: " + m1.shape + " " + m2.shape);
					temp.add(m1);
				}
			}	
		}
		return temp;
	}

	private List<Move> destructOpponent(Set<Point> neighbors, Dough dough, int index, Shape[] shapes, List<Move> moves)
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
					{
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

		// if (index == 11 && run == 1) {
		// 	for (int i = 1; i < dough.side(); i += 5) {
		// 		oddRow = oddRow ^ 1;
		// 		oddStep = 0;
		// 		for (int j = oddRow; j < dough.side(); j += (4 + oddStep)) {
		// 			oddStep = oddStep ^ 1;
		// 			Point p = new Point(i, j);
		// 			for (int si = 0 ; si != shapes.length ; ++si) {

		// 				if (shapes[si] == null) continue;
		// 				if (shapes[si].size() != index) continue;

		// 				Shape[] rotations = shapes[si].rotations();
		// 				int ri;
		// 				if (oddStep == 1) 
		// 					ri = 2;
		// 				else
		// 					ri = 1;
		// 				Shape s = rotations[ri];
		// 				if (dough.cuts(s, p)) {
		// 					Move cur_Move = new Move(si, ri, p);
		// 					moves.add(cur_Move);
		// 					move_rotation.put(cur_Move, rotations[ri]);
		// 					move_point.put(cur_Move, p);

		// 				}
		// 			}
		// 		}
		// 	}


		// }

		// else {
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
								// System.out.println("Found a move");
								Move cur_Move = new Move(si, ri, p);
								moves.add(cur_Move);
								move_rotation.put(cur_Move, rotations[ri]);
								move_point.put(cur_Move, p);
							}
						}
					}
				}
			}
		// }
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

	private Set<Point> convexHull(Set<Point> opponent_moves, Dough dough) {
		
		Set<Point> result = new HashSet<Point>();
		int minLength = Integer.MAX_VALUE;
		int minWidth = Integer.MAX_VALUE;
		int maxLength = Integer.MIN_VALUE;
		int maxWidth = Integer.MIN_VALUE;
		
		for(Point p : opponent_moves)
		{
			minLength = Math.min(minLength, p.i);
			maxLength = Math.max(maxLength, p.i);
			minWidth = Math.min(minWidth, p.j);
			maxWidth = Math.max(maxWidth, p.j);
		}

		// minLength = minLength>1? minLength-1: minLength;
		// maxLength = maxLength==dough.side()? maxLength: maxLength+1;
		// minWidth = minWidth>1? minWidth-1: minWidth;
		// maxWidth = maxWidth==dough.side()? maxWidth: maxWidth+1;

		int largerSide = maxLength-minLength > maxWidth-minWidth? maxLength-minLength: maxWidth-minWidth;
		System.out.println("larger side: " + largerSide);
		int midWidth = (int)((maxWidth+ minWidth)/2);
		int midLength = (int)((maxLength+ minLength)/2);
		double offset = 0.2;
		if(maxLength-minLength > maxWidth-minWidth)
		{
			minWidth = (int)(midWidth - (offset*largerSide)) >0? (int)(midWidth - (offset*largerSide)): 0;
			maxWidth = (int)(midWidth + (offset*largerSide))<dough.side()? (int)(midWidth + (offset*largerSide)): dough.side();

			minLength = Math.min((int)(midLength - (offset*largerSide)), dough.side());
			minLength = Math.max(minLength, 0);

			maxLength = Math.min((int)(midLength + (offset*largerSide)), dough.side());
			maxLength = Math.max(maxLength, 0);
		}
		else if (maxLength-minLength < maxWidth-minWidth)
		{
			// minWidth = (int)(midWidth + (offset*largerSide)) > 0 ? (int)(midWidth + (offset*largerSide)): 0;
			// maxWidth = (int)(midWidth - (offset*largerSide)) < dough.side() ? (int)(midWidth - (offset*largerSide)): dough.side();
			minWidth = Math.min((int)(midWidth - (offset*largerSide)), dough.side());
			minWidth = Math.max(minWidth, 0);

			maxWidth = Math.min((int)(midWidth + (offset*largerSide)), dough.side());
			maxWidth = Math.max(maxWidth, 0);

			minLength = (int)(midLength - (offset*largerSide)) >0? (int)(midLength - (offset*largerSide)): 0;
			maxLength = (int)(midLength + (offset*largerSide))<dough.side()? (int)(midLength + (offset*largerSide)): dough.side();
		}


		for(int row = minLength; row <= maxLength; row++)
		{
			for(int col = minWidth; col <= maxWidth; col++)
			{
				System.out.print(0);
				result.add(new Point(row, col));
			}
			System.out.println();
		}
		System.out.println((maxLength-minLength) + " by " + (maxWidth-minWidth));
		return result;
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

	private Map<Integer, List<Integer>> create5Shape(Shape opponent_shape) {
		
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

