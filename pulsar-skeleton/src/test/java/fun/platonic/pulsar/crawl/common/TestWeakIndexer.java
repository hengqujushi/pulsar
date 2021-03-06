package fun.platonic.pulsar.crawl.common;

import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.MetricsSystem;
import fun.platonic.pulsar.common.UrlUtil;
import fun.platonic.pulsar.common.WeakPageIndexer;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.crawl.fetch.TaskStatusTracker;
import fun.platonic.pulsar.persist.WebDb;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import org.apache.gora.store.DataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fun.platonic.pulsar.common.config.CapabilityTypes.CRAWL_ID;
import static fun.platonic.pulsar.common.config.PulsarConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * TODO: Test failed
 * */
@Ignore("TODO: Test failed")
public class TestWeakIndexer {
    public static final Logger LOG = LoggerFactory.getLogger(TestWeakIndexer.class);

    private MutableConfig conf;
    private WebDb webDb;
    private MetricsSystem metricsSystem;
    private WeakPageIndexer urlTrackerIndexer;
    private TaskStatusTracker taskStatusTracker;
    private DataStore<String, GWebPage> store;

    private List<CharSequence> exampleUrls = IntStream.range(10000, 10050)
            .mapToObj(i -> EXAMPLE_URL + "/" + i)
            .collect(Collectors.toList());

    private String exampleUrl;

    public TestWeakIndexer() {
        conf = new MutableConfig();
        conf.set(CRAWL_ID, "test");

        webDb = new WebDb(conf);
        metricsSystem = new MetricsSystem(webDb, conf);
        taskStatusTracker = new TaskStatusTracker(webDb, metricsSystem, conf);
        urlTrackerIndexer = new WeakPageIndexer(URL_TRACKER_HOME_URL, webDb);
        store = webDb.getStore();
    }

    @Before
    public void setup() {
        conf.set("storage.data.store.class", TOY_STORE_CLASS);
        exampleUrl = EXAMPLE_URL + "/" + DateTimeUtil.format(Instant.now(), "MMdd");
    }

    @After
    public void teardown() {
        // webDb.delete(exampleUrl);
        webDb.flush();
        webDb.close();

        LOG.debug("In shell: \nget '{}', '{}'", store.getSchemaName(), UrlUtil.reverseUrlOrEmpty(exampleUrl));
    }

    @Ignore("TODO: Test failed")
    @Test
    public void testWeakPageIndexer() {
        final int pageNo = 1;
        final String indexPageUrl = URL_TRACKER_HOME_URL + "/" + pageNo;
        webDb.delete(indexPageUrl);
        webDb.flush();

        urlTrackerIndexer.indexAll(exampleUrls);
        urlTrackerIndexer.commit();

        WebPage page = webDb.getOrNil(indexPageUrl);
        assertTrue(page.isNotNil());
        assertTrue(page.isInternal());
        assertEquals(exampleUrls.size(), page.getLiveLinks().size());

        urlTrackerIndexer.takeAll(pageNo);
        page = webDb.getOrNil(indexPageUrl);
        assertTrue(page.isNotNil());
        assertTrue(page.getLiveLinks().isEmpty());
    }

}
