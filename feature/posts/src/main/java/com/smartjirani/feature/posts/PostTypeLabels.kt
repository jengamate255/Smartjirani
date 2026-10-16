package com.smartjirani.feature.posts

/** Tanzania-friendly, code-switched label for a post type. */
fun PostType.label(): String =
    when (this) {
        PostType.GENERAL -> "Mjadala"
        PostType.ANNOUNCEMENT -> "Tangazo"
        PostType.ALERT -> "Tahadhari"
        PostType.LOST_FOUND -> "Imepotea / Imepatikana"
        PostType.BUY_SELL -> "Nunua / Uuze"
        PostType.SERVICE -> "Huduma"
        PostType.EVENT -> "Shughuli"
    }
