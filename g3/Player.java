package cc2.g3;

import cc2.sim.Point;
import cc2.sim.Shape;
import cc2.sim.Dough;
import cc2.sim.Move;
import cc2.g3.rectDough;

import java.util.*;

public class Player implements cc2.sim.Player {

    private static final int SIDE = 50;

    private boolean[] row_2 = new boolean [0];

    private Random gen = new Random();

    private Dough opponent, self;

    private boolean denied;

    public Player() {
	opponent = new Dough(SIDE);
	self = new Dough(SIDE);
	denied = false;
    }    

    public Shape cutter(int length, Shape[] shapes, Shape[] opponent_shapes)
    {
	if (opponent_shapes.length == 0 || denied == true) {return randLinearCutter(length, shapes, opponent_shapes);}
	Point dimensions = getBoundingBox(opponent_shapes[0]);
	rectDough space = new rectDough(dimensions);
	space.cut(opponent_shapes[0], new Point(0,0));
	Stack<Point> conn_comp = new Stack<Point>();

	while (!space.saturated()) { 
	    Point init = findAvailablePoint(space);
	    conn_comp.push(init);
	    ArrayList<Point> points = new ArrayList<Point>(); 
	    points.add(init);
	    space.cut(init);
	    while (!conn_comp.isEmpty()) { 
		Point next = conn_comp.pop();
		Point[] neighbors = next.neighbors();
		for (int i=0; i<neighbors.length; i++) {
		    if (space.uncut(neighbors[i])) {
			conn_comp.push(neighbors[i]);
			space.cut(neighbors[i]);
			points.add(neighbors[i]);
		    }
		}
	    }
	    if (points.size() == length) { 
		System.out.println("Denied!");
		denied = true;
		Point[] cutter = new Point[points.size()];
		return new Shape(points.toArray(cutter));
	    }
	}
	return randLinearCutter(length, shapes, opponent_shapes);
    }

    private Point findAvailablePoint(rectDough space) {
	int h = space.height;
	int w = space.width;
	for (int i=0; i<h; i++) {
	    for (int j=0; j<w; j++) {
		if (space.uncut(i,j)) {return new Point(i,j);}
	    }
	}
	System.out.println("No available points");
	return null;
    }

    public Shape randLinearCutter(int length, Shape[] shapes, Shape[] opponent_shapes) {
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
	    int n = cutter.length-1;
	    do {
		i = gen.nextInt(n*10000); //bias towards endpoints
		if (i < 4999*n) {i = 0;}
		else if (i >= 5000*n) {i = n-1;}
		else {i = i - 4999*n;}
	    } while (row_2[i]);
	    row_2[i] = true;
	    cutter[cutter.length - 1] = new Point(i, 1);
	    for (i = 0 ; i != cutter.length - 1 ; ++i)
		cutter[i] = new Point(i, 0);
	}
	return new Shape(cutter);
    }

    private int getMinWidth(Shape cutter) {
	Point b = getBoundingBox(cutter);
	return Math.min( b.i,b.j );
    }

    private Point getBoundingBox(Shape cutter) {
	int minI = Integer.MAX_VALUE;
	int minJ = Integer.MAX_VALUE;
	int maxI = Integer.MIN_VALUE;
	int maxJ = Integer.MIN_VALUE;
	Iterator<Point> pointsInShape = cutter.iterator();
	while (pointsInShape.hasNext()) {
	    Point p = pointsInShape.next();
	    minI = Math.min(minI, p.i);
	    maxI = Math.max(maxI, p.i);
	    minJ = Math.min(minJ, p.j);
	    maxJ = Math.max(maxJ, p.j);
	}
	return new Point(maxI - minI + 1, maxJ - minJ + 1);
    }

    // function that will be called multiple times in real_cut with different parameters. set searchDough to opponent for behavior from last submission
    public Move find_cut(Dough dough, Dough searchDough, Shape[] shapes, Shape[] opponent_shapes, int maxCutterIndex) { 
	ArrayList <ComparableMove> moves = new ArrayList <ComparableMove> ();
	for (int i = 0 ; i != searchDough.side() ; ++i)
	    for (int j = 0 ; j != searchDough.side() ; ++j) {
		Point p = new Point(i, j);
		for (int si = 0 ; si <= maxCutterIndex ; ++si) {
		    if (shapes[si] == null) continue;
		    Shape[] rotations = shapes[si].rotations();
		    for (int ri = 0 ; ri != rotations.length ; ++ri) {
			Shape s = rotations[ri];
			if (dough.cuts(s,p) && searchDough.cuts(s,p)) {
			    moves.add(new ComparableMove(new Move(si, ri, p), touched_edges(s,p,searchDough), s.size()));
			}
		    }
		}
	    }
	if (moves.size() >= 1) {
	    Collections.sort(moves);
	    //System.out.println(moves.get(moves.size() - 1).key);
	    return moves.get(moves.size() - 1).move;
	}
	else {
	    return null;
	}
    }
    
    // computes the cut to be made
    public Move real_cut(Dough dough, Shape[] shapes, Shape[] opponent_shapes) {
	// prune larger shapes if initial move	
	if (dough.uncut()) {
	    int min = Integer.MAX_VALUE;
	    for (Shape s : shapes)
		if (min > s.size())
		    min = s.size();
	    for (int s = 0 ; s != shapes.length ; ++s)
		if (shapes[s].size() != min)
		    shapes[s] = null;
	}
	int minWidth = getMinWidth(opponent_shapes[0]);	
	Move A = find_cut(dough, createPaddedBoard(dough, minWidth-1, minWidth-1, minWidth-1), shapes, opponent_shapes, 0); // pad all directions with minwidth
	if (A != null) {
	    System.out.println("Move A");
	    return A;
	}
	else {
	    Move B = find_cut(dough, createPaddedBoard(dough, minWidth / 2 - 1, minWidth / 2 - 1, minWidth), shapes, opponent_shapes, 0); // pad all directions with minwidth / 2
	    if (B != null) {
		System.out.println("Move B");
		return B;
	    }
	    else {
		Dough Board1 = createPaddedBoard(dough, minWidth / 2 - 1, 1, minWidth); // pad only one direction by minwidth / 2
		Dough Board2 = createPaddedBoard(dough, 1, minWidth / 2 - 1, minWidth);
		Move C1 = find_cut(dough, Board1, shapes, opponent_shapes, 0);
		Move C2 = find_cut(dough, Board2, shapes, opponent_shapes, 0);
		if (C1 == null ^ C2 == null) {
		    if (C1 == null) {
			System.out.println("Move C");
			return C2;
		    }
		    else {
			System.out.println("Move C");
			return C1;
		    }
		}
		else if (C1 != null && C2 != null) { // choose best direction
		    System.out.println("Move C");
		    if (touched_edges( shapes[C1.shape].rotations()[C1.rotation], C1.point, Board1) > touched_edges( shapes[C2.shape].rotations()[C2.rotation], C2.point, Board2) ) {
			return C1;
		    }
		    else {
			return C2;
		    }
		}
		else { // default to behavior of last submission
		    System.out.println("Move F");
		    return find_cut(dough, opponent, shapes, opponent_shapes, 2);
		}
	    }
	}
    }

    private Dough createPaddedBoard(Dough dough, int verticalPadding, int horizontalPadding, int borderPadding) {
	Dough padded = new Dough(SIDE);
	for (int i=0; i<SIDE; i++) {
	    for (int j=0; j<SIDE; j++) {
		if (!dough.uncut(i,j)) {
		    cutPadding(padded, i,j,verticalPadding, horizontalPadding);
		}
	    }
	}
	cutBorder(padded, borderPadding-1);
	return padded;
    }
    
    private void cutPadding(Dough padded, int i, int j, int verticalPadding, int horizontalPadding) {
	for (int x = Math.max(0,i-horizontalPadding); x<=Math.min(SIDE-1,i+horizontalPadding); x++) {
	    for (int y=Math.max(0,j-verticalPadding); y<=Math.min(SIDE-1,j+verticalPadding); y++) {
		padded.cut(new Shape(new Point[] {new Point(0, 0)}), new Point(x, y));
	    }
	}
    }

    private void cutBorder(Dough padded, int w) {
	for (int i=0; i<SIDE; i++) {cutPadding(padded, i,0,w,w);}
	for (int i=0; i<SIDE; i++) {cutPadding(padded, i,SIDE-1,w,w);}
	for (int j=0; j<SIDE; j++) {cutPadding(padded, 0,j,w,w);}
	for (int j=0; j<SIDE; j++) {cutPadding(padded, SIDE-1,j,w,w);}
    }

    // function called by simulator
    public Move cut(Dough dough, Shape[] shapes, Shape[] opponent_shapes)
    {
	// Get cut done by opponent
	for (int i = 0; i < SIDE; i++) {
	    for (int j = 0; j < SIDE; j++) {
		if (!dough.uncut(i, j) && opponent.uncut(i, j) && self.uncut(i, j)) {
		    opponent.cut(new Shape(new Point[] {new Point(0, 0)}), new Point(i, j));
		}
	    }
	}
	Move move = real_cut(dough, shapes, opponent_shapes);
	// Get cut done by ourselves
	if (move != null) 
	    self.cut(shapes[move.shape].rotations()[move.rotation], move.point);
	return move;
    }
    
    private long touched_edges(Shape s, Point p, Dough d) {
	long sum = 0;
	for (Point q : s) {
	    if (cut(d, p.i + q.i + 1, p.j + q.j)) sum += 1;
	    if (cut(d, p.i + q.i - 1, p.j + q.j)) sum += 1;
	    if (cut(d, p.i + q.i, p.j + q.j + 1)) sum += 1;
	    if (cut(d, p.i + q.i, p.j + q.j - 1)) sum += 1;
	}
	return sum;
    }
    
    private boolean cut(Dough d, int i, int j) {
	return  i >= 0 && i < d.side() && j >= 0 && j < d.side() && !d.uncut(i, j);
    }

    private class ComparableMove implements Comparable<ComparableMove> {

	public Move move;
	public long key1;
	public long key2;
	public int randomized;

	public ComparableMove(Move move, long key1, long key2) {
	    this.move = move;
	    this.key1 = key1;
	    this.key2 = key2;
	    this.randomized = gen.nextInt();
	}

	@Override
	public int compareTo(ComparableMove o) {
	    int c = Long.compare(this.key2, o.key2);
	    if (c != 0) {
		return c;
	    }
	    c = Long.compare(this.key1, o.key1);
	    if (c != 0) {
		return c;
	    }
	    return Integer.compare(this.randomized, o.randomized);
	}
    }
}
