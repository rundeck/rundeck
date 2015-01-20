/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* HibernateSessionBinderJobListener.java
* 
* User: greg
* Created: Jul 16, 2007 3:57:32 PM
* $Id: GrailsServiceInjectorJobListener.java 452 2008-02-13 01:02:24Z ahonor $
*/
package com.dtolabs.rundeck.core.utils;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;

import java.util.Map;


/**
 * GrailsServiceInjectorJobListener is a JobListener that supplies the executed job with entities configured
 * via spring injection.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 452 $
 */
public class GrailsServiceInjectorJobListener extends JobListenerSupport {
    private static final transient Logger LOG = Logger.getLogger(GrailsServiceInjectorJobListener.class);
    private String name;
    private Map services;
    private Scheduler quartzScheduler;

    public GrailsServiceInjectorJobListener(){

    }
    public GrailsServiceInjectorJobListener(String name, Map services, Scheduler quartzScheduler)
        throws SchedulerException {
        this.name = name;
        this.services = services;
        setQuartzScheduler(quartzScheduler);
        LOG.info("GrailsServiceInjectorJobListener initialized, added to quartzScheduler");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void jobToBeExecuted(JobExecutionContext context) {
        context.getJobDetail().getJobDataMap().putAll(services);
        LOG.info("services injected for job");
    }

    public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception) {
    }


    public Map getServices() {
        return services;
    }

    public void setServices(Map services) {
        this.services = services;
    }

    public Scheduler getQuartzScheduler() {
        return quartzScheduler;
    }

    public void setQuartzScheduler(Scheduler quartzScheduler) throws SchedulerException {
        this.quartzScheduler = quartzScheduler;

        quartzScheduler.getListenerManager().addJobListener(this);
    }
}
