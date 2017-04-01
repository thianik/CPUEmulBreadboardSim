package sk.uniza.fri.cp.BreadboardSim.Components;

import sk.uniza.fri.cp.BreadboardSim.Board;

/**
 * Created by Moris on 22.3.2017.
 */
public class Probe extends Component {

    private static int id = 1;

    public Probe(Board board) {
        super(board, id++);
    }
}
