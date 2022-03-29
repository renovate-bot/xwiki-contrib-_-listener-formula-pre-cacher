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

import org.xwiki.job.AbstractRequest;

/**
 * Job request for the {@link FormulaPreCachingJob}.
 *
 * @version $Id$
 * @since 1.0
 */
public class FormulaPreCachingJobRequest extends AbstractRequest
{
    /**
     * The key used to store the wiki ID in the request.
     */
    public static final String KEY_WIKI_ID = "wikiId";

    /**
     * Create a new request.
     *
     * @param wikiId the wiki ID to pre-cache.
     */
    public FormulaPreCachingJobRequest(String wikiId)
    {
        super();
        this.setWikiId(wikiId);
    }

    /**
     * @return the wiki id
     */
    public String getWikiId()
    {
        return this.getProperty(KEY_WIKI_ID);
    }

    /**
     * @param wikiId the wiki id
     */
    public void setWikiId(String wikiId)
    {
        this.setProperty(KEY_WIKI_ID, wikiId);
    }
}
