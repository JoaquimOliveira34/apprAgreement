package client;

import dispatcher.DispatcherFactory;

public interface RunnableConfigurable extends Runnable{

    void setConfigurationThenRun(ClientEventsRegister register, DispatcherFactory factory, String logsPathName );

}
