package ru.pilot.bpmnLayoutElk.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.DiagramElement;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.util.Pair;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.springframework.util.CollectionUtils;

public class GraphConverter {

    /**
     * Создание графа по BPMN модели
     * @param model bpmn модель
     * @param linkedMap клююч - ID элемента, значение - пара ELK-BPMN элементы
     * @return структура для сохранения связи элементов модели BPMN и графа ELK
     */
    public ElkNode elkGraphFromCamundaModel(BpmnModelInstance model, Map<String, Pair<ElkGraphElement, DiagramElement>> linkedMap){
        // преобразуем модель BPMN в граф ELK
        init();

        // получим элементы модели BPMN по типам
        Map<Class<?>, List<DiagramElement>> elementsByTypeMap = getModelItems(model);

        // структура для сохранения связи элементов модели BPMN и графа ELK

        // сначала пройдемся по вершинам - создадим ELK вершины и сохраним связь
        nodeCreateAndLink(elementsByTypeMap, linkedMap);

        // теперь пройдемся по ребрам, создадим ELK ребра и сохраним связь
        edgeCreateAndLink(elementsByTypeMap, linkedMap);

        return (ElkNode) linkedMap.values().stream().findFirst().map(Pair::getFirst).map(ElkGraphElement::eContainer).orElse(null);
    }



    public void applyLayoutToBpmn(Map<String, Pair<ElkGraphElement, DiagramElement>> linkedMap) {
        for (Map.Entry<String, Pair<ElkGraphElement, DiagramElement>> entry : linkedMap.entrySet()) {
            ElkGraphElement elkGraphElement = entry.getValue().getFirst();
//            mxGeometry grafElem = elkGraphElement.getGeometry();
            DiagramElement bpmnElem = entry.getValue().getSecond();

            if (BpmnShape.class.isAssignableFrom(bpmnElem.getClass())){
                Collection<Bounds> bounds = bpmnElem.getChildElementsByType(Bounds.class);
                ElkNode elkNode = (ElkNode) elkGraphElement;
                bounds.stream().findFirst().ifPresent(bound -> {
                    bound.setX(elkNode.getX());
                    bound.setY(elkNode.getY());
                    bound.setWidth(elkNode.getWidth());
                    bound.setHeight(elkNode.getHeight());
                });
            } else if (BpmnEdge.class.isAssignableFrom(bpmnElem.getClass())){
                BpmnEdge bpmnEdge = (BpmnEdge) bpmnElem;
                Collection<Waypoint> waypoints = bpmnEdge.getWaypoints();
                waypoints.clear();
                ElkEdge elkEdge = (ElkEdge) elkGraphElement;
                
                if (CollectionUtils.isEmpty(elkEdge.getSections())){
                    System.out.println("WARNING: edge without points " + entry.getKey());
                    continue;
                }

                for (ElkEdgeSection point : elkEdge.getSections()) {
                    Waypoint wp = bpmnEdge.getModelInstance().newInstance(Waypoint.class);
                    wp.setX(point.getStartX());
                    wp.setY(point.getStartY());
                    bpmnEdge.addChildElement(wp);

                    for (ElkBendPoint bendPoint : point.getBendPoints()) {
                        wp = bpmnEdge.getModelInstance().newInstance(Waypoint.class);
                        wp.setX(bendPoint.getX());
                        wp.setY(bendPoint.getY());
                        bpmnEdge.addChildElement(wp);
                    }


                    wp = bpmnEdge.getModelInstance().newInstance(Waypoint.class);
                    wp.setX(point.getEndX());
                    wp.setY(point.getEndY());
                    bpmnEdge.addChildElement(wp);
                }
            } else {
                System.out.println("WARNING: skip replace coord " + entry.getKey());
            }

        }

    }


    private void init() {
        LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(
                new LayeredMetaDataProvider()
//                new ForceMetaDataProvider(),
//                new MrTreeMetaDataProvider(),
//                new RadialMetaDataProvider(),
//                new StressMetaDataProvider(),
//                new PolyominoOptions()
//                new DisCoMetaDataProvider(),
//                new SporeMetaDataProvider()
        );
    }

    private Map<Class<?>, List<DiagramElement>> getModelItems(BpmnModelInstance model) {
        Definitions definitions = model.getDefinitions();
        BpmnDiagram diagram = definitions.getBpmDiagrams().iterator().next();
        Collection<DiagramElement> diagramElements = diagram.getBpmnPlane().getDiagramElements();
        return diagramElements.stream().collect(Collectors.groupingBy(DiagramElement::getClass));
    }


    private void nodeCreateAndLink(Map<Class<?>, List<DiagramElement>> elementsByTypeMap, Map<String, Pair<ElkGraphElement, DiagramElement>> linkedMap) {
        ElkNode graph = ElkGraphUtil.createGraph();
        graph.setIdentifier("root");
        
        for (Map.Entry<Class<?>, List<DiagramElement>> entry : elementsByTypeMap.entrySet()) {
            Class<?> type = entry.getKey();
            for (DiagramElement bpmnElement : entry.getValue()) {
                if (BpmnShape.class.isAssignableFrom(type)){
                    String id = bpmnElement.getAttributeValue("bpmnElement");
                    Collection<Bounds> bounds = bpmnElement.getChildElementsByType(Bounds.class);
                    Bounds bound = bounds.stream().findFirst().orElse(null);
                    if (bound == null){
                        System.out.println("WARNING: node DI without bound " + id);
                        continue;
                    }
                    
                    ElkNode elkNode = createNode(id, graph);
                    elkNode.setHeight(bound.getHeight());
                    elkNode.setWidth(bound.getWidth());
                    linkedMap.put(id, new Pair<>(elkNode, bpmnElement));
                }
            }
        }
    }


    private void edgeCreateAndLink(Map<Class<?>, List<DiagramElement>> elementsByTypeMap, Map<String, Pair<ElkGraphElement, DiagramElement>> linkedMap) {
        for (Map.Entry<Class<?>, List<DiagramElement>> entry : elementsByTypeMap.entrySet()) {
            Class<?> type = entry.getKey();
            for (DiagramElement diagramElement : entry.getValue()) {
                if (BpmnEdge.class.isAssignableFrom(type)){
                    String id = diagramElement.getAttributeValue("bpmnElement");
                    String condition = ((BpmnEdge) diagramElement).getBpmnElement().getAttributeValue("name");
                    // имя ребра "нода1-нода2"
                    String[] split = id.split("-");
                    if (split.length != 2){
                        System.out.println("WARNING: skip edge " + id);
                        continue;
                    }
                    String from = split[0];
                    String to = split[1];

                    Pair<ElkGraphElement, DiagramElement> fromPair = linkedMap.get(from);
                    if (fromPair == null){
                        System.out.println("WARNING: skip edge " + id + ". 'from' not found");
                        continue;
                    }
                    Pair<ElkGraphElement, DiagramElement> toPair = linkedMap.get(to);
                    if (toPair == null){
                        System.out.println("WARNING: skip edge " + id + ". 'to' not found");
                        continue;
                    }

                    ElkConnectableShape toNode = (ElkConnectableShape) toPair.getFirst();
                    
                    if (condition != null && condition.contains("1")){
                        toNode.setProperty(CoreOptions.PRIORITY, 10);
                    }
                    ElkEdge elkEdge = ElkGraphUtil.createSimpleEdge((ElkConnectableShape) fromPair.getFirst(), toNode);
                    elkEdge.setIdentifier(id);
                    
                    linkedMap.put(id, new Pair<>(elkEdge, diagramElement));
                }
            }
        }
    }    

    private ElkNode createNode(String name, ElkNode graph) {
        ElkNode node = ElkGraphUtil.createNode(graph);
        node.setIdentifier(name);
        return node;
    }
}
