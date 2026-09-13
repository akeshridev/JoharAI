package com.akeshridev.johar.data.source

import com.akeshridev.johar.domain.crawl.CrawlTarget

internal object SourceCatalog {
    fun resolve(target: CrawlTarget): SourceDescriptor = when (target) {
        CrawlTarget.DASSAM_FALLS -> SourceDescriptor(
            entityId = "dassam_falls",
            url = "https://ranchi.nic.in/tourist-place/dassam-fall/",
            publisher = "Ranchi District Administration",
        )
    }
}
