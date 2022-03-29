/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.rendering.formula.internal;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * Will trigger a {@link FormulaPreCachingJob} when a wiki is ready.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named(FormulaPreCacherListener.LISTENER_NAME)
public class FormulaPreCacherListener extends AbstractEventListener
{
    /**
     * The listener name.
     */
    public static final String LISTENER_NAME = "FormulaPreCacherListener";

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    /**
     * Create the listener.
     */
    public FormulaPreCacherListener()
    {
        super(LISTENER_NAME, Arrays.asList(new ApplicationReadyEvent(), new WikiReadyEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String wikiId;
        if (event instanceof ApplicationReadyEvent) {
            wikiId = xWikiContextProvider.get().getMainXWiki();
        } else {
            wikiId = (String) source;
        }

        FormulaPreCachingJobRequest request = new FormulaPreCachingJobRequest(wikiId);
        logger.debug("Starting formula pre-caching job.");

        try {
            jobExecutor.execute(FormulaPreCachingJob.JOB_TYPE, request);
        } catch (JobException e) {
            logger.error("Failed to start formula pre-caching job.", e);
        }
    }
}
