import java.io.Reader
import scala.util.parsing.combinator.RegexParsers

case class CodePoint(p: Int)
case class CodePointRange(start: CodePoint, end: CodePoint)
case class MissingLine(cpRange: CodePointRange, field: String, rest: List[String])

trait UCDParsers[A] extends RegexParsers {
  override val whiteSpace = """[ \t\x0B\f\r]""".r

  type Line = (Option[A], Option[MissingLine])

  def ucd: Parser[List[Line]] = rep(line)
  def line: Parser[Line] = (fields ~! opt(comment) | comment) <~! opt("""\n+""".r) ^^ {
    case fs ~ c: (A ~ Option[MissingLine]) => (Some(fs), c)
    case c: Option[MissingLine] => (None, c)
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
  def fields: Parser[A]
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
)

object PropertyValueAliases extends UCDParsers[PropertyValueAliasesFields] {
  override def fields: Parser[PropertyValueAliasesFields] = string ~! sepString ~! sepString ~! rep(sepString) ^^ {
    case f1 ~ f2 ~ f3 ~ rest =>
      if f1 == "ccc" then PropertyValueAliasesFields(f1, rest.head, f2::f3::rest.tail)
      else PropertyValueAliasesFields(f1, f3, f2::rest)
  }
  def apply(src: Reader): Unit = {
    println(parseAll(ucd, src))
  }
}

case class UnicodeDataFields(
  codePoint: CodePoint,
  generalCategory: Option[String]
)

object UnicodeData extends UCDParsers[UnicodeDataFields] {
  override def fields: Parser[UnicodeDataFields] = codePoint ~! rep(sepOptString) ^^ {
    case cp ~ fs => UnicodeDataFields(cp, fs(1))
  }
  def apply(src: Reader): Unit = {
    println(parseAll(ucd, src))
  }
}
