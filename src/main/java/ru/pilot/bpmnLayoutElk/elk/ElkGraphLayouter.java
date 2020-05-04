package ru.pilot.bpmnLayoutElk.elk;

import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

public class ElkGraphLayouter {

    /**
     * https://www.eclipse.org/elk/reference/algorithms.html
     */
    public void layout(ElkNode elkGraph){
        layeredProperty(elkGraph);
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        engine.layout(elkGraph, new BasicProgressMonitor());
    }

    private void layeredProperty(ElkNode elkGraph) {
        elkGraph.setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID);
        elkGraph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL);
        elkGraph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, 100.0);

        //new LayeredLayoutProvider().layout(elkGraph, new BasicProgressMonitor());
    }


}
