package graphwalk;

import java.util.*;
import java.io.*;

class Node {

    int id;
    int degree;

    Node(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
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

    @Override
    public int compareTo(Edge that) {
        return Integer.compare(this.src.id, that.src.id);
    }

    @Override
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
    Boolean directed;

    final void loadEdgeList(String file) throws FileNotFoundException, IOException {
        edgeMap = new HashMap<>();

        // an edge list file two columns (unweighted)
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;

        HashSet<String> nodes = new HashSet<>();

        while ((line = br.readLine()) != null) {
            Edge e = new Edge(line);
            edgeMap.put(e.toString(), e);
            String dest = line.split("\\s+")[0];
            if (!nodes.contains(dest)) {
                nodes.add(dest);
            }
        }

        /*Iterator it = nodes.iterator();
        while(it.hasNext()){
            String n = (String) it.next();
            Edge e = new  Edge("-1 "+ n);
            edgeMap.put("-1", e);
        }*/
        br.close();
        fr.close();
    }

    final void makeGraph() {

        List<Edge> edgeList = new ArrayList(edgeMap.values());

        nodePostings = new HashMap<>();
        NodeNeighbors np = null;

        for (Edge e : edgeList) {
            np = nodePostings.get(e.src.id);
            if (np == null) {
                np = new NodeNeighbors(e.src);
                nodePostings.put(e.src.id, np);
            }
            np.add(e.dest);
            if (!directed) {
                np = nodePostings.get(e.dest.id);
                if (np == null) {
                    np = new NodeNeighbors(e.dest);
                    nodePostings.put(e.dest.id, np);
                }
                np.add(e.src);
            }
        }

    }

    public Graph(String file, Boolean directed) throws IOException {
        this.file = file;
        this.directed = directed;
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
            i++;
        }
        return null; // should never be here
    }

    public Map<String, Edge> getEdgeMap() {
        return edgeMap;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (NodeNeighbors np : nodePostings.values()) {
            buff.append(String.format("%s <%d>: ", np.src, np.degree));
            for (Node adj : np.neighbors) {
                buff.append(adj).append(", ");
            }
            buff.append("\n");
        }
        return buff.toString();
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

    Set<Node>[] getTransitionGroup(Node t, Node v, int k, String rWalkMode) {
        Set<Node>[] tv_relgrp = null;
        String key = null;
        if (t != null) {
            Edge t_v = new Edge(t, v);
            key = t_v.toString();
            tv_relgrp = transitionGrps.get(key);
            if (tv_relgrp != null) {
                return tv_relgrp;
            }
        }
        // v:= e.src, k:=degree_threshold
        // If (t, v) \in E and d(x)>=k (degree threshold) then group in alpha
        tv_relgrp = new Set[3]; // 3 possible events 
        for (int i = 0; i < 3; i++) {
            tv_relgrp[i] = new HashSet<Node>();
        }
        Edge tx, xt;
        int graph_case = -1;
        NodeNeighbors neighbors = nodePostings.get(v.id);
        for (Node x : neighbors.neighbors) {
            NodeNeighbors l = nodePostings.get(x.id);
            if (t != null) {
                tx = new Edge(t, x);
                xt = new Edge(x, t);
                if (rWalkMode.equals("Biased_Random_Walk")) {
                    if (l.neighbors.size() >= k) {
                        if (edgeMap.get(tx.toString()) != null || edgeMap.get(xt.toString()) != null) {
                            graph_case = 0;
                        } else {
                            graph_case = 1;
                        }
                    } else {
                        graph_case = 2;
                    }
                }
                if (rWalkMode.equals("Node2vec")) {
                    if (x.id == t.id) {
                        graph_case = 0;
                    } else if (edgeMap.get(tx.toString()) == null && edgeMap.get(xt.toString()) == null) {
                        graph_case = 1;
                    } else if (edgeMap.get(tx.toString()) != null || edgeMap.get(xt.toString()) != null) {
                        graph_case = 2;
                    }
                }
                //System.out.println("graph case " + graph_case);
                tv_relgrp[graph_case].add(x);
                transitionGrps.put(key, tv_relgrp);
            } else {
                if (rWalkMode.equals("Biased_Random_Walk")) {
                    if (l.neighbors.size() >= k) {
                        graph_case = 0;
                    } else {
                        graph_case = 2;
                    }
                }
                if (rWalkMode.equals("Node2vec")) {
                    graph_case = 2;
                }
                tv_relgrp[graph_case].add(x);
            }
        }
        return tv_relgrp;
    }
}

public class RWalk {

    float alpha;
    float beta;
    int numSteps;
    int numWalks;
    Graph g;
    int k;
    TransitionGroups tg;
    String rWalkMode;

    RWalk(float alpha, float beta, int numSteps, int numWalks, int k, String rWalkMode) {
        this.alpha = alpha;
        this.beta = beta;
        System.out.println("Number of steps " + numSteps);
        this.numSteps = numSteps;
        this.numWalks = numWalks;
        System.out.println("Number of walks " + numWalks);
        this.k = k;
        this.rWalkMode = rWalkMode;
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

        return accumulatedWalkSeq;
    }

    void walk_r(Node t, Node v, List<Integer> accumulatedWalkSeq, int numStepsRemaining, float[] eventProbs) {
        if (numStepsRemaining == 0) {
            return;
        }

        // group neighbors x by 3 possible events
        Set<Node>[] eventGrps = tg.getTransitionGroup(t, v, k, rWalkMode);
        // sample an event
        Random r = new Random();
        float sampled_in_0_1 = r.nextFloat();
        int randomEvent = sampled_in_0_1 < eventProbs[0] ? 0 : sampled_in_0_1 < eventProbs[1] ? 1 : 2;

        //System.out.println(randomEvent + " " + sampled_in_0_1);
        // sample a neighbor from the group
        try {

            Set<Node> choiceSet = eventGrps[randomEvent];
            int elementToSelect = r.nextInt(choiceSet.size());

            Node x = null;

            int i = 0;
            for (Node n_i : choiceSet) {
                if (i == elementToSelect) {
                    x = n_i;
                    break;
                }
                i++;
            }

            // Now we have selected x. Add it and recurse into the next pair
            accumulatedWalkSeq.add(x.id);
            numStepsRemaining--;
            walk_r(v, x, accumulatedWalkSeq, numStepsRemaining, eventProbs);
        } catch (Exception e) {
            System.out.println("Wrong Choice" + v.id);
            walk_r(t, v, accumulatedWalkSeq, numStepsRemaining, eventProbs);
        }
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
        Iterator it = g.nodePostings.keySet().iterator();
        while (it.hasNext()) {
            Integer node_id = (Integer) it.next();
            Node t = g.nodePostings.get(node_id).src;
            System.out.println("Starting node " + t.id);
            // Select a node at random and start a walk
            for (int i = 0; i < numWalks; i++) {
                List<Integer> nodesequence = walk(t, numSteps, alpha, beta);
                buff.append(t.id).append(" ");
                for (int nodeid : nodesequence) {
                    buff.append(nodeid).append(" ");
                }
                buff.append("\n");
            }
        }
        return buff.toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 8) {
            // System.err.println("usage: java RWalk <edgelist file> <alpha> <beta> <walk-len> <num_walks> <k>");
            //return;
            args = new String[8];
            args[0] = "C:\\Users\\Procheta\\Downloads\\clique_graph_generator_code.tar\\clique_graph_generator_code/edge_file_100_1_0.1.txt";
            args[1] = "0.7";
            args[2] = "0.2";
            args[3] = "20";
            args[4] = "4";
            args[5] = "10";
            args[6] = "false";
            args[7] = "Biased_Random_Walk";
        }

        Graph g = new Graph(args[0], Boolean.parseBoolean(args[6]));
        System.out.println("Printing the Graph");
        //System.out.println(g.toString());

        RWalk rwalk = new RWalk(
                Float.parseFloat(args[1]),
                Float.parseFloat(args[2]),
                Integer.parseInt(args[3]),
                Integer.parseInt(args[4]),
                Integer.parseInt(args[5]), args[7]
        );
        rwalk.setGraph(g);

        rwalk.constructAndSaveWalks();
    }
}
