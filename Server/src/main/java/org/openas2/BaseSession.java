package org.openas2;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.Nullable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.cert.CertificateFactory;
import org.openas2.partner.PartnershipFactory;
import org.openas2.processor.Processor;


public abstract class BaseSession implements Session {

    private static final Log LOGGER = LogFactory.getLog(Session.class.getSimpleName());
    private Map<String, Component> components = new LinkedHashMap<String, Component>();
    private String baseDirectory;
    @Nullable
    private Thread shutdownHook;

    /**
     * Creates a <code>BaseSession</code> object, then calls the <code>init()</code> method.
     *
     * @throws OpenAS2Exception
     * @see #init()
     */
    public BaseSession() throws OpenAS2Exception
    {
        init();
    }

    @Override
    public void start() throws OpenAS2Exception
    {
        getProcessor().startActiveModules();
    }

    @Override
    public void stop() throws Exception
    {
        Processor processor = getProcessor();
        processor.destroy();

        for (Component component : components.values())
        {
            if (!component.equals(processor))
            {
                component.destroy();
            }
        }

        unregisteredShutdownHook();
    }

    private void unregisteredShutdownHook()
    {
        if (shutdownHook != null)
        {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    public CertificateFactory getCertificateFactory() throws ComponentNotFoundException
    {
        return (CertificateFactory) getComponent(CertificateFactory.COMPID_CERTIFICATE_FACTORY);
    }

    /**
     * Registers a component to a specified ID.
     *
     * @param componentID registers the component to this ID
     * @param comp        component to register
     * @see Component
     */
    void setComponent(String componentID, Component comp)
    {
        Map<String, Component> objects = getComponents();
        objects.put(componentID, comp);
    }

    public Component getComponent(String componentID) throws ComponentNotFoundException
    {
        Map<String, Component> comps = getComponents();
        Component comp = comps.get(componentID);

        if (comp == null)
        {
            throw new ComponentNotFoundException(componentID);
        }

        return comp;
    }

    public Map<String, Component> getComponents()
    {
        return components;
    }

    public PartnershipFactory getPartnershipFactory() throws ComponentNotFoundException
    {
        return (PartnershipFactory) getComponent(PartnershipFactory.COMPID_PARTNERSHIP_FACTORY);
    }

    public Processor getProcessor() throws ComponentNotFoundException
    {
        return (Processor) getComponent(Processor.COMPID_PROCESSOR);
    }

    /**
     * This method is called by the <code>BaseSession</code> constructor to set up any global
     * configuration settings.
     *
     * @throws OpenAS2Exception If an error occurs while initializing systems
     */
    protected void init() throws OpenAS2Exception
    {
        initJavaMail();
    }

    /**
     * Adds a group of content handlers to the Mailcap <code>CommandMap</code>. These handlers are
     * used by the JavaMail API to encode and decode information of specific mime types.
     *
     * @throws OpenAS2Exception If an error occurs while initializing mime types
     */
    private void initJavaMail() throws OpenAS2Exception
    {
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap(
                "message/disposition-notification;; x-java-content-handler=org.openas2.util.DispositionDataContentHandler");
        CommandMap.setDefaultCommandMap(mc);
    }

    public String getBaseDirectory()
    {
        return baseDirectory;
    }

    void setBaseDirectory(String dir)
    {
        baseDirectory = dir;
    }

    public void registerShutdownHook()
    {
        this.shutdownHook = new Thread() {
            @Override
            public void run()
            {
                try
                {
                    BaseSession.this.stop();
                } catch (Exception e)
                {
                    //nothing here
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        LOGGER.info("Shutdown hook registered.");
    }
}
