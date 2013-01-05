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
package org.apache.webbeans.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;

import javax.decorator.Decorator;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.interceptor.Interceptor;

import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.ProducerFieldBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.portable.creation.InjectionTargetProducer;
import org.apache.webbeans.portable.creation.ProducerBeansProducer;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.WebBeansAnnotatedTypeUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Configures Simple WebBeans Component.
 * <p>
 * Contains useful static methods for creating Simple WebBeans Components.
 * </p>
 *
 * @version $Rev$ $Date$
 */
public final class ManagedBeanConfigurator
{

    private final WebBeansContext webBeansContext;

    public ManagedBeanConfigurator(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Returns the newly created Simple WebBean Component.
     *
     * @param clazz Simple WebBean Component implementation class
     * @return the newly created Simple WebBean Component
     * @throws WebBeansConfigurationException if any configuration exception occurs
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public <T> ManagedBean<T> define(Class<T> clazz, WebBeansType type, AnnotatedType<T> anntotatedType) throws WebBeansConfigurationException
    {
        BeanManagerImpl manager = webBeansContext.getBeanManagerImpl();
        DefinitionUtil definitionUtil = webBeansContext.getDefinitionUtil();
        WebBeansAnnotatedTypeUtil annotatedTypeUtil = webBeansContext.getAnnotatedTypeUtil();

        int modifier = clazz.getModifiers();

        if (AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && AnnotationUtil.hasClassAnnotation(clazz, Interceptor.class))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + clazz.getName()
                                                     + " may not annotated with both @Interceptor and @Decorator annotation");
        }

        if (!AnnotationUtil.hasClassAnnotation(clazz, Decorator.class) && !AnnotationUtil.hasClassAnnotation(clazz, Interceptor.class))
        {
            webBeansContext.getInterceptorUtil().checkSimpleWebBeansInterceptorConditions(clazz);
        }

        if (Modifier.isInterface(modifier))
        {
            throw new WebBeansConfigurationException("ManagedBean implementation class : " + clazz.getName() + " may not _defined as interface");
        }

        ManagedBean<T> component = new ManagedBean<T>(clazz, type, anntotatedType, webBeansContext);
        manager.putProducer(component, new InjectionTargetProducer(component));

        webBeansContext.getWebBeansUtil().setInjectionTargetBeanEnableFlag(component);

        definitionUtil.defineSerializable(component);
        definitionUtil.defineStereoTypes(component, clazz.getDeclaredAnnotations());

        Annotation[] clazzAnns = clazz.getDeclaredAnnotations();

        definitionUtil.defineApiTypes(component, clazz);
        definitionUtil.defineScopeType(component, clazzAnns, "Simple WebBean Component implementation class : " + clazz.getName()
                                                             + " stereotypes must declare same @Scope annotations", false);
        // we fully initialize the bean in this case.
        component.setFullInit(true);

        WebBeansUtil.checkGenericType(component);
        definitionUtil.defineName(component, clazzAnns, WebBeansUtil.getManagedBeanDefaultName(clazz.getSimpleName()));
        definitionUtil.defineQualifiers(component, clazzAnns);

        Constructor<T> constructor = webBeansContext.getWebBeansUtil().defineConstructor(clazz);
        component.setConstructor(constructor);
        definitionUtil.addConstructorInjectionPointMetaData(component, constructor);

        //Dropped from the speicification
        //WebBeansUtil.checkSteroTypeRequirements(component, clazz.getDeclaredAnnotations(), "Simple WebBean Component implementation class : " + clazz.getName());

        Set<ProducerMethodBean<?>> producerMethods = annotatedTypeUtil.defineProducerMethods(component, component.getAnnotatedType());
        for (ProducerMethodBean<?> producerMethod : producerMethods)
        {
            // add them one after the other to enable serialization handling et al
            manager.addBean(producerMethod);
            manager.putProducer(producerMethod, new ProducerBeansProducer(producerMethod));
        }

        Set<ProducerFieldBean<?>> producerFields = annotatedTypeUtil.defineProducerFields(component, component.getAnnotatedType());
        for (ProducerFieldBean<?> producerField : producerFields)
        {
            // add them one after the other to enable serialization handling et al
            manager.addBean(producerField);
            manager.putProducer(producerField, new ProducerBeansProducer(producerField));
        }


        annotatedTypeUtil.defineDisposalMethods(component, component.getAnnotatedType());
        annotatedTypeUtil.defineInjectedFields(component, component.getAnnotatedType());
        annotatedTypeUtil.defineInjectedMethods(component, component.getAnnotatedType());
        annotatedTypeUtil.defineObserverMethods(component, component.getAnnotatedType());

        return component;
    }
}
