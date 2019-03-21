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
import org.fog.gui.dialog.AddActuator;
import org.fog.gui.dialog.AddFogDevice;
import org.fog.gui.dialog.AddSensor;
import org.fog.utils.Config;
import org.fog.utils.FogUtils;

/** Panel that displays a graph */
public class GraphView extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int ARR_SIZE = 5;

	private JPanel canvas;
	private Graph graph;

	private Image imgApp;
	private Image imgHost;
	private Image imgSensor;
	private Image imgActuator;

	Map<Node, Coordinates> coordFogNodes = new HashMap<Node, Coordinates>();
	private int size;
	
	public GraphView(final Graph graph) {
		this.graph = graph;
		imgHost = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/fog.png"));
		imgApp = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/app.png"));
		imgSensor = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/sensor.png"));
		imgActuator = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/actuator.png"));
		
		initComponents();
	}
	
	@SuppressWarnings("serial")
	private void initComponents() {
		canvas = new JPanel() {
			@Override
			public void paint(Graphics g) {
				if(graph.getDevicesList() == null) return;

				Map<Node, Coordinates> coordForNodes = new HashMap<Node, Coordinates>();
				Map<Integer, List<Node>> levelMap = new HashMap<Integer, List<Node>>();
				List<Node> endpoints = new ArrayList<Node>(); 

				FontMetrics f = g.getFontMetrics();

				int nodeHeight = Math.max(40, f.getHeight());
				int nodeWidth = size = nodeHeight;
				int maxLevel = -1;
				int minLevel = FogUtils.MAX;
				
				for (Node node : graph.getDevicesList().keySet()) {
					if(node.getType().equals(Config.FOG_TYPE)){
						int level = ((FogDeviceGui)node).getLevel();
						
						if(!levelMap.containsKey(level))
							levelMap.put(level, new ArrayList<Node>());
						
						levelMap.get(level).add(node);
						
						if(level > maxLevel)
							maxLevel = level;
						
						if(level < minLevel)
							minLevel = level;
					
					} else if(node.getType().equals(Config.SENSOR_TYPE) || node.getType().equals(Config.ACTUATOR_TYPE))
						endpoints.add(node);
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
				
				List<PlaceHolder> endpointPlaceHolders = new ArrayList<PlaceHolder>();
				
				double xDist = canvas.getWidth()/(endpoints.size()+1);
				for(int i=0; i < endpoints.size(); i++){
					Node node = endpoints.get(i);
					int x = (int)xDist*(i+1);
					int y = (int)yDist*k;
					
					endpointPlaceHolders.add(new PlaceHolder(x, y));
					
					coordForNodes.put(node, new Coordinates(x, y));
					node.setCoordinate(new Coordinates(x, y));
				}
				
				coordForNodes = getCoordForNodes(levelToPlaceHolderMap, endpointPlaceHolders, levelMap, endpoints, minLevel, maxLevel);
				coordFogNodes = coordForNodes;
				
				Map<Node, List<Node>> drawnList = new HashMap<Node, List<Node>>();
				for (Entry<Node, Coordinates> entry : coordForNodes.entrySet()) {
					g.setColor(Color.white);
					
					Coordinates wrapper = entry.getValue();
					String nodeName = entry.getKey().getName();
					switch(entry.getKey().getType()){
						case "APP_MODULE":
							g.drawImage(imgApp, wrapper.getX() - nodeWidth / 2, wrapper.getY() - nodeHeight / 2, nodeWidth, nodeHeight, this);
							g.drawString(nodeName, wrapper.getX() - f.stringWidth(nodeName) / 2, wrapper.getY() + nodeHeight);
							break;
						case Config.FOG_TYPE:
							g.drawImage(imgHost, wrapper.getX() - nodeWidth / 2, wrapper.getY() - nodeHeight / 2, nodeWidth, nodeHeight, this);
							g.drawString(nodeName, wrapper.getX() - f.stringWidth(nodeName) / 2, wrapper.getY() + nodeHeight);
							
							String appName = ((FogDeviceGui)entry.getKey()).getApplication();
							if(!appName.equals(""))
								g.drawString("[" + appName + "]", wrapper.getX() - f.stringWidth("[" + appName + "]") / 2, wrapper.getY() + nodeHeight + g.getFont().getSize());
							break;
						case Config.SENSOR_TYPE:
							g.drawImage(imgSensor, wrapper.getX() - nodeWidth / 2, wrapper.getY() - nodeHeight / 2, nodeWidth, nodeHeight, this);
							g.drawString(nodeName, wrapper.getX() - f.stringWidth(nodeName) / 2, wrapper.getY() + nodeHeight);
							break;
						case Config.ACTUATOR_TYPE:
							g.drawImage(imgActuator, wrapper.getX() - nodeWidth / 2, wrapper.getY() - nodeHeight / 2, nodeWidth, nodeHeight, this);
							g.drawString(nodeName, wrapper.getX() - f.stringWidth(nodeName) / 2, wrapper.getY() + nodeHeight);
							break;
					}
				}

				for (Entry<Node, List<Edge>> entry : graph.getDevicesList().entrySet()) {
					Coordinates startNode = coordForNodes.get(entry.getKey());

					for (Edge edge : entry.getValue()) {
						Coordinates targetNode = coordForNodes.get(edge.getNode());
						g.setColor(Color.RED);
						drawArrow(g, startNode.getX(), startNode.getY(), targetNode.getX(), targetNode.getY());

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

	protected Map<Node, Coordinates> getCoordForNodes(Map<Integer, List<PlaceHolder>> levelToPlaceHolderMap,
			List<PlaceHolder> endpointPlaceHolders, Map<Integer, List<Node>> levelMap, List<Node> endpoints,
			int minLevel, int maxLevel) {

		Map<Node, Coordinates> coordForNodesMap = new HashMap<Node, Coordinates>();
		
		for(Node node : graph.getDevicesList().keySet())node.setPlaced(false);
		for(Node node : endpoints)node.setPlaced(false);
		
		if(maxLevel < 0)
			return new HashMap<Node, Coordinates>();

		int i = 0;
		for(int level = minLevel; level <= maxLevel; level++){
			i = 0;
			for(PlaceHolder placeHolder : levelToPlaceHolderMap.get(level)){
				Node node = levelMap.get(level).get(i++);
				placeHolder.setNode(node);
				node.setCoordinate(placeHolder.getCoordinates());
				coordForNodesMap.put(node, node.getCoordinate());
				node.setPlaced(true);
			}
		}
		
		i = 0;
		for(Node node : endpoints){
			if(!node.isPlaced()){
				PlaceHolder placeHolder = endpointPlaceHolders.get(i++);
				placeHolder.setOccupied(true);
				placeHolder.setNode(node);
				node.setCoordinate(placeHolder.getCoordinates());
				coordForNodesMap.put(node, node.getCoordinate());
				node.setPlaced(true);
			}
		}
		return coordForNodesMap;
	}

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
	
	public void setGraph(Graph newGraph){
		this.graph = newGraph;
	}
	
	public void openDeviceDetails(final JFrame frame, GraphView physicalCanvas, Graph physicalGraph, java.awt.Point point) {
		for (Entry<Node, Coordinates> entry : coordFogNodes.entrySet()) {
			Coordinates wrapper = entry.getValue();
			if(wrapper.getX() - size/2 <= point.getX() && wrapper.getX() + size/2 >= point.getX() &&
					wrapper.getY() - size/2 <= point.getY() && wrapper.getY() + size/2 >= point.getY()) {
				switch(entry.getKey().getType()){
					case Config.FOG_TYPE:
						new AddFogDevice(physicalGraph, frame, (FogDeviceGui)entry.getKey());
				    	physicalCanvas.repaint();
						break;
					case Config.SENSOR_TYPE:
						new AddSensor(physicalGraph, frame, (SensorGui)entry.getKey());
						physicalCanvas.repaint();
						break;
					case Config.ACTUATOR_TYPE:
						new AddActuator(physicalGraph, frame, (ActuatorGui)entry.getKey());
						physicalCanvas.repaint();
						break;
				}
			}
		}
		Gui.verifyRun();
	}
}
