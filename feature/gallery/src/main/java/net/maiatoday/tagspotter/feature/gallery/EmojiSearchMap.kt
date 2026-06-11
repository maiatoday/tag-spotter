package net.maiatoday.tagspotter.feature.gallery

object EmojiSearchMap {
    private val emojiToKeywords = mapOf(
        // Graffiti / Street Art emojis
        "🎨" to listOf("graffiti", "mural", "stencil", "throwup", "pasteup", "sticker", "art", "paint"),
        "🖌️" to listOf("graffiti", "mural", "stencil", "throwup", "pasteup", "sticker", "art", "paint"),
        "🖍️" to listOf("graffiti", "stencil", "art"),
        "🛹" to listOf("graffiti", "street"),
        "🫟" to listOf("graffiti", "splatter", "paint"),

        // Sculpture emojis
        "🗿" to listOf("sculpture", "statue"),
        "🗽" to listOf("sculpture", "statue"),
        "🏺" to listOf("sculpture", "pottery", "vase"),

        // Nature emojis
        "🌳" to listOf("nature", "tree", "plant", "park"),
        "🌲" to listOf("nature", "tree", "forest"),
        "🌴" to listOf("nature", "tree", "palm"),
        "🌱" to listOf("nature", "plant", "sprout", "garden"),
        "🌿" to listOf("nature", "plant", "leaf", "herb"),
        "☘️" to listOf("nature", "plant", "shamrock"),
        "🍀" to listOf("nature", "plant", "clover"),
        "🍃" to listOf("nature", "plant", "leaf"),
        "🍂" to listOf("nature", "leaf", "autumn"),
        "🍁" to listOf("nature", "leaf", "maple"),
        "🍄" to listOf("nature", "mushroom"),
        "🌸" to listOf("nature", "flower", "cherry blossom"),
        "🌹" to listOf("nature", "flower", "rose"),
        "🌺" to listOf("nature", "flower", "hibiscus"),
        "🌻" to listOf("nature", "flower", "sunflower"),
        "🌼" to listOf("nature", "flower", "blossom"),
        "🌷" to listOf("nature", "flower", "tulip"),

        // Architecture emojis
        "🏛️" to listOf("architecture", "monument", "building"),
        "🏠" to listOf("architecture", "house", "building"),
        "🏢" to listOf("architecture", "building"),
        "⛪" to listOf("architecture", "church", "building"),
        "🏰" to listOf("architecture", "castle", "building"),
        "🏗️" to listOf("architecture", "construction", "building"),

        // Public Place emojis
        "⛲" to listOf("public_place", "fountain", "square", "park"),
        "🎡" to listOf("public_place", "ferris wheel", "park"),
        "🎢" to listOf("public_place", "roller coaster", "park"),
        "🎠" to listOf("public_place", "carousel", "park"),

        // Food emojis
        "🍔" to listOf("food", "burger"),
        "🍕" to listOf("food", "pizza"),
        "🍟" to listOf("food", "fries"),
        "🌭" to listOf("food", "hotdog"),
        "🍿" to listOf("food", "popcorn"),
        "🍳" to listOf("food", "cooking"),
        "🍞" to listOf("food", "bread"),
        "🥐" to listOf("food", "croissant"),
        "🥖" to listOf("food", "baguette"),
        "🌮" to listOf("food", "taco"),
        "🌯" to listOf("food", "burrito"),
        "🍜" to listOf("food", "noodles", "ramen"),
        "🍝" to listOf("food", "pasta", "spaghetti"),
        "🍣" to listOf("food", "sushi"),
        "🍨" to listOf("food", "ice cream"),
        "🍰" to listOf("food", "cake"),
        "🧁" to listOf("food", "cupcake"),
        "🍩" to listOf("food", "donut"),
        "🍪" to listOf("food", "cookie"),
        "☕" to listOf("food", "coffee", "cafe"),
        "🍺" to listOf("food", "beer"),
        "🍷" to listOf("food", "wine")
    )

    fun getKeywordsForEmoji(query: String): List<String> {
        val keywords = mutableListOf<String>()
        var i = 0
        while (i < query.length) {
            val codePoint = query.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val charStr = query.substring(i, i + charCount)

            val matches = emojiToKeywords[charStr]
            if (matches != null) {
                keywords.addAll(matches)
            }
            i += charCount
        }
        return keywords.distinct()
    }
}
