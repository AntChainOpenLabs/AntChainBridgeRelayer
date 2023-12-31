
package com.alipay.antchain.bridge.relayer.core.types.network.ws.client.generated;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.3.2
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "WSRelayerServerAPImplService", targetNamespace = "http://ws.offchainapi.oracle.mychain.alipay.com/", wsdlLocation = "http://127.0.0.1:8082/WSEndpointServer?wsdl")
public class WSRelayerServerAPImplService
    extends Service
{

    private final static URL WSRELAYERSERVERAPIMPLSERVICE_WSDL_LOCATION;
    private final static WebServiceException WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION;
    private final static QName WSRELAYERSERVERAPIMPLSERVICE_QNAME = new QName("http://ws.offchainapi.oracle.mychain.alipay.com/", "WSRelayerServerAPImplService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://127.0.0.1:8082/WSEndpointServer?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        WSRELAYERSERVERAPIMPLSERVICE_WSDL_LOCATION = url;
        WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION = e;
    }

    public WSRelayerServerAPImplService() {
        super(__getWsdlLocation(), WSRELAYERSERVERAPIMPLSERVICE_QNAME);
    }

    public WSRelayerServerAPImplService(WebServiceFeature... features) {
        super(__getWsdlLocation(), WSRELAYERSERVERAPIMPLSERVICE_QNAME, features);
    }

    public WSRelayerServerAPImplService(URL wsdlLocation) {
        super(wsdlLocation, WSRELAYERSERVERAPIMPLSERVICE_QNAME);
    }

    public WSRelayerServerAPImplService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, WSRELAYERSERVERAPIMPLSERVICE_QNAME, features);
    }

    public WSRelayerServerAPImplService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public WSRelayerServerAPImplService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns WSRelayerServerAPImpl
     */
    @WebEndpoint(name = "WSRelayerServerAPImplPort")
    public WSRelayerServerAPImpl getWSRelayerServerAPImplPort() {
        return super.getPort(new QName("http://ws.offchainapi.oracle.mychain.alipay.com/", "WSRelayerServerAPImplPort"), WSRelayerServerAPImpl.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WSRelayerServerAPImpl
     */
    @WebEndpoint(name = "WSRelayerServerAPImplPort")
    public WSRelayerServerAPImpl getWSRelayerServerAPImplPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ws.offchainapi.oracle.mychain.alipay.com/", "WSRelayerServerAPImplPort"), WSRelayerServerAPImpl.class, features);
    }

    private static URL __getWsdlLocation() {
        if (WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION!= null) {
            throw WSRELAYERSERVERAPIMPLSERVICE_EXCEPTION;
        }
        return WSRELAYERSERVERAPIMPLSERVICE_WSDL_LOCATION;
    }

}
