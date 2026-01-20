package org.epos.core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.IriTemplate;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import model.Ontology;
import model.StatusType;
import usermanagementapis.UserGroupManagementAPI;

public class MetadataPopulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataPopulator.class);

    private static BeansCreation beansCreation = new BeansCreation();

    public static Model retrieveModelMapping(String inputMappingModel){
        EposDataModelDAO eposDataModelDAO = EposDataModelDAO.getInstance();

        /**
         * GET ALL ONTOLOGIES FROM DB AND POPULATE MODEL AND MODEL MAPPING
         **/
        List<Ontology> ontologiesList = eposDataModelDAO.getAllFromDB(Ontology.class);

        String triples = null;
        for(Ontology ontologies : ontologiesList){
            if(ontologies.getName().equals(inputMappingModel)){
                triples = new String(Base64.getDecoder().decode(ontologies.getContent()));
            }
        }
        Model modelmapping = null;
        if(triples!=null) {
            modelmapping = ModelFactory.createDefaultModel()
                    .read(IOUtils.toInputStream(triples, StandardCharsets.UTF_8), null, "TURTLE");
        }
        return modelmapping;
    }

    public static Model retrieveMetadataModelFromTTL(String url){

        final Model model = ModelFactory.createDefaultModel();
        model.read(url, null, "TURTLE");

        return model;
    }

	public static Model retrieveMetadataModelFromString(String ttl) {
		final Model model = ModelFactory.createDefaultModel();
		InputStream inputStream = new ByteArrayInputStream(ttl.getBytes(StandardCharsets.UTF_8));
		model.read(inputStream, null, "TURTLE");
		return model;
	}

    public static void retrievePlainValueFromInnerMethods(Model modelmapping,BeansCreation beansCreation, List<EPOSDataModelEntity> classes, Graph graph, String subject, EPOSDataModelEntity activeClass, Group selectedGroup, String editorId){
        Map<String, String> prefixes = modelmapping.getNsPrefixMap();

        for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
            Map<String, String> itemValue = null;
            Map<String, Object> innerValue = new HashMap<>();
            Triple triple = it.next();

            if(subject.equals(triple.getSubject().toString())) {

                /** Get predicate value of triple and replace long prefix with short one **/
                String value = triple.getPredicate().toString();
                for (String key : prefixes.keySet()) {
                    if (value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key), key + ":");
                }
                if (!value.equals("rdf:type")) {
                    itemValue = SPARQLManager.retrievePropertyValueInEDM(value, activeClass.getClass().getSimpleName(), modelmapping);
                    /** TODO: add recursive gathering of the element **/
                    if(itemValue == null){
                        if(triple.getObject().isBlank()) retrievePlainValueFromInnerMethods(modelmapping, beansCreation, classes, graph, triple.getObject().toString(), activeClass, selectedGroup, editorId);
                    } else{
                        innerValue.put("itemValue",itemValue);
                        if(triple.getObject().isLiteral()) innerValue.put("plain",triple.getObject().getLiteralValue());
                        if(triple.getObject().isBlank()) innerValue.put("node",triple.getObject().toString());
                        if(innerValue.get("plain")!=null)
                            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, (Map<String, String>) innerValue.get("itemValue"), innerValue.get("plain").toString());
                        if(innerValue.get("node")!=null)
                            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, (Map<String, String>) innerValue.get("itemValue"), innerValue.get("node").toString(), selectedGroup, editorId);

                    }
                }
            }
        }
    }


    public static void exploreGraphAndCreateBeans(Model modelmapping, BeansCreation beansCreation, Graph graph, EPOSDataModelEntity activeClass, List<EPOSDataModelEntity> classes, List<String> uidDone, Group selectedGroup, String editorId) {
        /** SET PREFIXES **/
        Map<String, String> prefixes = modelmapping.getNsPrefixMap();

        for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
            Triple triple = it.next();
            /** Get predicate value of triple and replace long prefix with short one **/
            String value = triple.getPredicate().toString();
            for (String key : prefixes.keySet()) {
                if (value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key), key + ":");
            }

            /** Retrieve Active Class **/
            for(EPOSDataModelEntity entity : classes){
                if(!uidDone.contains(entity.getUid())) {
                    activeClass = entity;
                    if (activeClass != null && activeClass.getUid().equals(triple.getSubject().toString())) {
                        uidDone.add(activeClass.getUid());
                        //exploreGraphAndCreateBeans(modelmapping, beansCreation, graph, activeClass, classes, uidDone);
                        for (ExtendedIterator<Triple> iterator = graph.find(triple.getSubject(), Node.ANY, Node.ANY); iterator.hasNext();) {

                            Triple triple1 = iterator.next();
                            Map<String, String> itemValue = null;

                            /** Get predicate value of triple and replace long prefix with short one **/
                            String predicate = triple1.getPredicate().toString();

                            for (String key : prefixes.keySet()) {
                                if (predicate.contains(prefixes.get(key))) predicate = predicate.replaceAll(prefixes.get(key), key + ":");
                            }

                            /** Manage Properties of Active Class **/
                            if (!predicate.equals("rdf:type")) {
                                itemValue = SPARQLManager.retrievePropertyValueInEDM(predicate, activeClass.getClass().getSimpleName(), modelmapping);
                                //NOTE: !triple1.getSubject().isBlank() && triple1.getObject().isBlank() &&  removed
                                if(itemValue==null){
                                    retrievePlainValueFromInnerMethods(modelmapping, beansCreation, classes, graph, triple1.getObject().toString(), activeClass, selectedGroup, editorId);
                                }
                            }

                            if(itemValue != null) {
                                manageItemValue(activeClass, classes, itemValue, triple1.getObject(), selectedGroup, editorId);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void manageItemValue(EPOSDataModelEntity activeClass, List<EPOSDataModelEntity> classes, Map<String, String> itemValue, Node node, Group selectedGroup, String editorId) {
        //System.out.println("["+activeClass.getClass().getSimpleName()+"] "+node.toString()+" "+itemValue);
        if (node.isURI()) {
            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, itemValue, node.toString(), selectedGroup, editorId);
        } else if (node.isBlank()) {
            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, itemValue, node.toString(), selectedGroup, editorId);
        }else if (node.isLiteral()) {
            String s = null;
            if(node.toString().contains("\"")){
                s = node.toString();
                s = StringUtils.substringBetween(s, "\"", "\"");
            }
            //System.out.println("[NODE IS LITERAL "+activeClass.getClass().getSimpleName()+"] "+node.toString()+" "+itemValue+" "+s);
            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, itemValue, s==null? node.getLiteralValue() : s);
        } else if (node.isConcrete()) {
            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, itemValue, node.getLiteral().getValue());
        } else if (node.isVariable()
                || node.isExt()
                || node.isNodeGraph()) {
            /** NOT USED ATM **/
        }
    }

	private static Map<String, LinkedEntity> populateMetadata(Model model, String inputMappingModel,
			Group selectedGroup, StatusType status, String editorId) {
		/** RETRIEVE MAPPING MODEL AND MODEL FROM TTL **/
		Map<String, LinkedEntity> returnMap = new HashMap<>();
		Model modelmapping = retrieveModelMapping(inputMappingModel);
		Graph graph = model.getGraph();

		/** DEFINE VARIABLES **/
		List<EPOSDataModelEntity> classes = new ArrayList<>();
		List<String> uidDone = new ArrayList<>();
		BeansCreation beansCreation = new BeansCreation();

		/** PREPARE CLASSES **/
		Map<String, Map<String, String>> classesMap = SPARQLManager.retrieveMainEntities(model);
		for (String uid : classesMap.keySet()) {
			String className = SPARQLManager.retrieveEDMMappedClass(classesMap.get(uid).get("class").toString(),
					modelmapping);
			EPOSDataModelEntity entity = beansCreation.getEPOSDataModelClass(className, uid, selectedGroup, editorId);
			classes.add(entity);
		}
		classes.removeIf(Objects::isNull);

		/** PREPARE PROPERTIES **/
		exploreGraphAndCreateBeans(modelmapping, beansCreation, graph, null, classes, uidDone, selectedGroup, editorId);

		List<IriTemplate> templates = new ArrayList<>();
		for (EPOSDataModelEntity eposDataModelEntity : classes) {
			if (eposDataModelEntity instanceof IriTemplate) {
				templates.add((IriTemplate) eposDataModelEntity);
			}
		}
		classes.removeAll(templates);

		for (EPOSDataModelEntity eposDataModelEntity : classes) {
			if (eposDataModelEntity instanceof org.epos.eposdatamodel.Operation) {
				for (IriTemplate template : templates) {
					if (template.getUid().equals(
							((org.epos.eposdatamodel.Operation) eposDataModelEntity).getIriTemplate().getUid())) {
						((org.epos.eposdatamodel.Operation) eposDataModelEntity).setMapping(template.getMappings());
						((org.epos.eposdatamodel.Operation) eposDataModelEntity).setTemplate(template.getTemplate());
					}
				}
				// System.out.println("OPERATION "+eposDataModelEntity);
			}
		}

        String salt = RandomStringUtils.randomAlphanumeric(17).toUpperCase();


        /** DATABASE POPULATION **/
		for (EPOSDataModelEntity eposDataModelEntity : classes) {
            if(status!=null) eposDataModelEntity.setStatus(status);
            eposDataModelEntity.setFileProvenance(salt);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.ContactPoint) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.Category) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.CategoryScheme) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.Organization) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.Person) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
			// System.out.println("[ADDING TO DATABASE] "+eposDataModelEntity);
			try {
				AbstractAPI api = AbstractAPI.retrieveAPI(eposDataModelEntity.getClass().getSimpleName().toUpperCase());
                LOGGER.debug("Ingesting -> "+eposDataModelEntity);
				LinkedEntity le = api.create(eposDataModelEntity, null, null, null);
				returnMap.put(le.getUid(), le);
			} catch (Exception apiCreationException) {
				apiCreationException.printStackTrace();
				LOGGER.error("[ERROR] ON: " + eposDataModelEntity.toString() + "\n[EXCEPTION]: "
						+ apiCreationException.getLocalizedMessage());
			}
		}

		for (Map.Entry<String, LinkedEntity> uid : returnMap.entrySet()) {
			if (selectedGroup != null) {
				UserGroupManagementAPI.addMetadataElementToGroup(uid.getValue().getMetaId(), selectedGroup.getId());
			}
		}

		return returnMap;
	}

    public static Map<String,LinkedEntity> startMetadataPopulation(String url, String inputMappingModel, Group selectedGroup, StatusType status, String editorId){
		Model model = retrieveMetadataModelFromTTL(url);
		return populateMetadata(model, inputMappingModel, selectedGroup, status, editorId);
    }

	public static Map<String, LinkedEntity> startMetadataPopulationFromContent(String ttlContent, String inputMappingModel, Group selectedGroup, StatusType status, String editorId) {
		Model model = retrieveMetadataModelFromString(ttlContent);
		return populateMetadata(model, inputMappingModel, selectedGroup, status, editorId);
	}
}
