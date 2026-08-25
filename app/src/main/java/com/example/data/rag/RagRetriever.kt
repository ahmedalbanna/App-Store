package com.example.data.rag

import com.example.data.database.MarketplaceItem

/**
 * Phase 3.2: RAG over local catalog — simple keyword TF retrieval.
 * Scores items by token overlap with query (Arabic + English), returns top-k.
 */
object RagRetriever {

    fun retrieve(query: String, catalog: List<MarketplaceItem>, topK: Int = 3): List<MarketplaceItem> {
        if (query.isBlank() || catalog.isEmpty()) return emptyList()
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()

        return catalog.map { item ->
            val score = scoreItem(item, tokens)
            item to score
        }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[\\s،,.!?؛:()\\[\\]\"'\\-]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
    }

    private fun normalizeToken(token: String): List<String> {
        val variants = mutableListOf(token)
        // Strip Arabic prefixes ل, لل, ال
        if (token.startsWith("لل")) variants.add(token.removePrefix("لل"))
        if (token.startsWith("ال")) variants.add(token.removePrefix("ال"))
        if (token.startsWith("ل")) variants.add(token.removePrefix("ل"))
        return variants.distinct()
    }

    private val typeTokenMap = mapOf(
        "تطبيق" to "APP", "تطبيقات" to "APP",
        "لعبة" to "GAME", "ألعاب" to "GAME",
        "كتاب" to "BOOK", "كتب" to "BOOK",
        "فيلم" to "MOVIE", "أفلام" to "MOVIE",
        "موسيقى" to "MUSIC", "موسيقا" to "MUSIC"
    )

    private fun scoreItem(item: MarketplaceItem, tokens: List<String>): Int {
        var score = 0
        val fields = listOf(
            item.titleAr.lowercase(),
            item.titleEn.lowercase(),
            item.descriptionAr.lowercase(),
            item.category.lowercase(),
            item.developerOrCreatorAr.lowercase(),
            item.type.lowercase()
        )
        for (rawToken in tokens) {
            val tokenVariants = normalizeToken(rawToken)
            for (token in tokenVariants) {
                for (field in fields) {
                    when {
                        field == token -> score += 5
                        field.contains(token) -> score += 3
                        token.length >= 3 && field.contains(token.take(3)) -> score += 1
                        // Also check reverse: token contains field token (for short fields)
                        token.contains(field) && field.length >= 3 -> score += 1
                    }
                }
            }
            // Boost for type-specific tokens via map
            val mappedType = typeTokenMap[rawToken]
            if (mappedType != null && item.type == mappedType) {
                score += 4
            }
            // Also check normalized variants for type
            for (variant in tokenVariants) {
                val mapped = typeTokenMap[variant]
                if (mapped != null && item.type == mapped) score += 4
            }
        }
        score += (item.rating - 4.0).toInt().coerceAtLeast(0)
        return score
    }

    fun buildContextBlock(items: List<MarketplaceItem>): String {
        if (items.isEmpty()) return "لا يوجد محتوى مطابق في الكتالوج المحلي."
        return items.joinToString("\n---\n") { item ->
            """
            [${item.type}] ${item.titleAr} (${item.titleEn})
            التصنيف: ${item.category} | المطور: ${item.developerOrCreatorAr} | التقييم: ${item.rating} | الحجم: ${item.sizeMb}MB
            الوصف: ${item.descriptionAr.take(200)}
            الحالة: ${item.downloadState} | ID: ${item.id}
            """.trimIndent()
        }
    }

    fun buildRagSystemInstruction(catalogContext: String): String {
        return """
أنت مساعد ذكي لمتجر الواحة. لديك كتالوج محلي لمحتوى المتجر. استخدم السياق التالي للإجابة بدقة. 
إذا كان السياق يحتوي على عناصر ذات صلة، رشّحها بالاسم مع ذكر التقييم والحجم. إذا لم يكن هناك صلة، اقترح تصفح التبويبات.

### كتالوج الواحة (أعلى نتائج مطابقة):
$catalogContext

### تعليمات الأسلوب:
- الرد بالعربية الفصحى الراقية، مختصر (2-4 جمل)، مع ترشيح عنصرين على الأكثر.
- اذكر اسم العنصر العربي كما هو في الكتالوج.
- إذا كان العنصر من نوع BOOK/MOVIE/MUSIC، اذكر أنه متاح للفتح الفوري بعد التحميل.
""".trimIndent()
    }
}
