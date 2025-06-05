package kz.aday.bot.configuration;


import lombok.Getter;


@Getter
public class ServiceContainer {
    private static final ServiceContainer INSTANCE = new ServiceContainer();


    public ServiceContainer() {
    }

    public static ServiceContainer getInstance() {
        return INSTANCE;
    }

}
