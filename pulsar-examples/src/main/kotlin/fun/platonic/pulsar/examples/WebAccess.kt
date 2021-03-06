package `fun`.platonic.pulsar.examples

import `fun`.platonic.pulsar.common.PulsarContext
import `fun`.platonic.pulsar.common.options.LoadOptions
import `fun`.platonic.pulsar.persist.WebPageFormatter

object WebAccess {
    private val productPortalUrl = "http://list.mogujie.com/book/jiadian/10059513"
    private val newsPortalUrl = "http://www.sh.chinanews.com/jinrong/index.shtml"
    private val loadOptions = "--parse --reparse-links --no-link-filter --expires=1s --fetch-mode=selenium --browser=chrome"
    private val i = PulsarContext.createSession()

    fun load() {
        val page = i.load("$productPortalUrl $loadOptions")
        println(WebPageFormatter(page).withLinks())

        val document = i.parse(page)
        val title = document.first(".goods_item .title")?.text()
        println(title)
    }

    fun loadAllProducts() {
        val portal = i.load("$productPortalUrl $loadOptions")
        val doc = i.parse(portal)
        doc.select(".goods_item a[href~=detail]")
                .map { it.attr("abs:href").substringBefore("?") }
                .forEach { portal.vividLinks[it] = "" }
        println(WebPageFormatter(portal))
        println(portal.simpleVividLinks)
        val links = portal.simpleLiveLinks.filter { it.contains("item") }
        val pages = i.parallelLoadAll(links, LoadOptions.parse("--parse"))
        pages.forEach { println("${it.url} ${it.pageTitle}") }
    }

    fun loadAllNews() {
        val portal = i.load("$newsPortalUrl $loadOptions")
        val links = portal.simpleLiveLinks.filter { it.contains("jinrong") }
        val pages = i.parallelLoadAll(links, LoadOptions.parse("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }

    fun piped() {
        arrayOf(newsPortalUrl)
                .map { i.load(it) }
                .map { i.parse(it) }
                .forEach { println("${it.baseUri} ${it.title}") }
    }
}

fun main(args: Array<String>) {
    WebAccess.load()
    // WebAccess.loadAllProducts()
}
