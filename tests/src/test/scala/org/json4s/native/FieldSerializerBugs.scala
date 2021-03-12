package org.json4s

import org.specs2.mutable.Specification
import FieldSerializerBugs._

class FieldSerializerBugs extends Specification {
  import native.Serialization
  import Serialization.{read, write => swrite}

  implicit val formats: Formats = DefaultFormats + FieldSerializer[AnyRef]()

  /* FIXME: it doesn't cause a stack overflow but the ser/deser doesn't work
  "AtomicInteger should not cause stack overflow" in {
    import java.util.concurrent.atomic.AtomicInteger

    val ser = swrite(new AtomicInteger(1))
    val atomic = read[AtomicInteger](ser)
    atomic.get must_== 1
  }
   */

  "Serializing a singleton object should not cause stack overflow" in {
    swrite(SingletonObject) must not(throwAn[Exception])
  }

  "Name with symbols is correctly serialized" in {
    implicit val formats: Formats = DefaultFormats + FieldSerializer[AnyRef]()

    val s = WithSymbol(5)
    val str = Serialization.write(s)
    str must_== """{"a-b*c":5}"""
    read[WithSymbol](str) must_== s
  }

  "FieldSerialization should work with Options" in {
    implicit val formats: Formats = DefaultFormats + FieldSerializer[ClassWithOption]()

    val t = new ClassWithOption
    t.field = Some(5)
    read[ClassWithOption](Serialization.write(t)).field must_== Some(5)
  }

  "FieldSerializer's manifest should not be overridden when it's added to Formats" in {
    val fieldSerializer = FieldSerializer[Type1](FieldSerializer.renameTo("num", "yum"))
    implicit val formats: Formats = DefaultFormats + (fieldSerializer: FieldSerializer[_])
    val expected1 = JObject(JField("yum", JInt(123)))
    val expected2 = JObject(JField("num", JInt(456)))
    Extraction.decompose(Type1(123)) must_== expected1
    Extraction.decompose(Type2(456)) must_== expected2
  }
}

object FieldSerializerBugs {
  case class WithSymbol(`a-b*c`: Int)

  class ClassWithOption {
    var field: Option[Int] = None
  }

  case class Type1(num: Int)
  case class Type2(num: Int)

  object SingletonObject
}
