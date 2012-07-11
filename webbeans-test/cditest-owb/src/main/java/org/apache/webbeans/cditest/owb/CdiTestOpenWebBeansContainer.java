/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.cditest.owb;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.ResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;

import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.context.type.ContextTypes;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.util.InjectionExceptionUtils;

/**
 * OpenWebBeans specific implementation of {@link CdiTestContainer}.
 */
public class CdiTestOpenWebBeansContainer implements CdiTestContainer 
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(CdiTestOpenWebBeansContainer.class);

    private ContainerLifecycle  lifecycle = null;
    private MockServletContext  servletContext = null;
    private MockHttpSession     session = null;

    public void bootContainer() throws Exception 
    {
        servletContext = new MockServletContext();
        session = new MockHttpSession();
        lifecycle = WebBeansContext.getInstance().getService(ContainerLifecycle.class);
        lifecycle.startApplication(new ServletContextEvent(servletContext));
    }

    public void shutdownContainer() throws Exception 
    {
        if (lifecycle != null) 
        {
            lifecycle.stopApplication(new ServletContextEvent(servletContext));
        }
    }

    public void startContexts() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        contextFactory.initSingletonContext(servletContext);
        contextFactory.initApplicationContext(servletContext);
        contextFactory.initSessionContext(session);
        contextFactory.initConversationContext(null);
        contextFactory.initRequestContext(null);
    }

    public void startApplicationScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        contextFactory.initApplicationContext(servletContext);
    }

    public void startConversationScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        contextFactory.initConversationContext(null);
    }

    public void startCustomScope(Class<? extends Annotation> scopeClass) throws Exception 
    {
        //X TODO
    }

    public void startRequestScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        contextFactory.initRequestContext(null);
    }

    public void startSessionScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        contextFactory.initSessionContext(session);
    }

    public void stopContexts() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        stopSessionScope();
        stopConversationScope();
        stopRequestScope();
        stopApplicationScope();

        Context context = contextFactory.getStandardContext(ContextTypes.SINGLETON);
        if(context != null && context.isActive())
        {
            contextFactory.destroySingletonContext(servletContext);
        }
        else
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn("destroy was called for an inactive context (" + Singleton.class.getName() + ")");
            }
        }

        //Comment out for OWB-502
        //ContextFactory.cleanUpContextFactory();
    }

    public void stopApplicationScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        Context context = contextFactory.getStandardContext(ContextTypes.APPLICATION);
        if(context != null && context.isActive())
        {
            contextFactory.destroyApplicationContext(servletContext);
        }
        else
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn("destroy was called for an inactive context (" + ApplicationScoped.class.getName() + ")");
            }
        }
    }

    public void stopConversationScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        Context context = contextFactory.getStandardContext(ContextTypes.CONVERSATION);
        if(context != null && context.isActive())
        {
            contextFactory.destroyConversationContext();
        }
        else
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn("destroy was called for an inactive context (" + ConversationScoped.class.getName() + ")");
            }
        }
    }

    public void stopCustomScope(Class<? extends Annotation> scopeClass) throws Exception 
    {
        //X TODO
    }

    public void stopRequestScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        Context context = contextFactory.getStandardContext(ContextTypes.REQUEST);
        if(context != null && context.isActive())
        {
            contextFactory.destroyRequestContext(null);
        }
        else
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn("destroy was called for an inactive context (" + RequestScoped.class.getName() + ")");
            }
        }
    }

    public void stopSessionScope() throws Exception 
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        ContextFactory contextFactory = webBeansContext.getContextFactory();

        Context context = contextFactory.getStandardContext(ContextTypes.SESSION);
        if(context != null && context.isActive())
        {
            contextFactory.destroySessionContext(session);
        }
        else
        {
            if(logger.wblWillLogWarn())
            {
                logger.warn("destroy was called for an inactive context (" + SessionScoped.class.getName() + ")");
            }
        }
    }
    
    public  BeanManager getBeanManager() 
    {
        return lifecycle.getBeanManager();
    }

    public <T> T getInstance(Class<T> type, Annotation... qualifiers)
    throws ResolutionException 
    {
        Set<Bean<?>> beans = getBeanManager().getBeans(type, qualifiers);
        if (beans == null || beans.isEmpty()) 
        {
            InjectionExceptionUtils.throwBeanNotFoundException(type, qualifiers);
        }

        if (beans.size() > 1) 
        {
            InjectionExceptionUtils.throwAmbiguousResolutionException(beans, type, null, qualifiers);
        }

        @SuppressWarnings("unchecked")
        Bean<T> bean = (Bean<T>)beans.iterator().next();

        @SuppressWarnings("unchecked")
        T instance = (T) getBeanManager().getReference(bean, type, getBeanManager().createCreationalContext(bean));
        return instance;
    }

    public Object getInstance(String name)
    throws ResolutionException 
    {
        //X getBeanManager().getELResolver();
        // TODO implement
        return null;
    }

}
