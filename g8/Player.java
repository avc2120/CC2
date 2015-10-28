package cc2.g8;

import cc2.sim.Point;
import cc2.sim.Shape;
import cc2.sim.Dough;
import cc2.sim.Move;

import java.util.*;

public class Player implements cc2.sim.Player {

	private boolean[] row_2 = new boolean [0];
	private static int offset = 0;
	private Shape current_shape;
	private int shape_index;
	private HashMap<Integer, Shape> shape_map = new HashMap<Integer, Shape>();
	private Random gen = new Random();

	public Shape cutter(int length, Shape[] shapes, Shape[] opponent_shapes)
	{
		// check if first try of given cutter length
		Point[] cutter = new Point [length];

		Map<Integer, List<Integer>> pairs = new HashMap<Integer, List<Integer>>();
		
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
			pairs.put(0, Arrays.asList(0, 1));
			pairs.put(1, Arrays.asList(1));
			pairs.put(2, Arrays.asList(0, 1));

			// if (row_2.length != cutter.length - 1) {
			// // save cutter length to check for retries
			// row_2 = new boolean [cutter.length - 1];
			// for (int i = 0 ; i != cutter.length ; ++i)
			// 	cutter[i] = new Point(i, 0);
			// } else {
			// 	// pick a random cell from 2nd row but not same
			// 	int i;
			// 	do {
			// 		i = gen.nextInt(cutter.length - 1);
			// 	} while (row_2[i]);
			// 	row_2[i] = true;
			// 	cutter[cutter.length - 1] = new Point(i, 1);
			// 	for (i = 0 ; i != cutter.length - 1 ; ++i)
			// 		cutter[i] = new Point(i, 0);
			// }
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

		for (i = 0; i < cutter.length; i++)
			System.out.println(cutter[i].toString());

		return new Shape(cutter);
	}

	public Move cut(Dough dough, Shape[] shapes, Shape[] opponent_shapes)
	{
		// prune larger shapes if initial move
		if (dough.uncut()) {
			offset = (int)((3.0/8)*dough.side());
			current_shape = shape_map.get(11);
			shape_index = 0;
			for(Shape s: shapes)
			{
				shape_map.put(s.size(), s);
			}
			int min = Integer.MAX_VALUE;
			for (Shape s : shapes)
				if (min > s.size())
					min = s.size();
				for (int s = 0 ; s != shapes.length ; ++s)
					if (shapes[s].size() != min)
						shapes[s] = null;
				}
		// find all valid cuts
		//start using smaller shapes
				if (offset == 0)
				{
					switch(current_shape.size())
					{
						case 11: if(percentCut(dough) > 0.6) { current_shape = shape_map.get(8); shape_index = 1;} break;
						case 8: if(percentCut(dough) > 0.8) {current_shape = shape_map.get(5); shape_index =2;}break;
						case 5: break;
					}
				}

				if (percentCut(dough) > 0.5 && offset >= 0)
				{
					offset = offset/2;
				}

				ArrayList <Move> moves = new ArrayList <Move> ();
				for (int i = offset ; i != dough.side()- offset ; ++i)
				{
					for (int j = offset; j != dough.side()-offset; ++j) 
					{
						Point p = new Point(i, j);
						Shape[] rotations = shapes[shape_index].rotations();
						for (int ri = 0 ; ri != rotations.length ; ++ri) {
							Shape s = rotations[ri];
							if (dough.cuts(s, p))
							{
								System.out.println("index:" + shape_index);
								moves.add(new Move(shape_index, ri, p));
							}
							
						}
					}
				}

		// return a cut randomly
					return moves.get(gen.nextInt(moves.size()));
				}

				private double percentCut(Dough dough)
				{
					int area = (dough.side() - 2*offset)*(dough.side()-2*offset);
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

				private int getShapeIndex(Shape shapes[], Shape current_shape)
				{
					int idx = 0;
					for(int i = 0 ; i < shapes.length; i++)
					{
						if (shapes[i].size() == current_shape.size())
						{
							idx = i;
						}
					}
					return idx;
				}

			}
