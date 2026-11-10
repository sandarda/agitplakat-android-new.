package ru.agitplakat.app

/**
 * Промпт описывает только иллюстрацию. Любые надписи, цифры и псевдотекст
 * запрещены: точная типографика будет добавлена PosterRenderer отдельно.
 */
data class PosterPrompt(
    val positive: String,
    val negative: String
)

object PromptComposer {
    fun build(
        plot: String,
        style: String,
        palette: String,
        composition: String = "Центральный персонаж"
    ): String = buildSpec(plot, style, palette, composition).positive

    fun buildSpec(
        plot: String,
        style: String,
        palette: String,
        composition: String = "Центральный персонаж"
    ): PosterPrompt {
        require(plot.isNotBlank()) { "Опишите сюжет плаката" }

        val styleText = when (style) {
            "Винтажная живописная иллюстрация" -> "highly detailed vintage editorial poster illustration, painterly realism, expressive believable people, carefully rendered faces, hands, clothing and props, rich environmental storytelling, hand-painted lithograph and screenprint look, subtle distressed ink, aged cream paper, elegant dense but balanced poster composition"
            "Конструктивизм" -> "Soviet constructivist graphic poster, bold geometric shapes, diagonal movement, monumental simplified forms, strong visual hierarchy, screenprint texture"
            "Советский пинап" -> "1940s Soviet-inspired pin-up poster illustration, confident charismatic adult heroine, expressive pose, period clothing, painterly realism, vintage print grain"
            else -> "industrial vintage illustrated poster, detailed machinery and architecture, heroic workers, painterly realism, aged lithograph print texture"
        }

        val paletteText = when (palette) {
            "Яркая" -> "rich vermilion red, warm cream, deep charcoal and restrained cobalt accents, vivid but harmonized"
            "Тёплая винтажная" -> "warm aged ivory paper, ochre, burnt sienna, brick red, tobacco brown and muted olive"
            "Холодная индустриальная" -> "weathered cream, steel blue, graphite, muted teal and restrained signal red"
            else -> "restrained vintage palette, parchment cream, muted brick red, charcoal, ochre and dusty blue"
        }

        val compositionText = when (composition) {
            "Сюжетная сцена" -> "cinematic narrative scene with several interacting subjects, clear foreground middle ground and background, strong depth"
            "Предметная композиция" -> "thematic still life of meaningful objects, carefully arranged foreground props, rich tactile materials, atmospheric setting"
            "Пейзаж и атмосфера" -> "wide atmospheric environment, memorable architecture and landscape, strong depth, human scale details"
            else -> "one dominant central character or hero subject, clear silhouette, supporting environment and small thematic details around the edges"
        }

        val positive = listOf(
            plot.trim(), styleText, paletteText, compositionText,
            "vertical A3 poster artwork, full-bleed illustration, carefully composed margins and visual hierarchy",
            "authentic aged paper patina, fine paper fibers, subtle stains, worn print edges, layered brushwork, crisp detailed focal subject",
            "image only, leave intentional calm areas for later typography overlay"
        ).joinToString(", ")

        val negative = listOf(
            "text, letters, words, typography, captions, slogans, numbers, labels, signatures, watermark, logo",
            "pseudo writing, gibberish, malformed glyphs, random symbols, illegible inscriptions",
            "low detail, blurry face, malformed hands, extra fingers, duplicated limbs, distorted anatomy",
            "plastic 3D render, glossy CGI, flat modern corporate vector art, neon colors, sterile digital gradient"
        ).joinToString(", ")

        return PosterPrompt(positive, negative)
    }
}
