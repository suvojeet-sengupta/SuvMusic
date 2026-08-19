package com.suvojeet.suvmusic.player

import com.suvojeet.suvmusic.core.model.ArtistCreditInfo
import com.suvojeet.suvmusic.core.model.RemoteAudioMetadata
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The candidate lists below are the verbatim results the HQ Audio backend returned for
 * `search/songs?query=Kichu Kichu Kotha` (and the artist-qualified query) on 2026-08-19,
 * and the targets are the entries YouTube Music returned for the same search — the exact
 * shape of data the hybrid resolver sees.
 */
class HqSongMatcherTest {

    private fun hq(
        id: String,
        name: String,
        duration: Int,
        primary: List<String>,
        album: String,
        playCount: Long?,
        all: List<Pair<String, String>> = primary.map { it to "singer" },
        label: String? = null
    ): Song = Song.fromRemoteAudio(
        songId = id,
        title = name,
        artist = primary.joinToString(", "),
        album = album,
        duration = duration * 1000L,
        thumbnailUrl = null,
        remoteAudioMetadata = RemoteAudioMetadata(
            label = label,
            playCount = playCount,
            artists = all.map { (n, r) -> ArtistCreditInfo(n, r, null, null) }
        )
    )!!

    private fun yt(
        title: String,
        artist: String,
        durationSec: Int,
        album: String = "",
        isVideo: Boolean = false
    ): Song = Song(
        id = "yt-$title-$artist",
        title = title,
        artist = artist,
        album = album,
        duration = durationSec * 1000L,
        thumbnailUrl = null,
        source = SongSource.YOUTUBE,
        isVideo = isVideo
    )

    private val hqAll = listOf(
        hq("a1", "Kichu Kichu Kotha", 271, listOf("Arijit Singh"), "Best of Arijit Singh", 2571882,
            all = listOf("Prasenjit Mukherjee" to "music")),
        hq("a2", "Kichu Kichu Kotha", 304, listOf("Alka Yagnik"), "Sakal Sandhya", 452615,
            all = listOf("Ashok Bhadra" to "music", "Moslem Molla" to "music", "Alka Yagnik" to "singer")),
        hq("a3", "Kichhu Kichhu Kotha", 271, listOf("Arijit Singh"), "Lorai", 2571885,
            all = listOf("Arijit Singh" to "music", "Indraadip Das Gupta" to "music", "Prasen" to "music",
                "Arijit Singh" to "singer", "Anwessha" to "singer")),
        hq("a4", "Kichu Kichu Kotha (Female)", 229, listOf("Iman Chakraborty"), "FLAT No 609", 100237),
        hq("a5", "Kichu Kichu Kotha", 304, listOf("Shanu Kr."), "Kichu Kichu Kotha - Single", 10064),
        hq("a6", "Kichu Kichu Kotha (Male)", 290, listOf("Timir Biswas"), "FLAT No 609", 56953),
        hq("a7", "Kichu Kichu Kotha", 263, listOf("Mekhla Dasgupta"), "Kichu Kichu Kotha", 4330),
        hq("a8", "Kichu Kichu Kotha", 215, listOf("Arunava Roy"), "Kichu Kichu Kotha", 3158),
        hq("a9", "Kichu Kicha Kotha Sukhe", 295, listOf("Ashok Bhadra", "Kumar Sanu", "Anuradha Paudwal"), "Bhalobasar Choan", 112065),
        hq("a10", "Kichu Kichu Kotha", 299, listOf("Shobuj"), "Kichu Kichu Kotha", 14),
        hq("a11", "Kichu Kichu Kotha", 334, listOf("Momtaz"), "Lagba Bazi", 10),
        hq("a12", "Kichu Kichu Kotha", 266, listOf("Partho", "Nasim", "Tanim"), "Aj Din Katuk Gane", 2),
        hq("a13", "Kichu Kichu Kotha", 306, listOf("Azizur Rahman Aziz"), "Neelanjona", null),
        hq("a14", "KICHU KICHU KOTHA", 296, listOf("Malay Ray", "Poly Roy Bose"), "Kichu Kichu Kotha", 68),
        hq("a15", "Kichu Kichu Kotha", 305, listOf("Udit Narayan", "Kavita Krishnamurthy"), "Shesh Bongshodhor", 22),
        hq("a16", "Kichu Kichu Kotha", 258, listOf("Suvam Jalui"), "Kichu Kichu Kotha", 126),
        hq("a17", "Kichu Kichu Kotha", 298, listOf("Rudra Dey", "Indrani Chakrabarty"), "Kichu Kichu Kotha", 60),
        hq("a18", "Kichu Kichu Kotha", 188, listOf("Joy Singer"), "Kichu Kichu Kotha", 3),
        hq("a19", "Kichu Kichu Kotha", 60, listOf("Chinmoy Roy", "Sarbajit"), "Kichu Kichu Kotha", 21),
        hq("a20", "Kichu Kichu Kotha", 174, listOf("Ark"), "Jonmo Bhumi", 62)
    )

    @Test
    fun arijitLoraiVersionPicksTheLoraiRecordingDespiteSpellingDifference() {
        val target = yt("Kichhu Kichhu Kotha", "Arijit Singh", 272, "Lorai (Original Motion Picture Soundtrack)")
        val best = HqSongMatcher.pickBest(target, hqAll)
        assertNotNull(best)
        assertEquals("a3", best!!.id)
    }

    @Test
    fun spellingVariantAloneStillMatchesTheSameArtistRecording() {
        val target = yt("Kichhu Kichhu Kotha", "Arijit Singh", 272)
        val best = HqSongMatcher.pickBest(target, listOf(hqAll[0]))
        assertNotNull(best)
        assertEquals("a1", best!!.id)
    }

    @Test
    fun differentArijitCutWithNoHqCounterpartDoesNotFallToAnObscureCover() {
        // YouTube's 3:00 "Kichu Kichu Kotha" by Arijit (Bengali Hits) has no HQ twin; the
        // old matcher accepted Joy Singer (188 s) / Ark (174 s) on title + duration alone.
        val target = yt("Kichu Kichu Kotha", "Arijit Singh", 180, "Bengali Hits")
        assertNull(HqSongMatcher.pickBest(target, hqAll))
    }

    @Test
    fun alkaYagnikVersionPicksAlkaNotTheSameLengthShanuKr() {
        val target = yt("Kichu Kichu Kotha", "Alka Yagnik", 305, "Sakal Sandhya")
        assertEquals("a2", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun duetWithBothArtistsPicksTheDuet() {
        val target = yt("Kichu Kichu Kotha", "Udit Narayan & Kavita Krishnamurthy", 306,
            "Shesh Bongshodhor (Original Motion Picture Soundtrack)")
        assertEquals("a15", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun unknownArtistWithCoincidentalDurationIsRejected() {
        // Shams Evan feat. Trina (5:03) is not in the catalogue; Alka's 5:04 and Shanu Kr.'s
        // 5:04 are one second away and must not be played instead.
        val target = yt("Kichu Kichu Kotha (feat. Trina)", "Shams Evan", 303, "Kichu Kichu Kotha")
        assertNull(HqSongMatcher.pickBest(target, hqAll))
    }

    @Test
    fun catalogueSideFemaleTagDoesNotBlockTheSameArtistAndDuration() {
        val target = yt("Kichu Kichu Kotha", "Iman Chakraborty", 229, "FLAT No 609")
        assertEquals("a4", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun remixOnOneSideOnlyIsRejected() {
        val target = yt("Kichhu Kichhu Kotha (Remix)", "Arijit Singh", 271, "Lorai")
        assertNull(HqSongMatcher.pickBest(target, hqAll))
    }

    @Test
    fun lofiOnCatalogueSideOnlyIsRejected() {
        val target = yt("Kichhu Kichhu Kotha", "Arijit Singh", 271, "Lorai")
        val lofi = hq("l1", "Kichhu Kichhu Kotha (Lofi)", 271, listOf("Arijit Singh"), "Lorai", 1)
        assertNull(HqSongMatcher.pickBest(target, listOf(lofi)))
    }

    @Test
    fun movieSegmentInTitleActsAsAlbumHintForChannelUploads() {
        // A video upload: title cleaned to "Song · Movie", artist is the channel.
        val target = yt("Kichu Kichu Kotha · Lorai", "SVF", 275, isVideo = true)
        assertEquals("a3", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun channelUploadWithoutAnyHintNeedsAnExactTitleAndDuration() {
        val target = yt("Kichu Kichu Kotha", "Zee Music Company", 304, isVideo = true)
        // Alka (304) and Shanu Kr. (304) both qualify on title + duration; popularity picks Alka.
        assertEquals("a2", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun composerOnlyCreditIsFoundInTheFullCreditList() {
        val target = yt("Kichhu Kichhu Kotha", "Indraadip Das Gupta", 271, "Lorai")
        assertEquals("a3", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun sharedSurnameAloneIsNotAnArtistMatch() {
        val target = yt("Kichu Kichu Kotha", "Jaspinder Singh", 271)
        assertNull(HqSongMatcher.pickBest(target, hqAll))
    }

    @Test
    fun differentlyTransliteratedLyricTitleMatchesOnArtistAlbumAndDuration() {
        // YouTube: "Kichu Kichu Kotha Mukhe" — Kumar Sanu & Anuradha Paudwal, 4:55, Bhalobasar
        // Choan. HQ names the same recording "Kichu Kicha Kotha Sukhe".
        val target = yt("Kichu Kichu Kotha Mukhe", "Kumar Sanu & Anuradha Paudwal", 295,
            "Bhalobasar Choan (Original Motion Picture Soundtrack)")
        assertEquals("a9", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun paddedTitleWithoutArtistOrAlbumSupportIsRejected() {
        val target = yt("Kichu Kichu Kotha", "Shobuj", 296)
        assertNull(HqSongMatcher.pickBest(target, listOf(hqAll[8])))
    }

    @Test
    fun unknownDurationStillMatchesWithArtistAndExactTitle() {
        val target = yt("Kichu Kichu Kotha", "Alka Yagnik", 0)
        assertEquals("a2", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun variousArtistsIsTreatedAsUnknownArtist() {
        val target = yt("Kichu Kichu Kotha", "Various Artists", 305, "Sakal Sandhya")
        assertEquals("a2", HqSongMatcher.pickBest(target, hqAll)!!.id)
    }

    @Test
    fun transliterationFolding() {
        assertTrue(HqSongMatcher.tokenSimilarity("kichhu", "kichu") >= 0.9)
        assertTrue(HqSongMatcher.tokenSimilarity("kotha", "katha") >= 0.8)
        assertTrue(HqSongMatcher.tokenSimilarity("ghoshal", "ghosal") >= 0.9)
        assertTrue(HqSongMatcher.tokenSimilarity("bhalobasha", "bhalobasa") >= 0.9)
        assertTrue(HqSongMatcher.tokenSimilarity("hai", "hain") >= 0.8)
        assertEquals(0.0, HqSongMatcher.tokenSimilarity("dil", "din"), 0.0)
        assertEquals(0.0, HqSongMatcher.tokenSimilarity("tum", "tumhe"), 0.0)
        assertEquals(0.0, HqSongMatcher.tokenSimilarity("kichu", "kotha"), 0.0)
    }

    @Test
    fun rankOrdersByScoreAndExplainsEachVerdict() {
        val target = yt("Kichhu Kichhu Kotha", "Arijit Singh", 272, "Lorai")
        val ranked = HqSongMatcher.rank(target, hqAll)
        assertEquals(listOf("a3", "a1"), ranked.map { it.song.id })
        assertTrue(ranked.first().reason.contains("album=1.0"))
        assertTrue(HqSongMatcher.explain(target, hqAll[19]).contains("duration"))
    }
}
