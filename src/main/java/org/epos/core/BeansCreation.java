package org.epos.core;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import model.StatusType;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.epos.api.MetadataPopulationApiController;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;

public class BeansCreation <T extends EPOSDataModelEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeansCreation.class);

    public T getEPOSDataModelClass(String className, String uid, Group selectedGroup){
        //System.out.println("GET EDM class: "+className+" "+uid);
        try {
            Class clazz = Class.forName("org.epos.eposdatamodel."+className);
            Constructor[] ctor = clazz.getConstructors();
            T object = (T) ctor[0].newInstance();
            //object.setInstanceId(UUID.randomUUID().toString());
            //object.setMetaId(UUID.randomUUID().toString());
            object.setUid(uid);
            object.setEditorId("ingestor");
            object.setFileProvenance("ingestor");
            object.setStatus(StatusType.PUBLISHED);

            return object;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            //LOGGER.error(e.getLocalizedMessage());
            return null;
        }
    }

    public void getEPOSDataModelPropertiesLiteral(EPOSDataModelEntity classObject, List<EPOSDataModelEntity> classes, Map<String, String> property, Object propertyValue) {
        Class<?> propertyValueClass = propertyValue.getClass();
        String propertyName = property.get("property").substring(0, 1).toUpperCase() + property.get("property").substring(1);
        //System.out.println("PRE DEBUG: " + classObject.getClass().getName() + " " + propertyValueClass + " " + propertyValue.getClass() + " " + propertyName+" "+propertyValue);

        Method method = null;
        LinkedEntity le = null;
        EPOSDataModelEntity entity = null;

        if (propertyValueClass.getName().equals("org.apache.jena.datatypes.xsd.XSDDateTime")) {
            propertyValueClass = LocalDateTime.class;
            try {
                propertyValue = ParseLocalDateTime.parse((String) propertyValue);//LocalDateTime.parse((String)propertyValue,  DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss'Z']"));
                //System.out.println("DATE: "+propertyValue);

            } catch (DateTimeParseException ignored) {
                //LOGGER.error(ignored.getLocalizedMessage());
            }
        }
        if (propertyValueClass.getName().equals("java.lang.Boolean")) {
            propertyValueClass = java.lang.String.class;
            propertyValue = Boolean.toString((Boolean)propertyValue);
        }
        if (propertyValueClass.getName().equals("java.lang.Double")) {
            propertyValueClass = java.lang.String.class;
            propertyValue = Double.toString((Double) propertyValue);
        }
        if (propertyValueClass.getName().equals("java.lang.Integer")) {
            propertyValueClass = java.lang.String.class;
            propertyValue = Integer.toString((Integer) propertyValue);
        }
        if (propertyValueClass.getName().equals("java.lang.Float")) {
            propertyValueClass = java.lang.String.class;
            propertyValue = Float.toString((Float) propertyValue);
        }
        if (propertyValueClass.getName().equals("java.lang.Long")) {
            propertyValueClass = java.lang.String.class;
            propertyValue = Long.toString((Long) propertyValue);
        }
        if (propertyValue.getClass().getName().contains("org.apache.jena.datatypes.BaseDatatype")) {
            propertyValueClass = java.lang.String.class;
            propertyValue = ((BaseDatatype.TypedValue) propertyValue).lexicalValue;
        }
        try {
            method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
        } catch (NoSuchMethodException e) {
            //LOGGER.error(e.getLocalizedMessage());
        }

        if (method == null) {
            try {
                method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                //LOGGER.error(e.getLocalizedMessage());
            }
        }

        //System.out.println("METHOD: "+method);

        if(method != null && propertyValue != null){
            try {
                //System.out.println("Invoking: "+propertyName+" "+propertyValue);
                method.invoke(classObject, propertyValue);
            } catch (IllegalArgumentException |IllegalAccessException | InvocationTargetException e) {
                /*LOGGER.error("ERROR Invoking [\nProperty Name: " + propertyName +
                        "\nPropertyValue: " + propertyValue +
                        "\nPropertyClass: " + propertyValueClass +
                        "\nClass Name:" + classObject.getClass().getName() +
                        "\nMethod Name:" + method.getName() +
                        "\nExpected Parameters: " + Arrays.asList(method.getParameterTypes()).toString() + "\n]"+
                        "\nError message: " + e.getLocalizedMessage());*/
            }
        }
    }

    public void getEPOSDataModelPropertiesNode(EPOSDataModelEntity classObject, List<EPOSDataModelEntity> classes, Map<String, String> property, String propertyValue, Group selectedGroup) {

        Class<?> propertyValueClass = propertyValue.getClass();
        String propertyName = property.get("property").substring(0, 1).toUpperCase() + property.get("property").substring(1);
        //System.out.println("PRE DEBUG: " + property.get("range") +" " + propertyValueClass + " " + propertyValue.getClass() + " " + propertyName + " "+ propertyValue);

        Method method = null;
        LinkedEntity le = null;
        EPOSDataModelEntity entity = null;

        for (EPOSDataModelEntity eposDataModelEntity : classes) {
            if (eposDataModelEntity != null && eposDataModelEntity.getUid().equals(propertyValue)) {
                entity = eposDataModelEntity;
                break;
            }
        }

        if(entity==null){
            entity = getEPOSDataModelClass(property.get("range"),propertyValue, selectedGroup);
        }

        if (entity != null) {
            try {
                le = new LinkedEntity();
                le.setUid(entity.getUid());
                le.setEntityType(entity.getClass().getSimpleName().toUpperCase());
            } catch (Exception skip) {
                //LOGGER.error(skip.getLocalizedMessage());
            }
            propertyValueClass = LinkedEntity.class;

            try {
                method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                //LOGGER.error(e.getLocalizedMessage());
            }

            if (method == null) {
                try {
                    method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
                } catch (NoSuchMethodException e) {
                    //LOGGER.error(e.getLocalizedMessage());
                }
            }

            if(method == null && le!=null && property.get("range").equals("string")){
                    //System.out.println("[** OMG **] EXCEPTIONALLY IS A STRING!!");
                    propertyValue = le.getUid();
                    getEPOSDataModelPropertiesLiteral(classObject,classes,property,propertyValue);
            }
            if (method != null) {
                try {
                    method.invoke(classObject, le);
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    /*LOGGER.error("ERROR Invoking [\nProperty Name: " + propertyName +
                            "\nPropertyValue: " + propertyValue +
                            "\nPropertyClass: " + propertyValueClass +
                            "\nClass Name:" + classObject.getClass().getName() +
                            "\nMethod Name:" + method.getName() +
                            "\nExpected Parameters: " + Arrays.asList(method.getParameterTypes()).toString() + "\n]"+
                            "\n Error message: " + e.getLocalizedMessage());*/
                }
            }
        }
    }
}