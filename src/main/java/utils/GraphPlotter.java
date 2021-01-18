package utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class GraphPlotter extends JFrame {
    private static final int panelWidth = 640;
    private static final int panelHeight = 480;
    private final JFreeChart chart;

    public static void main(String[] args) {
        GraphPlotter.GraphPlotterBuilder gpBuilder = new GraphPlotter.GraphPlotterBuilder()
                .setTitle("Delay")
                .setXAxisLabel("latency (ns)")
                .setYAxisLabel("delay (ns)");

        Map<Integer, Double> map = new HashMap<>();

        map.put(1,1300000d);
        map.put(10,1300000d);
        map.put(100,1300000d);
        map.put(1000,1300000d);
        map.put(10000,1300000d);
        map.put(100000,1300000d);
        map.put(1000000,2500000d);
        map.put(10000000,3000000d);
        map.put(30000000,6000000d);
        map.put(50000000,10000000d);
        map.put(100000000,19000000d);
        map.put(300000000,60000000d);
        map.put(500000000,92000000d);
        map.put(1000000000,200000000d);
        gpBuilder.addDoubleSeries(map, "< 16 000");
        map.clear();

        map.put(1,4900000d);
        map.put(10,5200000d);
        map.put(100,5500000d);
        map.put(1000,5200000d);
        map.put(10000,5200000d);
        map.put(100000,5200000d);
        map.put(1000000,7500000d);
        map.put(10000000,36000000d);
        map.put(30000000,95000000d);
        map.put(50000000,155000000d);
        map.put(100000000,305000000d);
        map.put(300000000,910000000d);
        map.put(500000000,1510000000d);
        map.put(1000000000,3025000000d);
        gpBuilder.addDoubleSeries(map, "< 6 300");
        map.clear();


        gpBuilder.build().setVisible(true);
    }

    private GraphPlotter(final String title, XYSeriesCollection dataset, String xAxisLabel, String yAxisLabel ) {
        super(title);

        chart = ChartFactory.createXYLineChart(
                title,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // sets paint color for each series
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesPaint(1, Color.GREEN);
        renderer.setSeriesPaint(2, Color.YELLOW);
        plot.setRenderer(renderer);

        add(new ChartPanel(chart), BorderLayout.CENTER);
        setSize(panelWidth, panelHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    public void saveAsPNGTo(OutputStream out) throws IOException {
        ChartUtilities.writeChartAsPNG(out, chart, panelWidth, panelHeight);
    }

    public static class GraphPlotterBuilder{
        private String title, xAxisLabel, yAxisLabel;
        XYSeriesCollection dataset;

        public GraphPlotterBuilder(){
            dataset = new XYSeriesCollection();
        }

        public GraphPlotterBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public GraphPlotterBuilder setXAxisLabel(String xAxisLabel) {
            this.xAxisLabel = xAxisLabel;
            return this;

        }

        public GraphPlotterBuilder setYAxisLabel(String yAxisLabel) {
            this.yAxisLabel = yAxisLabel;
            return this;
        }

        public GraphPlotterBuilder addDoubleSeries(Map<Integer,Double> data, String name){
            XYSeries series = new XYSeries(name);
            for( Integer key : data.keySet())
                series.add(key, data.get(key));
            dataset.addSeries(series);
            return this;
        }
        public GraphPlotterBuilder addLongSeries(Map<Integer,Long> data, String name){
            XYSeries series = new XYSeries(name);
            for( Integer key : data.keySet())
                series.add(key, data.get(key));
            dataset.addSeries(series);
            return this;
        }

        public GraphPlotterBuilder addIntegerSeries(Map<Integer,Integer> data, String name){
            XYSeries series = new XYSeries(name);
            for( Integer key : data.keySet())
                series.add(key, data.get(key));
            dataset.addSeries(series);
            return this;
        }

        public GraphPlotter build(){
            return new GraphPlotter(title, dataset, xAxisLabel, yAxisLabel);
        }
    }
}