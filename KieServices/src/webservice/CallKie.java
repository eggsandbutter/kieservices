package webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.drools.compiler.runtime.pipeline.impl.DroolsJaxbHelperProviderImpl;
import org.kie.api.KieServices;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.internal.runtime.KnowledgeContext;
import org.kie.internal.runtime.helper.BatchExecutionHelper;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

public class CallKie {

	private static final String urlKieServer = "http://localhost:8080/kie-server-6.5.0.Final-ee7/services/rest/server";
	private static final String name = "kieserver";
	private static final String password = "holanda1!";
	public static final int REST_JSON = 0;
	public static final int REST_XSTREAM = 1;
	public static final int REST_JAXB = 2;
	
	public static void executeStateless(String containerName, String sessionName, List<String> classNames, List<Object> objects, HashMap<String, Object> globals, int restService) {
		try {

			if (containerName == null || containerName.equals("") || sessionName == null || sessionName.equals("")) {
				//Provoke error
				System.out.println("Session name and/or container name not informed.");
				Integer.parseInt("S");
			}
			
		    KieCommands commandsFactory = KieServices.Factory.get().getCommands();
			
			List<Command<?>> cmds = new ArrayList<Command<?>>();

			if (objects != null) {
				Iterator<Object> it = objects.iterator();
				while (it.hasNext()) {
					Object object = it.next();
					Command<?> insertObjectCommand = commandsFactory.newInsert(object, object.getClass().getName(), false, null); 
					cmds.add(insertObjectCommand);
				}
			}

			if (globals != null) {
				Iterator<Map.Entry<String, Object>> it2 = globals.entrySet().iterator();
				while (it2.hasNext()) {
					Map.Entry<String, Object> global = (Map.Entry<String, Object>)it2.next();
					Command<?> setGlobalCommand = commandsFactory.newSetGlobal(global.getKey(), global.getValue(), true);
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
				callWSJaxB(command, containerName, classNames);
				break;
			case REST_JSON:
				callWSJSon(command, containerName);
				break;
			case REST_XSTREAM:
				callWSXStream(command, containerName);
				break;
			default:
				//Provoke error
				System.out.println("Rest service not valid");
				Integer.parseInt("S");
			}
			

		} catch (Exception exception) {
			exception.printStackTrace();
		}
		
	}
	
	public static void callWSXStream(BatchExecutionCommand command, String containerName) {

		try {			
		    

			String xStream = BatchExecutionHelper.newXStreamMarshaller().toXML(command);

			System.out.println("xStream: "+xStream);

			HttpClient client = HttpClientBuilder.create().build();
			HttpPost postRequest = new HttpPost(urlKieServer+"/instances/"+containerName);

			String authString = name + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			postRequest.addHeader("Authorization", "Basic " + authStringEnc);
			postRequest.addHeader("Content-Type",  "application/xml");
			postRequest.addHeader("X-KIE-ContentType", "XSTREAM");
			
			HttpEntity entity = new ByteArrayEntity(xStream.getBytes("UTF-8"));
			postRequest.setEntity(entity);
	        
			System.out.println(postRequest.toString());
			
			HttpResponse response = client.execute(postRequest);
			
			System.out.println("Response: "+response);
			
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	}
	
	public static void callWSJaxB(BatchExecutionCommand command, String containerName, List<String> classNames) {

		try {			
			
			KieServicesConfiguration config =  KieServicesFactory.
			        newRestConfiguration(urlKieServer,
		        		name,
		        		password);
		    config.setMarshallingFormat(MarshallingFormat.JAXB); 
			KieServicesClient client = KieServicesFactory.newKieServicesClient(config);
		    //RuleServicesClient rulesClient = client.getServicesClient(RuleServicesClient.class);  

			JAXBContext jaxbContext = DroolsJaxbHelperProviderImpl.createDroolsJaxbContext(classNames, null);
			Marshaller marshaller = jaxbContext.createMarshaller(); 
			StringWriter xml = new StringWriter(); 
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			marshaller.marshal(command, System.out);
			marshaller.marshal(command, xml);

			ServiceResponse<String> response = client.executeCommands("instances/"+containerName, xml.toString()); 
			
			System.out.println("Message: "+response.getMsg());
			System.out.println("Result: "+response.getResult());
			System.out.println("Type: "+response.getType());
			System.out.println("Response: "+response);
			
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	}
		
	public static void callWSJSon(BatchExecutionCommand command, String containerName) {

		try {			
		    
			String sJson = BatchExecutionHelper.newJSonMarshaller().toXML(command);

			System.out.println("sJson: "+sJson);

			HttpClient client = HttpClientBuilder.create().build();
			HttpPost postRequest = new HttpPost(urlKieServer+"/instances/"+containerName);

			String authString = name + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			postRequest.addHeader("Authorization", "Basic " + authStringEnc);
			postRequest.addHeader("Content-Type",  "application/xml");
			postRequest.addHeader("X-KIE-ContentType", "JSON");
			
			HttpEntity entity = new ByteArrayEntity(sJson.getBytes("UTF-8"));
			postRequest.setEntity(entity);
	        
			System.out.println(postRequest.toString());
			
			HttpResponse response = client.execute(postRequest);
			
			System.out.println("Response: "+response);

		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	
	}
	
	public static void executeProcess(String containerName, String sessionName, List<String> classNames, List<Object> objects, HashMap<String, Object> globals, String processID, int restService) {
		try {

			if (containerName == null || containerName.equals("") || sessionName == null || sessionName.equals("") || processID == null || processID.equals("") ) {
				//Provoke error
				System.out.println("Session name, container name and/or processID not informed.");
				Integer.parseInt("S");
			}
			
		    List<Command<?>> cmds = new ArrayList<Command<?>>();
			
		    KieCommands commandsFactory = KieServices.Factory.get().getCommands();

			if (objects != null) {
				Iterator<Object> it = objects.iterator();
				while (it.hasNext()) {
					Object object = it.next();
					Command<?> insertObjectCommand = commandsFactory.newInsert(object, object.getClass().getName(), false, null); 
					cmds.add(insertObjectCommand);
				}
			}

			if (globals != null) {
				Iterator<Map.Entry<String, Object>> it2 = globals.entrySet().iterator();
				while (it2.hasNext()) {
					Map.Entry<String, Object> global = (Map.Entry<String, Object>)it2.next();
					Command<?> setGlobalCommand = commandsFactory.newSetGlobal(global.getKey(), global.getValue(), true);
					cmds.add(setGlobalCommand);
					Command<?> getObjectsCommand = commandsFactory.newGetObjects(global.getKey());
					cmds.add(getObjectsCommand);
				}
			}

		    Command<?> startProcessCommand = commandsFactory.newStartProcess(processID);
		    cmds.add(startProcessCommand);

			BatchExecutionCommand command = commandsFactory.newBatchExecution(cmds, sessionName);
			
			switch (restService) {
			case REST_JAXB:
				callWSJaxB(command, containerName, classNames);
				break;
			case REST_JSON:
				callWSJSon(command, containerName);
				break;
			case REST_XSTREAM:
				callWSXStream(command, containerName);
				break;
			default:
				//Provoke error
				System.out.println("Rest service not valid");
				Integer.parseInt("S");
			}
						
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	}
	
	
	public static void connectivityKieServer() {

		try {
			
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(urlKieServer);

			String authString = name + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			request.addHeader("Authorization", "Basic " + authStringEnc);

			HttpResponse response = client.execute(request);
			System.out.println(response.toString());

			//Continuar tratamiento
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}			
		  
			System.out.println(result.toString());
			
		} catch (IOException iOException) {
			iOException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	}
	
	public static void getPublishedContainers() {

		try {
			
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(urlKieServer+"/containers");

			String authString = name + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			request.addHeader("Authorization", "Basic " + authStringEnc);

			HttpResponse response = client.execute(request);
			System.out.println(response.toString());

			//Continuar tratamiento
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}			
		  
			System.out.println(result.toString());
			
		} catch (IOException iOException) {
			iOException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	}
	

	public static void publishContainer(String containerName, String artifact, String group, String version) {

		try {
			
			HttpClient client = HttpClientBuilder.create().build();
			HttpPut postRequest = new HttpPut(urlKieServer+"/containers/"+containerName);

			String authString = name + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			postRequest.addHeader("Authorization", "Basic " + authStringEnc);
			postRequest.addHeader("Content-Type",  "application/xml");

			String xml = 	"<kie-container container-id='"+containerName+"'>"
								+ "<release-id>"
									+ "<artifact-id>"+artifact+"</artifact-id>"
									+ "<group-id>"+group+"</group-id>"
									+ "<version>"+version+"</version>"
								+"</release-id>" 
							+"</kie-container>";
			
			HttpEntity entity = new ByteArrayEntity(xml.getBytes("UTF-8"));
			postRequest.setEntity(entity);
	        
			
			System.out.println(postRequest.toString());
			
			HttpResponse response = client.execute(postRequest);
			System.out.println(response.toString());

			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}			
		  
			System.out.println(result.toString());
			
		} catch (IOException iOException) {
			iOException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	}
	
	public static void deleteContainer(String containerName) {

		try {
			
			HttpClient client = HttpClientBuilder.create().build();
			HttpDelete deleteRequest = new HttpDelete(urlKieServer+"/containers/"+containerName);

			String authString = name + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			deleteRequest.addHeader("Authorization", "Basic " + authStringEnc);

			HttpResponse response = client.execute(deleteRequest);
			System.out.println(response.toString());

			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}			
		  
			System.out.println(result.toString());
			
		} catch (IOException iOException) {
			iOException.printStackTrace();
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			//Do something
		}
	}
	
}
