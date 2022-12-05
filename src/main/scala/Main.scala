import scala.io.Source

@main def ucdx: Unit = {
  val propValAliases = Source.fromURL("https://www.unicode.org/Public/13.0.0/ucd/PropertyValueAliases.txt")
  PropertyValueAliases(propValAliases.reader)
  propValAliases.close()

  val uniData = Source.fromURL("https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt")
  UnicodeData(uniData.reader)
  uniData.close()
}

import java.io.Reader
import scala.util.parsing.combinator.RegexParsers

case class CodePoint(p: Int)

trait UCDParsers[A] extends RegexParsers {
  override val whiteSpace = """[ \t\x0B\f\r]""".r
  def ucd: Parser[List[Any]] = rep(line)
  def line: Parser[Any] = (fields ~! opt(comment) | comment) <~! opt("""\n+""".r)
  def comment: Parser[Any] = missingLine | normalComment
  def missingLine: Parser[Any] = "# @missing:" ~>! (codePointRange ~! sepString ~! rep(sepString)) ^^ {
    case cp ~ f ~ rest => (cp, f, rest)
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
  def codePointRange: Parser[(CodePoint, CodePoint)] = codePoint ~! opt(".." ~>! codePoint) ^^ {
    case s~None => (s, s)
    case s~Some(e) => (s, e)
  }
  def codePoint: Parser[CodePoint] = """\p{XDigit}+""".r ^^ {
    case s => CodePoint(Integer.parseInt(s, 16))
  }
}

type PropertyValueAliasesFields = (String, String, String, List[String])

object PropertyValueAliases extends UCDParsers[PropertyValueAliasesFields] {
  override def fields: Parser[PropertyValueAliasesFields] = string ~! sepString ~! sepString ~! rep(sepString) ^^ {
    case f1 ~ f2 ~ f3 ~ rest => (f1, f2, f3, rest)
  }
  def apply(src: Reader): Unit = {
    println(parseAll(ucd, src))
  }
}

type UnicodeDataFields = (CodePoint, List[Option[String]])

object UnicodeData extends UCDParsers[UnicodeDataFields] {
  override def fields: Parser[UnicodeDataFields] = codePoint ~! rep(sepOptString) ^^ {
    case cp ~ fs => (cp, fs)
  }
  def apply(src: Reader): Unit = {
    println(parseAll(ucd, src))
  }
}
