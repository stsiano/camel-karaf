/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.core.osgi;

import org.apache.camel.TypeConverter;
import org.apache.camel.core.osgi.utils.BundleContextUtils;
import org.apache.camel.core.osgi.utils.BundleDelegatingClassLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.support.DefaultRegistry;
import org.osgi.framework.BundleContext;

public class OsgiDefaultCamelContext extends DefaultCamelContext {

    private final BundleContext bundleContext;

    public OsgiDefaultCamelContext(BundleContext bundleContext) {
        super(false);

        // remove the OnCamelContextLifecycleStrategy that camel-core adds by default which does not work well for OSGi
        getLifecycleStrategies().removeIf(l -> l.getClass().getSimpleName().contains("OnCamelContextLifecycleStrategy"));

        this.bundleContext = bundleContext;

        // inject common osgi
        OsgiCamelContextHelper.osgiUpdate(this, bundleContext);

        // and these are blueprint specific
        OsgiBeanRepository repo1 = new OsgiBeanRepository(bundleContext);
        setRegistry(new DefaultRegistry(repo1));
        // Need to clean up the OSGi service when camel context is closed.
        addLifecycleStrategy(repo1);
        // setup the application context classloader with the bundle classloader
        setApplicationContextClassLoader(new BundleDelegatingClassLoader(bundleContext.getBundle()));

        init();
    }

    @Override
    protected TypeConverter createTypeConverter() {
        // CAMEL-3614: make sure we use a bundle context which imports org.apache.camel.impl.converter package
        BundleContext ctx = BundleContextUtils.getBundleContext(getClass());
        if (ctx == null) {
            ctx = bundleContext;
        }
        FactoryFinder finder = new OsgiFactoryFinderResolver(bundleContext).resolveDefaultFactoryFinder(getClassResolver());
        return new OsgiTypeConverter(ctx, this, getInjector(), finder);
    }

}
