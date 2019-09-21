package org.fog.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.fog.core.Config;
import org.fog.placement.algorithm.Algorithm;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Class which is responsible for plotting the map iteration-value of the evolutionary algorithms.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class MatlabChartUtils extends JFrame implements ChartMouseListener {
	private static final long serialVersionUID = 3349088359078668808L;
	
	Popup popup;
	
	Map<Map<Integer, Integer>, Double> valueIterMap;
	ChartPanel chartPanel;
	
	public MatlabChartUtils(final Algorithm al, final String title) {
		valueIterMap = al.getValueIterMap();
		
    	XYDataset dataset = createDataset();
        JFreeChart chart = createChart(dataset);
        chartPanel = new ChartPanel(chart);
        chartPanel.addChartMouseListener(this);
        
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        add(chartPanel);
        
        pack();
        setTitle(title);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }
	
    private XYDataset createDataset() {
    	XYSeriesCollection dataset = new XYSeriesCollection();
    	
    	for(int i  = 0; i < Config.NR_OBJECTIVES; i++) {
    		XYSeries series = new XYSeries(Config.objectiveNames[i]);
    		
    		for(Map<Integer, Integer> map : valueIterMap.keySet()) {
    			if(map.entrySet().iterator().next().getKey() != i) continue;
    			
    			int iter = map.entrySet().iterator().next().getValue();
    			double value = valueIterMap.get(map);
    			series.add((double) iter, value);
    		}
    		
    		dataset.addSeries(series);
    	}
    	
        return dataset;
    }
    
    private JFreeChart createChart(XYDataset dataset) {
    	
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Best value per iteration", 
                "Iteration", 
                "Value", 
                dataset, 
                PlotOrientation.VERTICAL,
                true, 
                true, 
                false 
        );
        
        XYPlot plot = chart.getXYPlot();
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        /*renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));*/
        
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);
        
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);
        
        chart.getLegend().setFrame(BlockBorder.NONE);
        
        chart.setTitle(new TextTitle("Best value per iteration", new Font("Serif", java.awt.Font.BOLD, 18)));
        
        return chart;
    }

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {
		// Ignore
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) {
		int x = event.getTrigger().getX(); 
		int y = event.getTrigger().getY();
		
		Point2D.Double point = checkPointClick(x, y);
		
		if(point != null) {
			if(popup != null) {
				popup.hide();
			}
			
			new PopUpToolTip(new Point(x, y), chartPanel, "Iter: " + (int) point.getX() + ", Value: " + String.format("%.10f", point.getY()) + "");
			popup.show();
		}else if(popup != null) {
			popup.hide();
		}
	}
	
	private Point2D.Double checkPointClick(int x, int y) {
		XYPlot plot = chartPanel.getChart().getXYPlot();
		Rectangle2D dataArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
		
		Point2D p = chartPanel.translateScreenToJava2D(new Point(x, y));
		x = (int) p.getX();
		y = (int) p.getY();
		
		for(Map<Integer, Integer> map : valueIterMap.keySet()) {
			int iter = map.entrySet().iterator().next().getValue();
			double value = valueIterMap.get(map);
			
			double xx = plot.getDomainAxis().valueToJava2D(iter, dataArea, plot.getDomainAxisEdge());
		    double yy = plot.getRangeAxis().valueToJava2D(value, dataArea, plot.getRangeAxisEdge());
		    
		    if(x <= xx + 6 && x >= xx - 6 && y <= yy + 6 && y >= yy - 6) {
		    	return new Point2D.Double(iter, value);
		    }
		}		
		return null;
	}
	
	class PopUpToolTip {
		PopUpToolTip(Point point, JComponent comp, String text) {
			comp.setToolTipText(text);
			
		    JToolTip toolTip = comp.createToolTip();
		    toolTip.setTipText(text);
		    
		    int x = point.x + comp.getLocationOnScreen().x;
		    int y = point.y + comp.getLocationOnScreen().y;
		    
		    popup = PopupFactory.getSharedInstance().getPopup(comp, toolTip, x + 2, y + 2);
		}
	}
    
}
