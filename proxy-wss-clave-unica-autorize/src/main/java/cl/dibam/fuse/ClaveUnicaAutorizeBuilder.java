package cl.dibam.fuse;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.net.HttpURLConnection;
import java.net.URL;

public class ClaveUnicaAutorizeBuilder extends RouteBuilder {

   public void configure() {

	   from("cxf:bean:proxyClaveUnicaAutorizeService")
            
	    .convertBodyTo(String.class)
	   
	   	.to("log:input")
            
      	// send the request to the route to handle the operation
        // the name of the operation is in that header
        .recipientList(simple("direct:loginClaveUnicaAutorize"));

	   from("direct:loginClaveUnicaAutorize")
            
      	.process(new Processor() {
            
      		public void process(Exchange exchange) throws Exception {
                
      			Message in = exchange.getIn();
                
      			String body = (String) in.getBody();
      			
      			String clientId = body.substring(body.indexOf("<clientId>") + 1, body.indexOf("</clientId>"));
                 
      			clientId = clientId.replace("clientId>", "");
      			
      			String redirectUrl = body.substring(body.indexOf("<redirectUrl>") + 1, body.indexOf("</redirectUrl>"));

      			redirectUrl = redirectUrl.replace("redirectUrl>", "");
      			
      			redirectUrl = redirectUrl.replace(":", "%3A");
      			
      			redirectUrl = redirectUrl.replace("/", "%2F");
      			
      			String state = body.substring(body.indexOf("<state>") + 1, body.indexOf("</state>"));

      			state = state.replace("state>", "");
      			
      			String session = body.substring(body.indexOf("<session>") + 1, body.indexOf("</session>"));

      			session = session.replace("session>", "");
      			
      			String url = "https://accounts.claveunica.gob.cl/openid/authorize?client_id=" + clientId + "&redirect_uri=" + redirectUrl + "&response_type=code&scope=openid%20run%20name&state=" + state;
                  
      			in.setBody(getResponse(url, session));
      			
            }
            
      	});
      
   }

   public String getResponse(String urlString, String session) throws Exception {
	   
      URL url = new URL(urlString);
      
      HttpURLConnection c = (HttpURLConnection) url.openConnection();
      
      c.setRequestMethod("GET");
      
      c.setRequestProperty("Content-length", "0");
      
      c.setRequestProperty("Cookie", "sessionid=" + session);
      
      c.setUseCaches(false);
      
      c.setAllowUserInteraction(false);
      
      c.connect();
      
      int status = c.getResponseCode();

      switch (status) {
      
      	case 200:
   
      	case 201:
      		
      	case 302:
        
      		String respuesta = c.getHeaderField("Location");
      		
      		if(respuesta == null){
      		
      			return "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'><soap:Body><soap:Fault><faultCode>500</faultCode><faultString>Internal Server Error: Invalid Session</faultString></soap:Fault></soap:Body></soap:Envelope>";
      			
      		}else{
      			
      			String state = respuesta.substring(respuesta.indexOf("state=") + 1, respuesta.indexOf("&"));
      			
      			state = state.replace("tate=", "");
      		
      			String code = respuesta.substring(respuesta.indexOf("code=") + 1);
      			
      			code = code.replace("ode=", "");
      		
      			return "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'><soap:Body><loginClaveUnicaAutorizeResponse xmlns='http://tempuri.org/'><loginClaveUnicaAutorizeResponse><state>" + state + "</state><code>" + code + "</code></loginClaveUnicaAutorizeResponse></loginClaveUnicaAutorizeResponse></soap:Body></soap:Envelope>";
      			
      		}
      		
      	default:

      		return "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'><soap:Body><soap:Fault><faultCode>" + Integer.toString(status) + "</faultCode><faultString>" + c.getResponseMessage() + "</faultString></soap:Fault></soap:Body></soap:Envelope>";
      		
      }
      
   }
   
}