package com.scrolllight.bible.data.repository

import android.content.Context
import com.google.gson.Gson
import com.scrolllight.bible.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    // ── Static Bible data (bundled as assets) ──────────────────────────────

    private val _books: List<BibleBook> by lazy { buildBookList() }

    fun getAllBooks(): List<BibleBook> = _books
    fun getOldTestament(): List<BibleBook> = _books.filter { it.isOldTestament }
    fun getNewTestament(): List<BibleBook> = _books.filter { it.isNewTestament }

    fun getBook(bookId: String): BibleBook? = _books.find { it.id == bookId }

    suspend fun getChapter(bookId: String, chapter: Int): List<BibleVerse> =
        withContext(Dispatchers.IO) {
            // In production: load from Room/assets JSON
            // Demo: return sample verses for Matthew 17
            if (bookId == "mat" && chapter == 17) sampleMatthew17() else sampleVerses(chapter)
        }

    suspend fun search(query: String, scope: SearchScope = SearchScope.WHOLE_BIBLE): List<SearchResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            // Demo search — in production: full-text search on Room FTS
            val books = when (scope) {
                SearchScope.NEW_TESTAMENT -> getNewTestament()
                SearchScope.OLD_TESTAMENT -> getOldTestament()
                else -> _books
            }
            books.flatMap { book ->
                sampleVerses(1).filter { it.text.contains(query) }.map { verse ->
                    SearchResult(
                        bookId   = book.id,
                        bookName = book.name,
                        chapter  = 1,
                        verse    = verse.verse,
                        text     = verse.text,
                        matchStart = verse.text.indexOf(query),
                        matchEnd   = verse.text.indexOf(query) + query.length
                    )
                }
            }
        }

    fun getDailyVerse(): DailyVerse = DailyVerse(
        theme    = "爱",
        subTheme = "施舍",
        text     = "只是不可忘记行善和捐输的事，因为这样的祭是神所喜悦的。",
        reference = "希伯来书 13:16",
        date     = "2026年3月21日"
    )

    fun getReadingPlans(): List<ReadingPlan> = listOf(
        ReadingPlan("lent_plan", "大斋节期灵修系列（纽约救赎主教会）", null, 40, PlanCategory.THEME, "", "跟随耶稣走向十架之路"),
        ReadingPlan("lent_basic", "大斋期灵修", null, 46, PlanCategory.THEME, "", "大斋期40天灵修计划"),
        ReadingPlan("40days",    "40天灵修 - 大斋期", null, 46, PlanCategory.THEME, "", "40天深度灵修"),
        ReadingPlan("mcheyne",   "麦琴读经计划", null, 365, PlanCategory.WHOLE_BIBLE, "", "Robert Murray M'Cheyne 经典365天"),
        ReadingPlan("roberts",   "罗伯茨读经计划", null, 365, PlanCategory.WHOLE_BIBLE, "", "系统365天读完全本圣经"),
        ReadingPlan("180days",   "180天读经计划", null, 180, PlanCategory.WHOLE_BIBLE, "", "半年读完圣经"),
        ReadingPlan("30days_nt", "30天读一遍新约", null, 30, PlanCategory.NEW_TESTAMENT, "", "一个月速读新约"),
        ReadingPlan("4gospels",  "30天四福音读经计划", null, 30, PlanCategory.NEW_TESTAMENT, "", "深读四福音"),
        ReadingPlan("nt90",      "90天读完新约全书", null, 90, PlanCategory.NEW_TESTAMENT, "", "三个月读完新约"),
        ReadingPlan("ot366",     "366天读完旧约", null, 366, PlanCategory.OLD_TESTAMENT, "", "一年读完旧约"),
        ReadingPlan("moses",     "《旧约》：摩西的书", null, 69, PlanCategory.OLD_TESTAMENT, "", "五经深度研读"),
        ReadingPlan("wisdom",    "《旧约》：智慧之书", null, 70, PlanCategory.OLD_TESTAMENT, "", "诗歌书与智慧书"),
    )

    // ── Private helpers ────────────────────────────────────────────────────

    private fun buildBookList(): List<BibleBook> = listOf(
        // Old Testament
        BibleBook("gen", "创世记", "创", "old", 50),
        BibleBook("exo", "出埃及记", "出", "old", 40),
        BibleBook("lev", "利未记", "利", "old", 27),
        BibleBook("num", "民数记", "民", "old", 36),
        BibleBook("deu", "申命记", "申", "old", 34),
        BibleBook("jos", "约书亚记", "书", "old", 24),
        BibleBook("jdg", "士师记", "士", "old", 21),
        BibleBook("rut", "路得记", "得", "old", 4),
        BibleBook("1sa", "撒母耳记上", "撒上", "old", 31),
        BibleBook("2sa", "撒母耳记下", "撒下", "old", 24),
        BibleBook("1ki", "列王纪上", "王上", "old", 22),
        BibleBook("2ki", "列王纪下", "王下", "old", 25),
        BibleBook("1ch", "历代志上", "代上", "old", 29),
        BibleBook("2ch", "历代志下", "代下", "old", 36),
        BibleBook("ezr", "以斯拉记", "拉", "old", 10),
        BibleBook("neh", "尼希米记", "尼", "old", 13),
        BibleBook("est", "以斯帖记", "斯", "old", 10),
        BibleBook("job", "约伯记", "伯", "old", 42),
        BibleBook("psa", "诗篇", "诗", "old", 150),
        BibleBook("pro", "箴言", "箴", "old", 31),
        BibleBook("ecc", "传道书", "传", "old", 12),
        BibleBook("sng", "雅歌", "歌", "old", 8),
        BibleBook("isa", "以赛亚书", "赛", "old", 66),
        BibleBook("jer", "耶利米书", "耶", "old", 52),
        BibleBook("lam", "耶利米哀歌", "哀", "old", 5),
        BibleBook("ezk", "以西结书", "结", "old", 48),
        BibleBook("dan", "但以理书", "但", "old", 12),
        BibleBook("hos", "何西阿书", "何", "old", 14),
        BibleBook("jol", "约珥书", "珥", "old", 3),
        BibleBook("amo", "阿摩司书", "摩", "old", 9),
        BibleBook("oba", "俄巴底亚书", "俄", "old", 1),
        BibleBook("jon", "约拿书", "拿", "old", 4),
        BibleBook("mic", "弥迦书", "弥", "old", 7),
        BibleBook("nam", "那鸿书", "鸿", "old", 3),
        BibleBook("hab", "哈巴谷书", "哈", "old", 3),
        BibleBook("zep", "西番雅书", "番", "old", 3),
        BibleBook("hag", "哈该书", "该", "old", 2),
        BibleBook("zec", "撒迦利亚书", "亚", "old", 14),
        BibleBook("mal", "玛拉基书", "玛", "old", 4),
        // New Testament
        BibleBook("mat", "马太福音", "太", "new", 28),
        BibleBook("mrk", "马可福音", "可", "new", 16),
        BibleBook("luk", "路加福音", "路", "new", 24),
        BibleBook("jhn", "约翰福音", "约", "new", 21),
        BibleBook("act", "使徒行传", "徒", "new", 28),
        BibleBook("rom", "罗马书", "罗", "new", 16),
        BibleBook("1co", "哥林多前书", "林前", "new", 16),
        BibleBook("2co", "哥林多后书", "林后", "new", 13),
        BibleBook("gal", "加拉太书", "加", "new", 6),
        BibleBook("eph", "以弗所书", "弗", "new", 6),
        BibleBook("php", "腓立比书", "腓", "new", 4),
        BibleBook("col", "歌罗西书", "西", "new", 4),
        BibleBook("1th", "帖撒罗尼迦前书", "帖前", "new", 5),
        BibleBook("2th", "帖撒罗尼迦后书", "帖后", "new", 3),
        BibleBook("1ti", "提摩太前书", "提前", "new", 6),
        BibleBook("2ti", "提摩太后书", "提后", "new", 4),
        BibleBook("tit", "提多书", "多", "new", 3),
        BibleBook("phm", "腓利门书", "门", "new", 1),
        BibleBook("heb", "希伯来书", "来", "new", 13),
        BibleBook("jas", "雅各书", "雅", "new", 5),
        BibleBook("1pe", "彼得前书", "彼前", "new", 5),
        BibleBook("2pe", "彼得后书", "彼后", "new", 3),
        BibleBook("1jn", "约翰一书", "约一", "new", 5),
        BibleBook("2jn", "约翰二书", "约二", "new", 1),
        BibleBook("3jn", "约翰三书", "约三", "new", 1),
        BibleBook("jud", "犹大书", "犹", "new", 1),
        BibleBook("rev", "启示录", "启", "new", 22),
    )

    private fun sampleMatthew17(): List<BibleVerse> = listOf(
        BibleVerse(1,  "过了六天，耶稣带着彼得、雅各，和雅各的兄弟约翰，暗暗地上了高山，", "After six days Jesus took with him Peter, James and John the brother of James, and led them up a high mountain by themselves."),
        BibleVerse(2,  "就在他们面前变了形像，脸面明亮如日头，衣裳洁白如光。", "There he was transfigured before them. His face shone like the sun, and his clothes became as white as the light."),
        BibleVerse(3,  "忽然，有摩西、以利亚向他们显现，同耶稣说话。", "Just then there appeared before them Moses and Elijah, talking with Jesus."),
        BibleVerse(4,  "彼得对耶稣说：「主啊，我们在这里真好！你若愿意，我就在这里搭三座棚，一座为你，一座为摩西，一座为以利亚。」", "Peter said to Jesus, \"Lord, it is good for us to be here. If you wish, I will put up three shelters—one for you, one for Moses and one for Elijah.\""),
        BibleVerse(5,  "说话之间，忽然有一朵光明的云彩遮盖他们，且有声音从云彩里出来，说：「这是我的爱子，我所喜悦的，你们要听他！」", "While he was still speaking, a bright cloud covered them, and a voice from the cloud said, \"This is my Son, whom I love; with him I am well pleased. Listen to him!\""),
        BibleVerse(6,  "门徒听见，就俯伏在地，极其害怕。", "When the disciples heard this, they fell facedown to the ground, terrified."),
        BibleVerse(7,  "耶稣进前来，摸他们，说：「起来！不要害怕！」", "But Jesus came and touched them. \"Get up,\" he said. \"Don't be afraid.\""),
        BibleVerse(8,  "他们举目抬头，不见一人，只见耶稣在那里。", "When they looked up, they saw no one except Jesus."),
        BibleVerse(9,  "他们下山的时候，耶稣吩咐他们说：「人子还没有从死里复活，你们不要将所看见的告诉人。」", "As they were coming down the mountain, Jesus instructed them, \"Don't tell anyone what you have seen, until the Son of Man has been raised from the dead.\""),
        BibleVerse(10, "门徒问耶稣说：「文士为什么说以利亚务必先来？」", "The disciples asked him, \"Why then do the teachers of the law say that Elijah must come first?\""),
        BibleVerse(11, "耶稣回答说：「以利亚固然先来，并要复兴万事；", "Jesus replied, \"To be sure, Elijah comes and will restore all things.\""),
        BibleVerse(12, "只是我告诉你们，以利亚已经来了，人却不认识他，竟任意待他。人子也将要这样受他们的苦。」", "But I tell you, Elijah has already come, and they did not recognize him, but have done to him everything they wished. In the same way the Son of Man is going to suffer at their hands.\""),
        BibleVerse(13, "门徒这才明白耶稣所说的是指着施洗的约翰。", "Then the disciples understood that he was talking to them about John the Baptist."),
        BibleVerse(14, "耶稣和门徒到了众人那里，有一个人来见耶稣，跪下，", "When they came to the crowd, a man approached Jesus and knelt before him."),
        BibleVerse(15, "说：「主啊，怜悯我的儿子！他害癫痫的病很苦，屡次跌在火里，屡次跌在水里。", "\"Lord, have mercy on my son,\" he said. \"He has seizures and is suffering greatly. He often falls into the fire or into the water.\""),
        BibleVerse(16, "我带他到你门徒那里，他们不能医治他。」", "I brought him to your disciples, but they could not heal him.\""),
        BibleVerse(17, "耶稣说：「嗳！这又不信又悖谬的世代啊，我在你们这里要到几时呢？我忍耐你们要到几时呢？把他带到我这里来吧！」", "\"You unbelieving and perverse generation,\" Jesus replied, \"how long shall I stay with you? How long shall I put up with you? Bring the boy here to me.\""),
        BibleVerse(18, "耶稣斥责那鬼，鬼就出去；从此孩子就痊愈了。", "Jesus rebuked the demon, and it came out of the boy, and he was healed at that moment."),
        BibleVerse(19, "门徒暗暗地到耶稣跟前，说：「我们为什么不能赶出那鬼呢？」", "Then the disciples came to Jesus in private and asked, \"Why couldn't we drive it out?\""),
        BibleVerse(20, "耶稣说：「是因你们的信心小。我实在告诉你们，你们若有信心，像一粒芥菜种，就是对这座山说『你从这边挪到那边』，它也必挪去；并且你们没有一件不能做的事了。」", "He replied, \"Because you have so little faith. Truly I tell you, if you have faith as small as a mustard seed, you can say to this mountain, 'Move from here to there,' and it will move. Nothing will be impossible for you.\""),
        BibleVerse(22, "他们还住在加利利的时候，耶稣对门徒说：「人子将要被交在人手里，", "When they came together in Galilee, he said to them, \"The Son of Man is going to be delivered into the hands of men.\""),
        BibleVerse(23, "他们要杀害他，第三日他要复活。」门徒就大大地忧愁。", "\"They will kill him, and on the third day he will be raised to life.\" And the disciples were filled with grief."),
        BibleVerse(24, "到了迦百农，有收丁税的人来见彼得，说：「你们的先生不纳丁税（丁税约有半块钱）吗？」", "After Jesus and his disciples arrived in Capernaum, the collectors of the two-drachma temple tax came to Peter and asked, \"Doesn't your teacher pay the temple tax?\""),
        BibleVerse(25, "彼得说：「纳。」他进了屋子，耶稣先向他说：「西门，你的意思如何？世上的君王向谁征收关税、丁税？是向自己的儿子呢？是向外人呢？」", "\"Yes, he does,\" he replied. When Peter came into the house, Jesus was the first to speak. \"What do you think, Simon?\" he asked. \"From whom do the kings of the earth collect duty and taxes—from their own children or from others?\""),
        BibleVerse(26, "彼得说：「是向外人。」耶稣说：「既然如此，儿子就可以免税了。", "\"From others,\" Peter answered. \"Then the children are exempt,\" Jesus said to him."),
        BibleVerse(27, "但恐怕触犯他们，你且往海边去钓鱼，把先钓上来的鱼拿起来，开了它的口，必得一块钱，可以拿去给他们，作你我的税银。」", "\"But so that we may not cause offense, go to the lake and throw out your line. Take the first fish you catch; open its mouth and you will find a four-drachma coin. Take it and give it to them for my tax and yours.\""),
    )

    private fun sampleVerses(chapter: Int): List<BibleVerse> = (1..20).map { v ->
        BibleVerse(v, "第${chapter}章第${v}节示例经文内容，这里展示圣经文本。", "Sample verse $v text content for chapter $chapter.")
    }
}
