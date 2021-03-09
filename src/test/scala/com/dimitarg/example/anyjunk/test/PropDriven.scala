package com.dimitarg.example.anyjunk.test

import weaver.pure._
import fs2._
import cats.effect.IO
import org.scalacheck.rng.Seed
import org.scalacheck.Gen
import org.scalacheck.Arbitrary

object PropDriven extends Suite {

  val seeds: Stream[IO, Seed] = Stream.eval(
    IO(Seed.random())
  ).flatMap { initSeed =>
    Stream.unfold(initSeed) { seed =>
      Some((seed, seed.next))
    }
  }

  def gens[A](gen: Gen[A]): Stream[IO, A] = {
    seeds.map { seed =>
      gen.apply(Gen.Parameters.default, seed)
    }
  }.unNoneTerminate

  def arbs[A](implicit arb: Arbitrary[A]): Stream[IO, A] = {
    gens(arb.arbitrary)
  }

  override def suitesStream: Stream[IO,Test] = for {
    x <- arbs[Int].take(10)
    y <- arbs[Int].take(10)
    z <- arbs[Int].take(10)
  } yield test(s"associativity - $x $y $z") {
    expect((x+y)+z == x+(y+z))
  }

  
}
