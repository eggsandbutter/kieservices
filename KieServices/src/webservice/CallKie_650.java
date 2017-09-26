package webservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.api.KieServices;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.api.model.ServiceResponse.ResponseType;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;

public class CallKie_650 {

	private static final String urlKieServer = "http://localhost:8080/kie-server-6.5.0.Final-ee7/services/rest/server";
//	private static final String urlAquila = "https://aquiladev.asisa.es/kie-server/services/rest/server";
	private static final String name = "kieserver";
//	private static final String nameAquila = "rdekker";
	private static final String password = "holanda1!";
//	private static final String passAquila = "djwjjk12992wkk";
	private static final long timeout = 3000L;
	public static final int REST_JSON = 0;
	public static final int REST_XSTREAM = 1;
	public static final int REST_JAXB = 2;

	/**
	 * Service that calls the KIE execution server via REST
	 * @param containerName Name of KIE container where the service is published
	 * @param sessionName Name of the session defined in kmodule to be executed
	 * @param objects BOM objects that are involved in the service to be executed
	 * @param globals Any globals that are used in the service
	 * @param restService The type of rest service that is used (1=json, 2=xstream, 3=jaxb)
	 */
	public static List<Object> execute(String containerName, String sessionName, List<Object> objects, HashMap<String, Object> globals, int restService) {

		List<Object> resultObjects = null;
		
		try {
			
			Set<Class<?>> allClasses = new HashSet<Class<?>>();
			
			if (containerName == null || containerName.equals("")) {
				//Provoke error
				System.out.println("Container name not informed.");
				Integer.parseInt("S");
			}

			if (sessionName == null || sessionName.equals("")) {
				//Provoke error
				System.out.println("Session name not informed.");
				Integer.parseInt("S");
			}
			
		    KieCommands commandsFactory = KieServices.Factory.get().getCommands();
			
			List<Command<?>> cmds = new ArrayList<Command<?>>();

			if (objects != null) {
				Iterator<Object> it = objects.iterator();
				while (it.hasNext()) {
					Object object = it.next();
					//All objects are returned
					Command<?> insertObjectCommand = commandsFactory.newInsert(object, object.getClass().getName(), true, null); 
					cmds.add(insertObjectCommand);
					allClasses.add(object.getClass());
				}
			}

			if (globals != null) {
				Iterator<Map.Entry<String, Object>> it2 = globals.entrySet().iterator();
				while (it2.hasNext()) {
					Map.Entry<String, Object> global = (Map.Entry<String, Object>)it2.next();
					//Globals are not returned
					Command<?> setGlobalCommand = commandsFactory.newSetGlobal(global.getKey(), global.getValue(), false);
					cmds.add(setGlobalCommand);
					Command<?> getObjectsCommand = commandsFactory.newGetObjects(global.getKey());
					cmds.add(getObjectsCommand);
				}
			}

			Command<?> fireAllRulesCommand = commandsFactory.newFireAllRules();
			cmds.add(fireAllRulesCommand);

			BatchExecutionCommand command = commandsFactory.newBatchExecution(cmds, sessionName);
			
			switch (restService) {
			case REST_JAXB:
				resultObjects = callWSJaxB(command, containerName, allClasses);
				break;
			case REST_JSON:
				resultObjects = callWSJSon(command, containerName, allClasses);
				break;
			case REST_XSTREAM:
				resultObjects = callWSXStream(command, containerName, allClasses);
				break;
			default:
				//By default JAXB is sent
				resultObjects = callWSJaxB(command, containerName, allClasses);
				break;
			}
			

		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return resultObjects;
		
	}
	
	/**
	 * Service that sends a JAXB request to KIE execution server.
	 * @param command A valid BatchExecutionCommand constructed with the KIE API server.
	 * @param containerName Name of the container to be called for executing the server.
	 * @param allClasses The classes involved in the service to create a valid JAXB XML.
	 * @return List of Objects returned by the service.
	 */
	private static ArrayList<Object> callWSJaxB(BatchExecutionCommand command, String containerName, Set<Class<?>> allClasses) {

		ArrayList<Object> objects = null;
		try {			
			
			KieServicesConfiguration config =  KieServicesFactory.
			        newRestConfiguration(CallKie_650.urlKieServer,
			        		CallKie_650.name,
			        		CallKie_650.password);
		    config.setMarshallingFormat(MarshallingFormat.JAXB);
		    config.setTimeout(CallKie_650.timeout); //
		    config.addExtraClasses(allClasses);

		    KieServicesClient client = KieServicesFactory.newKieServicesClient(config);
		    RuleServicesClient rulesClient = client.getServicesClient(RuleServicesClient.class);  

			ServiceResponse<ExecutionResults> response = rulesClient.executeCommandsWithResults(containerName, command); 
			
			System.out.println("Message: "+response.getMsg());
			System.out.println("Result: "+response.getResult());
			System.out.println("Type: "+response.getType());
			System.out.println("Response: "+response);
			
			ExecutionResults executionResults = response.getResult();
			
			if (response.getType().equals(ResponseType.SUCCESS)) {
				objects = new ArrayList<Object>();
				Iterator<Class<?>> it = allClasses.iterator();
				while (it.hasNext()) {
					Class<?> clas = it.next();
					Object object = (Object)executionResults.getValue(clas.getName());
					objects.add(object);
				}
			}
			
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
		return objects;
	}

	/**
	 * Service that sends a XSTREAM request to KIE execution server.
	 * @param command A valid BatchExecutionCommand constructed with the KIE API server.
	 * @param containerName Name of the container to be called for executing the server.
	 * @param allClasses The classes involved in the service to create a valid JAXB XML.
	 * @return List of Objects returned by the service.
	 */
	private static ArrayList<Object> callWSXStream(BatchExecutionCommand command, String containerName, Set<Class<?>> allClasses) {

		ArrayList<Object> objects = null;
		try {			
			
			KieServicesConfiguration config =  KieServicesFactory.
			        newRestConfiguration(CallKie_650.urlKieServer,
			        		CallKie_650.name,
			        		CallKie_650.password);
		    config.setMarshallingFormat(MarshallingFormat.XSTREAM);
		    config.setTimeout(CallKie_650.timeout); //
		    config.addExtraClasses(allClasses);

		    KieServicesClient client = KieServicesFactory.newKieServicesClient(config);
		    RuleServicesClient rulesClient = client.getServicesClient(RuleServicesClient.class);  

			ServiceResponse<ExecutionResults> response = rulesClient.executeCommandsWithResults(containerName, command); 
			
			System.out.println("Message: "+response.getMsg());
			System.out.println("Result: "+response.getResult());
			System.out.println("Type: "+response.getType());
			System.out.println("Response: "+response);
			
			ExecutionResults executionResults = response.getResult();
			
			if (response.getType().equals(ResponseType.SUCCESS)) {
				objects = new ArrayList<Object>();
				Iterator<Class<?>> it = allClasses.iterator();
				while (it.hasNext()) {
					Class<?> clas = it.next();
					Object object = (Object)executionResults.getValue(clas.getName());
					objects.add(object);
				}
			}
			
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
		return objects;
	}

	/**
	 * Service that sends a JSON request to KIE execution server.
	 * @param command A valid BatchExecutionCommand constructed with the KIE API server.
	 * @param containerName Name of the container to be called for executing the server.
	 * @param allClasses The classes involved in the service to create a valid JAXB XML.
	 * @return List of Objects returned by the service.
	 */
	private static ArrayList<Object> callWSJSon(BatchExecutionCommand command, String containerName, Set<Class<?>> allClasses) {

		ArrayList<Object> objects = null;
		try {			
			
			KieServicesConfiguration config =  KieServicesFactory.
			        newRestConfiguration(CallKie_650.urlKieServer,
			        		CallKie_650.name,
			        		CallKie_650.password);
		    config.setMarshallingFormat(MarshallingFormat.JSON);
		    config.setTimeout(CallKie_650.timeout); //
		    config.addExtraClasses(allClasses);

		    KieServicesClient client = KieServicesFactory.newKieServicesClient(config);
		    RuleServicesClient rulesClient = client.getServicesClient(RuleServicesClient.class);  

			ServiceResponse<ExecutionResults> response = rulesClient.executeCommandsWithResults(containerName, command); 
			
			System.out.println("Message: "+response.getMsg());
			System.out.println("Result: "+response.getResult());
			System.out.println("Type: "+response.getType());
			System.out.println("Response: "+response);
			
			ExecutionResults executionResults = response.getResult();
			
			if (response.getType().equals(ResponseType.SUCCESS)) {
				objects = new ArrayList<Object>();
				Iterator<Class<?>> it = allClasses.iterator();
				while (it.hasNext()) {
					Class<?> clas = it.next();
					Object object = (Object)executionResults.getValue(clas.getName());
					objects.add(object);
				}
			}
			
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
		return objects;
	}

	/**
	 * Lists capabilities of KIE execution server
	 */
	public static void listCapabilities() {  
		try {
			KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(CallKie_650.urlKieServer, CallKie_650.name, CallKie_650.password);
			configuration.setMarshallingFormat(MarshallingFormat.JAXB);
			KieServicesClient kieServicesClient =  KieServicesFactory.newKieServicesClient(configuration);
		    KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();  
		    System.out.print("Server capabilities:");  
		    for(String capability: serverInfo.getCapabilities()) {  
		        System.out.print(" " + capability);  
		    }  
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * Lists deployed containers on KIE execution server
	 */
	public static void listContainers() {  
		try {
			KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(CallKie_650.urlKieServer, CallKie_650.name, CallKie_650.password);
			configuration.setMarshallingFormat(MarshallingFormat.JAXB);
			KieServicesClient kieServicesClient =  KieServicesFactory.newKieServicesClient(configuration);
		    KieContainerResourceList containersList = kieServicesClient.listContainers().getResult();  
		    List<KieContainerResource> kieContainers = containersList.getContainers();  
		    System.out.println("Available containers: ");
		    for (KieContainerResource container : kieContainers) {  
		        System.out.println("\t" + container.getContainerId() + " (" + container.getReleaseId() + ")");  
		    }  
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * Disposes a container
	 * @param containerName Name of the container to be deleted
	 * @return String response message
	 */
	public static String disposeContainer(String containerName) {
		String response = "";
		try {
			KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(CallKie_650.urlKieServer, CallKie_650.name, CallKie_650.password);
			configuration.setMarshallingFormat(MarshallingFormat.JAXB);
			KieServicesClient kieServicesClient =  KieServicesFactory.newKieServicesClient(configuration);
			List<KieContainerResource> kieContainers = kieServicesClient.listContainers().getResult().getContainers();  
		    if (kieContainers.size() == 0) {  
		        System.out.println("No containers available...");  
		        response = "No containers available...";
		    } 
		    ServiceResponse<Void> responseDispose = kieServicesClient.disposeContainer(containerName);  
		    if (responseDispose.getType() == ResponseType.FAILURE) {  
		        System.out.println("Error disposing " + containerName + ". Message: ");  
		        System.out.println(responseDispose.getMsg());
		    }  else {
			    System.out.println("Success Disposing container " + containerName);  			
		        System.out.println(responseDispose.getMsg());
		    }
	        response = responseDispose.getMsg();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	    return response;
	}
	
	public static String createContainer(String containerName) {
		String response = "";
		try {
			KieServicesConfiguration configuration = KieServicesFactory.newRestConfiguration(CallKie_650.urlKieServer, CallKie_650.name, CallKie_650.password);
			configuration.setMarshallingFormat(MarshallingFormat.JAXB);
			KieServicesClient kieServicesClient =  KieServicesFactory.newKieServicesClient(configuration);	
			
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		 return response;
	}
	
}
