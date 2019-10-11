package org.fog.gui.core;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.fog.gui.Gui;
import org.fog.gui.dialog.AddFogDevice;
import org.fog.utils.FogUtils;
import org.fog.utils.movement.Location;

/**
 * Panel that displays a graph.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class GraphView extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int ARR_SIZE = 5;
	
	private JPanel canvas;
	
	/** Object which holds the topology */
	private Graph graph;
	
	/** Image which represents a fog device */
	private Image imgHost;
	
	/** Map which the nodes and it's correspondent coordinates */
	Map<Node, Location> coordFogNodes;
	
	/** The node icon height */
	private int size;
	
	/**
	 * Creates a new context to display the topology.
	 * 
	 * @param graph the topology
	 */
	public GraphView(final Graph graph) {
		this.graph = graph;
		
		coordFogNodes = new HashMap<Node, Location>();
		imgHost = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/fog.png"));
		
		initComponents();
	}
	
	/**
	 * Draws the topology (i.e., nodes, links and applications).
	 */
	@SuppressWarnings("serial")
	private void initComponents() {
		canvas = new JPanel() {
			@Override
			public void paint(Graphics g) {
				if(graph.getDevicesList() == null) return;
				
				Map<Integer, List<Node>> levelMap = new HashMap<Integer, List<Node>>();

				FontMetrics f = g.getFontMetrics();

				int nodeHeight = Math.max(40, f.getHeight());
				int nodeWidth = size = nodeHeight;
				int maxLevel = -1;
				int minLevel = FogUtils.MAX;
				
				for (Node node : graph.getDevicesList().keySet()) {
					int level = node.getLevel();
					
					if(!levelMap.containsKey(level))
						levelMap.put(level, new ArrayList<Node>());
					
					levelMap.get(level).add(node);
					
					if(level > maxLevel)
						maxLevel = level;
					
					if(level < minLevel)
						minLevel = level;
				}
				
				double yDist = canvas.getHeight()/(maxLevel-minLevel+3);
				
				Map<Integer, List<PlaceHolder>> levelToPlaceHolderMap = new HashMap<Integer, List<PlaceHolder>>();
				
				int k = 1;
				for(int i = minLevel; i <= maxLevel; i++, k++){
					double xDist = canvas.getWidth()/(levelMap.get(i).size()+1);
					
					for(int j=1; j <= levelMap.get(i).size(); j++){
						int x = (int)xDist*j;
						int y = (int)yDist*k;
						
						if(!levelToPlaceHolderMap.containsKey(i))
							levelToPlaceHolderMap.put(i, new ArrayList<PlaceHolder>());
						
						levelToPlaceHolderMap.get(i).add(new PlaceHolder(x, y));
					}
				}
				
				coordFogNodes = getCoordForNodes(levelToPlaceHolderMap, levelMap, minLevel, maxLevel);
				
				// Draw nodes and it's applications
				Map<Node, List<Node>> drawnList = new HashMap<Node, List<Node>>();
				for (Entry<Node, Location> entry : coordFogNodes.entrySet()) {
					g.setColor(Color.black);
					
					Location wrapper = entry.getValue();
					String nodeName = entry.getKey().getName();
				
					g.drawImage(imgHost, (int)(wrapper.getX() - nodeWidth / 2), (int)(wrapper.getY() - nodeHeight / 2), nodeWidth, nodeHeight, this);
					g.drawString(nodeName, (int)(wrapper.getX() - f.stringWidth(nodeName) / 2), (int)(wrapper.getY() + nodeHeight));
					
					String appName = entry.getKey().getApplication();
					if(!appName.equals(""))
						g.drawString("[" + appName + "]", (int)(wrapper.getX() - f.stringWidth("[" + appName + "]") / 2), (int)(wrapper.getY() + nodeHeight + g.getFont().getSize()));
				}
				
				// Draw links
				for (Entry<Node, List<Link>> entry : graph.getDevicesList().entrySet()) {
					Location startNode = coordFogNodes.get(entry.getKey());

					for (Link edge : entry.getValue()) {
						Location targetNode = coordFogNodes.get(edge.getNode());
						g.setColor(Color.RED);
						drawArrow(g, (int)startNode.getX(), (int)startNode.getY(), (int)targetNode.getX(), (int)targetNode.getY());

						if (drawnList.containsKey(entry.getKey()))
							drawnList.get(entry.getKey()).add(edge.getNode());
						else {
							List<Node> nodes = new ArrayList<Node>();
							nodes.add(edge.getNode());
							drawnList.put(entry.getKey(), nodes);
						}
					}
				}
			}
		};
		
		JScrollPane scrollPane = new JScrollPane(canvas);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(scrollPane);
	}
	
	/**
	 * Gets the coordinates of the nodes to be drawn.
	 * 
	 * @param levelToPlaceHolderMap the map with levels and the corresponding place holders
	 * @param levelMap the map with levels and the corresponding nodes
	 * @param minLevel the minimum level of the nodes
	 * @param maxLevel the maximum level of the nodes
	 * @return the map which contains the nodes and the corresponding coordinates
	 */
	protected Map<Node, Location> getCoordForNodes(Map<Integer, List<PlaceHolder>> levelToPlaceHolderMap, Map<Integer, List<Node>> levelMap,
			int minLevel, int maxLevel) {

		Map<Node, Location> coordForNodesMap = new HashMap<Node, Location>();
		
		if(maxLevel < 0)
			return new HashMap<Node, Location>();

		int i = 0;
		for(int level = minLevel; level <= maxLevel; level++){
			i = 0;
			for(PlaceHolder placeHolder : levelToPlaceHolderMap.get(level)){
				Node node = levelMap.get(level).get(i++);
				placeHolder.setNode(node);
				node.setCoordinate(placeHolder.getCoordinates());
				coordForNodesMap.put(node, node.getCoordinate());
			}
		}
		return coordForNodesMap;
	}
	
	/**
	 * Draws an arrow from a given point to another.
	 * 
	 * @param g1 the context
	 * @param x1 the x coordinate of the first point
	 * @param y1 the y coordinate of the first point
	 * @param x2 the x coordinate of the second point
	 * @param y2 the y coordinate of the second point
	 */
	private void drawArrow(Graphics g1, int x1, int y1, int x2, int y2) {
		Graphics2D g = (Graphics2D) g1.create();
		double dx = x2 - x1, dy = y2 - y1;
		double angle = Math.atan2(dy, dx);
		int len = (int) Math.sqrt(dx * dx + dy * dy);
		AffineTransform at = AffineTransform.getTranslateInstance(x1, y1);
		at.concatenate(AffineTransform.getRotateInstance(angle));
		g.transform(at);

		// Draw horizontal arrow starting in (0, 0)
		QuadCurve2D.Double curve = new QuadCurve2D.Double(0,0,50+0.5*len,50,len,0);
		g.draw(curve);
		g.fillPolygon(new int[] { len, len - ARR_SIZE, len - ARR_SIZE, len }, new int[] { 0, -ARR_SIZE, ARR_SIZE, 0 }, 4);
		g.fillPolygon(new int[] { 0, 0 - ARR_SIZE, 0 - ARR_SIZE, 0 }, new int[] { -ARR_SIZE, 0, 0, ARR_SIZE}, 4);
	}
	
	/**
	 * Sets the new topology.
	 * 
	 * @param newGraph object which holds the new topology
	 */
	public void setGraph(Graph newGraph){
		this.graph = newGraph;
	}
	
	/**
	 * Opens the device drawn in a given point details to allow editing it.
	 * 
	 * @param frame the context
	 * @param physicalCanvas the panel that displays a graph
	 * @param physicalGraph the object which holds the topology
	 * @param point the point where the click was made
	 */
	public void openDeviceDetails(final JFrame frame, GraphView physicalCanvas, Graph physicalGraph, java.awt.Point point) {
		for (Entry<Node, Location> entry : coordFogNodes.entrySet()) {
			Location wrapper = entry.getValue();
			if(wrapper.getX() - size/2 <= point.getX() && wrapper.getX() + size/2 >= point.getX() &&
					wrapper.getY() - size/2 <= point.getY() && wrapper.getY() + size/2 >= point.getY()) {
				new AddFogDevice(physicalGraph, frame, entry.getKey());
		    	physicalCanvas.repaint();
				break;
			}
		}
		Gui.verifyRun();
	}
}
