package webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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


	public static void callWSXStream(String containerName, List<String> classNames, List<Object> objetos, String sessionName) {

		try {			
		    
			BatchExecutionCommand command = createBatchExecutionCommandStateless(objetos, sessionName);

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
	
	public static void callWSJaxB(String containerName, List<String> classNames, List<Object> objetos, String sessionName) {

		try {			
		    
			KieServicesConfiguration config =  KieServicesFactory.
			        newRestConfiguration(urlKieServer,
		        		name,
		        		password);
		    config.setMarshallingFormat(MarshallingFormat.JAXB); 
			KieServicesClient client = KieServicesFactory.newKieServicesClient(config);
		    //RuleServicesClient rulesClient = client.getServicesClient(RuleServicesClient.class);  

			BatchExecutionCommand command = createBatchExecutionCommandStateless(objetos, sessionName);

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
		
	@SuppressWarnings("rawtypes")
	private static BatchExecutionCommand createBatchExecutionCommandStateless(List<Object> objetos, String sessionName) {
		BatchExecutionCommand command = null;
		try {
		    KieCommands commandsFactory = KieServices.Factory.get().getCommands();

			Iterator<Object> it = objetos.iterator();
			
			List<Command> cmds = new ArrayList<Command>();
			
			Command insertObjectCommand = null; 
			while (it.hasNext()) {
				Object objeto = it.next();
				insertObjectCommand = commandsFactory.newInsert(objeto, objeto.getClass().getName(), false, null); 
				cmds.add(insertObjectCommand);
			}

//			Command setGlobalCommand = commandsFactory.newSetGlobal("globalListado", new java.util.ArrayList<>(), true);
//			cmds.add(setGlobalCommand);
//			Command getObjectsCommand = commandsFactory.newGetObjects("globalListado");
//			cmds.add(getObjectsCommand);

			Command setGlobalCommand = commandsFactory.newSetGlobal("dato", new java.lang.String(), "datoOut");
			cmds.add(setGlobalCommand);
			Command getGlobalCommand = commandsFactory.newGetGlobal("datoOut");
			cmds.add(getGlobalCommand);

			Command fireAllRulesCommand = commandsFactory.newFireAllRules(1);
			cmds.add(fireAllRulesCommand);

			command = commandsFactory.newBatchExecution(cmds, sessionName);

		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return command;
	}

	@SuppressWarnings("rawtypes")
	private static BatchExecutionCommand createBatchExecutionCommandStateful(List<Object> objetos, String sessionName) {
		BatchExecutionCommand command = null;
		try {
		    KieCommands commandsFactory = KieServices.Factory.get().getCommands();

			Iterator<Object> it = objetos.iterator();
			
			List<Command> cmds = new ArrayList<Command>();
			
			Command insertObjectCommand = null; 
			while (it.hasNext()) {
				Object objeto = it.next();
				insertObjectCommand = commandsFactory.newInsert(objeto, objeto.getClass().getName(), false, null); 
				cmds.add(insertObjectCommand);
				Command getObjectsCommand = commandsFactory.newGetObjects(objeto.getClass().getName());
				cmds.add(getObjectsCommand);
			}

			Command fireAllRulesCommand = commandsFactory.newFireAllRules(1);
			cmds.add(fireAllRulesCommand);

			command = commandsFactory.newBatchExecution(cmds, sessionName);

		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return command;
	}
	
	public static void callWSJSon(String containerName, List<String> classNames, List<Object> objetos, String sessionName) {

		try {			
		    
			BatchExecutionCommand command = createBatchExecutionCommandStateless(objetos, sessionName);

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
	
	@SuppressWarnings("rawtypes")
	public static void executeProcess(String containerName, List<String> classNames, List<Object> objects, String sessionName, String proceso) {
		try {

			KieServicesConfiguration config =  KieServicesFactory.
			        newRestConfiguration(urlKieServer,
		        		name,
		        		password);
		    config.setMarshallingFormat(MarshallingFormat.JAXB); 
			KieServicesClient client = KieServicesFactory.newKieServicesClient(config);

		    List<Command> cmds = new ArrayList<Command>();
			
		    KieCommands commandsFactory = KieServices.Factory.get().getCommands();

		    Command startProcessCommand = commandsFactory.newStartProcess(proceso);
		    cmds.add(startProcessCommand);

			BatchExecutionCommand command = commandsFactory.newBatchExecution(cmds, sessionName);
			
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
