package cl.dibam.fuse;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ClaveUnicaTokenBuilder extends RouteBuilder {

   public void configure() {

	   from("cxf:bean:proxyClaveUnicaTokenService")
            
	    .convertBodyTo(String.class)
	   
	   	.to("log:input")
            
      	// send the request to the route to handle the operation
        // the name of the operation is in that header
        .recipientList(simple("direct:loginClaveUnicaToken"));

	   from("direct:loginClaveUnicaToken")
            
      	.process(new Processor() {
            
      		public void process(Exchange exchange) throws Exception {
                
      			Message in = exchange.getIn();
                
      			String body = (String) in.getBody();
      			
      			String clientId = body.substring(body.indexOf("<clientId>") + 1, body.indexOf("</clientId>"));
                 
      			clientId = clientId.replace("clientId>", "");
      			
      			String clientSecret = body.substring(body.indexOf("<clientSecret>") + 1, body.indexOf("</clientSecret>"));
      			
      			clientSecret = clientSecret.replace("clientSecret>", "");
      			
      			String redirectUrl = body.substring(body.indexOf("<redirectUrl>") + 1, body.indexOf("</redirectUrl>"));

      			redirectUrl = redirectUrl.replace("redirectUrl>", "");
      			
      			redirectUrl = redirectUrl.replace(":", "%3A");
      			
      			redirectUrl = redirectUrl.replace("/", "%2F");
      			
      			String code = body.substring(body.indexOf("<code>") + 1, body.indexOf("</code>"));

      			code = code.replace("code>", "");
      			
      			String state = body.substring(body.indexOf("<state>") + 1, body.indexOf("</state>"));

      			state = state.replace("state>", "");
      			
      			String url = "https://accounts.claveunica.gob.cl/openid/token/";
                  
      			in.setBody(getResponse(clientId, clientSecret, redirectUrl, code, state, url));
      			
            }
            
      	});
      
   }

   public String getResponse(String clientId, String clientSecret, String redirectUrl, String code, String state, String urlString) throws Exception {
	   
      URL url = new URL(urlString);
      
      HttpURLConnection c = (HttpURLConnection) url.openConnection();
      
      c.setRequestMethod("POST");
      
      c.setRequestProperty("Content-length", "0");
      
      c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      
      c.setRequestProperty("Accept", "application/json");
      
      c.setUseCaches(false);
      
      c.setAllowUserInteraction(false);
      
      c.setDoOutput(true);
      
      OutputStreamWriter writer = new OutputStreamWriter(c.getOutputStream());
	    
      writer.write("code=" + code + "&client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=" + redirectUrl + "&grant_type=authorization_code&state=" + state);
	    
      writer.flush();
      
      c.connect();
      
      int status = c.getResponseCode();

      switch (status) {
      
      	case 200:
   
      	case 201:
        
      		BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            
      		StringBuilder sb = new StringBuilder();
            
      		String line;
            
      		while ((line = br.readLine()) != null) {
               sb.append(line).append("\n");
            }
            
      		br.close();
      		
      		//Extraigo los valores del JSON de respuesta, Germán González (13-02-2017)
      		ObjectMapper objectMapper = new ObjectMapper();
      		
      		Token tokenJSON = objectMapper.readValue(sb.toString(), Token.class);
      		
      		String access_token = tokenJSON.getAccess_token();
      		
      	    String token_type = tokenJSON.getToken_type();
      		
      	    Double expires_in = tokenJSON.getExpires_in();
      	    
      	    String id_token = tokenJSON.getId_token();
      	    
      	    return "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'><soap:Body><loginClaveUnicaTokenResponse xmlns='http://tempuri.org/'><loginClaveUnicaTokenResponse><access_token>" + access_token + "</access_token><token_type>" + token_type + "</token_type><expires_in>" + expires_in + "</expires_in><id_token>" + id_token + "</id_token></loginClaveUnicaTokenResponse></loginClaveUnicaTokenResponse></soap:Body></soap:Envelope>";
      		
      	default:

      		return "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'><soap:Body><soap:Fault><faultCode>" + Integer.toString(status) + "</faultCode><faultString>" + c.getResponseMessage() + ": Invalid Code</faultString></soap:Fault></soap:Body></soap:Envelope>";
      		
      }
      
   }
   
}