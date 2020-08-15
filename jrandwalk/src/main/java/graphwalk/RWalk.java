package graphwalk;

import java.util.*;
import java.io.*;

class Node {

    int id;
    int degree;

    Node(int id) {
        this.id = id;
    }
}

class Edge implements Comparable<Edge> {

    Node src;
    Node dest;

    Edge(String line) {
        String[] tokens = line.split("\\s+");
        src = new Node(Integer.parseInt(tokens[0]));
        dest = new Node(Integer.parseInt(tokens[1]));
    }

    Edge(Node src, Node dest) {
        this.src = src;
        this.dest = dest;
    }

    public int compareTo(Edge that) {
        return Integer.compare(this.src.id, that.src.id);
    }

    public String toString() {
        return src.id + ":" + dest.id;
    }
}

class NodeNeighbors {

    Node src;
    int degree;
    List<Node> neighbors;

    NodeNeighbors(Node src) {
        this.src = src;
        neighbors = new ArrayList<>();
        degree = 0;
    }

    void add(Node dst) {
        neighbors.add(dst);
        degree++;
    }
}

class Graph {

    String file;
    Map<Integer, NodeNeighbors> nodePostings;
    Map<String, Edge> edgeMap;
    static Random r = new Random();

    void loadEdgeList(String file) throws FileNotFoundException, IOException {
        edgeMap = new HashMap<>();

        // an edge list file two columns (unweighted)
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;

        while ((line = br.readLine()) != null) {
            Edge e = new Edge(line);
            edgeMap.put(e.toString(), e);
        }

        br.close();
        fr.close();
    }

    void makeGraph() {

        List<Edge> edgeList = (List<Edge>) edgeMap.values();
        Collections.sort(edgeList); // ensures that all edges with the same src are consecutive

        nodePostings = new HashMap<>();
        int prev_src_id = -1; // impossible value to start with
        NodeNeighbors np = null;
        for (Edge e : edgeList) {
            if (prev_src_id != e.src.id) { // a new source node encountered
                if (prev_src_id != -1) {
                    nodePostings.put(prev_src_id, np);
                }
            }
            np = nodePostings.get(e.src.id);
            if (np == null) {
                np = new NodeNeighbors(e.src);
            }
            if (prev_src_id != -1) {
                np.add(e.dest);
            }
            prev_src_id = e.src.id;
        }
    }

    public Graph(String file) throws IOException {
        this.file = file;
        loadEdgeList(file);
        makeGraph();
    }

    Node getRandomNode() {
        int randomChoiceIndex = r.nextInt(nodePostings.size());
        int i = 0;
        for (Integer x_i : nodePostings.keySet()) {
            if (i == randomChoiceIndex) {
                return nodePostings.get(x_i).src;
            }
        }
        return null; // should never be here
    }

    public Map<String, Edge> getEdgeMap() {
        return edgeMap;
    }
}

class TransitionGroups {

    Map<String, Set<Node>[]> transitionGrps;
    Map<String, Edge> edgeMap;
    Map<Integer, NodeNeighbors> nodePostings;

    TransitionGroups(Graph g) {
        this.edgeMap = g.getEdgeMap();
        transitionGrps = new HashMap<>();
        this.nodePostings = g.nodePostings;
    }

    Set<Node>[] getTransitionGroup(Node t, Node v, int k) {
        String key = new Edge(t,v).toString();
        Set<Node>[] tv_relgrp = transitionGrps.get(key);
        if (tv_relgrp != null) {
            return tv_relgrp;
        }

        // v:= e.src, k:=degree_threshold
        // If (t, v) \in E and d(x)>=k (degree threshold) then group in alpha
        tv_relgrp = new Set[3]; // 3 possible events 
        for (int i = 0; i < 3; i++) {
            tv_relgrp[i] = new HashSet<Node>();
        }
        edgeMap.get(key);
        Edge tx, xt;
        int graph_case = -1;
        NodeNeighbors neighbors = nodePostings.get(t.id);
        for (Node x : neighbors.neighbors) {
            tx = new Edge(t, x);
            xt = new Edge(x, t);
            if (x.degree >= k) {
                if (edgeMap.get(tx) != null || edgeMap.get(xt) != null) {
                    graph_case = 0;
                } else {
                    graph_case = 1;
                }
            } else {
                graph_case = 2;
            }
            tv_relgrp[graph_case].add(x);
        }
        transitionGrps.put(key, tv_relgrp);
        return tv_relgrp;
    }
}


public class RWalk {

    float alpha;
    float beta;
    int numSteps;
    int numWalks;
    Graph g;
    TransitionGroups tg;

    RWalk(float alpha, float beta, int numSteps, int numWalks) {
        this.alpha = alpha;
        this.beta = beta;
        this.numSteps = numSteps;
        this.numWalks = numWalks;
    }

    void setGraph(Graph g) {
        this.g = g;
        tg = new TransitionGroups(g);
    }

    List<Integer> walk(Node src, int steps, float alpha, float beta) {
        List<Integer> accumulatedWalkSeq = new ArrayList<>();
        float[] eventProbs = new float[2];
        eventProbs[0] = alpha;
        eventProbs[1] = alpha + beta;
        walk_r(null, src, accumulatedWalkSeq, steps, eventProbs);             
         return null;
    }

    void walk_r(Node t, Node v, List<Integer> accumulatedWalkSeq, int numStepsRemaining, float[] eventProbs) {
        if (numStepsRemaining == 0) {
            return;
        }

        // group neighbors x by 3 possible events
        NodeNeighbors v_info = g.nodePostings.get(v.id);
        String key = new Edge(t, v).toString();
        Set<Node>[] eventGrps = tg.transitionGrps.get(key);

        // sample an event
        Random r = new Random();
        float sampled_in_0_1 = r.nextFloat();
        int randomEvent = sampled_in_0_1 < eventProbs[0] ? 0 : sampled_in_0_1 < eventProbs[1] ? 1 : 2;

        // sample a neighbor from the group
        Set<Node> choiceSet = eventGrps[randomEvent];
        int elementToSelect = r.nextInt(choiceSet.size());
        Node x = null;

        int i = 0;
        for (Node n_i : choiceSet) {
            if (i == elementToSelect) {
                x = n_i;
            }
            i++;
        }
        // Now we have selected x. Add it and recurse into the next pair
        accumulatedWalkSeq.add(x.id);
        numStepsRemaining--;
        walk_r(v, x, accumulatedWalkSeq, numStepsRemaining, eventProbs);
    }

    void constructAndSaveWalks() throws IOException {
        FileWriter fw = new FileWriter(g.file + ".walk");
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(constructAndSaveWalkFromRandomSrc());
        bw.newLine();
        bw.close();
        fw.close();
    }

    String constructAndSaveWalkFromRandomSrc() {

        StringBuffer buff = new StringBuffer();

        // Select a node at random and start a walk
        Node t = null;
                //getRandomNode();
        List<Integer> nodesequence = walk(t, numSteps, alpha, beta);
        for (int nodeid : nodesequence) {
            buff.append(nodeid).append(" ");
        }
        return buff.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 5) {
            System.err.println("usage: java RWalk <edgelist file> <alpha> <beta> <walk-len> <num_walks>");
            return;
        }

        Graph g = new Graph(args[0]);
        RWalk rwalk = new RWalk(
                Float.parseFloat(args[1]),
                Float.parseFloat(args[2]),
                Integer.parseInt(args[3]),
                Integer.parseInt(args[4])
        );
        rwalk.setGraph(g);

        rwalk.constructAndSaveWalks();
    }
}
