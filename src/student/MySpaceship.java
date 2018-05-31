package student;

import controllers.Spaceship;
import models.Edge;
import models.Node;
import models.NodeStatus;
import student.Paths.SF;
import controllers.SearchPhase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import controllers.RescuePhase;

/** An instance implements the methods needed to complete the mission. */
public class MySpaceship implements Spaceship {

	/** The spaceship is on the location given by parameter state.
	 * Move the spaceship to Planet X and then return (with the spaceship is on
	 * Planet X). This completes the first phase of the mission.
	 * 
	 * If the spaceship continues to move after reaching Planet X, rather than
	 * returning, it will not count. If you return from this procedure while
	 * not on Planet X, it will count as a failure.
	 *
	 * There is no limit to how many steps you can take, but your score is
	 * directly related to how long it takes you to find Planet X.
	 *
	 * At every step, you know only the current planet's ID, the IDs of
	 * neighboring planets, and the strength of the signal from Planet X at
	 * each planet.
	 *
	 * In this rescuePhase,
	 * (1) In order to get information about the current state, use functions
	 * currentID(), neighbors(), and signal().
	 *
	 * (2) Use method onPlanetX() to know if you are on Planet X.
	 *
	 * (3) Use method moveTo(int id) to move to a neighboring planet with the
	 * given ID. Doing this will change state to reflect your new position.
	 */

	long startTime= System.nanoTime(); // start time of rescue phase


	@Override
	public void search(SearchPhase state) {
		// TODO: Find the missing spaceship
		HashSet<Integer> visited = new HashSet<Integer>();
		searchHelperOpt(state, visited);
	}

	/**find planetX without any optimization*/
	public boolean searchHelper(SearchPhase state, HashSet<Integer> visitedSet) {
		visitedSet.add(state.currentID());
		int id = state.currentID();
		for(NodeStatus neighbor: state.neighbors())  {
			if(state.onPlanetX()) {
				return true;
			} else {
				if(!visitedSet.contains(neighbor.id())) {
					state.moveTo(neighbor.id());
					boolean foundPlanetX = searchHelper(state, visitedSet);
					if(foundPlanetX) {
						return true;
					}
					state.moveTo(id);
				}
			}
		}
		return false;
	}


	/**find planetX with optimization*/
	public boolean searchHelperOpt(SearchPhase state, HashSet<Integer> visitedSet) {
		visitedSet.add(state.currentID());
		int id = state.currentID();
		NodeStatus[] sortedNeighbor = state.neighbors(); 
		//sort the array of neighbors of the planet
		Arrays.sort(sortedNeighbor, new SortByPing());

		for(NodeStatus neighbor: sortedNeighbor)  {
			if(state.onPlanetX()) {
				return true;
			} else {
				if(!visitedSet.contains(neighbor.id())) {
					state.moveTo(neighbor.id());
					boolean foundPlanetX = searchHelperOpt(state, visitedSet);
					if(foundPlanetX) {
						return true;
					}
					state.moveTo(id);
				}
			}
		}
		return false;
	}


	/** The spaceship is on the location given by state. Get back to Earth
	 * without running out of fuel and return while on Earth. Your ship can
	 * determine how much fuel it has left via method fuelRemaining().
	 * 
	 * In addition, each Planet has some gems. Passing over a Planet will
	 * automatically collect any gems it carries, which will increase your
	 * score; your objective is to return to earth successfully with as many
	 * gems as possible.
	 * 
	 * You now have access to the entire underlying graph, which can be accessed
	 * through parameter state. Functions currentNode() and earth() return Node
	 * objects of interest, and nodes() returns a collection of all nodes on the
	 * graph.
	 *
	 * Note: Use moveTo() to move to a destination node adjacent to your current
	 * node. */
	@Override
	public void rescue(RescuePhase state) {
		// TODO: Complete the rescue mission and collect gems
		HashSet<Node> visitedSet = new HashSet<Node>();
		rescueHelperOpt(state, visitedSet);
		//rescueHelperLongest(state, visitedSet);
	}

	/**go back to Earth along the shortest path*/
	public void rescueHelper(RescuePhase state) {
		List<Node> s = Paths.minPath(state.currentNode(), state.earth());
		for (Node n: s) {
			if (!n.equals(state.currentNode())) {
				state.moveTo(n);
			}
		}
	}

	/**go to the neighbor with the most gems if the fuel is enough, or go back to Earth along the shortest path*/
	public void rescueHelperOpt(RescuePhase state, HashSet<Node> visitedSet) {
		//add the current node into the hashMap
		visitedSet.add(state.currentNode());
		//sort the neighbors of the current node in descending gems numbers
		Set<Node> nodePlanets = state.currentNode().neighbors().keySet();
		List<Node> nodeByGems = new ArrayList(nodePlanets);
		Collections.sort(nodeByGems, new SortByGem()) ;

		//find the shortest path from the intending node to earth
		List<Node> shortestPath = Paths.minPath(nodeByGems.get(0), state.earth());
		List<Node> singleEdge = Paths.minPath(state.currentNode(), nodeByGems.get(0));

		//lengthOfShortest is the length of the edge between current node and the intending node plus
		//the length of the shortest path from intending node to earth
		int lengthOfShortest = Paths.pathWeight(shortestPath) + Paths.pathWeight(singleEdge);

		//if there is enough fuel then always go to the planet that has most gems
		while(lengthOfShortest <= state.fuelRemaining()) {
			int index = 0;
			while (index <= nodeByGems.size()-1 && visitedSet.contains(nodeByGems.get(index)) ) {
				index = index + 1;
			}

			//if all the neighbors are visited then go along the shortest path to earth until there is a node with unvisited neighbor
			if(index > nodeByGems.size()-1) {
				if (!state.currentNode().equals(state.earth())) {
					List<Node> s = Paths.minPath(state.currentNode(), state.earth());
					int current = 1;
					state.moveTo(s.get(current));
					int sum = 0;
					for (Node next: s.get(current).neighbors().keySet()) {
						if (!visitedSet.contains(next)){
							sum = sum + 1;
						}
					}
					while(sum == 0) {	
						current = current +1;
						if (!state.currentNode().equals(state.earth())) {
							state.moveTo(s.get(current));
							if (!visitedSet.contains(s.get(current))) {
								visitedSet.add(s.get(current));
							}
							for (Node next: s.get(current).neighbors().keySet()) {
								if (!visitedSet.contains(next)){
									sum = sum + 1;
								}
							}
						}
						else {
							return;
						}
					}
					nodePlanets = state.currentNode().neighbors().keySet();
					nodeByGems = new ArrayList(nodePlanets);
					Collections.sort(nodeByGems, new SortByGem()) ;
					shortestPath = Paths.minPath(nodeByGems.get(0), state.earth());
					singleEdge = Paths.minPath(state.currentNode(), nodeByGems.get(0));
					lengthOfShortest = Paths.pathWeight(shortestPath) + Paths.pathWeight(singleEdge);
				}
				//if the planet is earth then stop
				else {
					return;
				}
			}
			else {
				state.moveTo(nodeByGems.get(index));
				visitedSet.add(nodeByGems.get(index));
				nodePlanets = state.currentNode().neighbors().keySet();
				nodeByGems = new ArrayList(nodePlanets);
				Collections.sort(nodeByGems, new SortByGem()) ;
				shortestPath = Paths.minPath(nodeByGems.get(0), state.earth());
				singleEdge = Paths.minPath(state.currentNode(), nodeByGems.get(0));
				lengthOfShortest = Paths.pathWeight(shortestPath) + Paths.pathWeight(singleEdge);
			}
		}
		long totalTime= System.nanoTime() - startTime;
		if (totalTime > 20 * 1000000000.0) { 
			rescueHelper(state);
			return;
		}
		//if the fuel is not enough to go to the intending planet, then take the shortest path and go home
		rescueHelper(state);
	}


	public void rescueHelperLongest(RescuePhase state, HashSet<Node> visitedSet) {
		//add the current node into the hashMap
		visitedSet.add(state.currentNode());
		//sort the neighbors of the current node in descending gems numbers
		Set<Node> nodePlanets = state.currentNode().neighbors().keySet();
		List<Node> nodeByGems = new ArrayList(nodePlanets);
		

		int bsfpathToEarth = Paths.pathWeight(Paths.minPath(nodeByGems.get(0), state.earth()));
		Node next = nodeByGems.get(0);
		for(Node n : nodeByGems) {
			int pathToEarth = Paths.pathWeight(Paths.minPath(n, state.earth()));
			if (pathToEarth > bsfpathToEarth) {
				bsfpathToEarth = pathToEarth;
				next = n;
			}
		}
		while (bsfpathToEarth + state.currentNode().getEdge(next).fuelNeeded() <= state.fuelRemaining()) {
			state.moveTo(next);
			visitedSet.add(next);
			nodePlanets = state.currentNode().neighbors().keySet();
			nodeByGems = new ArrayList(nodePlanets);
			bsfpathToEarth = Paths.pathWeight(Paths.minPath(nodeByGems.get(0), state.earth()));
			next = nodeByGems.get(0);
			boolean hasUnvisited = false;
			for(Node n : nodeByGems) {
				if (!visitedSet.contains(n)) {
					int pathToEarth = Paths.pathWeight(Paths.minPath(n, state.earth()));
					hasUnvisited = true;
					if (pathToEarth > bsfpathToEarth) {
						bsfpathToEarth = pathToEarth;
						next = n;
					}
				}
			}
			if (!hasUnvisited) {
				rescueHelperOpt(state, visitedSet);
				return;
			}
		}
		rescueHelper(state);
	}
	


	class SortByGem implements Comparator<Node> {
		public int compare(Node a, Node b) {
			if ( a.gems() > b.gems() ) return -1;
			else if ( a.gems() == b.gems() ) return 0;
			else return 1;
		}
	}

	class SortByPing implements Comparator<NodeStatus> {
		public int compare(NodeStatus a, NodeStatus b) {
			if ( a.signal() > b.signal() ) return -1;
			else if ( a.signal() == b.signal() ) return 0;
			else return 1;
		}
	}

}


