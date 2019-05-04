package org.fog.placement.algorithms.placement.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.fog.placement.algorithms.placement.Algorithm;
import org.jfree.chart.ChartFactory;
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

public class MatlabChartUtils extends JFrame {
	private static final long serialVersionUID = 3349088359078668808L;
	
	public MatlabChartUtils(Algorithm al, String title) {
    	XYDataset dataset = createDataset(al.getValueIterMap());
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        add(chartPanel);
        
        pack();
        setTitle(title);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }
	
    private XYDataset createDataset(Map<Integer, Double> map) {
        XYSeries series = new XYSeries("BestValue");
		double best = Short.MAX_VALUE;
		
		for(Integer iter : map.keySet()) {			
			double value = map.get(iter);
			
			if(best > value) {
				best = value;
				series.add((double) iter, map.get(iter));
			}
		}
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        
        return dataset;
    }
    
    private JFreeChart createChart(XYDataset dataset) {
    	
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Best value per iteration", 
                "Best", 
                "Iteration", 
                dataset, 
                PlotOrientation.VERTICAL,
                true, 
                true, 
                false 
        );
        
        XYPlot plot = chart.getXYPlot();
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));
        
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
}
