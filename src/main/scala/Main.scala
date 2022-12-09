import scala.util.{Try, Using}
import scala.io.Source
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods

@main def ucdx: Unit = {
  Using(Source.fromURL(ucdFileURL("PropertyValueAliases.txt"))) { src =>
    PropertyValueAliases(src.reader) match {
      case Right(result) => println(JsonMethods.compact(JsonMethods.render(serialize(result))))
      case Left(error) => println(error)
    }
  }

  Using(Source.fromURL(ucdFileURL("UnicodeData.txt"))) { src =>
    UnicodeData(src.reader) match {
      case Right(result) => println(JsonMethods.compact(JsonMethods.render(serialize(result))))
      case Left(error) => println(error)
    }
  }
}

def serialize(lines: List[Line]): JValue = lines.map(line => line.serialize)

def ucdFileURL(fileName: String): String = s"https://www.unicode.org/Public/15.0.0/ucd/${fileName}"
