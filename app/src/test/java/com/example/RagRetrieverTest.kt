package com.example

import com.example.data.database.MarketplaceItem
import com.example.data.rag.RagRetriever
import org.junit.Assert.*
import org.junit.Test

class RagRetrieverTest {

    private fun catalog() = listOf(
        MarketplaceItem(id="a1", titleAr="زاد المسلم", titleEn="Zad Muslim", type="APP", category="إسلامي / أدوات", sizeMb=10.0, rating=4.9, developerOrCreatorAr="مطور إسلامي", descriptionAr="حقيبة المسلم الشاملة أذكار الصباح والمساء"),
        MarketplaceItem(id="g1", titleAr="فارس الصحراء", titleEn="Desert Knight", type="GAME", category="ألعاب / مغامرات", sizeMb=120.0, rating=4.8, developerOrCreatorAr="ستوديو", descriptionAr="مغامرة في الصحراء"),
        MarketplaceItem(id="b1", titleAr="مقدمة ابن خلدون", titleEn="Ibn Khaldun", type="BOOK", category="كتب / تاريخ", sizeMb=5.0, rating=4.9, developerOrCreatorAr="ابن خلدون", descriptionAr="علم العمران البشري"),
        MarketplaceItem(id="m1", titleAr="تاريخ الأندلس", titleEn="Andalus", type="MOVIE", category="أفلام / وثائقي", sizeMb=300.0, rating=4.9, developerOrCreatorAr="الجزيرة", descriptionAr="وثائقي عن الأندلس"),
        MarketplaceItem(id="mu1", titleAr="أنغام الشرق", titleEn="Oriental", type="MUSIC", category="موسيقى / تراثية", sizeMb=8.0, rating=4.8, developerOrCreatorAr="فرقة", descriptionAr="معزوفات عود")
    )

    @Test
    fun `retrieve returns relevant APP for تطبيق query`() {
        val res = RagRetriever.retrieve("أريد تطبيق للأذكار", catalog(), topK = 2)
        assertTrue(res.isNotEmpty())
        assertEquals("APP", res[0].type)
    }

    @Test
    fun `retrieve returns GAME for لعبة query`() {
        val res = RagRetriever.retrieve("لعبة استراتيجية", catalog())
        assertTrue(res.any { it.type == "GAME" })
    }

    @Test
    fun `buildContextBlock containsTitles`() {
        val items = catalog().take(2)
        val block = RagRetriever.buildContextBlock(items)
        assertTrue(block.contains("زاد المسلم"))
        assertTrue(block.contains("فارس الصحراء"))
    }

    @Test
    fun `buildRagSystemInstruction contains catalog`() {
        val ctx = RagRetriever.buildContextBlock(catalog().take(1))
        val instr = RagRetriever.buildRagSystemInstruction(ctx)
        assertTrue(instr.contains("كتالوج الواحة"))
        assertTrue(instr.contains("زاد المسلم") || instr.contains(ctx.take(20)))
    }

    @Test
    fun `retrieve empty for blank`() {
        assertTrue(RagRetriever.retrieve("", catalog()).isEmpty())
        assertTrue(RagRetriever.retrieve("   ", catalog()).isEmpty())
    }
}
