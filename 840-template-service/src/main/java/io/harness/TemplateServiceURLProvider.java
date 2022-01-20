package io.harness;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

public class TemplateServiceURLProvider {
    private final String url;

    public TemplateServiceURLProvider(String url) {
        this.url = url;
    }

//    public String getServerName() {
//        Client client = ClientBuilder.newClient();
//        Response response = client.target(url + "/accounts/summary/accountId123?clusterType=FREE").request().get();
//        String result = response.readEntity(String.class);
//        response.close();
//        client.close();
//        return result;
//    }

    //    public String getEdition() {
//        Client client = ClientBuilder.newClient();
//        Response response = client.target(url + "/system/properties/key/wlp.user.dir.isDefault").request().get();
//        String result = response.readEntity(String.class);
//        response.close();
//        client.close();
//        return result;
//    }
//
    public String getVersion() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(url + "/api/gateway-version-pact").request().get();
        String result = response.readEntity(String.class);
        response.close();
        client.close();
        return result;

    }

    public String getInvalidProperty() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(url + "/invalidurl").request().get();
        String result = response.readEntity(String.class);
        response.close();
        client.close();
        return result;
    }
}
