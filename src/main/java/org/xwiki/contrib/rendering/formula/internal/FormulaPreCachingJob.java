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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.properties.BeanManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.TransformationContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Job for pre-caching formula content.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(FormulaPreCachingJob.JOB_TYPE)
public class FormulaPreCachingJob extends AbstractJob<FormulaPreCachingJobRequest, JobStatus>
{
    /**
     * The type of the job.
     */
    public static final String JOB_TYPE = "formula.precache";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    private BeanManager beanManager;

    @Inject
    private ComponentManagerManager componentManagerManager;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        Macro formulaMacro = getFormulaMacroForCurrentWiki(request.getWikiId());
        if (formulaMacro == null) {
            logger.info("Macro formula not present on wiki {}, skipping.", request.getWikiId());
            return;
        }
        // Start by getting a list of every document in the database
        Query query =
            queryManager.createQuery(
                "select doc.fullName, doc.language "
                    + "from XWikiDocument doc where doc.content like :likeFormula", Query.HQL)
                .bindValue("likeFormula", "%{{/formula}}%")
                .setWiki(request.getWikiId());

        List<Object[]> results = query.execute();

        XWikiContext xWikiContext = xWikiContextProvider.get();

        if (xWikiContext != null) {
            XWikiContext xwikiRenderingContext = xWikiContext.clone();
            xwikiRenderingContext.dropPermissions();

            for (Object[] result : results) {
                DocumentReference documentReference =
                    documentReferenceResolver.resolve(String.format("%s:%s", request.getWikiId(), result[0]));

                XWikiDocument document = xWikiContext.getWiki().getDocument(documentReference, xWikiContext);

                String documentLanguage = (String) result[1];
                if (StringUtils.isNotBlank(documentLanguage)) {
                    document = document.getTranslatedDocument(documentLanguage, xWikiContext);
                }
                logger.debug("Pre-caching formulas in document [{}], with language [{}]", documentReference, result[1]);
                preCacheMacrosInDocument(document, formulaMacro);
            }
        } else {
            logger.error("Failed to pre-cache document containing the formula macro. The XWikiContext is null.");
        }
    }

    private Macro getFormulaMacroForCurrentWiki(String wikiId) throws ComponentLookupException
    {
        ComponentManager currentWikiCM = this.componentManagerManager.getComponentManager("wiki:" + wikiId, false);
        if (currentWikiCM != null) {
            String formulaMacroId = "formula";
            if (currentWikiCM.hasComponent(Macro.class, formulaMacroId)) {
                return currentWikiCM.getInstance(Macro.class, formulaMacroId);
            }
        }
        return null;
    }

    private void preCacheMacrosInDocument(XWikiDocument document, Macro formulaMacro)
    {
        TransformationContext transformationContext = new TransformationContext(document.getXDOM(), Syntax.HTML_5_0,
            true);
        MacroTransformationContext macroTransformationContext = new MacroTransformationContext(transformationContext);
        for (Block block : document.getXDOM()
            .getBlocks(new MacroBlockMatcher(formulaMacro.getDescriptor().getId().getId()), Block.Axes.DESCENDANT)) {
            MacroBlock macroBlock = (MacroBlock) block;
            logger.debug("Pre-caching macro [{}] in document [{}]", macroBlock.getContent(), document);

            macroTransformationContext.setCurrentMacroBlock(macroBlock);

            try {
                Object macroParameters = formulaMacro.getDescriptor().getParametersBeanClass().newInstance();
                this.beanManager.populate(macroParameters, macroBlock.getParameters());
                ((Macro) formulaMacro).execute(macroParameters, macroBlock.getContent(), macroTransformationContext);

            } catch (Throwable e) {
                logger.error("Failed to pre-cache macro [{}] in document [{}]", macroBlock,
                    document.getDocumentReference(), e);
            }
        }
    }
}
