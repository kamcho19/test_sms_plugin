package com.telepo.plugin.sms.example;

import com.telepo.plugin.sms.AsyncSmsPlugin;
import com.telepo.plugin.sms.PluginAttribute;
import com.telepo.plugin.sms.SmsCallback;
import com.telepo.plugin.sms.SmsException;
import com.telepo.plugin.sms.SmsException.ReasonCode;
import com.telepo.plugin.sms.SmsOrigin;
import com.telepo.plugin.sms.SmsPlugin;
import com.telepo.plugin.sms.SmsPluginCapability;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExamplePlugin implements AsyncSmsPlugin, SmsPluginCapability, BundleActivator {

    private static ServiceRegistration serviceRegistration;
    private static org.osgi.framework.Version bundleVersion;
    private static final List<Locale> supportedLocales;
    private Map<String, String> settings;

    static {
        Locale[] locales = {
                Locale.ENGLISH,
                Locale.GERMAN,
                Locale.FRENCH,
                new Locale("sv"),
                new Locale("da"),
                new Locale("nl"),
                new Locale("nl_BE")};

        supportedLocales = Arrays.asList(locales);
    }

    public ExamplePlugin() {
        // Public constructor used only when instantiating as plugin factory.
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        serviceRegistration = bundleContext.registerService(
                SmsPlugin.class.getName(), new ExamplePlugin(), null);
        bundleVersion = bundleContext.getBundle().getVersion();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        serviceRegistration.unregister();
    }

    @Override
    public List<PluginAttribute> getAttributes(Locale locale) {
        List<PluginAttribute> attributeList = new LinkedList<>();
        attributeList.add(new PluginAttribute("First",
                "First attribute, only characters",
                "[-a-zA-Z]+",
                PluginAttribute.AttributeType.TEXT));
        attributeList.add(new PluginAttribute("Second",
                "Second attribute, password",
                "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]+",
                PluginAttribute.AttributeType.PASSWORD));
        attributeList.add(new PluginAttribute("Third",
                "Third attribute, only digits (values 0-7 will generate errors when sending messages",
                "[-0-9]+",
                PluginAttribute.AttributeType.TEXT));

        return attributeList;
    }

    @Override
    public String getDescription(Locale locale) {
        return "A plugin skeleton implementation";
    }

    @Override
    public String getName() {
        return "ExamplePlugin";
    }

    @Override
    public Version getVersion() {
        return new Version(bundleVersion.getMajor(), bundleVersion.getMinor(), bundleVersion.getMicro());
    }

    @Override
    public SmsPlugin instance(Map<String, String> attributeNameToValues) throws SmsException {
        ExamplePlugin plugin = new ExamplePlugin();
        plugin.loadSettings(attributeNameToValues);
        return plugin;
    }

    private void loadSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    @Override
    public void send(String toNumber, String fromNr, String fromName, String text) throws SmsException {
        throw new SmsException("Synchronous sending is not supported");
    }

    @SuppressWarnings({"ThrowFromFinallyBlock", "ConstantConditions"})
    @Override
    public void send(String toNumber, String fromNr, String fromName, String text, SmsOrigin origin, SmsCallback callback) {
        int error = Integer.parseInt(settings.get("Third"));
        SmsException exception = null;
        switch (error) {
            case 0:
                exception = new SmsException(ReasonCode.ACCOUNT_FROZEN, "account frozen");
                break;
            case 1:
                exception = new SmsException(ReasonCode.CONNECT_FAILED, "connect failed");
                break;
            case 2:
                exception = new SmsException(ReasonCode.INVALID_CONFIGURATION, "invalid configuration");
                break;
            case 3:
                exception = new SmsException(ReasonCode.INVALID_CREDENTIALS, "invalid credentials");
                break;
            case 4:
                exception = new SmsException(ReasonCode.INVALID_NUMBER, "invalid number");
                break;
            case 5:
                exception = new SmsException(ReasonCode.MAX_LENGTH_EXCEEDED, "max length exceeded");
                break;
            case 6:
                exception = new SmsException(ReasonCode.NO_CREDITS_LEFT, "no credits left");
                break;
            case 7:
                exception = new SmsException(ReasonCode.OTHER_ERROR, "other error");
                break;
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("/tmp/sms-log", true));
            bw.write(new Date().toString());
            bw.newLine();
            bw.write("First setting  :" + settings.get("First"));
            bw.newLine();
            bw.write("Second setting :" + settings.get("Second"));
            bw.newLine();
            bw.write("Third setting  :" + settings.get("Third"));
            bw.newLine();
            bw.write("to number      :" + toNumber);
            bw.newLine();
            bw.write("from number    :" + fromNr);
            bw.newLine();
            bw.write("from           :" + fromName);
            bw.newLine();
            bw.write("-----------");
            bw.newLine();
            bw.write(text);
            bw.newLine();
            bw.write("-----------");
            if (exception != null) {
                bw.newLine();
                bw.write("Generating error: " + exception.getMessage());
                bw.newLine();
                bw.write("-----------");
            }
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (exception != null) {
                callback.onFailure(exception);
            }
        }

        callback.onSent();
    }

	@Override
	public boolean isLangSupported(Locale locale) {
		return supportedLocales.contains(locale);
	}

	@Override
	public String getMaxSMSLength() {
		return "160";
	}
}
