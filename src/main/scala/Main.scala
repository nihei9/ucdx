import scala.io.Source
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods

@main def ucdx: Unit = {
  val propValAliases = Source.fromURL("https://www.unicode.org/Public/15.0.0/ucd/PropertyValueAliases.txt")
  PropertyValueAliases(propValAliases.reader) match {
    case Right(result) => println(JsonMethods.compact(JsonMethods.render(serialize(result))))
    case Left(error) => println(error)
  }
  propValAliases.close()

  val uniData = Source.fromURL("https://www.unicode.org/Public/15.0.0/ucd/UnicodeData.txt")
  UnicodeData(uniData.reader) match {
    case Right(result) => println(JsonMethods.compact(JsonMethods.render(serialize(result))))
    case Left(error) => println(error)
  }
  uniData.close()
}

def serialize(lines: List[Line]): JValue = lines.map(line => line.serialize)
