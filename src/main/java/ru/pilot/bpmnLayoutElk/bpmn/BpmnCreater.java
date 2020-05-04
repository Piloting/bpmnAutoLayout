package ru.pilot.bpmnLayoutElk.bpmn;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class BpmnCreater {
    
    public BpmnModelInstance getModelFromResource(String fileName) throws FileNotFoundException {
        URL resource = this.getClass().getClassLoader().getResource(fileName);
        if (resource != null){
            return getModelFromFile(resource.getFile());
        } else {
            throw new FileNotFoundException(fileName);
        }
    }
    
    public BpmnModelInstance getModelFromFile(String fileName){
        File file = new File(fileName);
        return Bpmn.readModelFromFile(file);
    }
    
}
