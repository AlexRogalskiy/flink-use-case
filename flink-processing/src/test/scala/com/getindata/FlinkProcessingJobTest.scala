package com.getindata

import org.scalatest.{Matchers, WordSpec}

class FlinkProcessingJobTest extends WordSpec with Matchers {


  class CollectorMock {

    var innerCollection: Seq[Iterable[Event]] = Seq()

    def collect(record: Iterable[Event]): Unit = innerCollection :+= record

  }

  val splitPredicate: Event => Boolean = _.isInstanceOf[SearchEvent]

  "mapToSubSession" should {
    "not produce any results for only search events" in {
      val mock = new CollectorMock
      FlinkProcessingJob.mapToSubSession(Seq(SearchEvent(1L, "1", EventType.NextSong, DeviceType.Desktop)),
        splitPredicate,
        mock.collect)
      mock.innerCollection should be(Seq())
    }

    "produce one subsession" in {
      val mock = new CollectorMock
      FlinkProcessingJob.mapToSubSession(Seq(
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "2"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "4"),
        SearchEvent(1L, "1", EventType.NextSong, DeviceType.Desktop)
      ), splitPredicate, mock.collect)
      mock.innerCollection should be(Seq(Seq(
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "2"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "4"))
      ))
    }

    "produce subsession when starts with next" in {
      val mock = new CollectorMock
      FlinkProcessingJob.mapToSubSession(Seq(
        SearchEvent(1L, "1", EventType.NextSong, DeviceType.Desktop),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "2"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "4")
      ), splitPredicate, mock.collect)
      mock.innerCollection should be(Seq(Seq(
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "2"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "4"))
      ))
    }

    "produce multiple subsession" in {
      val mock = new CollectorMock
      FlinkProcessingJob.mapToSubSession(Seq(
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "2"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
        SearchEvent(1L, "1", EventType.NextSong, DeviceType.Desktop),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
        SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "4")
      ), splitPredicate, mock.collect)
      mock.innerCollection should be(Seq(
        Seq(
          SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "2"),
          SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
          SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3")
        ),
        Seq(
          SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "3"),
          SongEvent(1L, "1", EventType.EndSong, DeviceType.Desktop, PlaylistType.ChillHits, "4")
        )
      ))
    }
  }
}
