package ru.pilot.bpmnLayoutElk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.di.DiagramElement;
import org.eclipse.elk.core.util.Pair;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkNode;
import ru.pilot.bpmnLayoutElk.bpmn.BpmnCreater;
import ru.pilot.bpmnLayoutElk.common.GraphConverter;
import ru.pilot.bpmnLayoutElk.elk.ElkGraphLayouter;

public class BpmnModelLayouter {

    public static void main(String[] args) throws FileNotFoundException {
        // загрузка модели
        BpmnModelInstance model = new BpmnCreater().getModelFromResource("C2CPushInDiagram.bpmn");
        
        // структура для сохранения связи элементов модели BPMN и графа ELK
        Map<String, Pair<ElkGraphElement, DiagramElement>> linkedMap = new HashMap<>(1000);
        GraphConverter graphConverter = new GraphConverter();
        ElkNode elkGraph = graphConverter.elkGraphFromCamundaModel(model, linkedMap);
        
        // непосредственно позиционирование
        new ElkGraphLayouter().layout(elkGraph);

        // перенос координат обратно в BPMN
        graphConverter.applyLayoutToBpmn(linkedMap);
        
        // сохраним измененный файл
        saveToFile(model, "C2CPushIn-layouted.bpmn");
    }
    
    private static void saveToFile(BpmnModelInstance model, String fileName) {
        File file = new File(fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bpmn.writeModelToFile(file, model);

        System.out.println("BPMN diagram save to file " + file.getAbsolutePath());
    }
}
