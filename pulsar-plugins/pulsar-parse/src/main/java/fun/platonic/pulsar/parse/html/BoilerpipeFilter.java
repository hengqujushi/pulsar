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
package fun.platonic.pulsar.parse.html;

import fun.platonic.pulsar.boilerpipe.document.TextDocument;
import fun.platonic.pulsar.boilerpipe.extractors.ChineseNewsExtractor;
import fun.platonic.pulsar.boilerpipe.sax.SAXInput;
import fun.platonic.pulsar.boilerpipe.utils.ProcessingException;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.parse.ParseFilter;
import fun.platonic.pulsar.crawl.parse.ParseResult;
import fun.platonic.pulsar.crawl.parse.html.ParseContext;
import fun.platonic.pulsar.crawl.parse.html.PrimerParser;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.PageCategory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static fun.platonic.pulsar.common.config.CapabilityTypes.METATAG_NAMES;

/**
 * Parse html document into fields
 */
public class BoilerpipeFilter implements ParseFilter {

    private static final Log LOG = LogFactory.getLog(BoilerpipeFilter.class.getName());

    private ImmutableConfig conf;
    private PrimerParser primerParser;
    private Set<String> metatagset = new HashSet<>();

    public BoilerpipeFilter(ImmutableConfig conf) {
        reload(conf);
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;
        this.primerParser = new PrimerParser(conf);
        // Specify whether we want a specific subset of metadata
        // by default take everything we can find
        String[] values = conf.getStrings(METATAG_NAMES, "*");
        for (String val : values) {
            metatagset.add(val.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public ImmutableConfig getConf() {
        return this.conf;
    }

    @Override
    public void filter(ParseContext parseContext) {
        WebPage page = parseContext.getPage();
        ParseResult parseResult = parseContext.getParseResult();

        extract(page, page.getEncodingOrDefault("UTF-8"));

        parseResult.setSuccessOK();
    }

    /**
     * Extract the page into fields
     */
    public TextDocument extract(@Nonnull WebPage page, @Nonnull String encoding) {
        Objects.requireNonNull(page);

        TextDocument doc = extract(page);

        if (doc == null) {
            return null;
        }

        page.setContentTitle(doc.getContentTitle());
        page.setContentText(doc.getTextContent());
        page.setPageCategory(PageCategory.valueOf(doc.getPageCategoryAsString()));
        page.updateContentPublishTime(doc.getPublishTime());
        page.updateContentModifiedTime(doc.getModifiedTime());

        long id = 1000;
        page.getPageModel().emplace(id, 0, "boilerpipe", doc.getFields());

        return doc;
    }

    private TextDocument extract(WebPage page) {
        Objects.requireNonNull(page);

        if (page.getContent() == null) {
            LOG.warn("Can not extract page without content, url : " + page.getUrl());
            return null;
        }

        try {
            if (page.getEncoding() == null) {
                primerParser.detectEncoding(page);
            }

            InputSource inputSource = page.getContentAsSaxInputSource();
            TextDocument doc = new SAXInput().parse(page.getBaseUrl(), inputSource);

            ChineseNewsExtractor extractor = new ChineseNewsExtractor();
            extractor.process(doc);

            return doc;
        } catch (ProcessingException e) {
            LOG.warn("Failed to extract text content by boilerpipe, " + e.getMessage());
        }

        return null;
    }
}
