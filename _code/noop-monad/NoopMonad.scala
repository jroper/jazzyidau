//#imports
import scalaz._
import Scalaz._
//#imports

//#nooptrait
/**
 * A noop of type T
 */
sealed trait Noop[T] {

  /**
   * Run the noop
   */
  def run: Unit
}
//#nooptrait

object Noop {
  private def apply[T]: Noop[T] = new Noop[T] {
    def run = ()
  }

  implicit def NoopInstances[T]: Monad[Noop] = new Monad[Noop] {
    //#point
    def point[A](a: => A): Noop[A] = Noop[A]
    //#point
    //#bind
    def bind[A, B](fa: Noop[A])(f: A => Noop[B]): Noop[B] = Noop[B]
    //#bind
  }
}

object Main extends App {

  //#calculateprimes
  def calculatePrimes(upTo: Int): List[Int]
  //#calculateprimes
  = {
    println("calculating primes")
    Seq(10)
  }

  def allIntPrimes =
    //#allintprimes
    calculatePrimes(Int.MaxValue)
    //#allintprimes

  //#noopprimes
  val noopAllIntPrimes = calculatePrimes(Int.MaxValue).point[Noop]
  //#noopprimes

  //#runnoopprimes
  noopAllIntPrimes.run
  //#runnoopprimes

  //#summed
  val summedPrimesString = for {
    primes <- noopAllIntPrimes
    summed <- primes.reduce(_ + _).point[Noop]
    asString <- summed.toString.point[Noop]
  } yield asString
  //#summed

  //#runsummed
  summedPrimesString.run
  //#runsummed

  //#optimise1
  (for {
    primes <- calculatePrimes(Int.MaxValue).point[Noop]
    summed <- primes.reduce(_ + _).point[Noop]
    asString <- summed.toString.point[Noop]
  } yield asString).run
  //#optimise1
  
  //#optimise2
  (for {
    primes <- calculatePrimes(Int.MaxValue).point[Noop]
    summed <- primes.reduce(_ + _).point[Noop]
  } yield summed).run
  //#optimise2
  
  //#optimise3
  (for {
    primes <- calculatePrimes(Int.MaxValue).point[Noop]
  } yield primes).run
  //#optimise3

}
