import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.*;

// Completed EC Features: Moves Counter, Maze Reset

// Representation for a vertex
class Vertex {
  int x;
  int y;
  Vertex left;
  Vertex right;
  Vertex top;
  Vertex bottom;
  Vertex previous;
  ArrayList<Edge> outEdges = new ArrayList<Edge>();
  boolean rightRender;
  boolean bottomRender;
  boolean travelled;

  Vertex(int x, int y) {
    this.x = x;
    this.y = y;
    this.left = null;
    this.right = null;
    this.top = null;
    this.bottom = null;
    this.previous = null;
    this.rightRender = true;
    this.bottomRender = true;
    this.travelled = false;
  }

  // draws right edge
  WorldImage drawRightEdge() {
    return new LineImage(new Posn(0, MazeGame.CELL_SIZE), Color.BLACK)
        .movePinhole(-1 * MazeGame.CELL_SIZE, MazeGame.CELL_SIZE / -2);
  }

  // draws left edge
  WorldImage drawBottomEdge() {
    return new LineImage(new Posn(MazeGame.CELL_SIZE, 0), Color.BLACK)
        .movePinhole(MazeGame.CELL_SIZE / -2, -1 * MazeGame.CELL_SIZE);
  }

  // draws rectangles
  WorldImage drawRect(int x, int y, Color c) {
    return new RectangleImage(MazeGame.CELL_SIZE - 2, MazeGame.CELL_SIZE - 2, OutlineMode.SOLID, c)
        .movePinhole(-x * MazeGame.CELL_SIZE / x / 2, -x * MazeGame.CELL_SIZE / x / 2);
  }

  // finds previous vertex
  void findPrevious() {
    if (this.top != null && !this.top.bottomRender && this.top.previous == null) {
      this.previous = this.top;
    }
    else if (this.left != null && !this.left.rightRender && this.left.previous == null) {
      this.previous = this.left;
    }
    else if (this.bottom != null && !this.bottomRender && this.bottom.previous == null) {
      this.previous = this.bottom;
    }
    else if (this.right != null && !this.rightRender && this.right.previous == null) {
      this.previous = this.right;
    }
  }

}

// Representation for an edge
class Edge {
  Vertex from;
  Vertex to;
  int weight;

  Edge(Vertex from, Vertex to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }
}

// Representation for a weight comparator
class WeightComparator implements Comparator<Edge> {

  // compares edge weights
  public int compare(Edge e1, Edge e2) {
    return e1.weight - e2.weight;
  }
}

//Representation for a Maze Game
class MazeGame extends World {
  static final int CELL_SIZE = 10;
  int bSizeX;
  int bSizeY;
  HashMap<Vertex, Vertex> map = new HashMap<Vertex, Vertex>();
  ArrayList<Edge> loe = new ArrayList<Edge>();
  ArrayList<Edge> mts = new ArrayList<Edge>();
  ArrayList<Vertex> path = new ArrayList<Vertex>();
  Vertex endCell;
  boolean finished;
  TextImage solved = new TextImage("Maze Solved", 30, Color.RED);
  TextImage moves;
  static int numMoves;
  WorldScene scene = new WorldScene(0, 0);
  ArrayList<ArrayList<Vertex>> board;

  double tick = 0.01;

  MazeGame(int bSizeX, int bSizeY) {
    this.bSizeX = bSizeX;
    this.bSizeY = bSizeY;
    this.board = this.makeGrid(bSizeX, bSizeY);
    this.createEdges(this.board);
    this.createMap(board);
    this.kruskals();
    this.endCell = this.board.get(bSizeY - 1).get(bSizeX - 1);
    MazeGame.numMoves = 0;
    this.moves = new TextImage("Moves: " + (int) MazeGame.numMoves, 14, Color.MAGENTA);
    this.drawWorld();
    this.finished = false;

  }

  // constructor for testing
  MazeGame() {
    this.bSizeX = 2;
    this.bSizeY = 3;
    this.board = this.makeGrid(2, 3);

    this.board.get(0).get(0).rightRender = false;
    this.board.get(0).get(1).rightRender = true;
    this.board.get(1).get(0).rightRender = true;
    this.board.get(1).get(1).rightRender = true;
    this.board.get(2).get(0).rightRender = true;
    this.board.get(2).get(1).rightRender = true;

    this.map.put(this.board.get(0).get(0), this.board.get(0).get(0));
    this.map.put(this.board.get(0).get(1), this.board.get(0).get(1));
    this.map.put(this.board.get(1).get(0), this.board.get(1).get(0));
    this.map.put(this.board.get(1).get(1), this.board.get(1).get(1));
    this.map.put(this.board.get(2).get(0), this.board.get(2).get(0));
    this.map.put(this.board.get(2).get(1), this.board.get(2).get(1));

    this.board.get(0).get(0).bottomRender = false;
    this.board.get(0).get(1).bottomRender = false;
    this.board.get(1).get(0).bottomRender = false;
    this.board.get(1).get(1).bottomRender = false;
    this.board.get(2).get(0).bottomRender = true;
    this.board.get(2).get(1).bottomRender = true;

    this.loe = new ArrayList<Edge>(Arrays.asList(new Edge(new Vertex(0, 0), new Vertex(1, 0), 1),
        new Edge(new Vertex(0, 0), new Vertex(0, 1), 2),
        new Edge(new Vertex(1, 0), new Vertex(1, 1), 3),
        new Edge(new Vertex(0, 1), new Vertex(1, 1), 4),
        new Edge(new Vertex(0, 1), new Vertex(0, 2), 5),
        new Edge(new Vertex(1, 1), new Vertex(1, 2), 6),
        new Edge(new Vertex(0, 2), new Vertex(1, 2), 7)));

    this.mts = new ArrayList<Edge>(Arrays.asList(new Edge(new Vertex(0, 0), new Vertex(1, 0), 1),
        new Edge(new Vertex(0, 0), new Vertex(0, 1), 2),
        new Edge(new Vertex(1, 0), new Vertex(1, 1), 3),
        new Edge(new Vertex(0, 1), new Vertex(0, 2), 5),
        new Edge(new Vertex(1, 1), new Vertex(1, 2), 6)));

    this.endCell = this.board.get(2).get(1);
    this.finished = false;
    this.path = new ArrayList<Vertex>();
    MazeGame.numMoves = 0;
    this.moves = new TextImage("Moves: " + (int) MazeGame.numMoves, 14, Color.MAGENTA);

    this.drawWorld();
  }

  // draws the world's grid, start, and ending positions
  WorldScene drawWorld() {

    WorldImage background = new RectangleImage(bSizeX * MazeGame.CELL_SIZE,
        bSizeY * MazeGame.CELL_SIZE, OutlineMode.SOLID, Color.GRAY);
    this.scene.placeImageXY(background, (bSizeX * MazeGame.CELL_SIZE) / 2,
        (bSizeY * MazeGame.CELL_SIZE) / 2);

    this.scene.placeImageXY(
        board.get(0).get(0).drawRect(this.bSizeX, this.bSizeY, new Color(0, 152, 0)), 0, 0);

    this.scene.placeImageXY(board.get(this.bSizeY - 1).get(this.bSizeX - 1).drawRect(this.bSizeX,
        this.bSizeY, new Color(102, 0, 153)), (bSizeX - 1) * CELL_SIZE, (bSizeY - 1) * CELL_SIZE);

    // draw the grid
    for (int i = 0; i < bSizeY; i++) {
      for (int j = 0; j < bSizeX; j++) {
        this.changeBottomRender(this.board.get(i).get(j));
        this.changeRightRender(this.board.get(i).get(j));
        if (board.get(i).get(j).rightRender) {
          this.scene.placeImageXY(board.get(i).get(j).drawRightEdge(), (MazeGame.CELL_SIZE * j),
              (MazeGame.CELL_SIZE * i));
        }
        if (board.get(i).get(j).bottomRender) {
          this.scene.placeImageXY(board.get(i).get(j).drawBottomEdge(), (MazeGame.CELL_SIZE * j),
              (MazeGame.CELL_SIZE * i));
        }
      }
    }
    this.scene.placeImageXY(this.moves, MazeGame.CELL_SIZE + 25,
        bSizeY * MazeGame.CELL_SIZE + MazeGame.CELL_SIZE / 2);
    return scene;
  }

  // draws the scene
  public WorldScene makeScene() {
    if (path.size() > 1) {
      this.findEnd();
      MazeGame.numMoves += 1;
      this.moves.text = "Moves: " + (int) MazeGame.numMoves;
    }
    else if (path.size() > 0) {
      this.drawEnd();
      MazeGame.numMoves += 1;
      this.moves.text = "Moves: " + (int) MazeGame.numMoves;
    }
    // If the maze is complete, trace back the solution
    else if (this.finished && this.endCell.previous != null) {
      this.retrace();
      this.scene.placeImageXY(solved, bSizeX * MazeGame.CELL_SIZE / 2,
          bSizeY * MazeGame.CELL_SIZE / 2);
    }
    return scene;
  }

  // changes the rightRender of a vertex
  void changeRightRender(Vertex v) {
    for (Edge edge : this.mts) {
      if (edge.to.y == edge.from.y) {
        edge.from.rightRender = false;
      }
    }
  }

  // changes bottomRender of a vertex
  void changeBottomRender(Vertex v) {
    for (Edge edge : this.mts) {
      if (edge.to.x == edge.from.x) {
        edge.from.bottomRender = false;
      }
    }
  }

  // creates the grid for each cell in the maze
  ArrayList<ArrayList<Vertex>> makeGrid(int width, int height) {
    ArrayList<ArrayList<Vertex>> board = new ArrayList<ArrayList<Vertex>>();
    for (int i = 0; i < height; i++) {
      board.add(new ArrayList<Vertex>());
      ArrayList<Vertex> r = board.get(i);
      for (int j = 0; j < width; j++) {
        r.add(new Vertex(j, i));
      }
    }
    this.connectVertices(board);
    this.createEdges(board);
    this.createMap(board);
    return board;
  }

  // connects all the vertices
  void connectVertices(ArrayList<ArrayList<Vertex>> v) {
    for (int i = 0; i < this.bSizeY; i++) {
      for (int j = 0; j < this.bSizeX; j++) {
        if (j + 1 < this.bSizeX) {
          v.get(i).get(j).right = v.get(i).get(j + 1);
        }

        if (j - 1 >= 0) {
          v.get(i).get(j).left = v.get(i).get(j - 1);
        }

        if (i + 1 < this.bSizeY) {
          v.get(i).get(j).bottom = v.get(i + 1).get(j);
        }

        if (i - 1 >= 0) {
          v.get(i).get(j).top = v.get(i - 1).get(j);
        }
      }
    }
  }

  // creates a list of edges
  ArrayList<Edge> createEdges(ArrayList<ArrayList<Vertex>> arr) {
    Random r = new Random();
    for (int i = 0; i < arr.size(); i++) {
      for (int j = 0; j < arr.get(i).size(); j++) {
        if (j < arr.get(i).size() - 1) {
          loe.add(new Edge(arr.get(i).get(j), arr.get(i).get(j).right, r.nextInt(50)));
        }
        if (i < arr.size() - 1) {
          loe.add(new Edge(arr.get(i).get(j), arr.get(i).get(j).bottom, r.nextInt(50)));
        }
      }
    }
    Collections.sort(loe, new WeightComparator());
    return loe;
  }

  // creates a hash map where each vertex is connected to itself
  HashMap<Vertex, Vertex> createMap(ArrayList<ArrayList<Vertex>> v) {
    for (int i = 0; i < v.size(); i++) {
      for (int j = 0; j < v.get(i).size(); j++) {
        this.map.put(v.get(i).get(j), v.get(i).get(j));
      }
    }
    return map;
  }

  // creates a minimum spanning tree
  ArrayList<Edge> kruskals() {
    int i = 0;
    while (this.mts.size() < this.loe.size() && i < this.loe.size()) {
      Edge e = loe.get(i);
      if (this.find(this.find(e.from)).equals(this.find(this.find(e.to)))) {
        // nothing happens
      }
      else {
        mts.add(e);
        union(this.find(e.from), this.find(e.to));
      }
      i += 1;
    }
    for (int y = 0; y < this.bSizeY; y += 1) {
      for (int x = 0; x < this.bSizeX; x += 1) {
        for (Edge e : this.mts) {
          if (this.board.get(y).get(x).equals(e.from) || this.board.get(y).get(x).equals(e.to)) {
            this.board.get(y).get(x).outEdges.add(e);
          }
        }
      }
    }
    return this.mts;
  }

  // unionizes two vertices
  void union(Vertex v, Vertex newVal) {
    this.map.put(this.find(v), this.find(newVal));
  }

  // finds this specific vertex
  Vertex find(Vertex v) {
    if (v.equals(this.map.get(v))) {
      return v;
    }
    else {
      return this.find(this.map.get(v));
    }
  }

  // records user's actions (creating a new game, running bfs / dfs)
  public void onKeyEvent(String key) {
    if (key.equals("n")) {
      this.scene = this.getEmptyScene();
      this.board = this.makeGrid(bSizeX, bSizeY);
      this.createEdges(this.board);
      this.createMap(board);
      this.kruskals();
      this.endCell = this.board.get(this.bSizeY - 1).get(this.bSizeX - 1);
      MazeGame.numMoves = 0;
      this.moves = new TextImage("Moves: " + (int) MazeGame.numMoves, 14, Color.MAGENTA);
      this.drawWorld();
    }
    else if (key.equals("d")) {
      this.endCell = this.board.get(this.bSizeY - 1).get(this.bSizeX - 1);
      this.path = new Graph().searchDFS(this.board.get(0).get(0),
          this.board.get(this.bSizeY - 1).get(this.bSizeX - 1));
    }
    else if (key.equals("b")) {
      this.endCell = this.board.get(this.bSizeY - 1).get(this.bSizeX - 1);
      this.path = new Graph().searchBFS(this.board.get(0).get(0),
          this.board.get(this.bSizeY - 1).get(this.bSizeX - 1));
    }
    this.drawWorld();
  }

  // draws out the path
  void findEnd() {
    Vertex next = path.remove(0);
    this.scene.placeImageXY(next.drawRect(this.bSizeX, this.bSizeY, new Color(51, 204, 255)),
        next.x * CELL_SIZE, next.y * CELL_SIZE);
  }

  // initiates ending procedure
  void drawEnd() {
    Vertex next = path.remove(0);
    this.scene.placeImageXY(next.drawRect(this.bSizeX, this.bSizeY, new Color(51, 204, 255)),
        next.x * CELL_SIZE, next.y * CELL_SIZE);
    if (!this.endCell.left.rightRender && this.endCell.left.previous != null) {
      this.endCell.previous = this.endCell.left;
    }
    else if (!this.endCell.top.bottomRender && this.endCell.top.previous != null) {
      this.endCell.previous = this.endCell.top;
    }
    else {
      this.endCell.previous = next;
    }
    this.finished = true;
  }

  // traces the solution to the maze
  void retrace() {
    if (this.endCell.x == this.bSizeX - 1 && this.endCell.y == this.bSizeY - 1) {
      this.scene.placeImageXY(
          this.endCell.drawRect(this.bSizeX, this.bSizeY, new Color(17, 137, 242)),
          this.endCell.x * CELL_SIZE, this.endCell.y * CELL_SIZE);
    }
    this.scene.placeImageXY(
        this.endCell.previous.drawRect(this.bSizeX, this.bSizeY, new Color(17, 137, 242)),
        this.endCell.previous.x * CELL_SIZE, this.endCell.previous.y * CELL_SIZE);
    this.endCell = this.endCell.previous;
  }

}

// an interface for an ICollection
interface ICollection<T> {
  // adds an item to this ICollection
  void add(T item);

  // removes an item from this ICollection
  T remove();

  // returns the size of this ICollection
  int size();
}

// Representation for a Queue
class Queue<T> implements ICollection<T> {
  Deque<T> contents;

  Queue() {
    this.contents = new ArrayDeque<T>();
  }

  // adds an item to this Queue
  public void add(T item) {
    this.contents.addLast(item);
  }

  // removes an item from this Queue
  public T remove() {
    return this.contents.removeFirst();
  }

  // returns the size of this Queue
  public int size() {
    return this.contents.size();
  }
}

//Represents a Stack
class Stack<T> implements ICollection<T> {
  Deque<T> contents;

  Stack() {
    this.contents = new ArrayDeque<T>();
  }

  // adds an item to a Stack
  public void add(T item) {
    this.contents.addFirst(item);
  }

  // removes and item to a Stack
  public T remove() {
    return this.contents.pop();
  }

  // returns the size of this Stack
  public int size() {
    return this.contents.size();
  }
}

// Representation for a graph
class Graph {

  ArrayList<Vertex> allVertices;

  Graph() {
  }

  // depth-first search: finds a path using a stack
  ArrayList<Vertex> searchDFS(Vertex from, Vertex to) {
    return this.searchHelp(from, to, new Stack<Vertex>());
  }

  // breadth-first search: finds a path using a queue
  ArrayList<Vertex> searchBFS(Vertex from, Vertex to) {
    return this.searchHelp(from, to, new Queue<Vertex>());
  }

  // finds given the two vertices whilst using ICollection
  ArrayList<Vertex> searchHelp(Vertex from, Vertex to, ICollection<Vertex> wlist) {
    ArrayList<Vertex> path = new ArrayList<Vertex>();

    wlist.add(from);
    while (wlist.size() > 0) {
      Vertex next = wlist.remove();
      if (next == to) {
        return path;
      }
      else if (path.contains(next)) {
        // Do nothing
      }
      else {
        for (Edge e : next.outEdges) {
          wlist.add(e.from);
          wlist.add(e.to);
          if (path.contains(e.from)) {
            next.previous = e.from;
          }
          else if (path.contains(e.to)) {
            next.previous = e.to;
          }
        }
        path.add(next);
      }
    }
    return path;
  }
}

// Representation for an examples maze game
class ExamplesMazeGame {
  MazeGame game = new MazeGame(40, 40);
  Graph graph = new Graph();

  void testdrawRects(Tester t) {
    MazeGame maze = new MazeGame();

    t.checkExpect(maze.board.get(0).get(0).drawRect(2, 3, Color.GREEN),
        new RectangleImage(MazeGame.CELL_SIZE - 2, MazeGame.CELL_SIZE - 2, OutlineMode.SOLID,
            Color.GREEN).movePinhole(-2 * MazeGame.CELL_SIZE / 2 / 2,
                -2 * MazeGame.CELL_SIZE / 2 / 2));

    t.checkExpect(maze.board.get(1).get(0).drawRect(2, 3, Color.MAGENTA),
        new RectangleImage(MazeGame.CELL_SIZE - 2, MazeGame.CELL_SIZE - 2, OutlineMode.SOLID,
            Color.MAGENTA).movePinhole(-2 * MazeGame.CELL_SIZE / 2 / 2,
                -2 * MazeGame.CELL_SIZE / 2 / 2));

  }

  void testdrawBottomEdge(Tester t) {
    MazeGame maze = new MazeGame();

    t.checkExpect(maze.board.get(2).get(1).drawBottomEdge(),
        new LineImage(new Posn(MazeGame.CELL_SIZE, 0), Color.BLACK)
            .movePinhole(MazeGame.CELL_SIZE / -2, -1 * MazeGame.CELL_SIZE));

    t.checkExpect(maze.board.get(2).get(0).drawBottomEdge(),
        new LineImage(new Posn(MazeGame.CELL_SIZE, 0), Color.BLACK)
            .movePinhole(MazeGame.CELL_SIZE / -2, -1 * MazeGame.CELL_SIZE));
  }

  void testdrawRightEdge(Tester t) {
    MazeGame maze = new MazeGame();

    t.checkExpect(maze.board.get(0).get(1).drawRightEdge(),
        new LineImage(new Posn(0, MazeGame.CELL_SIZE), Color.BLACK)
            .movePinhole(-1 * MazeGame.CELL_SIZE, MazeGame.CELL_SIZE / -2));

    t.checkExpect(maze.board.get(1).get(0).drawRightEdge(),
        new LineImage(new Posn(0, MazeGame.CELL_SIZE), Color.BLACK)
            .movePinhole(-1 * MazeGame.CELL_SIZE, MazeGame.CELL_SIZE / -2));
  }

  void testCompare(Tester t) {
    MazeGame maze = new MazeGame();
    WeightComparator c = new WeightComparator();

    t.checkExpect(c.compare(maze.loe.get(0), maze.loe.get(1)), -1);
    t.checkExpect(c.compare(maze.loe.get(0), maze.loe.get(2)), -2);
    t.checkExpect(c.compare(maze.loe.get(0), maze.loe.get(3)), -3);
  }

  void testMakeGrid(Tester t) {
    MazeGame maze = new MazeGame();
    t.checkExpect(maze.board, new ArrayList<ArrayList<Vertex>>(Arrays.asList(
        new ArrayList<Vertex>(Arrays.asList(maze.board.get(0).get(0), maze.board.get(0).get(1))),
        new ArrayList<Vertex>(Arrays.asList(maze.board.get(1).get(0), maze.board.get(1).get(1))),
        new ArrayList<Vertex>(Arrays.asList(maze.board.get(2).get(0), maze.board.get(2).get(1))))));
  }

  void testconnectVertices(Tester t) {
    MazeGame maze = new MazeGame();
    t.checkExpect(maze.board.get(0).get(0).right, maze.board.get(0).get(1));
    t.checkExpect(maze.board.get(0).get(0).bottom, maze.board.get(1).get(0));
    t.checkExpect(maze.board.get(0).get(0).top, null);
    t.checkExpect(maze.board.get(0).get(0).left, null);
  }

  void testCreateEdges(Tester t) {
    MazeGame maze = new MazeGame();
    t.checkExpect(maze.loe.get(0),
        new Edge(new Vertex(maze.board.get(0).get(0).x, maze.board.get(0).get(0).y),
            new Vertex(maze.board.get(0).get(1).x, maze.board.get(0).get(1).y), 1));
    t.checkExpect(maze.loe.get(1),
        new Edge(new Vertex(maze.board.get(0).get(0).x, maze.board.get(0).get(0).y),
            new Vertex(maze.board.get(1).get(0).x, maze.board.get(1).get(0).y), 2));
    t.checkExpect(maze.loe.get(2),
        new Edge(new Vertex(maze.board.get(0).get(1).x, maze.board.get(0).get(1).y),
            new Vertex(maze.board.get(1).get(1).x, maze.board.get(1).get(1).y), 3));
  }

  void testCreateMap(Tester t) {
    MazeGame maze = new MazeGame();
    t.checkExpect(maze.map.get(maze.board.get(0).get(0)), maze.board.get(0).get(0));
    t.checkExpect(maze.map.get(maze.board.get(0).get(1)), maze.board.get(0).get(1));
    t.checkExpect(maze.map.get(maze.board.get(1).get(0)), maze.board.get(1).get(0));
    t.checkExpect(maze.map.get(maze.board.get(1).get(1)), maze.board.get(1).get(1));
    t.checkExpect(maze.map.get(maze.board.get(2).get(0)), maze.board.get(2).get(0));
    t.checkExpect(maze.map.get(maze.board.get(2).get(1)), maze.board.get(2).get(1));
  }

  void testKruskals(Tester t) {
    MazeGame maze = new MazeGame();
    maze.makeGrid(maze.bSizeX, maze.bSizeY);
    t.checkExpect(maze.mts.get(0), new Edge(maze.mts.get(0).from, maze.mts.get(0).to, 1));
    t.checkExpect(maze.mts.get(1), new Edge(maze.mts.get(1).from, maze.mts.get(1).to, 2));
    t.checkExpect(maze.mts.get(2), new Edge(maze.mts.get(2).from, maze.mts.get(2).to, 3));
    t.checkExpect(maze.mts.get(3), new Edge(maze.mts.get(3).from, maze.mts.get(3).to, 5));
    t.checkExpect(maze.mts.get(4), new Edge(maze.mts.get(4).from, maze.mts.get(4).to, 6));
  }

  void testUnion(Tester t) {
    MazeGame maze = new MazeGame();

    maze.union(maze.board.get(0).get(0), maze.board.get(0).get(1));
    t.checkExpect(maze.find(maze.board.get(0).get(0)), maze.board.get(0).get(1));

    maze.union(maze.board.get(0).get(1), maze.board.get(1).get(1));
    t.checkExpect(maze.find(maze.board.get(0).get(1)), maze.board.get(1).get(1));

    maze.union(maze.board.get(2).get(0), maze.board.get(0).get(1));
    t.checkExpect(maze.find(maze.board.get(0).get(0)), maze.board.get(1).get(1));
  }

  void testFind(Tester t) {
    MazeGame maze = new MazeGame();
    t.checkExpect(maze.find(maze.board.get(0).get(0)), maze.board.get(0).get(0));
    t.checkExpect(maze.find(maze.board.get(2).get(0)), maze.board.get(2).get(0));
  }

  void testchangeRightRender(Tester t) {
    MazeGame maze = new MazeGame();
    maze.changeRightRender(maze.board.get(0).get(0));
    t.checkExpect(maze.board.get(0).get(0).rightRender, false);

    maze.changeRightRender(maze.board.get(2).get(0));
    t.checkExpect(maze.board.get(2).get(0).rightRender, true);
  }

  void testchangeBottomRender(Tester t) {
    MazeGame maze = new MazeGame();
    maze.changeBottomRender(maze.board.get(0).get(0));
    t.checkExpect(maze.board.get(0).get(0).bottomRender, false);

    maze.changeBottomRender(maze.board.get(0).get(1));
    t.checkExpect(maze.board.get(0).get(1).bottomRender, false);

    maze.changeBottomRender(maze.board.get(2).get(0));
    t.checkExpect(maze.board.get(2).get(0).bottomRender, true);
  }

  void testMakeScene(Tester t) {
    MazeGame maze = new MazeGame();
    WorldScene testScene = new WorldScene(0, 0);

    testScene.placeImageXY(maze.moves, MazeGame.CELL_SIZE + 25,
        maze.bSizeY * MazeGame.CELL_SIZE + MazeGame.CELL_SIZE / 2);

    testScene.placeImageXY(maze.board.get(0).get(0).drawRect(2, 3, new Color(0, 152, 0)), 0, 0);

    testScene.placeImageXY(maze.board.get(2).get(1).drawRect(2, 3, new Color(102, 0, 153)),
        1 * MazeGame.CELL_SIZE, 2 * MazeGame.CELL_SIZE);

    WorldImage background = new RectangleImage(2 * MazeGame.CELL_SIZE, 3 * MazeGame.CELL_SIZE,
        OutlineMode.SOLID, Color.GRAY);
    testScene.placeImageXY(background, (2 * MazeGame.CELL_SIZE) / 2, (3 * MazeGame.CELL_SIZE) / 2);

    testScene.placeImageXY(maze.board.get(0).get(1).drawRightEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 0));
    testScene.placeImageXY(maze.board.get(1).get(0).drawRightEdge(), (MazeGame.CELL_SIZE * 0),
        (MazeGame.CELL_SIZE * 1));
    testScene.placeImageXY(maze.board.get(1).get(1).drawRightEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 1));
    testScene.placeImageXY(maze.board.get(2).get(0).drawRightEdge(), (MazeGame.CELL_SIZE * 0),
        (MazeGame.CELL_SIZE * 2));
    testScene.placeImageXY(maze.board.get(2).get(1).drawRightEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 2));
    testScene.placeImageXY(maze.board.get(2).get(0).drawBottomEdge(), (MazeGame.CELL_SIZE * 0),
        (MazeGame.CELL_SIZE * 2));
    testScene.placeImageXY(maze.board.get(2).get(1).drawBottomEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 2));

    t.checkExpect(maze.makeScene(), testScene);

  }

  void testDrawWorld(Tester t) {
    MazeGame maze = new MazeGame();
    WorldScene testScene = new WorldScene(0, 0);

    testScene.placeImageXY(maze.moves, MazeGame.CELL_SIZE + 25,
        maze.bSizeY * MazeGame.CELL_SIZE + MazeGame.CELL_SIZE / 2);

    WorldImage background = new RectangleImage(2 * MazeGame.CELL_SIZE, 3 * MazeGame.CELL_SIZE,
        OutlineMode.SOLID, Color.GRAY);
    testScene.placeImageXY(background, (2 * MazeGame.CELL_SIZE) / 2, (3 * MazeGame.CELL_SIZE) / 2);

    testScene.placeImageXY(maze.board.get(0).get(0).drawRect(2, 3, new Color(0, 152, 0)), 0, 0);

    testScene.placeImageXY(maze.board.get(2).get(1).drawRect(2, 3, new Color(102, 0, 153)),
        1 * MazeGame.CELL_SIZE, 2 * MazeGame.CELL_SIZE);

    testScene.placeImageXY(maze.board.get(0).get(1).drawRightEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 0));
    testScene.placeImageXY(maze.board.get(1).get(0).drawRightEdge(), (MazeGame.CELL_SIZE * 0),
        (MazeGame.CELL_SIZE * 1));
    testScene.placeImageXY(maze.board.get(1).get(1).drawRightEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 1));
    testScene.placeImageXY(maze.board.get(2).get(0).drawRightEdge(), (MazeGame.CELL_SIZE * 0),
        (MazeGame.CELL_SIZE * 2));
    testScene.placeImageXY(maze.board.get(2).get(1).drawRightEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 2));
    testScene.placeImageXY(maze.board.get(2).get(0).drawBottomEdge(), (MazeGame.CELL_SIZE * 0),
        (MazeGame.CELL_SIZE * 2));
    testScene.placeImageXY(maze.board.get(2).get(1).drawBottomEdge(), (MazeGame.CELL_SIZE * 1),
        (MazeGame.CELL_SIZE * 2));

    t.checkExpect(maze.drawWorld(), testScene);
  }

  // Tests add (queue) method
  void testAddAtTail(Tester t) {
    Queue<Vertex> queue = new Queue<Vertex>();

    t.checkExpect(queue.size(), 0);
    queue.add(new Vertex(0, 0));
    t.checkExpect(queue.size(), 1);
  }

  // Tests size
  void testSize(Tester t) {
    Queue<Vertex> queue = new Queue<Vertex>();
    Stack<Vertex> stack = new Stack<Vertex>();

    t.checkExpect(stack.size(), 0);
    stack.add(new Vertex(1, 0));
    t.checkExpect(stack.size(), 1);

    t.checkExpect(queue.size(), 0);
    queue.add(new Vertex(0, 0));
    t.checkExpect(queue.size(), 1);
  }

  void testRemoveFromHead(Tester t) {
    Queue<Vertex> queue = new Queue<Vertex>();
    queue.add(new Vertex(0, 0));

    t.checkExpect(queue.remove(), new Vertex(0, 0));
  }

  void testAddToHead(Tester t) {
    Stack<Vertex> stack = new Stack<Vertex>();
    t.checkExpect(stack.size(), 0);
    stack.add(new Vertex(0, 0));

    t.checkExpect(stack.size(), 1);
  }

  void testAddToTail(Tester t) {
    Stack<Vertex> stack = new Stack<Vertex>();
    t.checkExpect(stack.size(), 0);
    stack.add(new Vertex(0, 0));

    t.checkExpect(stack.size(), 1);
  }

  void testsearchDFS(Tester t) {
    MazeGame maze = new MazeGame();

    t.checkExpect(graph.searchDFS(maze.board.get(0).get(0), maze.board.get(2).get(1)),
        new ArrayList<Vertex>(Arrays.asList(maze.board.get(0).get(0))));
  }

  void testsearchBFS(Tester t) {
    MazeGame maze = new MazeGame();

    t.checkExpect(graph.searchBFS(maze.board.get(0).get(0), maze.board.get(2).get(1)),
        new ArrayList<Vertex>(Arrays.asList(maze.board.get(0).get(0))));
  }

  void testSearchHelp(Tester t) {
    MazeGame maze = new MazeGame();

    t.checkExpect(
        graph.searchHelp(maze.board.get(0).get(0), maze.board.get(2).get(1), new Stack<Vertex>()),
        new ArrayList<Vertex>(Arrays.asList(maze.board.get(0).get(0))));

    t.checkExpect(
        graph.searchHelp(maze.board.get(0).get(0), maze.board.get(2).get(1), new Queue<Vertex>()),
        new ArrayList<Vertex>(Arrays.asList(maze.board.get(0).get(0))));
  }

  void testFindEnd(Tester t) {
    MazeGame maze = new MazeGame();
    WorldScene testScene = new WorldScene(0, 0);

    testScene.placeImageXY(maze.moves, MazeGame.CELL_SIZE + 25,
        maze.bSizeY * MazeGame.CELL_SIZE + MazeGame.CELL_SIZE / 2);

    WorldImage background = new RectangleImage(2 * MazeGame.CELL_SIZE, 3 * MazeGame.CELL_SIZE,
        OutlineMode.SOLID, Color.GRAY);
    testScene.placeImageXY(background, (2 * MazeGame.CELL_SIZE) / 2, (3 * MazeGame.CELL_SIZE) / 2);

    testScene.placeImageXY(maze.board.get(0).get(0).drawRect(2, 3, new Color(0, 152, 0)), 0, 0);

    testScene.placeImageXY(maze.board.get(2).get(1).drawRect(2, 3, new Color(102, 0, 153)),
        1 * MazeGame.CELL_SIZE, 2 * MazeGame.CELL_SIZE);

    maze.onKeyEvent("d");
    Vertex next = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next.x * MazeGame.CELL_SIZE, next.y * MazeGame.CELL_SIZE);
    t.checkExpect(maze.makeScene(), testScene);
    maze.onKeyEvent("d");
    Vertex next2 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next2.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next2.x * MazeGame.CELL_SIZE, next2.y * MazeGame.CELL_SIZE);
    t.checkExpect(maze.makeScene(), testScene);
    maze.onKeyEvent("d");
    Vertex next3 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next3.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next3.x * MazeGame.CELL_SIZE, next3.y * MazeGame.CELL_SIZE);
    t.checkExpect(maze.makeScene(), testScene);

  }

  void testDrawEnd(Tester t) {
    MazeGame maze = new MazeGame();
    WorldScene testScene = new WorldScene(0, 0);

    testScene.placeImageXY(maze.moves, MazeGame.CELL_SIZE + 25,
        maze.bSizeY * MazeGame.CELL_SIZE + MazeGame.CELL_SIZE / 2);

    WorldImage background = new RectangleImage(2 * MazeGame.CELL_SIZE, 3 * MazeGame.CELL_SIZE,
        OutlineMode.SOLID, Color.GRAY);
    testScene.placeImageXY(background, (2 * MazeGame.CELL_SIZE) / 2, (3 * MazeGame.CELL_SIZE) / 2);

    testScene.placeImageXY(maze.board.get(0).get(0).drawRect(2, 3, new Color(0, 152, 0)), 0, 0);

    testScene.placeImageXY(maze.board.get(2).get(1).drawRect(2, 3, new Color(102, 0, 153)),
        1 * MazeGame.CELL_SIZE, 2 * MazeGame.CELL_SIZE);

    maze.onKeyEvent("d");
    Vertex next = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next.x * MazeGame.CELL_SIZE, next.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next2 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next2.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next2.x * MazeGame.CELL_SIZE, next2.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next3 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next3.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next3.x * MazeGame.CELL_SIZE, next3.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next4 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next4.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next4.x * MazeGame.CELL_SIZE, next4.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next5 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next5.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next5.x * MazeGame.CELL_SIZE, next5.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next6 = maze.path.get(0);
    maze.drawEnd();
    testScene.placeImageXY(next6.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next6.x * MazeGame.CELL_SIZE, next6.y * MazeGame.CELL_SIZE);
    t.checkExpect(maze.makeScene(), testScene);

  }

  void testRetrace(Tester t) {
    MazeGame maze = new MazeGame();
    WorldScene testScene = new WorldScene(0, 0);

    testScene.placeImageXY(maze.moves, MazeGame.CELL_SIZE + 25,
        maze.bSizeY * MazeGame.CELL_SIZE + MazeGame.CELL_SIZE / 2);

    WorldImage background = new RectangleImage(2 * MazeGame.CELL_SIZE, 3 * MazeGame.CELL_SIZE,
        OutlineMode.SOLID, Color.GRAY);
    testScene.placeImageXY(background, (2 * MazeGame.CELL_SIZE) / 2, (3 * MazeGame.CELL_SIZE) / 2);

    testScene.placeImageXY(maze.board.get(0).get(0).drawRect(2, 3, new Color(0, 152, 0)), 0, 0);

    testScene.placeImageXY(maze.board.get(2).get(1).drawRect(2, 3, new Color(102, 0, 153)),
        1 * MazeGame.CELL_SIZE, 2 * MazeGame.CELL_SIZE);

    maze.onKeyEvent("d");
    Vertex next = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next.x * MazeGame.CELL_SIZE, next.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next2 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next2.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next2.x * MazeGame.CELL_SIZE, next2.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next3 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next3.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next3.x * MazeGame.CELL_SIZE, next3.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next4 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next4.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next4.x * MazeGame.CELL_SIZE, next4.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next5 = maze.path.get(0);
    maze.findEnd();
    testScene.placeImageXY(next5.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next5.x * MazeGame.CELL_SIZE, next5.y * MazeGame.CELL_SIZE);
    maze.onKeyEvent("d");
    Vertex next6 = maze.path.get(0);
    maze.drawEnd();
    testScene.placeImageXY(next6.drawRect(maze.bSizeX, maze.bSizeY, new Color(51, 204, 255)),
        next6.x * MazeGame.CELL_SIZE, next6.y * MazeGame.CELL_SIZE);
    maze.retrace();
    while (maze.endCell != maze.board.get(0).get(0)) {
      if (maze.endCell.x == maze.bSizeX - 1 && maze.endCell.y == maze.bSizeY - 1) {
        testScene.placeImageXY(
            maze.endCell.drawRect(maze.bSizeX, maze.bSizeY, new Color(17, 137, 242)),
            maze.endCell.x * MazeGame.CELL_SIZE, maze.endCell.y * MazeGame.CELL_SIZE);
      }
      testScene.placeImageXY(
          maze.endCell.previous.drawRect(maze.bSizeX, maze.bSizeY, new Color(17, 137, 242)),
          maze.endCell.previous.x * MazeGame.CELL_SIZE,
          maze.endCell.previous.y * MazeGame.CELL_SIZE);
      maze.endCell = maze.endCell.previous;
    }
    t.checkExpect(maze.makeScene(), testScene);

  }

  void testOnKeyEvent(Tester t) {
    MazeGame maze = new MazeGame();
    t.checkExpect(maze.bSizeX == 2, true);
    t.checkExpect(maze.bSizeY == 3, true);
    maze.onKeyEvent("d");
    t.checkExpect(maze.bSizeX == 2, true);
    t.checkExpect(maze.bSizeY == 3, true);
    maze.onKeyEvent("b");
    t.checkExpect(maze.bSizeX == 2, true);
    t.checkExpect(maze.bSizeY == 3, true);
    //maze.onKeyEvent("n");
    // can't reset game on n since we delegated values of the maze

  }

  void testBigBang(Tester t) {
    this.game.bigBang(this.game.bSizeX * MazeGame.CELL_SIZE,
        this.game.bSizeY * MazeGame.CELL_SIZE + MazeGame.CELL_SIZE, this.game.tick);
  }
}

public class MainGame {
  public static void main(String[] args) {

  }
}
