package ch.hsr.servicecutter.solver;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gephi.clustering.api.Cluster;
import org.gephi.clustering.spi.Clusterer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.jfree.util.Log;
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.hsr.servicecutter.model.solver.EntityPair;
import ch.hsr.servicecutter.model.solver.Service;
import ch.hsr.servicecutter.model.solver.SolverResult;
import ch.hsr.servicecutter.model.usersystem.Nanoentity;
import ch.hsr.servicecutter.model.usersystem.UserSystem;
import ch.hsr.servicecutter.scorer.Score;
import cz.cvut.fit.krizeji1.girvan_newman.GirvanNewmanClusterer;

public class GephiSolver extends AbstractSolver<Node, Edge> {

	private Map<String, Node> nodes;
	private UndirectedGraph undirectedGraph;
	private GraphModel graphModel;

	private Logger log = LoggerFactory.getLogger(GephiSolver.class);
	private Integer numberOfClusters;
	private char serviceIdGenerator = 'A';

	public GephiSolver(final UserSystem userSystem, final Map<EntityPair, Map<String, Score>> scores, final Integer numberOfClusters) {
		super(userSystem, scores);
		this.numberOfClusters = numberOfClusters;
		if (userSystem == null || userSystem.getNanoentities().isEmpty()) {
			throw new InvalidParameterException("invalid userSystem!");
		}

		nodes = new HashMap<>();

		graphModel = bootstrapGephi();
		undirectedGraph = graphModel.getUndirectedGraph();

		log.info("gephi solver created");
		buildNodes();
		buildEdges();

		log.info("final edges: ");
		for (Edge edge : undirectedGraph.getEdges()) {
			log.info("{}-{}: {}", edge.getSource().getNodeData().getLabel(), edge.getTarget().getNodeData().getLabel(), edge.getWeight());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SolverResult solve() {
		return solveWithGirvanNewman(numberOfClusters);
	}

	SolverResult solveWithGirvanNewman(final int numberOfClusters) {
		Log.debug("solve cluster with numberOfClusters = " + numberOfClusters);
		GirvanNewmanClusterer clusterer = new GirvanNewmanClusterer();
		clusterer.setPreferredNumberOfClusters(numberOfClusters);
		clusterer.execute(graphModel);
		SolverResult solverResult = new SolverResult(getClustererResult(clusterer));
		return solverResult;
	}

	// Returns a HashSet as the algorithms return redundant clusters
	private Set<Service> getClustererResult(final Clusterer clusterer) {
		Set<Service> result = new HashSet<>();
		if (clusterer.getClusters() != null) {
			for (Cluster cluster : clusterer.getClusters()) {
				List<String> nanoentities = new ArrayList<>();
				for (Node node : cluster.getNodes()) {
					nanoentities.add(node.toString());
				}
				Service boundedContext = new Service(nanoentities, serviceIdGenerator++);
				result.add(boundedContext);
				log.debug("BoundedContext found: {}, {}", boundedContext.getNanoentities().toString(), boundedContext.hashCode());
			}
		}
		return result;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Iterable<Edge> getEdges() {
		return undirectedGraph.getEdges();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Edge getEdge(final Nanoentity first, final Nanoentity second) {
		return undirectedGraph.getEdge(getNode(first), getNode(second));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void removeEdge(final Edge edge) {
		undirectedGraph.removeEdge(edge);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createEdgeAndSetWeight(final Nanoentity first, final Nanoentity second, final double weight) {
		Edge edge = graphModel.factory().newEdge(getNode(first), getNode(second), (float) weight, false);
		undirectedGraph.addEdge(edge);
		edge.getEdgeData().setLabel(edge.getWeight() + "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected double getWeight(final Edge edge) {
		return edge.getWeight();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setWeight(final Edge edge, final double weight) {
		edge.setWeight((float) weight);
		edge.getEdgeData().setLabel(edge.getWeight() + "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Node getNode(final String name) {
		return nodes.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createNode(final String name) {
		Node node = graphModel.factory().newNode(name);
		node.getNodeData().setLabel(name);
		undirectedGraph.addNode(node);
		nodes.put(name, node);
	}

	private GraphModel bootstrapGephi() {
		// boostrap gephi
		Lookup lookup = Lookup.getDefault();
		ProjectController pc = lookup.lookup(ProjectController.class);
		pc.newProject();
		@SuppressWarnings("unused")
		Workspace workspace = pc.getCurrentWorkspace();
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		return graphModel;
	}

}
