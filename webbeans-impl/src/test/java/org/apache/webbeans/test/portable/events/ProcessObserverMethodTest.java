/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.portable.events;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.spi.Context;
import javax.enterprise.event.Observes;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.portable.events.extensions.ProcessObserverMethodExtension;
import org.junit.Assert;
import org.junit.Test;

public class ProcessObserverMethodTest extends AbstractUnitTest
{

    @Test
    public void testProcessObserverMethodIsInvoked()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(MyObserver.class);

        addExtension(new ProcessObserverMethodExtension.BrokenExtension());  
        addExtension(new ProcessObserverMethodExtension());
        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertTrue(ProcessObserverMethodExtension.processObserverMethodInvoked);
        
        shutDownContainer();
    }

    public static class MyObserver {
        public void observe(@Observes Context context) {
            // just any observer
        }
    }
}
