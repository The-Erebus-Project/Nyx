package io.github.vizanarkonin.nyx.Handlers;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * There are cases when we need to access a bean/component from a type, which is not declared component/controller/service.
 * In this case Autowiring won't work.
 * To go around it - we're going to use this type - it uses ApplicationContextAware, which allows us to retrieve initiated beans by their class
 */
@Component
public class ServiceNexus implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        ServiceNexus.context = context;
    }

    public static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }
}