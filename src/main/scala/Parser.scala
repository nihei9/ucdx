import java.io.Reader
import scala.util.parsing.combinator.RegexParsers
import org.json4s.JValue
import org.json4s.JsonDSL._

trait UCDElement {
  def serialize: JValue
}

case class CodePoint(p: Int) extends UCDElement {
  def serialize: JValue = p
}

case class CodePointRange(
  start: CodePoint,
  end: CodePoint
) extends UCDElement {
  def serialize: JValue = Map(
    "s" -> start.serialize,
    "e" -> end.serialize
  )
}

case class MissingLine(
  cpRange: CodePointRange,
  field: String,
  rest: List[String]
) extends UCDElement {
  def serialize: JValue = ("cp" -> cpRange.serialize) ~~ ("default" -> (field::rest))
}

case class Line(
  fields: Option[UCDElement],
  missingLine: Option[MissingLine]
) extends UCDElement {
  def serialize: JValue = {
    ("fields" ->
      (fields match
        case Some(fs) => Some(fs.serialize)
        case None => None))
    ~~ ("missingLine" ->
      (missingLine match
        case Some(ml) => Some(ml.serialize)
        case None => None))
  }
}

trait UCDParsers extends RegexParsers {
  override val whiteSpace = """[ \t\x0B\f\r]""".r

  def ucd: Parser[List[Line]] = rep(line)
  def line: Parser[Line] = (fields ~! opt(comment) | comment) <~! opt("""\n+""".r) ^^ {
    case fs ~ c: (UCDElement ~ Option[Option[MissingLine]]) => Line(Some(fs), c.getOrElse(None))
    case c: Option[MissingLine] => Line(None, c)
  }
  def comment: Parser[Option[MissingLine]] = (missingLine | normalComment) ^^ {
    case m: MissingLine => Some(m)
    case _ => None
  }
  def missingLine: Parser[MissingLine] = "# @missing:" ~>! (codePointRange ~! sepString ~! rep(sepString)) ^^ {
    case cp ~ f ~ rest => MissingLine(cp, f, rest)
  }
  def normalComment: Parser[String] = """#.*""".r ^^ {
    _.toString
  }
  def fields: Parser[UCDElement]
  def string: Parser[String] = """[^\n;#]+""".r ^^ {
    _.toString.trim()
  }
  def sepString: Parser[String] = ";" ~>! string
  def sepOptString: Parser[Option[String]] = ";" ~>! opt(string)
  def codePointRange: Parser[CodePointRange] = codePoint ~! opt(".." ~>! codePoint) ^^ {
    case s~None => CodePointRange(s, s)
    case s~Some(e) => CodePointRange(s, e)
  }
  def codePoint: Parser[CodePoint] = """\p{XDigit}+""".r ^^ {
    case s => CodePoint(Integer.parseInt(s, 16))
  }
}

case class PropertyValueAliasesFields(
  propertyName: String,
  propertyValueNameFormal: String,
  propertyValueNameAliases: List[String],
) extends UCDElement {
  def serialize: JValue = {
    ("name" -> propertyName)
    ~~ ("formal" -> propertyValueNameFormal)
    ~~ ("aliases" -> propertyValueNameAliases)
  }
}

object PropertyValueAliases extends UCDParsers {
  override def fields: Parser[PropertyValueAliasesFields] = string ~! sepString ~! sepString ~! rep(sepString) ^^ {
    case f1 ~ f2 ~ f3 ~ rest =>
      if f1 == "ccc" then PropertyValueAliasesFields(f1, rest.head, f2::f3::rest.tail)
      else PropertyValueAliasesFields(f1, f3, f2::rest)
  }
  def apply(src: Reader): Either[String, List[Line]] =
    parseAll(ucd, src) match {
      case Success(result, _) => Right(result)
      case NoSuccess(error, _) => Left(error)
    }
}

case class UnicodeDataFields(
  codePoint: CodePoint,
  generalCategory: Option[String]
) extends UCDElement {
  def serialize: JValue = {
    ("cp" -> codePoint.serialize)
    ~~ ("gc" -> generalCategory)
  }
}

object UnicodeData extends UCDParsers {
  override def fields: Parser[UnicodeDataFields] = codePoint ~! rep(sepOptString) ^^ {
    case cp ~ fs => UnicodeDataFields(cp, fs(1))
  }
  def apply(src: Reader): Either[String, List[Line]] =
    parseAll(ucd, src) match {
      case Success(result, _) => Right(result)
      case NoSuccess(error, _) => Left(error)
    }
}
