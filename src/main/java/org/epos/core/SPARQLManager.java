package org.epos.core;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SPARQLManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SPARQLManager.class);

    public static Map<String,Map<String,String>> retrieveMainEntities(Model model){

        Map<String,String> prefixes = model.getNsPrefixMap();
        String queryString = "";
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
        }
        queryString += "SELECT ?class ?uid  WHERE {\n" +
                "   ?uid a ?class .\n" +
                //"   FILTER (!isBlank(?uid))\n" +
                "}";

        Map<String,Map<String,String>> mapUidItems = new HashMap<>();

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                Map<String,String> mapItems = new HashMap<>();
                /** RETRIEVE ROW RESULT **/
                QuerySolution soln = results.nextSolution();

                /** GET CLASS AND UID **/
                String className = soln.get("class").toString();
                String classUid = soln.get("uid").toString();

                /** ADD MAIN CLASSES TO MAP **/
                mapItems.put("class",className);
                mapItems.put("uid",classUid);
                mapUidItems.put(classUid, mapItems);
            }
            return mapUidItems;
        } finally {
            qexec.close();
        }
    }
    public static String retrieveEDMMappedClass(String value, Model modelmapping){

        Map<String,String> prefixes = modelmapping.getNsPrefixMap();
        String queryString = "";
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
            if(value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key),key+":");
        }

        queryString +=
                "SELECT ?mapped WHERE { ?mapped owl:equivalentClass "+value+" . }";
        Query query = QueryFactory.create(queryString);

        QueryExecution qexec = QueryExecutionFactory.create(query, modelmapping);
        try {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                if (soln.toString().contains("http://www.epos-eu.org/epos-data-model#"))
                    return soln.get("mapped").toString().replaceAll("http://www.epos-eu.org/epos-data-model#", "");
            }
        }catch(Exception e){
            LOGGER.error("Error with query "+queryString+"\n"+e.getLocalizedMessage());
        } finally {
            qexec.close();
        }
        return null;
    }

    public static Map<String, String> retrievePropertyValueInEDM(String value, String className, Model model){

        Map<String, String> returnMapQueryProperty = new HashMap<>();

        Map<String,String> prefixes = model.getNsPrefixMap();
        String queryString = "";
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
        }
        queryString +=
                "SELECT ?property ?range\n" +
                        "WHERE {\n" +
                        "  ?property rdfs:domain edm:"+className+" .\n" +
                        "  ?property owl:equivalentProperty "+value+" .\n" +
                        "  ?property rdfs:range  ?range .\n" +
                        "}";

        Query query = null;
        try {
            query = QueryFactory.create(queryString);
        }catch(Exception e){
            LOGGER.error(e.getLocalizedMessage());
            System.exit(0);
        }
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        prefixes.put("edm","http://www.epos-eu.org/epos-data-model#");
        try {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                if(soln!=null && soln.get("property").toString().contains("http://www.epos-eu.org/epos-data-model#")) {
                    //String clazz = soln.get("class").toString();
                    String property = soln.get("property").toString();
                    String range = soln.get("range").toString();
                    String subproperty = soln.get("prop")!=null? soln.get("prop").toString() : "";
                    String rangeprop = soln.get("rangeprop")!=null? soln.get("rangeprop").toString() : "";
                    for(String val : prefixes.values()){
                        //if(clazz.contains(val)) clazz = clazz.replaceAll(val,"");
                        if(property.contains(val)) property = property.replaceAll(val,"");
                        if(range.contains(val)) range = range.replaceAll(val,"");
                        if(subproperty.contains(val)) subproperty = subproperty.replaceAll(val,"");
                        if(rangeprop.contains(val)) rangeprop = rangeprop.replaceAll(val,"");
                    }
                    //returnMapQueryProperty.put("class", clazz);
                    returnMapQueryProperty.put("property", property);
                    returnMapQueryProperty.put("range", range);
                    returnMapQueryProperty.put("prop", subproperty);
                    returnMapQueryProperty.put("rangeprop", rangeprop);
                    return returnMapQueryProperty;

                }
            }
        } finally {
            qexec.close();
        }

        return null;
    }

}
