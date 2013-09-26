/*
 Copyright 2013 Twitter, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.twitter.summingbird

import org.scalacheck._
import Gen._
import Arbitrary.arbInt
import Arbitrary.arbitrary

object TestGraphGenerators {
	// Put the non-recursive calls first, otherwise you blow the stack
  def genOptMap11[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[(Int) => Option[Int]]
    in <- genProd1
  } yield OptionMappedProducer(in, fn)

  def genOptMap12[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[(Int) => Option[(Int,Int)]]
    in <- genProd1
  } yield IdentityKeyedProducer(OptionMappedProducer(in, fn))

  def genOptMap21[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[((Int,Int)) => Option[Int]]
    in <- genProd2
  } yield OptionMappedProducer(in, fn)

  def genOptMap22[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[((Int,Int)) => Option[(Int,Int)]]
    in <- genProd2
  } yield IdentityKeyedProducer(OptionMappedProducer(in, fn))

  def aDependency[P <: Platform[P]](p: KeyedProducer[P, Int, Int])(implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]): Gen[KeyedProducer[P, Int, Int]] = {
    val deps = Producer.transitiveDependenciesOf(p).collect{case x:KeyedProducer[_, _, _] => x.asInstanceOf[KeyedProducer[P,Int,Int]]}
    if(deps.size == 1) genProd2 else oneOf(deps)
  }

  def genMerged2[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    _  <- Gen.choose(0,1) 
    p1 <- genProd2
    p2 <- oneOf(genProd2, aDependency(p1))
  } yield IdentityKeyedProducer(MergedProducer(p1, p2))

  def genFlatMap22[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[((Int, Int)) => List[(Int, Int)]]
    in <- genProd2
  } yield IdentityKeyedProducer(FlatMappedProducer(in, fn))

   def genFlatMap21[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[((Int, Int)) => List[Int]]
    in <- genProd2
  } yield FlatMappedProducer(in, fn)

  def genFlatMap11[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[(Int) => List[Int]]
    in <- genProd1
  } yield FlatMappedProducer(in, fn)

  def genMerged1[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    _  <- Gen.choose(0,1)
    p1 <- genProd1
    p2 <- genProd1
  } yield MergedProducer(p1, p2)

  def genFlatMap12[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    fn <- arbitrary[(Int) => List[(Int, Int)]]
    in <- genProd1
  } yield IdentityKeyedProducer(FlatMappedProducer(in, fn))

  def genWrite22[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]],
  					genSource2 : Arbitrary[KeyedProducer[P, Int, Int]],
  					testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]): Gen[KeyedProducer[P, Int, Int]] = for {
    _  <- Gen.choose(0,1)
    p1 <- genProd2
  } yield IdentityKeyedProducer(p1.write(sink2))


  def also1[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    _ <- Gen.choose(0, 1) // avoids blowup on self recursion
    out <- genProd1
    ignored <- oneOf(genProd2, genProd1, oneOf(Producer.transitiveDependenciesOf(out))): Gen[Producer[P, _]]
  } yield ignored.also(out)

  def also2[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    _ <- Gen.choose(0, 1) // avoids blowup on self recursion
    out <- genProd2
    ignored <- oneOf(genProd2, genProd1, oneOf(Producer.transitiveDependenciesOf(out))): Gen[Producer[P, _]]
  } yield IdentityKeyedProducer(ignored.also(out))



  // TODO (https://github.com/twitter/summingbird/issues/74): add more
  // nodes, abstract over Platform
  def summed[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]) = for {
    in <- genProd2 
  } yield in.sumByKey(testStore)

  def genProd2[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]], 
  									genSource2 : Arbitrary[KeyedProducer[P, Int, Int]], 
  									testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]): Gen[KeyedProducer[P, Int, Int]] = 
  					frequency((25, genSource2.arbitrary), (3, genOptMap12), (3, genOptMap22), (4, genWrite22), (1, genMerged2),
  							 (0, also2), (3, genFlatMap22), (3, genFlatMap12))

  def genProd1[P <: Platform[P]](implicit genSource1 : Arbitrary[Producer[P, Int]],
  									 genSource2 : Arbitrary[KeyedProducer[P, Int, Int]],
  									 testStore: P#Store[Int, Int], sink1: P#Sink[Int], sink2: P#Sink[(Int, Int)]): Gen[Producer[P, Int]] = 
  					frequency((25, genSource1.arbitrary), (3, genOptMap11), (3, genOptMap21), (1, genMerged1), (3, genFlatMap11),
  							 (0, also1), (3, genFlatMap21))

}