/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fun.platonic.pulsar.crawl.scoring;

import fun.platonic.pulsar.common.config.ReloadableParameterized;
import fun.platonic.pulsar.crawl.index.IndexDocument;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.graph.WebEdge;
import fun.platonic.pulsar.persist.graph.WebGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * A contract defining behavior of scoring plugins.
 * <p>
 * A scoring filter will manipulate scoring variables in CrawlDatum and in
 * resulting search indexes. Filters can be chained in a specific order, to
 * provide multi-stage scoring adjustments.
 *
 * @author Andrzej Bialecki
 */
public interface ScoringFilter extends ReloadableParameterized {
    Logger LOG = LoggerFactory.getLogger(ScoringFilter.class);

    /**
     * Set an initial score for newly injected pages. Note: newly injected pages
     * may have no inlinks, so filter implementations may wish to set this score
     * to a non-zero value, to give newly injected pages some initial credit.
     *
     * @param page new page. Filters will modify it in-place.
     */
    default void injectedScore(WebPage page) {
    }

    /**
     * Set an initial score for newly discovered pages. Note: newly discovered
     * pages have at least one inlink with its score contribution, so filter
     * implementations may choose to set initial score to zero (unknown value),
     * and then the inlink score contribution will set the "real" value of the new
     * page.
     *
     * @param page page row.
     */
    default void initialScore(WebPage page) {
    }

    /**
     * This method prepares a sort value for the purpose of sorting and selecting
     * top N scoring pages during fetchlist generation.
     *
     * @param page     page row. Modifications will be persisted.
     * @param initSort initial sort value, or a value from previous filters in chain
     */
    default ScoreVector generatorSortValue(WebPage page, float initSort) {
        return new ScoreVector("1", (int) (page.getScore() * initSort));
    }

    /**
     * Distribute score value from the current page to all its outlinked pages.
     *
     * @param page         page row
     * @param outLinkEdges A list of {@link WebEdge}s for every outlink. These
     *                     {@link WebEdge}s will be passed to
     *                     {@link #updateScore(WebPage, WebGraph, Collection)} for every
     *                     outlinked URL.
     * @param allCount     number of all collected outlinks from the source page
     */
    default void distributeScoreToOutlinks(WebPage page, WebGraph graph, Collection<WebEdge> outLinkEdges, int allCount) {
    }

    /**
     * This method calculates a new score during table update, based on the values
     * contributed by inlinked pages.
     *
     * @param page        page row
     * @param inLinkEdges list of {@link WebEdge}s for all inlinks pointing to
     *                    this URL.
     */
    default void updateScore(WebPage page, WebGraph graph, Collection<WebEdge> inLinkEdges) {
    }

    default void updateContentScore(WebPage page) {
    }

    /**
     * This method calculates a Lucene document boost.
     *
     * @param url       url of the page
     * @param doc       document. NOTE: this already contains all information collected by
     *                  indexing filters. Implementations may modify this instance, in
     *                  order to store/remove some information.
     * @param page      page row
     * @param initScore initial boost value for the Lucene document.
     * @return boost value for the Lucene document. This value is passed as an
     * argument to the next scoring filter in chain. NOTE: implementations
     * may also express other scoring strategies by modifying Lucene
     * document directly.
     */
    default float indexerScore(String url, IndexDocument doc, WebPage page, float initScore) {
        return 0.0f;
    }
}
